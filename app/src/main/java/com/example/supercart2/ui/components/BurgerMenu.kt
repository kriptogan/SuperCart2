package com.example.supercart2.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.supercart2.R
import com.example.supercart2.ui.theme.SuperCartSpacing
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.data.BackupManager
import com.example.supercart2.data.FirebaseManager
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import kotlinx.coroutines.launch

@Composable
fun BurgerMenu(
    onCategoriesManagementClick: () -> Unit,
    onManageStoresClick: () -> Unit = {},
    onManageGroupClick: () -> Unit = {},
    onImportGroceriesClick: () -> Unit,
    onSettingsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var hasGroupCode by remember { mutableStateOf(false) }
    
    // Check if group code exists when menu opens
    LaunchedEffect(expanded) {
        if (expanded) {
            hasGroupCode = DataStoreManager.hasGroupCode(context)
        }
    }
    
    // Sync state: null = idle, "uploading" = phase 1, "downloading" = phase 2
    var syncPhase by remember { mutableStateOf<String?>(null) }
    var showSyncSuccess by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val isSyncing = syncPhase != null

    // Export / Import state
    var showExportImportDialog by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var showExportSuccess by remember { mutableStateOf(false) }
    var showExportError by remember { mutableStateOf(false) }
    var exportErrorMessage by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .size(72.dp) // Smaller circle size
            .padding(SuperCartSpacing.sm),
        shape = CircleShape,
        colors = CardDefaults.cardColors(
            containerColor = SuperCartColors.darkGray // Dark grey background
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = stringResource(R.string.menu),
                tint = SuperCartColors.white, // White icon
                modifier = Modifier.size(36.dp) // Exact size requested
            )
        }
    }
    
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        containerColor = SuperCartColors.white
    ) {
        // 1. Import Groceries
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_import_groceries), fontWeight = FontWeight.Bold, color = SuperCartColors.black) },
            onClick = {
                onImportGroceriesClick()
                expanded = false
            }
        )
        // 2. Manage Categories
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_categories_management), fontWeight = FontWeight.Bold, color = SuperCartColors.black) },
            onClick = {
                onCategoriesManagementClick()
                expanded = false
            }
        )
        // 3. Manage Stores
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_manage_stores), fontWeight = FontWeight.Bold, color = SuperCartColors.black) },
            onClick = {
                onManageStoresClick()
                expanded = false
            }
        )
        // 4. Settings
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_settings), fontWeight = FontWeight.Bold, color = SuperCartColors.black) },
            onClick = {
                onSettingsClick()
                expanded = false
            }
        )
        // 5. Group Sharing
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_family_group), fontWeight = FontWeight.Bold, color = SuperCartColors.black) },
            onClick = {
                onManageGroupClick()
                expanded = false
            }
        )
        // 6. Sync data (upload then download)
        DropdownMenuItem(
            text = {
                Column {
                    Text(stringResource(R.string.menu_sync_data), fontWeight = FontWeight.Bold, color = SuperCartColors.black)
                    if (!hasGroupCode) {
                        Text(
                            stringResource(R.string.requires_family_group),
                            fontSize = 10.sp,
                            color = Color.Red
                        )
                    }
                }
            },
            onClick = {
                if (hasGroupCode) {
                    expanded = false
                    scope.launch {
                        syncPhase = "uploading"
                        try {
                            FirebaseManager.uploadData()
                            syncPhase = "downloading"
                            FirebaseManager.downloadData()
                            syncPhase = null
                            showSyncSuccess = true
                        } catch (e: Exception) {
                            syncPhase = null
                            errorMessage = context.getString(R.string.sync_failed, e.message ?: "Unknown error")
                            showError = true
                            android.util.Log.e("BurgerMenu", "Sync failed", e)
                        }
                    }
                } else {
                    Toast.makeText(
                        context,
                        context.getString(R.string.please_create_or_join_group),
                        Toast.LENGTH_LONG
                    ).show()
                }
            },
            enabled = hasGroupCode && !isSyncing
        )
        // 7. Export / Import Data
        DropdownMenuItem(
            text = {
                Text(
                    stringResource(R.string.menu_export_import),
                    fontWeight = FontWeight.Bold,
                    color = SuperCartColors.black
                )
            },
            onClick = {
                expanded = false
                showExportImportDialog = true
            }
        )
    }
    
    // Progress Dialog for Sync (phase: uploading or downloading)
    syncPhase?.let { phase ->
        val (titleRes, messageRes) = when (phase) {
            "uploading" -> R.string.uploading_to_cloud to R.string.please_wait_upload
            else -> R.string.downloading_from_cloud to R.string.please_wait_download
        }
        AlertDialog(
            onDismissRequest = { /* Prevent dismissal during sync */ },
            title = {
                Text(
                    text = stringResource(titleRes),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = SuperCartColors.primaryGreen
                    )
                    Spacer(modifier = Modifier.height(SuperCartSpacing.md))
                    Text(
                        text = stringResource(messageRes),
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = { /* No button during sync */ }
        )
    }
    
    // Success Dialog for Sync
    if (showSyncSuccess) {
        AlertDialog(
            onDismissRequest = { showSyncSuccess = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuperCartColors.primaryGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(SuperCartSpacing.sm))
                    Text(
                        text = stringResource(R.string.sync_successful),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = SuperCartColors.primaryGreen
                    )
                }
            },
            text = {
                Text(
                    text = stringResource(R.string.sync_success_message),
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = { showSyncSuccess = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuperCartColors.primaryGreen
                    )
                ) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
    
    // Error Dialog (Sync)
    if (showError) {
        AlertDialog(
            onDismissRequest = { showError = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(SuperCartSpacing.sm))
                    Text(
                        text = stringResource(R.string.error),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )
                }
            },
            text = {
                Text(
                    text = errorMessage,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = { showError = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    )
                ) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    // ── Export / Import picker dialog ─────────────────────────────────────────
    if (showExportImportDialog) {
        AlertDialog(
            onDismissRequest = { showExportImportDialog = false },
            containerColor = SuperCartColors.white,
            title = {
                Text(
                    text = stringResource(R.string.menu_export_import),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(SuperCartSpacing.sm)
                ) {
                    // Export option
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showExportImportDialog = false
                                scope.launch {
                                    isExporting = true
                                    try {
                                        val file = BackupManager.createBackup(context)
                                        isExporting = false
                                        if (file != null) {
                                            showExportSuccess = true
                                        } else {
                                            exportErrorMessage = "Unknown error"
                                            showExportError = true
                                        }
                                    } catch (e: Exception) {
                                        isExporting = false
                                        exportErrorMessage = e.message ?: "Unknown error"
                                        showExportError = true
                                    }
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = SuperCartColors.lightGreen.copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(SuperCartSpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(SuperCartSpacing.md)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Upload,
                                contentDescription = null,
                                tint = SuperCartColors.primaryGreen,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.export_to_file),
                                    fontWeight = FontWeight.Bold,
                                    color = SuperCartColors.black
                                )
                                Text(
                                    text = stringResource(R.string.export_to_file_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SuperCartColors.gray
                                )
                            }
                        }
                    }

                    // Import option
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showExportImportDialog = false
                                showImportDialog = true
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = SuperCartColors.lightGreen.copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(SuperCartSpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(SuperCartSpacing.md)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = SuperCartColors.primaryGreen,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.import_from_file),
                                    fontWeight = FontWeight.Bold,
                                    color = SuperCartColors.black
                                )
                                Text(
                                    text = stringResource(R.string.import_from_file_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SuperCartColors.gray
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showExportImportDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuperCartColors.primaryGreen,
                        contentColor = SuperCartColors.white
                    )
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // ── Export progress dialog ────────────────────────────────────────────────
    if (isExporting) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(
                    text = stringResource(R.string.exporting_data),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = SuperCartColors.primaryGreen
                    )
                    Spacer(modifier = Modifier.height(SuperCartSpacing.md))
                    Text(
                        text = stringResource(R.string.please_wait_export),
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {}
        )
    }

    // ── Export success dialog ─────────────────────────────────────────────────
    if (showExportSuccess) {
        AlertDialog(
            onDismissRequest = { showExportSuccess = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuperCartColors.primaryGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(SuperCartSpacing.sm))
                    Text(
                        text = stringResource(R.string.export_successful),
                        fontWeight = FontWeight.Bold,
                        color = SuperCartColors.primaryGreen,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Text(
                    text = stringResource(R.string.export_success_message),
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = { showExportSuccess = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuperCartColors.primaryGreen
                    )
                ) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    // ── Export error dialog ───────────────────────────────────────────────────
    if (showExportError) {
        AlertDialog(
            onDismissRequest = { showExportError = false },
            title = {
                Text(
                    text = stringResource(R.string.error),
                    fontWeight = FontWeight.Bold,
                    color = Color.Red,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = context.getString(R.string.export_failed, exportErrorMessage),
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = { showExportError = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    // ── Import dialog (reuses existing BackupRestoreDialog) ───────────────────
    if (showImportDialog) {
        BackupRestoreDialog(
            onDismiss = { showImportDialog = false },
            onRestoreComplete = { _, _ -> showImportDialog = false }
        )
    }
}