package com.example.supercart2.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.supercart2.R
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing

@Composable
fun BurgerMenu(
    onCategoriesManagementClick: () -> Unit,
    onManageStoresClick: () -> Unit = {},
    onManageGroupClick: () -> Unit = {},
    onImportGroceriesClick: () -> Unit,
    onSettingsClick: () -> Unit = {}
) {
    BurgerMenuContent(listOf(
        R.string.menu_import_groceries to onImportGroceriesClick,
        R.string.menu_categories_management to onCategoriesManagementClick,
        R.string.menu_manage_stores to onManageStoresClick,
        R.string.menu_settings to onSettingsClick, R.string.menu_family_group to onManageGroupClick,
    ))
}

/** Round menu button, its dropdown and every dialog the menu can open. */
@Composable
private fun BurgerMenuContent(navigation: List<Pair<Int, () -> Unit>>) {
    val scope = rememberCoroutineScope()
    val state = remember { BurgerMenuState(scope) }
    BurgerMenuGroupCodeCheck(state)
    BurgerMenuButton(onClick = { state.expanded = true })
    BurgerMenuDropdown(state, navigation)
    BurgerMenuDialogs(state)
}

/** The group code is re-checked every time the menu opens. */
@Composable
private fun BurgerMenuGroupCodeCheck(state: BurgerMenuState) {
    val context = LocalContext.current
    LaunchedEffect(state.expanded) { if (state.expanded) state.hasGroupCode = DataStoreManager.hasGroupCode(context) }
}

@Composable
private fun BurgerMenuButton(onClick: () -> Unit) {
    Card(
        modifier = Modifier.size(72.dp).padding(SuperCartSpacing.sm), shape = CircleShape,
        colors = CardDefaults.cardColors(containerColor = SuperCartColors.darkGray),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) { BurgerMenuIconButton(onClick) }
}

@Composable
private fun BurgerMenuIconButton(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
        Icon(
            imageVector = Icons.Default.Menu, contentDescription = stringResource(R.string.menu),
            tint = SuperCartColors.white, modifier = Modifier.size(36.dp),
        )
    }
}

/** Navigation items (each closes the menu after its callback), then Sync data, then Export / Import. */
@Composable
private fun BurgerMenuDropdown(state: BurgerMenuState, navigation: List<Pair<Int, () -> Unit>>) {
    val close = { state.expanded = false }
    DropdownMenu(expanded = state.expanded, onDismissRequest = close, containerColor = SuperCartColors.white) {
        navigation.forEach { (label, onClick) -> BurgerMenuItem(label) { onClick(); state.expanded = false } }
        BurgerMenuSyncItem(state)
        BurgerMenuItem(R.string.menu_export_import) { state.expanded = false; state.showExportImportDialog = true }
    }
}

@Composable
private fun BurgerMenuItem(label: Int, onClick: () -> Unit) {
    DropdownMenuItem(text = { BurgerMenuItemLabel(label) }, onClick = onClick)
}

@Composable
private fun BurgerMenuItemLabel(label: Int) {
    Text(stringResource(label), fontWeight = FontWeight.Bold, color = SuperCartColors.black)
}

@Composable
private fun BurgerMenuSyncItem(state: BurgerMenuState) {
    val context = LocalContext.current
    DropdownMenuItem(
        text = { BurgerMenuSyncLabel(state.hasGroupCode) },
        onClick = { state.onSyncClick(context) },
        enabled = state.hasGroupCode && !state.isSyncing,
    )
}

@Composable
private fun BurgerMenuSyncLabel(hasGroupCode: Boolean) {
    Column {
        BurgerMenuItemLabel(R.string.menu_sync_data)
        if (!hasGroupCode) Text(stringResource(R.string.requires_family_group), fontSize = 10.sp, color = Color.Red)
    }
}
