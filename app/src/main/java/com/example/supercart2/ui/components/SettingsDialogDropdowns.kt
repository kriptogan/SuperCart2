package com.example.supercart2.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.supercart2.R
import com.example.supercart2.ui.components.common.superCartTextFieldColors
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.utils.AppLanguage

@Composable
internal fun SettingsDialogLanguageSection(state: SettingsDialogState) {
    SettingsDialogSection(stringResource(R.string.menu_language)) { SettingsDialogLanguageDropdown(state) }
}

@Composable
private fun SettingsDialogLanguageDropdown(state: SettingsDialogState) {
    SettingsDialogDropdown(
        value = state.currentLanguage?.nativeDisplayName ?: "", expanded = state.expandedLanguage,
        onToggle = { state.expandedLanguage = !state.expandedLanguage }, onDismiss = { state.expandedLanguage = false },
    ) { SettingsDialogLanguageOptions(state) }
}

@Composable
private fun SettingsDialogLanguageOptions(state: SettingsDialogState) {
    AppLanguage.values().forEach { language ->
        DropdownMenuItem(
            text = { SettingsDialogLanguageText(language) }, onClick = { state.selectLanguage(language) },
        )
    }
}

@Composable
private fun SettingsDialogLanguageText(language: AppLanguage) {
    Column {
        SettingsDialogOptionText(language.displayName)
        Text(language.nativeDisplayName, style = MaterialTheme.typography.bodySmall, color = SuperCartColors.gray)
    }
}

@Composable
internal fun SettingsDialogPaletteSection(state: SettingsDialogState) {
    SettingsDialogSection(stringResource(R.string.color_palette)) { SettingsDialogPaletteDropdown(state) }
}

@Composable
private fun SettingsDialogPaletteDropdown(state: SettingsDialogState) {
    val nameRes = SettingsDialogPaletteNames[state.currentPaletteId] ?: R.string.palette_green
    SettingsDialogDropdown(
        value = stringResource(nameRes), expanded = state.expandedPalette,
        onToggle = { state.expandedPalette = !state.expandedPalette }, onDismiss = { state.expandedPalette = false },
    ) { SettingsDialogPaletteOptions(state) }
}

@Composable
private fun SettingsDialogPaletteOptions(state: SettingsDialogState) {
    SettingsDialogPaletteNames.forEach { (id, nameRes) ->
        DropdownMenuItem(
            text = { SettingsDialogOptionText(stringResource(nameRes)) }, onClick = { state.selectPalette(id) },
        )
    }
}

@Composable
private fun SettingsDialogOptionText(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyLarge, color = SuperCartColors.black)
}

/** Read-only field that toggles a menu of [options] (the toggle ignores the requested state). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDialogDropdown(
    value: String, expanded: Boolean, onToggle: () -> Unit, onDismiss: () -> Unit,
    options: @Composable ColumnScope.() -> Unit,
) {
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { onToggle() }) {
        val anchor = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth()
        SettingsDialogDropdownField(value, expanded, anchor)
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = onDismiss, content = options)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDialogDropdownField(value: String, expanded: Boolean, modifier: Modifier) {
    OutlinedTextField(
        value = value, onValueChange = {}, readOnly = true, modifier = modifier, colors = superCartTextFieldColors(),
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
    )
}
