package com.example.supercart2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.supercart2.data.SettingsManager
import com.example.supercart2.ui.components.BurgerMenu
import com.example.supercart2.ui.components.CategoriesManagementDialog
import com.example.supercart2.ui.components.GroceryCreationDialog
import com.example.supercart2.ui.components.GroupManagementDialog
import com.example.supercart2.ui.components.HierarchicalCategoryDisplay
import com.example.supercart2.ui.components.ImportGroceriesDialog
import com.example.supercart2.ui.components.SettingsDialog
import com.example.supercart2.ui.components.StoresManagementDialog
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing

@Composable
fun HomeScreen(onNavigateToStoreEdit: (String) -> Unit = {}) {
    val state = remember { HomeScreenState() }
    val showAlerts by SettingsManager.showAlerts.collectAsState(initial = true)
    HomeScreenEffects(state, showAlerts)
    HomeScreenContent(state, showAlerts, onNavigateToStoreEdit)
    HomeScreenDialogs(state, onNavigateToStoreEdit)
}

@Composable
private fun HomeScreenEffects(state: HomeScreenState, showAlerts: Boolean) {
    // Disable the alert filter when alerts are turned off or the last alert item disappears
    val hasAlertItems = state.hasAlertItems(showAlerts)
    LaunchedEffect(hasAlertItems) { if (!hasAlertItems && state.isAlertFilterActive) state.isAlertFilterActive = false }
    // An active search expands everything; clearing it collapses everything
    val searchQuery = state.searchQuery
    LaunchedEffect(searchQuery) { state.isAllExpanded = searchQuery.isNotBlank() }
}

@Composable
private fun HomeScreenContent(state: HomeScreenState, showAlerts: Boolean, onNavigateToStoreEdit: (String) -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(SuperCartColors.lightGreen)) {
        HomeScreenTopBar(state, showAlerts)
    }
    HomeScreenCategoryList(state, showAlerts, onNavigateToStoreEdit)
}

@Composable
private fun HomeScreenTopBar(state: HomeScreenState, showAlerts: Boolean) {
    val padding = Modifier.padding(start = SuperCartSpacing.md, end = SuperCartSpacing.md, top = SuperCartSpacing.xl)
    Column(modifier = Modifier.fillMaxWidth().background(SuperCartColors.lightGreen).then(padding)) {
        HomeScreenActionRow(state, showAlerts)
        HomeScreenSearchRow(state)
    }
}

/** Burger menu (left), alert filter toggle (center, only with alert items) and add-grocery button (right). */
@Composable
private fun HomeScreenActionRow(state: HomeScreenState, showAlerts: Boolean) {
    val rowModifier = Modifier.fillMaxWidth().padding(bottom = SuperCartSpacing.md)
    Row(rowModifier, Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Box(modifier = Modifier.padding(start = 5.dp)) { HomeScreenBurgerMenu(state) }
        HomeScreenAlertButton(state.hasAlertItems(showAlerts), state::toggleAlertFilter)
        Box(modifier = Modifier.padding(end = 12.dp)) { HomeScreenAddButton(state) }
    }
}

@Composable
private fun HomeScreenBurgerMenu(state: HomeScreenState) {
    val open = state::opener
    BurgerMenu(
        onCategoriesManagementClick = open(HomeScreenDialog.CATEGORIES),
        onManageStoresClick = open(HomeScreenDialog.STORES), onManageGroupClick = open(HomeScreenDialog.GROUP),
        onImportGroceriesClick = open(HomeScreenDialog.IMPORT), onSettingsClick = open(HomeScreenDialog.SETTINGS),
    )
}

@Composable
private fun HomeScreenAlertButton(visible: Boolean, onClick: () -> Unit) {
    if (visible) {
        HomeScreenCircleButton(Icons.Default.Notifications, "Alerts", SuperCartColors.orange, onClick)
    } else {
        Spacer(modifier = Modifier.size(56.dp)) // keeps the layout spacing
    }
}

@Composable
private fun HomeScreenAddButton(state: HomeScreenState) {
    HomeScreenCircleButton(Icons.Default.Add, "Add New Grocery", SuperCartColors.primaryGreen, state::startCreate)
}

@Composable
private fun HomeScreenCircleButton(icon: ImageVector, description: String, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.size(56.dp), shape = CircleShape, colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) { HomeScreenWhiteIcon(icon, description) }
    }
}

@Composable
private fun HomeScreenWhiteIcon(icon: ImageVector, description: String) {
    Icon(icon, description, Modifier.size(34.dp), tint = SuperCartColors.white)
}

/** The list sits below the fixed top bar: top padding = height of the top bar + extra gap. */
private val homeScreenCategoryListModifier = Modifier.fillMaxSize()
    .padding(top = 180.dp, bottom = SuperCartSpacing.md, start = SuperCartSpacing.md, end = SuperCartSpacing.md)

@Composable
private fun HomeScreenCategoryList(state: HomeScreenState, showAlerts: Boolean, onNavigateToStore: (String) -> Unit) {
    Box(modifier = homeScreenCategoryListModifier) {
        // visibleCategories may force isAllExpanded, so it must be evaluated before isAllExpanded is read
        HierarchicalCategoryDisplay(
            state.visibleCategories(showAlerts), state.searchQuery, state.isAllExpanded,
            onEditGrocery = state::startEdit, showAlerts = showAlerts, onNavigateToStore = onNavigateToStore,
        )
    }
}

@Composable
private fun HomeScreenDialogs(state: HomeScreenState, onNavigateToStoreEdit: (String) -> Unit) {
    when (state.openDialog) {
        HomeScreenDialog.CATEGORIES -> CategoriesManagementDialog(onDismiss = state::closeDialog)
        HomeScreenDialog.STORES -> HomeScreenStoresDialog(state, onNavigateToStoreEdit)
        HomeScreenDialog.GROUP -> GroupManagementDialog(onDismiss = state::closeDialog)
        else -> HomeScreenOtherDialogs(state)
    }
}

@Composable
private fun HomeScreenOtherDialogs(state: HomeScreenState) {
    when (state.openDialog) {
        HomeScreenDialog.SETTINGS -> SettingsDialog(onDismiss = state::closeDialog)
        HomeScreenDialog.IMPORT -> ImportGroceriesDialog(state::closeDialog, onImportComplete = state::closeDialog)
        HomeScreenDialog.GROCERY -> HomeScreenGroceryDialog(state)
        else -> Unit
    }
}

@Composable
private fun HomeScreenStoresDialog(state: HomeScreenState, onNavigateToStoreEdit: (String) -> Unit) {
    StoresManagementDialog(
        onDismiss = state::closeDialog,
        onEnterStoreEditMode = { storeId -> state.closeDialog(); onNavigateToStoreEdit(storeId) },
    )
}

@Composable
private fun HomeScreenGroceryDialog(state: HomeScreenState) {
    val groceryToEdit = state.groceryToEdit
    GroceryCreationDialog(
        groceryToEdit = groceryToEdit, initialGroceryName = if (groceryToEdit != null) "" else state.searchQuery,
        onDismiss = state::dismissGroceryDialog,
    )
}
