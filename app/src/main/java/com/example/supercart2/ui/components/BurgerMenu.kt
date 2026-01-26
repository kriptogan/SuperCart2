package com.example.supercart2.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.supercart2.ui.theme.SuperCartSpacing
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.data.FirebaseManager
import com.example.supercart2.data.DataManagerObject
import kotlinx.coroutines.launch

@Composable
fun BurgerMenu(
    onCategoriesManagementClick: () -> Unit,
    onImportGroceriesClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Upload/Download state
    var isUploading by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var showUploadSuccess by remember { mutableStateOf(false) }
    var showDownloadSuccess by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    Card(
        modifier = Modifier
            .size(72.dp) // Smaller circle size
            .padding(SuperCartSpacing.sm),
        shape = CircleShape,
        colors = CardDefaults.cardColors(
            containerColor = SuperCartColors.white
        ),
        border = CardDefaults.outlinedCardBorder(),
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
                contentDescription = "Menu",
                modifier = Modifier.size(36.dp) // Exact size requested
            )
        }
    }
    
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        DropdownMenuItem(
            text = { Text("Categories Management") },
            onClick = {
                onCategoriesManagementClick()
                expanded = false
            }
        )
        DropdownMenuItem(
            text = { Text("Import Groceries") },
            onClick = {
                onImportGroceriesClick()
                expanded = false
            }
        )
        // Firebase Upload Option
        DropdownMenuItem(
            text = { Text("Upload to Cloud") },
            onClick = {
                expanded = false
                scope.launch {
                    isUploading = true
                    try {
                        FirebaseManager.uploadData()
                        isUploading = false
                        showUploadSuccess = true
                    } catch (e: Exception) {
                        isUploading = false
                        errorMessage = "Upload failed: ${e.message ?: "Unknown error"}"
                        showError = true
                        android.util.Log.e("BurgerMenu", "Upload failed", e)
                    }
                }
            },
            enabled = !isUploading && !isDownloading
        )
        // Firebase Download Option
        DropdownMenuItem(
            text = { Text("Download from Cloud") },
            onClick = {
                expanded = false
                scope.launch {
                    isDownloading = true
                    try {
                        FirebaseManager.downloadData()
                        // UI update is already triggered inside downloadData()
                        isDownloading = false
                        showDownloadSuccess = true
                    } catch (e: Exception) {
                        isDownloading = false
                        errorMessage = "Download failed: ${e.message ?: "Unknown error"}"
                        showError = true
                        android.util.Log.e("BurgerMenu", "Download failed", e)
                    }
                }
            },
            enabled = !isUploading && !isDownloading
        )
    }
    
    // Progress Dialog for Upload
    if (isUploading) {
        AlertDialog(
            onDismissRequest = { /* Prevent dismissal during upload */ },
            title = {
                Text(
                    text = "Uploading to Cloud",
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
                        text = "Please wait while your data is being uploaded...",
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = { /* No button during upload */ }
        )
    }
    
    // Progress Dialog for Download
    if (isDownloading) {
        AlertDialog(
            onDismissRequest = { /* Prevent dismissal during download */ },
            title = {
                Text(
                    text = "Downloading from Cloud",
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
                        text = "Please wait while your data is being downloaded...",
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = { /* No button during download */ }
        )
    }
    
    // Success Dialog for Upload
    if (showUploadSuccess) {
        AlertDialog(
            onDismissRequest = { showUploadSuccess = false },
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
                        text = "Upload Successful",
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = SuperCartColors.primaryGreen
                    )
                }
            },
            text = {
                Text(
                    text = "Your data has been successfully uploaded to the cloud.",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = { showUploadSuccess = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuperCartColors.primaryGreen
                    )
                ) {
                    Text("OK")
                }
            }
        )
    }
    
    // Success Dialog for Download
    if (showDownloadSuccess) {
        AlertDialog(
            onDismissRequest = { showDownloadSuccess = false },
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
                        text = "Download Successful",
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = SuperCartColors.primaryGreen
                    )
                }
            },
            text = {
                Text(
                    text = "Your data has been successfully downloaded from the cloud.",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = { showDownloadSuccess = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuperCartColors.primaryGreen
                    )
                ) {
                    Text("OK")
                }
            }
        )
    }
    
    // Error Dialog
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
                        text = "Error",
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
                    Text("OK")
                }
            }
        )
    }
}