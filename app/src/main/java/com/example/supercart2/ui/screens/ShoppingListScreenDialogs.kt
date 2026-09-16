package com.example.supercart2.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.example.supercart2.R
import com.example.supercart2.ui.components.CategoriesManagementDialog
import com.example.supercart2.ui.components.GroceryCreationDialog
import com.example.supercart2.ui.components.GroupManagementDialog
import com.example.supercart2.ui.components.HideStoresDialog
import com.example.supercart2.ui.components.ImportGroceriesDialog
import com.example.supercart2.ui.components.SettingsDialog
import com.example.supercart2.ui.components.StoresManagementDialog
import com.example.supercart2.ui.components.common.DialogIconButton
import com.example.supercart2.ui.components.common.primaryButtonColors
import com.example.supercart2.ui.components.common.secondaryButtonColors
import com.example.supercart2.ui.theme.SuperCartSpacing
import kotlinx.coroutines.CoroutineScope

/** Every dialog the screen can open, in their original stacking order. */
@Composable
internal fun ShoppingListScreenDialogs(state: ShoppingListScreenState, onNavigateToStoreEdit: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    if (state.showFinishConfirmation) ShoppingListScreenFinishDialog(state, scope)
    ShoppingListScreenManagementDialogs(state, onNavigateToStoreEdit)
    if (state.showGroceryCreation) ShoppingListScreenGroceryDialog(state, scope)
}

@Composable
private fun ShoppingListScreenManagementDialogs(
    state: ShoppingListScreenState,
    onNavigateToStoreEdit: (String) -> Unit,
) {
    if (state.showCategoriesManagement) {
        CategoriesManagementDialog(onDismiss = { state.showCategoriesManagement = false })
    }
    if (state.showStoresManagement) ShoppingListScreenStoresDialog(state, onNavigateToStoreEdit)
    if (state.showHideStoresDialog) HideStoresDialog(onDismiss = { state.showHideStoresDialog = false })
    ShoppingListScreenMenuDialogs(state)
}

@Composable
private fun ShoppingListScreenMenuDialogs(state: ShoppingListScreenState) {
    if (state.showSettings) SettingsDialog(onDismiss = { state.showSettings = false })
    if (state.showGroupManagement) GroupManagementDialog(onDismiss = { state.showGroupManagement = false })
    if (state.showImportGroceries) ShoppingListScreenImportDialog(state)
}

@Composable
private fun ShoppingListScreenStoresDialog(state: ShoppingListScreenState, onNavigateToStoreEdit: (String) -> Unit) {
    val onEnterStoreEditMode = { storeId: String ->
        state.showStoresManagement = false
        onNavigateToStoreEdit(storeId)
    }
    StoresManagementDialog(onDismiss = { state.showStoresManagement = false }, onEnterStoreEditMode)
}

@Composable
private fun ShoppingListScreenImportDialog(state: ShoppingListScreenState) {
    ImportGroceriesDialog(
        onDismiss = { state.showImportGroceries = false },
        onImportComplete = { state.showImportGroceries = false },
    )
}

@Composable
private fun ShoppingListScreenGroceryDialog(state: ShoppingListScreenState, scope: CoroutineScope) {
    GroceryCreationDialog(
        groceryToEdit = state.groceryToEdit,
        initialGroceryName = if (state.isEditMode) "" else state.searchQuery,
        // new groceries created from the shopping list go straight into the list
        addToShoppingList = !state.isEditMode, onDismiss = { state.dismissGroceryDialog() },
        onGroceryCreated = { ShoppingListScreenActions.onGroceryCreated(state, it, scope) },
    )
}

@Composable
private fun ShoppingListScreenFinishDialog(state: ShoppingListScreenState, scope: CoroutineScope) {
    AlertDialog(
        onDismissRequest = { state.showFinishConfirmation = false },
        title = { ShoppingListScreenFinishTitle() }, text = { ShoppingListScreenFinishText() },
        confirmButton = { ShoppingListScreenFinishButtons(state, scope) },
    )
}

@Composable
private fun ShoppingListScreenFinishTitle() {
    Text(
        text = stringResource(R.string.finish_shopping), style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ShoppingListScreenFinishText() {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        ShoppingListScreenDialogText(R.string.finish_shopping_confirmation_intro, TextAlign.Center)
        Spacer(modifier = Modifier.height(SuperCartSpacing.sm))
        ShoppingListScreenDialogText(R.string.finish_shopping_bullet_history, TextAlign.Start)
        ShoppingListScreenDialogText(R.string.finish_shopping_bullet_remove, TextAlign.Start)
    }
}

@Composable
private fun ShoppingListScreenDialogText(@StringRes id: Int, textAlign: TextAlign) {
    Text(text = stringResource(id), style = MaterialTheme.typography.bodyMedium, textAlign = textAlign)
}

@Composable
private fun ShoppingListScreenFinishButtons(state: ShoppingListScreenState, scope: CoroutineScope) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SuperCartSpacing.sm)) {
        DialogIconButton(Icons.Default.Close, stringResource(R.string.cancel), secondaryButtonColors(),
            Modifier.weight(1f), onClick = { state.showFinishConfirmation = false })
        DialogIconButton(Icons.Default.CheckCircle, stringResource(R.string.confirm), primaryButtonColors(),
            Modifier.weight(1f), onClick = { ShoppingListScreenActions.finishShopping(state, scope) })
    }
}
