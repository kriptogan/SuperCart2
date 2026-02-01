package com.example.supercart2.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.supercart2.R
import androidx.core.content.FileProvider
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted
import java.io.File

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ImageSourceDialog(
    onDismiss: () -> Unit,
    onImageSelected: (Uri) -> Unit
) {
    val context = LocalContext.current
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    
    // Permission state for camera
    val cameraPermission = rememberPermissionState(
        android.Manifest.permission.CAMERA
    )
    
    // Gallery picker launcher (works for all Android versions)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            onImageSelected(it)
            onDismiss()
        }
    }
    
    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempCameraUri?.let {
                onImageSelected(it)
                onDismiss()
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_image_source)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(SuperCartSpacing.sm)
            ) {
                // Gallery Option
                Button(
                    onClick = {
                        galleryLauncher.launch("image/*")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuperCartColors.primaryGreen,
                        contentColor = SuperCartColors.white
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(SuperCartSpacing.sm))
                    Text(stringResource(R.string.choose_from_gallery))
                }
                
                // Camera Option
                Button(
                    onClick = {
                        if (cameraPermission.status.isGranted) {
                            // Permission granted, launch camera
                            val photoFile = createTempImageFile(context)
                            tempCameraUri = FileProvider.getUriForFile(
                                context,
                                "com.example.supercart2.fileprovider",
                                photoFile
                            )
                            cameraLauncher.launch(tempCameraUri!!)
                        } else {
                            // Request permission
                            cameraPermission.launchPermissionRequest()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuperCartColors.primaryGreen,
                        contentColor = SuperCartColors.white
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(SuperCartSpacing.sm))
                    Text(stringResource(R.string.take_photo))
                }
                
                // Show permission explanation if denied
                if (!cameraPermission.status.isGranted) {
                    Text(
                        text = "Camera permission required to take photos",
                        style = MaterialTheme.typography.bodySmall,
                        color = SuperCartColors.gray
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * Create temporary file for camera capture
 */
private fun createTempImageFile(context: android.content.Context): File {
    val cameraDir = File(context.cacheDir, "camera")
    if (!cameraDir.exists()) {
        cameraDir.mkdirs()
    }
    return File(cameraDir, "temp_${System.currentTimeMillis()}.jpg")
}
