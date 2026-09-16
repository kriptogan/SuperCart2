package com.example.supercart2.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.supercart2.R
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.models.Category
import com.example.supercart2.models.Grocery
import com.example.supercart2.models.SubCategory
import com.example.supercart2.ui.components.common.superCartTextFieldColors
import com.example.supercart2.ui.theme.SuperCartColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Pastes grocery names (one per line): unknown names are created under Other / General, then offers to add
 * the new and the already existing groceries to the shopping list.
 */
@Composable
fun ImportGroceriesDialog(
    onDismiss: () -> Unit,
    onImportComplete: () -> Unit
) {
    val state = remember { ImportGroceriesDialogState() }
    val scope = rememberCoroutineScope()
    if (state.askAddToShoppingList) AddToShoppingListAlert(state, scope, onImportComplete)
    ImportAlert(state, onDismiss, onImport = { state.startImport(scope, onImportComplete) })
}

@Stable
internal class ImportGroceriesDialogState {
    var text by mutableStateOf("")
    var askAddToShoppingList by mutableStateOf(false)
    val newlyCreated = mutableStateListOf<Grocery>()
    val existingNames = mutableStateListOf<String>()
}

@Composable
private fun ImportAlert(state: ImportGroceriesDialogState, onDismiss: () -> Unit, onImport: () -> Unit) {
    ImportGroceriesAlert(
        R.string.import_groceries, onDismissRequest = onDismiss,
        confirm = { ImportTextButton(R.string.import_action, enabled = state.text.isNotBlank(), onClick = onImport) },
        dismiss = { ImportTextButton(R.string.cancel, onClick = onDismiss) },
    ) { NamesInput(state) }
}

@Composable
private fun AddToShoppingListAlert(state: ImportGroceriesDialogState, scope: CoroutineScope, onComplete: () -> Unit) {
    val close = { state.closeQuestion(onComplete) }
    ImportGroceriesAlert(
        R.string.add_to_shopping_list_question, onDismissRequest = close,
        confirm = { ImportTextButton(R.string.yes) { state.addAllToShoppingList(scope, onComplete) } },
        dismiss = { ImportTextButton(R.string.no, onClick = close) },
    ) { Text(stringResource(R.string.should_add_to_shopping_list)) }
}

@Composable
private fun ImportGroceriesAlert(
    @StringRes title: Int, onDismissRequest: () -> Unit,
    confirm: @Composable () -> Unit, dismiss: @Composable () -> Unit, text: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest, containerColor = SuperCartColors.lightGreen,
        title = { Text(stringResource(title), color = SuperCartColors.black) },
        text = text, confirmButton = confirm, dismissButton = dismiss,
    )
}

@Composable
private fun ImportTextButton(@StringRes label: Int, enabled: Boolean = true, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = enabled) {
        Text(stringResource(label))
    }
}

@Composable
private fun NamesInput(state: ImportGroceriesDialogState) {
    Column {
        Text(stringResource(R.string.enter_grocery_names))
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = state.text, onValueChange = { state.text = it },
            modifier = Modifier.fillMaxWidth().height(200.dp), colors = superCartTextFieldColors())
    }
}

/** Imports every non-blank line, saves, then asks about the shopping list (or completes when nothing matched). */
private fun ImportGroceriesDialogState.startImport(scope: CoroutineScope, onImportComplete: () -> Unit) {
    scope.launch {
        importLines()
        DataStoreManager.saveDataGlobally()
        if (newlyCreated.isNotEmpty() || existingNames.isNotEmpty()) askAddToShoppingList = true else onImportComplete()
    }
}

private fun ImportGroceriesDialogState.importLines() {
    newlyCreated.clear()
    existingNames.clear()
    val other = otherCategory()
    val general = generalSubCategory(other)
    text.split("\\r?\\n".toRegex()).map { it.trim() }.filter { it.isNotEmpty() }
        .forEach { importName(it, other.uuid, general.uuid) }
}

/** Names already used by any grocery (deleted ones included) are remembered; others are created. */
private fun ImportGroceriesDialogState.importName(name: String, categoryId: String, subCategoryId: String) {
    if (groceryNameExists(name)) {
        existingNames.add(name)
    } else {
        newlyCreated.add(addGrocery(name, categoryId, subCategoryId))
    }
}

private fun addGrocery(name: String, categoryId: String, subCategoryId: String): Grocery {
    val grocery = Grocery(uuid = UUID.randomUUID().toString(), name = name, categoryId = categoryId,
        subCategoryId = subCategoryId)
    DataManagerObject.addGrocery(grocery)
    return grocery
}

private fun groceryNameExists(name: String): Boolean = DataManagerObject.categories.any { category ->
    category.subCategories.any { sub -> sub.groceries.any { it.name == name } }
}

/** The first category named "Other" (deleted or not), created when missing. */
private fun otherCategory(): Category =
    DataManagerObject.categories.find { it.category.name == "Other" }?.category
        ?: Category(name = "Other").also { DataManagerObject.addCategory(it) }

/** The first "General" sub-category of [other], created when missing. */
private fun generalSubCategory(other: Category): SubCategory =
    DataManagerObject.categories.find { it.category.uuid == other.uuid }
        ?.subCategories?.find { it.subCategory.name == "General" }?.subCategory
        ?: SubCategory(categoryId = other.uuid, name = "General")
            .also { DataManagerObject.addSubCategory(other.uuid, it) }

private fun ImportGroceriesDialogState.addAllToShoppingList(scope: CoroutineScope, onImportComplete: () -> Unit) {
    scope.launch {
        newlyCreated.forEach { DataManagerObject.toggleShoppingListStatus(it.uuid) }
        existingNames.forEach { addToShoppingListByName(it) }
        DataStoreManager.saveDataGlobally()
        closeQuestion(onImportComplete)
    }
}

/** Adds the first grocery with [name] of every sub-category, unless it is already listed. */
private fun addToShoppingListByName(name: String) {
    DataManagerObject.categories.forEach { category ->
        category.subCategories.forEach { sub ->
            sub.groceries.find { it.name == name }?.takeIf { !it.inShoppingList }
                ?.let { DataManagerObject.toggleShoppingListStatus(it.uuid) }
        }
    }
}

private fun ImportGroceriesDialogState.closeQuestion(onImportComplete: () -> Unit) {
    askAddToShoppingList = false
    onImportComplete()
}
