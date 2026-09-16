package com.example.supercart2.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.supercart2.R
import com.example.supercart2.ui.components.common.DialogIconButton
import com.example.supercart2.ui.components.common.primaryButtonColors
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val state = remember { SettingsDialogState(scope, context) }
    LaunchedEffect(Unit) { state.load() }
    SettingsDialogAlert(state, onDismiss)
    if (state.showRestoreDialog) SettingsDialogRestorePicker(state)
}

@Composable
private fun SettingsDialogAlert(state: SettingsDialogState, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = SuperCartColors.lightGreen,
        title = { SettingsDialogTitle() }, text = { SettingsDialogSections(state) },
        confirmButton = { SettingsDialogCloseRow(onDismiss) },
    )
}

@Composable
private fun SettingsDialogTitle() {
    Text(
        text = stringResource(R.string.menu_settings), style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = SuperCartColors.black,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Show alerts, language, color palette, backup & restore — in that order. */
@Composable
private fun SettingsDialogSections(state: SettingsDialogState) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsDialogAlertsRow(checked = state.showAlerts, onCheckedChange = state::onShowAlertsChange)
        SettingsDialogLanguageSection(state)
        SettingsDialogPaletteSection(state)
        SettingsDialogBackupSection(state)
    }
}

@Composable
private fun SettingsDialogCloseRow(onDismiss: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SuperCartSpacing.sm)) {
        val close = stringResource(R.string.close)
        DialogIconButton(Icons.Default.Check, close, primaryButtonColors(), Modifier.weight(1f), onClick = onDismiss)
    }
}

/** Section label; shared by every section title. */
@Composable
internal fun SettingsDialogLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium,
        color = SuperCartColors.black, modifier = modifier,
    )
}

/** Full-width section with vertical padding and a label above its [content]. */
@Composable
internal fun SettingsDialogSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = SuperCartSpacing.sm)) {
        SettingsDialogLabel(title, Modifier.padding(bottom = SuperCartSpacing.xs))
        content()
    }
}

@Composable
private fun SettingsDialogAlertsRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val modifier = Modifier.fillMaxWidth().padding(vertical = SuperCartSpacing.sm)
    Row(modifier, Arrangement.SpaceBetween, Alignment.CenterVertically) {
        SettingsDialogAlertsTexts(Modifier.weight(1f))
        SettingsDialogAlertsSwitch(checked, onCheckedChange)
    }
}

@Composable
private fun SettingsDialogAlertsTexts(modifier: Modifier) {
    Column(modifier = modifier) {
        SettingsDialogLabel(stringResource(R.string.settings_show_alerts))
        SettingsDialogAlertsDescription()
    }
}

@Composable
private fun SettingsDialogAlertsDescription() {
    Text(
        text = stringResource(R.string.settings_show_alerts_description),
        style = MaterialTheme.typography.bodySmall, color = SuperCartColors.gray,
        modifier = Modifier.padding(top = SuperCartSpacing.xs),
    )
}

@Composable
private fun SettingsDialogAlertsSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val green = SuperCartColors.primaryGreen
    Switch(
        checked = checked, onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(checkedThumbColor = green, checkedTrackColor = green.copy(alpha = 0.5f)),
    )
}

@Composable
private fun SettingsDialogBackupSection(state: SettingsDialogState) {
    SettingsDialogSection(stringResource(R.string.backup_and_restore)) {
        state.backupStatus?.let { SettingsDialogStatusMessage(it) }
        state.restoreStatus?.let { SettingsDialogStatusMessage(it) }
        SettingsDialogBackupButtons(onBackup = state::createBackup, onRestore = { state.showRestoreDialog = true })
    }
}

/** Green for a successful operation, error color otherwise. */
@Composable
private fun SettingsDialogStatusMessage(status: SettingsDialogStatus) {
    val color = if (status.success) SuperCartColors.primaryGreen else MaterialTheme.colorScheme.error
    val modifier = Modifier.padding(bottom = SuperCartSpacing.xs)
    Text(text = status.text, style = MaterialTheme.typography.bodySmall, color = color, modifier = modifier)
}

@Composable
private fun SettingsDialogBackupButtons(onBackup: () -> Unit, onRestore: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SuperCartSpacing.sm)) {
        SettingsDialogHalfButton(Icons.Default.CloudUpload, R.string.create_backup, R.string.backup, onBackup)
        SettingsDialogHalfButton(Icons.Default.CloudDownload, R.string.restore_backup, R.string.restore, onRestore)
    }
}

/** Icon + label button taking an equal share of the row. */
@Composable
private fun RowScope.SettingsDialogHalfButton(
    icon: ImageVector, @StringRes description: Int, @StringRes label: Int, onClick: () -> Unit,
) {
    Button(onClick = onClick, colors = primaryButtonColors(), modifier = Modifier.weight(1f)) {
        Icon(imageVector = icon, contentDescription = stringResource(description), modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(SuperCartSpacing.xs))
        Text(stringResource(label))
    }
}

@Composable
private fun SettingsDialogRestorePicker(state: SettingsDialogState) {
    BackupRestoreDialog(onDismiss = { state.showRestoreDialog = false }, onRestoreComplete = state::onRestoreComplete)
}
