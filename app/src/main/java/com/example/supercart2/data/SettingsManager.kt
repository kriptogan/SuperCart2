package com.example.supercart2.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "supercart_settings")

object SettingsManager {
    private val SHOW_ALERTS_KEY = booleanPreferencesKey("show_alerts")
    
    // Global context reference
    private var globalContext: Context? = null
    
    fun setGlobalContext(context: Context) {
        globalContext = context
    }
    
    // Flow to observe show alerts setting
    val showAlerts: Flow<Boolean> get() {
        val context = globalContext ?: throw IllegalStateException("Global context not set")
        return context.settingsDataStore.data.map { preferences ->
            preferences[SHOW_ALERTS_KEY] ?: true // Default to true (show alerts)
        }
    }
    
    // Get current value synchronously (for one-time reads)
    suspend fun getShowAlerts(): Boolean {
        val context = globalContext ?: return true // Default to true if context not set
        val preferences = context.settingsDataStore.data.first()
        return preferences[SHOW_ALERTS_KEY] ?: true
    }
    
    // Set show alerts setting
    suspend fun setShowAlerts(value: Boolean) {
        val context = globalContext ?: throw IllegalStateException("Global context not set")
        context.settingsDataStore.edit { preferences ->
            preferences[SHOW_ALERTS_KEY] = value
        }
        android.util.Log.d("SettingsManager", "Show alerts setting updated: $value")
    }
}
