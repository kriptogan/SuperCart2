package com.example.supercart2.ui.screens

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.example.supercart2.R
import com.example.supercart2.ui.components.GroceryCreationDialog
import com.example.supercart2.ui.components.StoreCategoryOrderDialog
import com.example.supercart2.ui.components.common.DialogIconButton
import com.example.supercart2.ui.components.common.primaryButtonColors
import com.example.supercart2.ui.components.common.secondaryButtonColors
import com.example.supercart2.ui.theme.SuperCartSpacing

/** Finish-shopping confirmation, store category order (needs an existing store) and grocery create/edit dialogs. */
@Composable
internal fun StoresScreenDetailDialogs(
    model: StoresScreenDetailModel, state: StoresScreenDetailState, onExit: () -> Unit,
) {
    if (state.showFinishConfirmation) StoresScreenFinishShoppingDialog(model.storeId, state, onExit)
    if (state.showReorderCategories && model.store != null) {
        val close = { state.showReorderCategories = false }
        StoreCategoryOrderDialog(store = model.store, onDismiss = close, onOrderUpdated = close)
    }
    if (state.showGroceryCreation) StoresScreenGroceryDialog(model, state)
}

@Composable
private fun StoresScreenFinishShoppingDialog(storeId: String, state: StoresScreenDetailState, onExit: () -> Unit) {
    val dismiss = { state.showFinishConfirmation = false }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { StoresScreenFinishShoppingTitle() }, text = { StoresScreenFinishShoppingText() },
        confirmButton = { StoresScreenFinishShoppingButtons(dismiss) { state.finishShopping(storeId, onExit) } },
    )
}

@Composable
private fun StoresScreenFinishShoppingTitle() {
    Text(
        text = stringResource(R.string.finish_shopping), style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun StoresScreenFinishShoppingText() {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        val style = MaterialTheme.typography.bodyMedium
        Text(stringResource(R.string.finish_shopping_confirmation_intro), style = style, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(SuperCartSpacing.sm))
        StoresScreenFinishShoppingBullets()
    }
}

/** Both bullet lines, emitted as separate children of the dialog text column. */
@Composable
private fun StoresScreenFinishShoppingBullets() {
    Text(text = stringResource(R.string.finish_shopping_bullet_history), style = MaterialTheme.typography.bodyMedium)
    Text(text = stringResource(R.string.finish_shopping_bullet_remove), style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun StoresScreenFinishShoppingButtons(onCancel: () -> Unit, onConfirm: () -> Unit) {
    val cancel = stringResource(R.string.cancel)
    val confirm = stringResource(R.string.confirm)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SuperCartSpacing.sm)) {
        DialogIconButton(Icons.Default.Close, cancel, secondaryButtonColors(), Modifier.weight(1f)) { onCancel() }
        DialogIconButton(Icons.Default.CheckCircle, confirm, primaryButtonColors(), Modifier.weight(1f)) { onConfirm() }
    }
}

/**
 * Create (linked to the store; added to the shopping list in buying mode) or edit a grocery.
 * GroceryCreationDialog saves by itself and currently never calls onGroceryCreated.
 */
@Composable
private fun StoresScreenGroceryDialog(model: StoresScreenDetailModel, state: StoresScreenDetailState) {
    GroceryCreationDialog(
        groceryToEdit = if (state.isGroceryEditMode) state.groceryToEdit else null, initialGroceryName = "",
        initialStoreId = if (!state.isGroceryEditMode) model.storeId else null,
        addToShoppingList = model.mode == StoreMode.Buying && !state.isGroceryEditMode,
        onDismiss = { state.closeGroceryDialog() }, onGroceryCreated = { state.onGroceryCreated(it) },
    )
}
