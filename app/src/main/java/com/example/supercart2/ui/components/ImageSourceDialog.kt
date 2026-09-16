package com.example.supercart2.ui.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.supercart2.R
import com.example.supercart2.ui.components.common.primaryButtonColors
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ImageSourceDialog(
    onDismiss: () -> Unit,
    onImageSelected: (Uri) -> Unit
) {
    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)
    val onPicked: (Uri) -> Unit = { onImageSelected(it); onDismiss() }
    val openGallery = rememberGalleryPicker(onPicked)
    val onCamera = cameraOrPermissionRequest(cameraPermission, rememberCameraCapture(onPicked))
    ImageSourceAlert(onDismiss, openGallery, onCamera, showPermissionHint = !cameraPermission.status.isGranted)
}

/** Opens the camera when the permission is granted (checked at click time), otherwise requests it. */
@OptIn(ExperimentalPermissionsApi::class)
private fun cameraOrPermissionRequest(permission: PermissionState, openCamera: () -> Unit): () -> Unit = {
    if (permission.status.isGranted) openCamera() else permission.launchPermissionRequest()
}

/** Returns an action opening the system image picker; a picked image is passed to [onPicked]. */
@Composable
private fun rememberGalleryPicker(onPicked: (Uri) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let(onPicked)
    }
    return { launcher.launch("image/*") }
}

/** Returns an action taking a photo into a new cache file; on success its content uri is passed to [onPicked]. */
@Composable
private fun rememberCameraCapture(onPicked: (Uri) -> Unit): () -> Unit {
    val context = LocalContext.current
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicture()) { success ->
        if (success) tempCameraUri?.let(onPicked)
    }
    return { launcher.launch(newCameraImageUri(context).also { tempCameraUri = it }) }
}

@Composable
private fun ImageSourceAlert(
    onDismiss: () -> Unit, onGallery: () -> Unit, onCamera: () -> Unit, showPermissionHint: Boolean,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_image_source), color = SuperCartColors.black) },
        text = { ImageSourceOptions(onGallery, onCamera, showPermissionHint) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun ImageSourceOptions(onGallery: () -> Unit, onCamera: () -> Unit, showPermissionHint: Boolean) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(SuperCartSpacing.sm)) {
        ImageSourceButton(Icons.Default.PhotoLibrary, R.string.choose_from_gallery, onGallery)
        ImageSourceButton(Icons.Default.CameraAlt, R.string.take_photo, onCamera)
        if (showPermissionHint) CameraPermissionHint()
    }
}

@Composable
private fun ImageSourceButton(icon: ImageVector, @StringRes label: Int, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth(), colors = primaryButtonColors()) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(SuperCartSpacing.sm))
        Text(stringResource(label))
    }
}

@Composable
private fun CameraPermissionHint() {
    Text(
        text = stringResource(R.string.camera_permission_required),
        style = MaterialTheme.typography.bodySmall, color = SuperCartColors.gray,
    )
}

private fun newCameraImageUri(context: Context): Uri =
    FileProvider.getUriForFile(context, "com.example.supercart2.fileprovider", createTempImageFile(context))

/**
 * Create temporary file for camera capture
 */
private fun createTempImageFile(context: Context): File {
    val cameraDir = File(context.cacheDir, "camera")
    if (!cameraDir.exists()) {
        cameraDir.mkdirs()
    }
    return File(cameraDir, "temp_${System.currentTimeMillis()}.jpg")
}
