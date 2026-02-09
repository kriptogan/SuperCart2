package com.example.supercart2.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.supercart2.R
import com.example.supercart2.data.BackupManager
import com.example.supercart2.data.SettingsManager
import com.example.supercart2.ui.theme.AppPalettes
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing
import com.example.supercart2.utils.AppLanguage
import com.example.supercart2.utils.LanguageManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    var showAlerts by remember { mutableStateOf(true) }
    var currentLanguage by remember { mutableStateOf<AppLanguage?>(null) }
    var expandedLanguage by remember { mutableStateOf(false) }
    var currentPaletteId by remember { mutableStateOf("green") }
    var expandedPalette by remember { mutableStateOf(false) }
    var showBackupMessage by remember { mutableStateOf<String?>(null) }
    var showRestoreMessage by remember { mutableStateOf<String?>(null) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Load current settings
    LaunchedEffect(Unit) {
        showAlerts = SettingsManager.getShowAlerts()
        currentLanguage = LanguageManager.getCurrentLanguage(context)
        currentPaletteId = SettingsManager.getColorPalette()
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SuperCartColors.lightGreen,
        title = {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = SuperCartColors.black,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Show Alerts Setting
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = SuperCartSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Show Alerts",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = SuperCartColors.black
                        )
                        Text(
                            text = "Display alert bell icon and highlight items with expiring dates or purchase reminders",
                            style = MaterialTheme.typography.bodySmall,
                            color = SuperCartColors.gray,
                            modifier = Modifier.padding(top = SuperCartSpacing.xs)
                        )
                    }
                    Switch(
                        checked = showAlerts,
                        onCheckedChange = { 
                            showAlerts = it
                            scope.launch {
                                SettingsManager.setShowAlerts(it)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SuperCartColors.primaryGreen,
                            checkedTrackColor = SuperCartColors.primaryGreen.copy(alpha = 0.5f)
                        )
                    )
                }
                
                // Language Selection
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = SuperCartSpacing.sm)
                ) {
                    Text(
                        text = stringResource(R.string.menu_language),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = SuperCartColors.black,
                        modifier = Modifier.padding(bottom = SuperCartSpacing.xs)
                    )
                    ExposedDropdownMenuBox(
                        expanded = expandedLanguage,
                        onExpandedChange = { expandedLanguage = !expandedLanguage }
                    ) {
                        OutlinedTextField(
                            value = currentLanguage?.nativeDisplayName ?: "",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLanguage)
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                .fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SuperCartColors.primaryGreen,
                                unfocusedBorderColor = SuperCartColors.gray,
                                focusedLabelColor = SuperCartColors.black,
                                unfocusedLabelColor = SuperCartColors.black,
                                focusedTextColor = SuperCartColors.black,
                                unfocusedTextColor = SuperCartColors.black,
                                focusedContainerColor = SuperCartColors.white,
                                unfocusedContainerColor = SuperCartColors.white
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expandedLanguage,
                            onDismissRequest = { expandedLanguage = false }
                        ) {
                            AppLanguage.values().forEach { language ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = language.displayName,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = SuperCartColors.black
                                            )
                                            Text(
                                                text = language.nativeDisplayName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = SuperCartColors.gray
                                            )
                                        }
                                    },
                                    onClick = {
                                        scope.launch {
                                            LanguageManager.setLanguage(context, language)
                                            currentLanguage = language
                                            expandedLanguage = false
                                            // Recreate activity to apply new language
                                            activity?.recreate()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Color palette selection
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = SuperCartSpacing.sm)
                ) {
                    Text(
                        text = stringResource(R.string.color_palette),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = SuperCartColors.black,
                        modifier = Modifier.padding(bottom = SuperCartSpacing.xs)
                    )
                    val paletteOptionNames = mapOf(
                        "green" to R.string.palette_green,
                        "orange" to R.string.palette_orange,
                        "purple" to R.string.palette_purple,
                        "blue" to R.string.palette_blue,
                        "pink" to R.string.palette_pink
                    )
                    ExposedDropdownMenuBox(
                        expanded = expandedPalette,
                        onExpandedChange = { expandedPalette = !expandedPalette }
                    ) {
                        OutlinedTextField(
                            value = stringResource(paletteOptionNames[currentPaletteId] ?: R.string.palette_green),
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPalette)
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                .fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SuperCartColors.primaryGreen,
                                unfocusedBorderColor = SuperCartColors.gray,
                                focusedLabelColor = SuperCartColors.black,
                                unfocusedLabelColor = SuperCartColors.black,
                                focusedTextColor = SuperCartColors.black,
                                unfocusedTextColor = SuperCartColors.black,
                                focusedContainerColor = SuperCartColors.white,
                                unfocusedContainerColor = SuperCartColors.white
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expandedPalette,
                            onDismissRequest = { expandedPalette = false }
                        ) {
                            paletteOptionNames.forEach { (id, nameRes) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(nameRes),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = SuperCartColors.black
                                        )
                                    },
                                    onClick = {
                                        scope.launch {
                                            SettingsManager.setColorPalette(id)
                                            currentPaletteId = id
                                            expandedPalette = false
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Backup and Restore Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = SuperCartSpacing.sm)
                ) {
                    Text(
                        text = "Backup & Restore",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = SuperCartColors.black,
                        modifier = Modifier.padding(bottom = SuperCartSpacing.xs)
                    )
                    
                    // Backup message
                    showBackupMessage?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (message.contains("success", ignoreCase = true)) 
                                SuperCartColors.primaryGreen 
                            else 
                                MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = SuperCartSpacing.xs)
                        )
                    }
                    
                    // Restore message
                    showRestoreMessage?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (message.contains("success", ignoreCase = true)) 
                                SuperCartColors.primaryGreen 
                            else 
                                MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = SuperCartSpacing.xs)
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(SuperCartSpacing.sm)
                    ) {
                        // Create Backup Button
                        Button(
                            onClick = {
                                scope.launch {
                                    val backupFile = BackupManager.createBackup(context)
                                    if (backupFile != null) {
                                        showBackupMessage = "Backup created: ${backupFile.name}"
                                    } else {
                                        showBackupMessage = "Failed to create backup"
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SuperCartColors.primaryGreen,
                                contentColor = SuperCartColors.white
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Create Backup",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(SuperCartSpacing.xs))
                            Text("Backup")
                        }
                        
                        // Restore Backup Button
                        Button(
                            onClick = {
                                showRestoreDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SuperCartColors.primaryGreen,
                                contentColor = SuperCartColors.white
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = "Restore Backup",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(SuperCartSpacing.xs))
                            Text("Restore")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SuperCartSpacing.sm)
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuperCartColors.primaryGreen,
                        contentColor = SuperCartColors.white
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Close"
                    )
                }
            }
        }
    )
    
    // Show restore dialog
    if (showRestoreDialog) {
        BackupRestoreDialog(
            onDismiss = { showRestoreDialog = false },
            onRestoreComplete = { success, message ->
                showRestoreMessage = message
                showRestoreDialog = false
            }
        )
    }
}
