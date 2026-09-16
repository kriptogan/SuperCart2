package com.example.supercart2.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.supercart2.R
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.SubCategoryWithGroceries
import com.example.supercart2.models.Category
import com.example.supercart2.models.SubCategory
import com.example.supercart2.ui.components.common.CancelConfirmButtons
import com.example.supercart2.ui.theme.SuperCartColors
import kotlinx.coroutines.CoroutineScope
import java.time.LocalDateTime

/**
 * Edits a category's name and its sub-categories.
 * Only [onDismiss] and [onCategoryDeleted] are used: the other callbacks are ignored, sub-category changes are
 * applied to DataManagerObject directly.
 */
@Composable
fun EditCategoryDialog(
    category: Category,
    subCategories: List<SubCategoryWithGroceries>,
    onDismiss: () -> Unit,
    onCategoryUpdated: (Category) -> Unit,
    onCategoryDeleted: (Category) -> Unit,
    onSubCategoryCreated: (SubCategory) -> Unit,
    onSubCategoryUpdated: (SubCategory) -> Unit,
    onSubCategoryDeleted: (SubCategory) -> Unit
) {
    val state = remember { EditCategoryDialogState(category.name) }
    val scope = rememberCoroutineScope()
    val current = remember { derivedStateOf { editCategoryDialogSubCategoriesOf(category.uuid, subCategories) } }
    EditCategoryDialogSubCategoryDialogs(category.uuid, state, scope)
    if (state.showDeleteConfirmation) EditCategoryDialogConfirmDelete(category, state, onCategoryDeleted)
    EditCategoryDialogAlert(category, current, state, scope, onDismiss)
}

/** UI state of [EditCategoryDialog]. */
internal class EditCategoryDialogState(name: String) {
    var categoryName by mutableStateOf(name)
    var showDeleteConfirmation by mutableStateOf(false)
    var showCreateSubCategory by mutableStateOf(false)
    var editingSubCategory by mutableStateOf<SubCategory?>(null)
}

/** Fresh non-deleted sub-categories (without deleted groceries); [fallback] when the category is not in the data. */
private fun editCategoryDialogSubCategoriesOf(
    categoryUuid: String, fallback: List<SubCategoryWithGroceries>,
): List<SubCategoryWithGroceries> {
    DataManagerObject.version // read so the derived state is recomputed on every data change
    val found = DataManagerObject.categories.find { it.category.uuid == categoryUuid }?.subCategories
    return (found ?: fallback).editCategoryDialogWithoutDeleted()
}

private fun List<SubCategoryWithGroceries>.editCategoryDialogWithoutDeleted() = filterNot { it.subCategory.deleted }
    .map { sub -> sub.copy(groceries = sub.groceries.filterNot { it.deleted }.toMutableList()) }

@Composable
private fun EditCategoryDialogConfirmDelete(
    category: Category, state: EditCategoryDialogState, onCategoryDeleted: (Category) -> Unit,
) {
    val close = { state.showDeleteConfirmation = false }
    val texts = editCategoryDialogDeleteTexts(category.name)
    EditCategoryDialogDeleteConfirmation(texts, category.protected, close) { onCategoryDeleted(category); close() }
}

@Composable
private fun editCategoryDialogDeleteTexts(name: String) = EditCategoryDialogDeleteTexts(
    title = stringResource(R.string.delete_category), titleColor = SuperCartColors.black,
    protectedWarning = stringResource(R.string.category_protected_cannot_delete),
    message = stringResource(R.string.delete_category_confirmation, name),
    protectedDescription = stringResource(R.string.category_protected),
)

@Composable
private fun EditCategoryDialogAlert(
    category: Category, subCategories: State<List<SubCategoryWithGroceries>>, state: EditCategoryDialogState,
    scope: CoroutineScope, onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = SuperCartColors.lightGreen,
        title = { EditCategoryDialogTitle(category.protected) { state.showDeleteConfirmation = true } },
        text = { EditCategoryDialogBody(category.uuid, subCategories, state, scope) },
        confirmButton = { EditCategoryDialogButtons(category.uuid, state, scope, onDismiss) },
    )
}

@Composable
private fun EditCategoryDialogTitle(isProtected: Boolean, onDeleteClick: () -> Unit) {
    val protectedDescription = stringResource(R.string.category_protected)
    val deleteDescription = stringResource(R.string.delete_category)
    EditCategoryDialogTitleRow(stringResource(R.string.edit_category)) {
        EditCategoryDialogDeleteIcon(isProtected, protectedDescription, deleteDescription, onDeleteClick)
    }
}

@Composable
private fun EditCategoryDialogBody(
    categoryUuid: String, subCategories: State<List<SubCategoryWithGroceries>>, state: EditCategoryDialogState,
    scope: CoroutineScope,
) {
    val nameLabel = stringResource(R.string.category_name)
    Column(modifier = Modifier.fillMaxWidth()) {
        EditCategoryDialogNameField(state.categoryName, { state.categoryName = it }, nameLabel)
        EditCategoryDialogSubCategoriesHeader { state.showCreateSubCategory = true }
        EditCategoryDialogSubCategoryList(categoryUuid, subCategories, state, scope)
    }
}

@Composable
private fun EditCategoryDialogButtons(
    categoryUuid: String, state: EditCategoryDialogState, scope: CoroutineScope, onDismiss: () -> Unit,
) {
    CancelConfirmButtons(onDismiss, stringResource(R.string.save_changes), state.categoryName.isNotBlank()) {
        editCategoryDialogRename(categoryUuid, state.categoryName, scope, onDismiss)
    }
}

/** Renames the category, saves to DataStore and dismisses; ignored when the name is blank. */
private fun editCategoryDialogRename(categoryUuid: String, name: String, scope: CoroutineScope, onDismiss: () -> Unit) {
    if (name.isBlank()) return
    DataManagerObject.updateCategory(categoryUuid) { it.copy(name = name.trim(), lastUpdate = LocalDateTime.now()) }
    editCategoryDialogSave(scope, "Saved category rename to DataStore")
    onDismiss()
}
