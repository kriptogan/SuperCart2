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

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "supercart_data")

object DataStoreManager {
    private const val CATEGORIES_KEY = "categories"
    
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
        val gson = Gson()
        val json = gson.toJson(DataManagerObject.categories)
        
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey(CATEGORIES_KEY)] = json
        }
        android.util.Log.d("DataStoreManager", "Data saved. Categories count: ${DataManagerObject.categories.size}")
    }
    
    suspend fun loadData(context: Context) {
        android.util.Log.d("DataStoreManager", "Loading data from storage...")
        val gson = Gson()
        val type = object : TypeToken<List<CategoryWithSubCategories>>() {}.type
        
        // Get the first value from the DataStore
        val preferences = context.dataStore.data.first()
        val json = preferences[stringPreferencesKey(CATEGORIES_KEY)]
        
        if (json != null) {
            android.util.Log.d("DataStoreManager", "Found stored data, loading...")
            val loadedCategories = gson.fromJson<List<CategoryWithSubCategories>>(json, type)
            DataManagerObject.categories.clear()
            DataManagerObject.categories.addAll(loadedCategories)
            android.util.Log.d("DataStoreManager", "Loaded ${loadedCategories.size} categories")
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
