package com.example.supercart2.ui.components

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.supercart2.R
import com.example.supercart2.data.BackupManager
import com.example.supercart2.ui.components.common.primaryButtonColors
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BackupRestoreDialog(
    onDismiss: () -> Unit,
    onRestoreComplete: (success: Boolean, message: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = remember { BackupRestoreDialogState(context, scope) }
    LaunchedEffect(Unit) { state.backupFiles = BackupManager.listBackupFiles() }
    BackupRestoreAlert(state, onDismiss) { file -> state.restore(file, onDismiss, onRestoreComplete) }
}

/** Backup list, loading flag and last error of an open restore dialog. */
private class BackupRestoreDialogState(private val context: Context, private val scope: CoroutineScope) {
    var backupFiles by mutableStateOf<List<File>>(emptyList())
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    fun restore(file: File, onDismiss: () -> Unit, onRestoreComplete: (Boolean, String) -> Unit) {
        isLoading = true
        errorMessage = null
        scope.launch {
            val success = BackupManager.restoreFromBackup(context, file)
            onRestoreFinished(success, onDismiss, onRestoreComplete)
        }
    }

    private fun onRestoreFinished(
        success: Boolean, onDismiss: () -> Unit, onRestoreComplete: (Boolean, String) -> Unit,
    ) {
        isLoading = false
        when {
            success -> { onRestoreComplete(true, context.getString(R.string.backup_restored)); onDismiss() }
            else -> errorMessage = context.getString(R.string.restore_failed)
        }
    }
}

@Composable
private fun BackupRestoreAlert(state: BackupRestoreDialogState, onDismiss: () -> Unit, onFileClick: (File) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = SuperCartColors.lightGreen,
        title = { BackupRestoreTitle() }, text = { BackupRestoreBody(state, onFileClick) },
        confirmButton = { BackupRestoreCancelButton(onDismiss) },
    )
}

@Composable
private fun BackupRestoreCancelButton(onClick: () -> Unit) {
    Button(onClick = onClick, colors = primaryButtonColors()) { Text(stringResource(R.string.cancel)) }
}

@Composable
private fun BackupRestoreTitle() {
    Text(
        text = stringResource(R.string.select_backup_file), style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = SuperCartColors.black,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun BackupRestoreBody(state: BackupRestoreDialogState, onFileClick: (File) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (state.isLoading) RestoreProgress() else BackupFilesSection(state, onFileClick)
    }
}

@Composable
private fun RestoreProgress() {
    Box(modifier = Modifier.fillMaxWidth().padding(SuperCartSpacing.lg), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun BackupFilesSection(state: BackupRestoreDialogState, onFileClick: (File) -> Unit) {
    state.errorMessage?.let { RestoreError(it) }
    if (state.backupFiles.isEmpty()) NoBackupFiles() else BackupFileList(state.backupFiles, onFileClick)
}

@Composable
private fun RestoreError(error: String) {
    Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = SuperCartSpacing.sm))
}

@Composable
private fun NoBackupFiles() {
    Text(
        text = stringResource(R.string.no_backup_files), color = SuperCartColors.gray,
        modifier = Modifier.padding(SuperCartSpacing.md),
    )
}

@Composable
private fun BackupFileList(files: List<File>, onFileClick: (File) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
        verticalArrangement = Arrangement.spacedBy(SuperCartSpacing.xs),
    ) {
        items(files) { file -> BackupFileItem(file = file, onClick = { onFileClick(file) }) }
    }
}

@Composable
private fun BackupFileItem(file: File, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SuperCartColors.white),
    ) { BackupFileDetails(file) }
}

@Composable
private fun BackupFileDetails(file: File) {
    Column(modifier = Modifier.fillMaxWidth().padding(SuperCartSpacing.md)) {
        Text(text = file.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(SuperCartSpacing.xs))
        Text(formatModifiedDate(file), style = MaterialTheme.typography.bodySmall, color = SuperCartColors.gray)
        Text(formatFileSize(file.length()), style = MaterialTheme.typography.bodySmall, color = SuperCartColors.gray)
    }
}

private fun formatModifiedDate(file: File): String =
    SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(file.lastModified()))

@Composable
private fun formatFileSize(bytes: Long): String = when {
    bytes / 1024.0 / 1024.0 >= 1 -> stringResource(R.string.file_size_mb, bytes / 1024.0 / 1024.0)
    bytes / 1024.0 >= 1 -> stringResource(R.string.file_size_kb, bytes / 1024.0)
    else -> stringResource(R.string.file_size_bytes, bytes)
}
