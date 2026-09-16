package com.example.supercart2.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.supercart2.R
import com.example.supercart2.data.BackupManager
import com.example.supercart2.data.FirebaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

/**
 * UI state of [BurgerMenu]. Work is launched in the menu's own [scope] (not a dialog's or the dropdown's),
 * so closing the dropdown or a dialog does not cancel a running sync/export.
 */
internal class BurgerMenuState(private val scope: CoroutineScope) {
    var expanded by mutableStateOf(false)
    var hasGroupCode by mutableStateOf(false)

    // Sync state: null = idle, "uploading" = phase 1, "downloading" = phase 2
    var syncPhase by mutableStateOf<String?>(null)
    var showSyncSuccess by mutableStateOf(false)
    var showError by mutableStateOf(false)
    var errorMessage by mutableStateOf("")
    val isSyncing: Boolean get() = syncPhase != null

    // Export / Import state
    var showExportImportDialog by mutableStateOf(false)
    var isExporting by mutableStateOf(false)
    var showExportSuccess by mutableStateOf(false)
    var showExportError by mutableStateOf(false)
    var exportErrorMessage by mutableStateOf("")
    var showImportDialog by mutableStateOf(false)

    /** Uploads then downloads; without a group code only shows a toast (unreachable: the item is disabled). */
    fun onSyncClick(context: Context) {
        if (hasGroupCode) {
            expanded = false
            scope.launch { sync(context) }
        } else {
            Toast.makeText(context, context.getString(R.string.please_create_or_join_group), Toast.LENGTH_LONG).show()
        }
    }

    private suspend fun sync(context: Context) {
        syncPhase = "uploading"
        try {
            FirebaseManager.uploadData(); syncPhase = "downloading"
            FirebaseManager.downloadData(); syncPhase = null
            showSyncSuccess = true
        } catch (e: Exception) { onSyncFailed(context, e) }
    }

    private fun onSyncFailed(context: Context, e: Exception) {
        syncPhase = null
        errorMessage = context.getString(R.string.sync_failed, e.message ?: context.getString(R.string.unknown_error))
        showError = true
        android.util.Log.e("BurgerMenu", "Sync failed", e)
    }

    fun onExportClick(context: Context) {
        showExportImportDialog = false
        scope.launch { export(context) }
    }

    fun onImportClick() {
        showExportImportDialog = false
        showImportDialog = true
    }

    private suspend fun export(context: Context) {
        isExporting = true
        try {
            onExportFinished(context, BackupManager.createBackup(context))
        } catch (e: Exception) {
            onExportFailed(e.message ?: context.getString(R.string.unknown_error))
        }
    }

    private fun onExportFinished(context: Context, file: File?) {
        isExporting = false
        if (file != null) showExportSuccess = true else onExportFailed(context.getString(R.string.unknown_error))
    }

    private fun onExportFailed(message: String) {
        isExporting = false
        exportErrorMessage = message
        showExportError = true
    }
}
