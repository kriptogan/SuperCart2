package com.example.supercart2.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.supercart2.models.CategoryWithSubCategories
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "grocery_data")

class DataStoreManager(private val context: Context) {
    private val gson: Gson = Gson()
    private val CATEGORIES_KEY = stringPreferencesKey("categories")

    suspend fun saveData(categories: List<CategoryWithSubCategories>) {
        android.util.Log.d("datastore test", "Saving ${categories.size} categories to DataStore")
        val json = gson.toJson(categories)
        context.dataStore.edit { preferences ->
            preferences[CATEGORIES_KEY] = json
        }
        android.util.Log.d("datastore test", "Successfully saved data to DataStore")
    }

    suspend fun loadData(): List<CategoryWithSubCategories> {
        android.util.Log.d("datastore test", "Loading data from DataStore")
        val preferences = context.dataStore.data.first()
        val json = preferences[CATEGORIES_KEY]
        
        return if (json != null) {
            try {
                val type = object : TypeToken<List<CategoryWithSubCategories>>() {}.type
                val loadedCategories = gson.fromJson<List<CategoryWithSubCategories>>(json, type)
                android.util.Log.d("datastore test", "Successfully loaded ${loadedCategories.size} categories from DataStore")
                loadedCategories
            } catch (e: Exception) {
                android.util.Log.e("datastore test", "Error loading data from DataStore", e)
                emptyList()
            }
        } else {
            android.util.Log.d("datastore test", "No data found in DataStore")
            emptyList()
        }
    }
    
    suspend fun clearData() {
        android.util.Log.d("datastore test", "Clearing DataStore")
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
        android.util.Log.d("datastore test", "DataStore cleared successfully")
    }
}
