package com.example.supercart2.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "supercart_data")

object DataStoreManager {
    private const val CATEGORIES_KEY = "categories"
    
    suspend fun saveData(context: Context) {
        val gson = Gson()
        val json = gson.toJson(DataManagerObject.categories)
        
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey(CATEGORIES_KEY)] = json
        }
    }
    
    suspend fun loadData(context: Context) {
        val gson = Gson()
        val type = object : TypeToken<List<CategoryWithSubCategories>>() {}.type
        
        context.dataStore.data.collect { preferences ->
            val json = preferences[stringPreferencesKey(CATEGORIES_KEY)]
            if (json != null) {
                val loadedCategories = gson.fromJson<List<CategoryWithSubCategories>>(json, type)
                DataManagerObject.categories.clear()
                DataManagerObject.categories.addAll(loadedCategories)
            }
        }
    }
}
