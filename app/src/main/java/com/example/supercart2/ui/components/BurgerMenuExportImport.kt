package com.example.supercart2.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.supercart2.R
import com.example.supercart2.ui.components.common.primaryButtonColors
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing

/** "Export / Import Data" picker: export to a backup file, or open the backup restore dialog. */
@Composable
internal fun BurgerMenuExportImportPicker(state: BurgerMenuState) {
    val context = LocalContext.current
    val close = { state.showExportImportDialog = false }
    BurgerMenuPickerAlert(onClose = close, onExport = { state.onExportClick(context) }, onImport = state::onImportClick)
}

@Composable
private fun BurgerMenuPickerAlert(onClose: () -> Unit, onExport: () -> Unit, onImport: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose, containerColor = SuperCartColors.white, title = { BurgerMenuPickerTitle() },
        text = { BurgerMenuPickerOptions(onExport, onImport) },
        confirmButton = { BurgerMenuPickerCancelButton(onClose) },
    )
}

@Composable
private fun BurgerMenuPickerCancelButton(onClick: () -> Unit) {
    Button(onClick = onClick, colors = primaryButtonColors()) {
        Text(stringResource(R.string.cancel))
    }
}

@Composable
private fun BurgerMenuPickerTitle() {
    Text(
        text = stringResource(R.string.menu_export_import), style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun BurgerMenuPickerOptions(onExport: () -> Unit, onImport: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(SuperCartSpacing.sm)) {
        BurgerMenuExportOption(onExport)
        BurgerMenuImportOption(onImport)
    }
}

@Composable
private fun BurgerMenuExportOption(onClick: () -> Unit) {
    BurgerMenuPickerOption(Icons.Default.Upload, R.string.export_to_file, R.string.export_to_file_description, onClick)
}

@Composable
private fun BurgerMenuImportOption(onClick: () -> Unit) {
    BurgerMenuPickerOption(
        Icons.Default.Download, R.string.import_from_file, R.string.import_from_file_description, onClick,
    )
}

@Composable
private fun BurgerMenuPickerOption(icon: ImageVector, titleRes: Int, descriptionRes: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SuperCartColors.lightGreen.copy(alpha = 0.2f)),
    ) { BurgerMenuPickerOptionRow(icon, titleRes, descriptionRes) }
}

@Composable
private fun BurgerMenuPickerOptionRow(icon: ImageVector, titleRes: Int, descriptionRes: Int) {
    val spacing = Arrangement.spacedBy(SuperCartSpacing.md)
    Row(Modifier.fillMaxWidth().padding(SuperCartSpacing.md), spacing, Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = SuperCartColors.primaryGreen, modifier = Modifier.size(28.dp))
        BurgerMenuPickerOptionTexts(titleRes, descriptionRes)
    }
}

@Composable
private fun BurgerMenuPickerOptionTexts(titleRes: Int, descriptionRes: Int) {
    Column {
        Text(text = stringResource(titleRes), fontWeight = FontWeight.Bold, color = SuperCartColors.black)
        Text(stringResource(descriptionRes), style = MaterialTheme.typography.bodySmall, color = SuperCartColors.gray)
    }
}
