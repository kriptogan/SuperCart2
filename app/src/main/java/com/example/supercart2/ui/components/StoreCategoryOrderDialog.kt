package com.example.supercart2.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.example.supercart2.R
import com.example.supercart2.data.CategoryWithSubCategories
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.models.Store
import com.example.supercart2.ui.components.common.CancelConfirmButtons
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun StoreCategoryOrderDialog(
    store: Store,
    onDismiss: () -> Unit,
    onOrderUpdated: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val version = DataManagerObject.version
    val allCategories = remember(version) { DataManagerObject.getSortedCategories() }
    val state = storeCategoryOrderDialogRememberState(store.uuid, version, allCategories)
    val save = { storeCategoryOrderDialogSave(store.uuid, state, scope); onOrderUpdated(); onDismiss() }
    StoreCategoryOrderDialogAlert(store.name, allCategories, state, onDismiss, save)
}

/** Local, unsaved order; reset whenever the store's current order changes. */
@Composable
private fun storeCategoryOrderDialogRememberState(
    storeId: String, version: Int, allCategories: List<CategoryWithSubCategories>,
): StoreCategoryOrderDialogState {
    val current = remember(version, allCategories) { storeCategoryOrderDialogCurrentOrder(storeId, allCategories) }
    val state = remember { StoreCategoryOrderDialogState(storeId, current) }
    LaunchedEffect(current) { state.reset(storeId, current) }
    return state
}

@Composable
private fun StoreCategoryOrderDialogAlert(
    storeName: String, categories: List<CategoryWithSubCategories>, state: StoreCategoryOrderDialogState,
    onDismiss: () -> Unit, onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss, modifier = Modifier.fillMaxSize(), containerColor = SuperCartColors.lightGreen,
        title = { StoreCategoryOrderDialogTitle(storeName) },
        text = { StoreCategoryOrderDialogBody(categories, state) },
        confirmButton = { StoreCategoryOrderDialogButtons(onDismiss, saveEnabled = categories.isNotEmpty(), onSave) },
    )
}

@Composable
private fun StoreCategoryOrderDialogTitle(storeName: String) {
    Text(
        text = stringResource(R.string.reorder_categories_for_store, storeName),
        style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Cancel (left) and save (right, disabled without categories). */
@Composable
private fun StoreCategoryOrderDialogButtons(onDismiss: () -> Unit, saveEnabled: Boolean, onSave: () -> Unit) {
    CancelConfirmButtons(onDismiss, stringResource(R.string.save_order), saveEnabled, onSave)
}

@Composable
private fun StoreCategoryOrderDialogBody(
    categories: List<CategoryWithSubCategories>, state: StoreCategoryOrderDialogState,
) {
    if (categories.isEmpty()) StoreCategoryOrderDialogEmpty() else StoreCategoryOrderDialogList(categories, state)
}

@Composable
private fun StoreCategoryOrderDialogEmpty() {
    Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
        val text = stringResource(R.string.no_categories_yet)
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = SuperCartColors.gray)
    }
}

@Composable
private fun StoreCategoryOrderDialogList(
    categories: List<CategoryWithSubCategories>, state: StoreCategoryOrderDialogState,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(items = state.categoryOrder, key = { _, id -> id }) { index, categoryId ->
            val group = categories.find { it.category.uuid == categoryId }
            if (group != null) StoreCategoryOrderDialogGroup(group, index, state)
        }
    }
}

/** A category card, its sub-category cards and the gap below them. */
@Composable
private fun StoreCategoryOrderDialogGroup(
    group: CategoryWithSubCategories, index: Int, state: StoreCategoryOrderDialogState,
) {
    StoreCategoryOrderDialogCategoryCard(group.category, state.categoryMoves(index))
    StoreCategoryOrderDialogSubCards(group, state)
    Spacer(modifier = Modifier.height(SuperCartSpacing.sm))
}

@Composable
private fun StoreCategoryOrderDialogSubCards(group: CategoryWithSubCategories, state: StoreCategoryOrderDialogState) {
    val subOrder = state.subOrders[group.category.uuid] ?: group.subCategories.map { it.subCategory.uuid }
    subOrder.forEachIndexed { index, subCategoryId ->
        val sub = group.subCategories.find { it.subCategory.uuid == subCategoryId }
        val moves = state.subCategoryMoves(group.category.uuid, subOrder, index)
        if (sub != null) StoreCategoryOrderDialogSubCategoryCard(sub.subCategory.name, moves)
    }
}

private fun storeCategoryOrderDialogSave(storeId: String, state: StoreCategoryOrderDialogState, scope: CoroutineScope) {
    DataManagerObject.setStoreCategoryOrder(storeId, state.categoryOrder)
    DataManagerObject.setStoreSubCategoryOrders(storeId, state.subOrders)
    scope.launch { DataStoreManager.saveDataGlobally() }
}
