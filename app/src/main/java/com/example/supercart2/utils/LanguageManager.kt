package com.example.supercart2.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.ui.unit.LayoutDirection
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.util.Locale

// DataStore extension
private val Context.languageDataStore by preferencesDataStore(name = "language_settings")

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val nativeDisplayName: String,
    val isRtl: Boolean
) {
    ENGLISH("en", "English", "English", isRtl = false),
    HEBREW("iw", "Hebrew", "עברית", isRtl = true),  // iw = Hebrew legacy code
    RUSSIAN("ru", "Russian", "Русский", isRtl = false),
    BULGARIAN("bg", "Bulgarian", "Български", isRtl = false);

    fun toLocale(): Locale = Locale(code)
    
    fun toLayoutDirection(): LayoutDirection = 
        if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    
    companion object {
        fun fromCode(code: String?): AppLanguage {
            return values().find { it.code == code } ?: ENGLISH
        }
    }
}

object LanguageManager {
    private val LANGUAGE_KEY = stringPreferencesKey("app_language")
    
    /**
     * Get currently selected language
     */
    suspend fun getCurrentLanguage(context: Context): AppLanguage {
        val preferences = context.languageDataStore.data.first()
        val code = preferences[LANGUAGE_KEY]
        return AppLanguage.fromCode(code)
    }
    
    /**
     * Save language preference
     */
    suspend fun setLanguage(context: Context, language: AppLanguage) {
        context.languageDataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language.code
        }
        android.util.Log.d("LanguageManager", "Language set to: ${language.code} (${language.nativeDisplayName})")
    }
    
    /**
     * Apply language to context (creates new context with locale)
     */
    fun applyLanguage(context: Context, language: AppLanguage): Context {
        val locale = language.toLocale()
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        android.util.Log.d("LanguageManager", "Applying language: ${language.code}, RTL: ${language.isRtl}")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return applyLegacyLocale(context, config, locale)
        return createLocalizedContext(context, config, locale)
    }

    private fun createLocalizedContext(context: Context, config: Configuration, locale: Locale): Context {
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }

    // Before API 24 the locale is applied to the given context's resources in place
    @Suppress("DEPRECATION")
    private fun applyLegacyLocale(context: Context, config: Configuration, locale: Locale): Context {
        config.locale = locale
        config.setLayoutDirection(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
        return context
    }
    
    /**
     * Get layout direction for current language
     */
    fun getLayoutDirection(language: AppLanguage): LayoutDirection {
        return language.toLayoutDirection()
    }
}
