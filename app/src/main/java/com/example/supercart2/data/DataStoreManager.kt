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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.example.supercart2.models.Category
import com.example.supercart2.models.SubCategory
import com.example.supercart2.models.Grocery
import com.example.supercart2.models.Store

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "supercart_data")

object DataStoreManager {
    private val CATEGORIES_KEY = stringPreferencesKey("categories_flat")
    private val SUBCATEGORIES_KEY = stringPreferencesKey("subcategories_flat")
    private val GROCERIES_KEY = stringPreferencesKey("groceries_flat")
    private val STORES_KEY = stringPreferencesKey("stores_flat")
    private val HIDDEN_STORES_KEY = stringPreferencesKey("hidden_stores")
    private val STORE_VIEW_MODE_KEY = stringPreferencesKey("store_view_mode")
    
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
    
    // LocalDateTime adapter for Gson
    private class LocalDateTimeAdapter : JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        
        override fun serialize(src: LocalDateTime?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
            return JsonPrimitive(src?.format(formatter))
        }
        
        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): LocalDateTime {
            return try {
                json?.asString?.let { LocalDateTime.parse(it, formatter) }
                    ?: LocalDateTime.now()
            } catch (e: Exception) {
                android.util.Log.e("DataStoreManager", "Error deserializing LocalDateTime: ${json?.asString}", e)
                LocalDateTime.now()
            }
        }
    }
    
    // Configured Gson instance with LocalDate and LocalDateTime support
    private val gson: Gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter())
            .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
            .create()
    }
    
    // Global context reference for saving data from anywhere
    internal var globalContext: Context? = null
    
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
            
            // Get stores list
            val stores = DataManagerObject.stores.toList()
            val hiddenStoreIds = DataManagerObject.hiddenStoreIds.toList()
            
            // Save each list separately
            context.dataStore.edit { preferences ->
                preferences[CATEGORIES_KEY] = gson.toJson(categories)
                preferences[SUBCATEGORIES_KEY] = gson.toJson(subCategories)
                preferences[GROCERIES_KEY] = gson.toJson(groceries)
                preferences[STORES_KEY] = gson.toJson(stores)
                preferences[HIDDEN_STORES_KEY] = gson.toJson(hiddenStoreIds)
            }
            
            android.util.Log.d("DataStoreManager", "Data saved successfully. Categories: ${categories.size}, SubCategories: ${subCategories.size}, Groceries: ${groceries.size}, Stores: ${stores.size}")
        } catch (e: Exception) {
            android.util.Log.e("DataStoreManager", "Error saving data", e)
            throw e
        }
    }
    
    suspend fun loadData(context: Context) {
        try {
            android.util.Log.d("DataStoreManager", "Loading data from storage...")
            val preferences = context.dataStore.data.first()
            
            // Load each list with explicit type parameters
            val categories = preferences[CATEGORIES_KEY]?.let {
                gson.fromJson<List<Category>>(it, object : TypeToken<List<Category>>() {}.type)
            } ?: emptyList()
            
            val subCategories = preferences[SUBCATEGORIES_KEY]?.let {
                gson.fromJson<List<SubCategory>>(it, object : TypeToken<List<SubCategory>>() {}.type)
            } ?: emptyList()
            
            val groceries = preferences[GROCERIES_KEY]?.let {
                gson.fromJson<List<Grocery>>(it, object : TypeToken<List<Grocery>>() {}.type)
            } ?: emptyList()
            
            val stores = preferences[STORES_KEY]?.let {
                gson.fromJson<List<Store>>(it, object : TypeToken<List<Store>>() {}.type)
            } ?: emptyList()
            
            val hiddenStoreIds = preferences[HIDDEN_STORES_KEY]?.let {
                gson.fromJson<List<String>>(it, object : TypeToken<List<String>>() {}.type)
            } ?: emptyList()
            
            // Validate relationships
            if (DataConverter.validateRelationships(categories, subCategories, groceries)) {
                // Convert to nested structure
                val nestedData = DataConverter.buildNestedStructure(categories, subCategories, groceries)
                DataManagerObject.categories.clear()
                DataManagerObject.categories.addAll(nestedData)
                
                // Load stores (filter out deleted ones)
                DataManagerObject.stores.clear()
                DataManagerObject.stores.addAll(stores.filter { !it.deleted })
                
                // Load hidden store IDs
                DataManagerObject.hiddenStoreIds.clear()
                DataManagerObject.hiddenStoreIds.addAll(hiddenStoreIds)
                
                android.util.Log.d("DataStoreManager", "Data loaded successfully. Categories: ${categories.size}, SubCategories: ${subCategories.size}, Groceries: ${groceries.size}, Stores: ${stores.size}")
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
    
    // Save store view mode preference
    suspend fun saveStoreViewMode(context: Context, isStoreView: Boolean) {
        try {
            context.dataStore.edit { preferences ->
                preferences[STORE_VIEW_MODE_KEY] = if (isStoreView) "STORE" else "CATEGORY"
            }
            android.util.Log.d("DataStoreManager", "Store view mode saved: ${if (isStoreView) "STORE" else "CATEGORY"}")
        } catch (e: Exception) {
            android.util.Log.e("DataStoreManager", "Error saving store view mode", e)
        }
    }
    
    // Load store view mode preference
    suspend fun loadStoreViewMode(context: Context): Boolean {
        return try {
            val preferences = context.dataStore.data.first()
            val mode = preferences[STORE_VIEW_MODE_KEY] ?: "CATEGORY"
            android.util.Log.d("DataStoreManager", "Store view mode loaded: $mode")
            mode == "STORE"
        } catch (e: Exception) {
            android.util.Log.e("DataStoreManager", "Error loading store view mode", e)
            false // Default to category view
        }
    }
}