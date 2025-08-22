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

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "supercart_data")

object DataStoreManager {
    private const val CATEGORIES_KEY = "categories"
    
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
    
    // Global save function that can be called from anywhere
    suspend fun saveDataGlobally() {
        globalContext?.let { context ->
            saveData(context)
        } ?: run {
            android.util.Log.w("DataStoreManager", "Global context not set, cannot save data")
        }
    }
    
    suspend fun saveData(context: Context) {
        android.util.Log.d("DataStoreManager", "Saving data to storage...")
        val json = gson.toJson(DataManagerObject.categories)
        
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey(CATEGORIES_KEY)] = json
        }
        android.util.Log.d("DataStoreManager", "Data saved. Categories count: ${DataManagerObject.categories.size}")
    }
    
    suspend fun loadData(context: Context) {
        android.util.Log.d("DataStoreManager", "Loading data from storage...")
        val type = object : TypeToken<List<CategoryWithSubCategories>>() {}.type
        
        // Get the first value from the DataStore
        val preferences = context.dataStore.data.first()
        val json = preferences[stringPreferencesKey(CATEGORIES_KEY)]
        
        if (json != null) {
            android.util.Log.d("DataStoreManager", "Found stored data, loading...")
            try {
                val loadedCategories = gson.fromJson<List<CategoryWithSubCategories>>(json, type)
                DataManagerObject.categories.clear()
                DataManagerObject.categories.addAll(loadedCategories)
                android.util.Log.d("DataStoreManager", "Loaded ${loadedCategories.size} categories")
            } catch (e: Exception) {
                android.util.Log.e("DataStoreManager", "Error loading data from JSON", e)
                // If loading fails, initialize default data
                DataInitializer.initializeDefaultData(context)
            }
        } else {
            android.util.Log.d("DataStoreManager", "No stored data found")
        }
        
        // Initialize default data if no categories exist
        if (DataManagerObject.categories.isEmpty()) {
            android.util.Log.d("DataStoreManager", "No categories found, initializing defaults...")
            DataInitializer.initializeDefaultData(context)
        } else {
            android.util.Log.d("DataStoreManager", "Categories already loaded: ${DataManagerObject.categories.size}")
        }
    }
}
