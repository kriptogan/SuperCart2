package com.example.supercart2.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.example.supercart2.models.Category
import com.example.supercart2.models.SubCategory
import com.example.supercart2.models.Grocery

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "supercart_data")

object DataStoreManager {
    private val CATEGORIES_KEY = stringPreferencesKey("categories_flat")
    private val SUBCATEGORIES_KEY = stringPreferencesKey("subcategories_flat")
    private val GROCERIES_KEY = stringPreferencesKey("groceries_flat")
    
    // LocalDate adapter for Gson
    private class LocalDateAdapter : JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        
        override fun serialize(src: LocalDate?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
            return JsonPrimitive(src?.format(formatter))
        }
        
        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): LocalDate? {
            return try {
                json?.asString?.let { LocalDate.parse(it, formatter) }
            } catch (e: Exception) {
                android.util.Log.e("DataStoreManager", "Error deserializing LocalDate: ${json?.asString}", e)
                null
            }
        }
    }
    
    // Configured Gson instance with LocalDate support
    private val gson: Gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter())
            .create()
    }
    
    // Global context reference for saving data from anywhere
    private var globalContext: Context? = null
    
    fun setGlobalContext(context: Context) {
        globalContext = context
    }
    
    suspend fun saveDataGlobally() {
        globalContext?.let { context ->
            saveData(context)
        } ?: run {
            android.util.Log.w("DataStoreManager", "Global context not set, cannot save data")
        }
    }
    
    suspend fun saveData(context: Context) {
        try {
            // Convert nested structure to flat lists
            val (categories, subCategories, groceries) = DataConverter.flattenCategories(DataManagerObject.categories)
            
            // Save each list separately
            context.dataStore.edit { preferences ->
                preferences[CATEGORIES_KEY] = gson.toJson(categories)
                preferences[SUBCATEGORIES_KEY] = gson.toJson(subCategories)
                preferences[GROCERIES_KEY] = gson.toJson(groceries)
            }
            
            android.util.Log.d("DataStoreManager", "Data saved successfully. Categories: ${categories.size}, SubCategories: ${subCategories.size}, Groceries: ${groceries.size}")
        } catch (e: Exception) {
            android.util.Log.e("DataStoreManager", "Error saving data", e)
            throw e
        }
    }
    
    suspend fun loadData(context: Context) {
        try {
            android.util.Log.d("DataStoreManager", "Loading data from storage...")
            val preferences = context.dataStore.data.first()
            
            // Load each list
            val categories = preferences[CATEGORIES_KEY]?.let {
                gson.fromJson<List<Category>>(it, object : TypeToken<List<Category>>() {}.type)
            } ?: emptyList()
            
            val subCategories = preferences[SUBCATEGORIES_KEY]?.let {
                gson.fromJson<List<SubCategory>>(it, object : TypeToken<List<SubCategory>>() {}.type)
            } ?: emptyList()
            
            val groceries = preferences[GROCERIES_KEY]?.let {
                gson.fromJson<List<Grocery>>(it, object : TypeToken<List<Grocery>>() {}.type)
            } ?: emptyList()
            
            // Validate relationships
            if (DataConverter.validateRelationships(categories, subCategories, groceries)) {
                // Convert to nested structure and update DataManagerObject
                val nestedData = DataConverter.buildNestedStructure(categories, subCategories, groceries)
                DataManagerObject.categories.clear()
                DataManagerObject.categories.addAll(nestedData)
                
                android.util.Log.d("DataStoreManager", "Data loaded successfully. Categories: ${categories.size}, SubCategories: ${subCategories.size}, Groceries: ${groceries.size}")
            } else {
                android.util.Log.e("DataStoreManager", "Invalid data relationships detected")
                DataInitializer.initializeDefaultData(context)
            }
            
            // Initialize default data if no categories exist
            if (DataManagerObject.categories.isEmpty()) {
                android.util.Log.d("DataStoreManager", "No data found, initializing defaults...")
                DataInitializer.initializeDefaultData(context)
            }
        } catch (e: Exception) {
            android.util.Log.e("DataStoreManager", "Error loading data", e)
            DataInitializer.initializeDefaultData(context)
        }
    }
}