package com.example.supercart2.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.supercart2.R
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.models.SubCategory
import com.example.supercart2.ui.components.common.CancelConfirmButtons
import com.example.supercart2.ui.theme.SuperCartColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@Composable
fun EditSubCategoryDialog(
    subCategory: SubCategory,
    onDismiss: () -> Unit,
    onSubCategoryUpdated: (SubCategory) -> Unit,
    categoryId: String = subCategory.categoryId
) {
    var subCategoryName by remember { mutableStateOf(subCategory.name) }
    val scope = rememberCoroutineScope()
    val name = subCategoryName
    val save = { editSubCategoryDialogRename(subCategory, name, categoryId, scope, onSubCategoryUpdated, onDismiss) }
    val delete = { editSubCategoryDialogDelete(subCategory, categoryId, scope, onDismiss) }
    EditSubCategoryDialogContent(subCategory, subCategoryName, { subCategoryName = it }, onDismiss, save, delete)
}

@Composable
private fun EditSubCategoryDialogContent(
    subCategory: SubCategory, name: String, onNameChange: (String) -> Unit,
    onDismiss: () -> Unit, onSave: () -> Unit, onDelete: () -> Unit,
) {
    var showConfirmation by remember { mutableStateOf(false) }
    val close = { showConfirmation = false }
    if (showConfirmation) EditSubCategoryDialogDeleteConfirmation(subCategory, close) { onDelete(); close() }
    EditSubCategoryDialogAlert(subCategory.protected, name, onNameChange, onDismiss, onSave) { showConfirmation = true }
}

@Composable
private fun EditSubCategoryDialogAlert(
    isProtected: Boolean, name: String, onNameChange: (String) -> Unit,
    onDismiss: () -> Unit, onSave: () -> Unit, onDeleteClick: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = SuperCartColors.lightGreen,
        title = { EditSubCategoryDialogTitle(isProtected, onDeleteClick) },
        text = { EditCategoryDialogNameField(name, onNameChange, stringResource(R.string.subcategory_name)) },
        confirmButton = { EditSubCategoryDialogButtons(name, onDismiss, onSave) },
    )
}

@Composable
private fun EditSubCategoryDialogButtons(name: String, onDismiss: () -> Unit, onSave: () -> Unit) {
    CancelConfirmButtons(onDismiss, stringResource(R.string.save_changes), name.isNotBlank(), onSave)
}

@Composable
private fun EditSubCategoryDialogTitle(isProtected: Boolean, onDeleteClick: () -> Unit) {
    val protectedDescription = stringResource(R.string.subcategory_protected)
    val deleteDescription = stringResource(R.string.delete_subcategory)
    EditCategoryDialogTitleRow(stringResource(R.string.edit_subcategory)) {
        EditCategoryDialogDeleteIcon(isProtected, protectedDescription, deleteDescription, onDeleteClick)
    }
}

@Composable
private fun EditSubCategoryDialogDeleteConfirmation(
    subCategory: SubCategory, onCancel: () -> Unit, onDelete: () -> Unit,
) {
    val texts = editSubCategoryDialogDeleteTexts(subCategory.name)
    EditCategoryDialogDeleteConfirmation(texts, subCategory.protected, onCancel, onDelete)
}

/** Unlike the category confirmation, the title keeps the default text color. */
@Composable
private fun editSubCategoryDialogDeleteTexts(name: String) = EditCategoryDialogDeleteTexts(
    title = stringResource(R.string.delete_subcategory), titleColor = Color.Unspecified,
    protectedWarning = stringResource(R.string.subcategory_protected_cannot_delete),
    message = stringResource(R.string.delete_subcategory_confirmation, name),
    protectedDescription = stringResource(R.string.subcategory_protected),
)

/** Renames the sub-category (keeping its flags), saves, notifies and dismisses; ignored when the name is blank. */
private fun editSubCategoryDialogRename(
    subCategory: SubCategory, name: String, categoryId: String, scope: CoroutineScope,
    onUpdated: (SubCategory) -> Unit, onDismiss: () -> Unit,
) {
    if (name.isBlank()) return
    val updated = subCategory.copy(name = name.trim(), lastUpdate = LocalDateTime.now())
    DataManagerObject.updateSubCategory(categoryId, subCategory.uuid) { updated }
    editSubCategoryDialogSave(scope, "Saved sub-category update to DataStore")
    onUpdated(updated)
    onDismiss()
}

/** Soft-deletes a non-protected sub-category, saves and dismisses. */
private fun editSubCategoryDialogDelete(
    subCategory: SubCategory, categoryId: String, scope: CoroutineScope, onDismiss: () -> Unit,
) {
    if (subCategory.protected) return
    DataManagerObject.deleteSubCategory(categoryId, subCategory.uuid)
    editSubCategoryDialogSave(scope, "Saved sub-category deletion to DataStore")
    onDismiss()
}

private fun editSubCategoryDialogSave(scope: CoroutineScope, logMessage: String) {
    scope.launch {
        DataStoreManager.saveDataGlobally()
        android.util.Log.d("EditSubCategoryDialog", logMessage)
    }
}
