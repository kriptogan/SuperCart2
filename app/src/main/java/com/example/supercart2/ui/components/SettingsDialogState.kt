package com.example.supercart2.ui.components

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.supercart2.R
import com.example.supercart2.data.BackupManager
import com.example.supercart2.data.SettingsManager
import com.example.supercart2.utils.AppLanguage
import com.example.supercart2.utils.LanguageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Palette id → display name, in menu order. Unknown ids are displayed as Green. */
internal val SettingsDialogPaletteNames = mapOf(
    "green" to R.string.palette_green,
    "orange" to R.string.palette_orange,
    "purple" to R.string.palette_purple,
    "blue" to R.string.palette_blue,
    "pink" to R.string.palette_pink,
)

/** A backup/restore result message; [success] picks its color. */
internal data class SettingsDialogStatus(val text: String, val success: Boolean)

/**
 * UI state of [SettingsDialog]. Saves run in the dialog-level [scope]; [context] is the host (activity) context
 * read outside the AlertDialog, so a language change can recreate the activity.
 */
internal class SettingsDialogState(private val scope: CoroutineScope, private val context: Context) {
    var showAlerts by mutableStateOf(true)
    var currentLanguage by mutableStateOf<AppLanguage?>(null)
    var expandedLanguage by mutableStateOf(false)
    var currentPaletteId by mutableStateOf("green")
    var expandedPalette by mutableStateOf(false)
    var backupStatus by mutableStateOf<SettingsDialogStatus?>(null)
    var restoreStatus by mutableStateOf<SettingsDialogStatus?>(null)
    var showRestoreDialog by mutableStateOf(false)

    suspend fun load() {
        showAlerts = SettingsManager.getShowAlerts()
        currentLanguage = LanguageManager.getCurrentLanguage(context)
        currentPaletteId = SettingsManager.getColorPalette()
    }

    fun onShowAlertsChange(value: Boolean) {
        showAlerts = value
        scope.launch { SettingsManager.setShowAlerts(value) }
    }

    fun selectLanguage(language: AppLanguage) {
        scope.launch {
            LanguageManager.setLanguage(context, language)
            currentLanguage = language
            expandedLanguage = false
            (context as? ComponentActivity)?.recreate() // Recreate activity to apply new language
        }
    }

    fun selectPalette(id: String) {
        scope.launch {
            SettingsManager.setColorPalette(id)
            currentPaletteId = id
            expandedPalette = false
        }
    }

    /** Creates a backup; a null result or an exception is reported as a failure. */
    fun createBackup() {
        scope.launch {
            val backupFile = try { BackupManager.createBackup(context) } catch (e: Exception) { null }
            backupStatus = backupFile?.let { SettingsDialogStatus(backupCreatedText(it.name), success = true) }
                ?: SettingsDialogStatus(context.getString(R.string.backup_failed), success = false)
        }
    }

    private fun backupCreatedText(fileName: String): String = context.getString(R.string.backup_created, fileName)

    fun onRestoreComplete(success: Boolean, message: String) {
        restoreStatus = SettingsDialogStatus(message, success)
        showRestoreDialog = false
    }
}
