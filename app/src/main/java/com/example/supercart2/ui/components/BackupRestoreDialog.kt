package com.example.supercart2.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.supercart2.data.BackupManager
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing
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
    var backupFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Load backup files on dialog open
    LaunchedEffect(Unit) {
        backupFiles = BackupManager.listBackupFiles()
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SuperCartColors.lightGreen,
        title = {
            Text(
                text = "Select Backup File",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(SuperCartSpacing.lg),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    errorMessage?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = SuperCartSpacing.sm)
                        )
                    }
                    
                    if (backupFiles.isEmpty()) {
                        Text(
                            text = "No backup files found in Downloads folder.",
                            color = SuperCartColors.gray,
                            modifier = Modifier.padding(SuperCartSpacing.md)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp),
                            verticalArrangement = Arrangement.spacedBy(SuperCartSpacing.xs)
                        ) {
                            items(backupFiles) { file ->
                                BackupFileItem(
                                    file = file,
                                    onClick = {
                                        isLoading = true
                                        errorMessage = null
                                        scope.launch {
                                            val success = BackupManager.restoreFromBackup(context, file)
                                            isLoading = false
                                            if (success) {
                                                onRestoreComplete(true, "Backup restored successfully!")
                                                onDismiss()
                                            } else {
                                                errorMessage = "Failed to restore backup. Please check the file format."
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SuperCartColors.primaryGreen,
                    contentColor = SuperCartColors.white
                )
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun BackupFileItem(
    file: File,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(file.lastModified()))
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = SuperCartColors.white
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SuperCartSpacing.md)
        ) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(SuperCartSpacing.xs))
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodySmall,
                color = SuperCartColors.gray
            )
            Text(
                text = formatFileSize(file.length()),
                style = MaterialTheme.typography.bodySmall,
                color = SuperCartColors.gray
            )
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1 -> String.format("%.2f MB", mb)
        kb >= 1 -> String.format("%.2f KB", kb)
        else -> "$bytes bytes"
    }
}
