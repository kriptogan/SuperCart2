package com.example.supercart2.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.supercart2.R
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing

/** All dialogs opened from the burger menu, in their original stacking order. */
@Composable
internal fun BurgerMenuDialogs(state: BurgerMenuState) {
    BurgerMenuSyncDialogs(state)
    if (state.showExportImportDialog) BurgerMenuExportImportPicker(state)
    BurgerMenuExportDialogs(state)
    if (state.showImportDialog) BurgerMenuImportDialog(onClose = { state.showImportDialog = false })
}

@Composable
private fun BurgerMenuSyncDialogs(state: BurgerMenuState) {
    state.syncPhase?.let { BurgerMenuSyncProgressDialog(it) }
    if (state.showSyncSuccess) BurgerMenuSyncSuccessDialog(onDismiss = { state.showSyncSuccess = false })
    if (state.showError) BurgerMenuSyncErrorDialog(state.errorMessage, onDismiss = { state.showError = false })
}

@Composable
private fun BurgerMenuExportDialogs(state: BurgerMenuState) {
    if (state.isExporting) BurgerMenuProgressDialog(R.string.exporting_data, R.string.please_wait_export)
    if (state.showExportSuccess) BurgerMenuExportSuccessDialog(onDismiss = { state.showExportSuccess = false })
    if (state.showExportError) {
        BurgerMenuExportErrorDialog(state.exportErrorMessage, onDismiss = { state.showExportError = false })
    }
}

/** Reuses the backup picker; a completed restore just closes it. */
@Composable
private fun BurgerMenuImportDialog(onClose: () -> Unit) {
    BackupRestoreDialog(onDismiss = onClose, onRestoreComplete = { _, _ -> onClose() })
}

@Composable
private fun BurgerMenuSyncProgressDialog(phase: String) {
    val (titleRes, messageRes) = when (phase) {
        "uploading" -> R.string.uploading_to_cloud to R.string.please_wait_upload
        else -> R.string.downloading_from_cloud to R.string.please_wait_download
    }
    BurgerMenuProgressDialog(titleRes, messageRes)
}

/** Non-dismissable dialog with a spinner and no buttons. */
@Composable
private fun BurgerMenuProgressDialog(titleRes: Int, messageRes: Int) {
    AlertDialog(
        onDismissRequest = { /* Prevent dismissal while working */ }, confirmButton = { /* No button */ },
        title = { Text(stringResource(titleRes), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = { BurgerMenuProgressBody(messageRes) },
    )
}

@Composable
private fun BurgerMenuProgressBody(messageRes: Int) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(modifier = Modifier.size(48.dp), color = SuperCartColors.primaryGreen)
        Spacer(modifier = Modifier.height(SuperCartSpacing.md))
        Text(text = stringResource(messageRes), textAlign = TextAlign.Center)
    }
}

@Composable
private fun BurgerMenuSyncSuccessDialog(onDismiss: () -> Unit) {
    BurgerMenuSuccessDialog(R.string.sync_successful, R.string.sync_success_message, onDismiss)
}

@Composable
private fun BurgerMenuExportSuccessDialog(onDismiss: () -> Unit) {
    BurgerMenuSuccessDialog(R.string.export_successful, R.string.export_success_message, onDismiss)
}

@Composable
private fun BurgerMenuSuccessDialog(titleRes: Int, messageRes: Int, onDismiss: () -> Unit) {
    val green = SuperCartColors.primaryGreen
    val title: @Composable () -> Unit = { BurgerMenuIconTitle(Icons.Default.CheckCircle, titleRes, green) }
    BurgerMenuResultDialog(title, stringResource(messageRes), green, onDismiss)
}

@Composable
private fun BurgerMenuSyncErrorDialog(message: String, onDismiss: () -> Unit) {
    val title: @Composable () -> Unit = { BurgerMenuIconTitle(Icons.Default.Build, R.string.error, Color.Red) }
    BurgerMenuResultDialog(title, message, Color.Red, onDismiss)
}

/** Unlike the sync error, the export error title has no icon. */
@Composable
private fun BurgerMenuExportErrorDialog(errorMessage: String, onDismiss: () -> Unit) {
    val title: @Composable () -> Unit = { BurgerMenuPlainErrorTitle() }
    BurgerMenuResultDialog(title, stringResource(R.string.export_failed, errorMessage), Color.Red, onDismiss)
}

/** Title, centered message and an OK button in [buttonColor]. */
@Composable
private fun BurgerMenuResultDialog(
    title: @Composable () -> Unit, message: String, buttonColor: Color, onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss, title = title,
        text = { Text(text = message, textAlign = TextAlign.Center) },
        confirmButton = { BurgerMenuOkButton(buttonColor, onDismiss) },
    )
}

@Composable
private fun BurgerMenuOkButton(color: Color, onClick: () -> Unit) {
    Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = color)) {
        Text(stringResource(R.string.ok))
    }
}

@Composable
private fun BurgerMenuIconTitle(icon: ImageVector, textRes: Int, color: Color) {
    val arrangement = Arrangement.Center
    Row(Modifier.fillMaxWidth(), horizontalArrangement = arrangement, verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(SuperCartSpacing.sm))
        Text(stringResource(textRes), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun BurgerMenuPlainErrorTitle() {
    Text(
        stringResource(R.string.error), fontWeight = FontWeight.Bold, color = Color.Red,
        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
    )
}
