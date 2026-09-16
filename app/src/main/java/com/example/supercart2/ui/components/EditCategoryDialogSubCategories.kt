package com.example.supercart2.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.supercart2.R
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.data.SubCategoryWithGroceries
import com.example.supercart2.models.SubCategory
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartShapes
import com.example.supercart2.ui.theme.SuperCartSpacing
import com.example.supercart2.utils.localizedSubCategoryDisplayName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/** Sub-category section of EditCategoryDialog: create/edit dialogs, header and list of cards. */

@Composable
internal fun EditCategoryDialogSubCategoryDialogs(
    categoryUuid: String, state: EditCategoryDialogState, scope: CoroutineScope,
) {
    if (state.showCreateSubCategory) EditCategoryDialogCreateSubCategory(categoryUuid, state, scope)
    state.editingSubCategory?.let { EditCategoryDialogEditSubCategory(it, state) }
}

@Composable
private fun EditCategoryDialogCreateSubCategory(
    categoryUuid: String, state: EditCategoryDialogState, scope: CoroutineScope,
) {
    CreateSubCategoryDialog(onDismiss = { state.showCreateSubCategory = false }) { name ->
        editCategoryDialogAddSubCategory(categoryUuid, name, scope)
        state.showCreateSubCategory = false
    }
}

/** Sub-category changes are already applied by the dialog, so both callbacks just close it. */
@Composable
private fun EditCategoryDialogEditSubCategory(subCategory: SubCategory, state: EditCategoryDialogState) {
    val close = { state.editingSubCategory = null }
    EditSubCategoryDialog(subCategory = subCategory, onDismiss = close, onSubCategoryUpdated = { close() })
}

/** "Sub-Categories" title with an add button, between the name field and the list. */
@Composable
internal fun EditCategoryDialogSubCategoriesHeader(onAddClick: () -> Unit) {
    Spacer(modifier = Modifier.height(SuperCartSpacing.md))
    EditCategoryDialogSpacedRow {
        EditCategoryDialogSubCategoriesTitle()
        IconButton(onClick = onAddClick) { EditCategoryDialogGreenIcon(Icons.Default.Add, R.string.add_sub_category) }
    }
    Spacer(modifier = Modifier.height(SuperCartSpacing.sm))
}

@Composable
private fun EditCategoryDialogSubCategoriesTitle() {
    val title = stringResource(R.string.sub_categories)
    Text(text = title, style = MaterialTheme.typography.titleMedium, color = SuperCartColors.black)
}

@Composable
internal fun EditCategoryDialogSubCategoryList(
    categoryUuid: String, subCategories: State<List<SubCategoryWithGroceries>>, state: EditCategoryDialogState,
    scope: CoroutineScope,
) {
    val onEdit: (SubCategory) -> Unit = { state.editingSubCategory = it }
    val onDelete: (SubCategory) -> Unit = { editCategoryDialogDeleteSubCategory(categoryUuid, it, scope) }
    val uuidKey: (SubCategoryWithGroceries) -> Any = { it.subCategory.uuid }
    LazyColumn(modifier = Modifier.height(200.dp)) {
        items(subCategories.value, key = uuidKey) { EditCategoryDialogSubCategoryCard(it, onEdit, onDelete) }
    }
}

@Composable
private fun EditCategoryDialogSubCategoryCard(
    item: SubCategoryWithGroceries, onEdit: (SubCategory) -> Unit, onDelete: (SubCategory) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = SuperCartSpacing.sm),
        colors = CardDefaults.cardColors(containerColor = SuperCartColors.white),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), shape = SuperCartShapes.small,
    ) { EditCategoryDialogSubCategoryRow(item, onEdit, onDelete) }
}

@Composable
private fun EditCategoryDialogSubCategoryRow(
    item: SubCategoryWithGroceries, onEdit: (SubCategory) -> Unit, onDelete: (SubCategory) -> Unit,
) {
    EditCategoryDialogSpacedRow(Modifier.fillMaxWidth().padding(SuperCartSpacing.sm)) {
        EditCategoryDialogSubCategoryInfo(item.subCategory, item.groceries.size, Modifier.weight(1f))
        val subCategory = item.subCategory
        EditCategoryDialogSubCategoryActions(subCategory, { onEdit(subCategory) }, { onDelete(subCategory) })
    }
}

/** Name, "(Protected)" label and groceries count. */
@Composable
private fun EditCategoryDialogSubCategoryInfo(subCategory: SubCategory, groceriesCount: Int, modifier: Modifier) {
    Column(modifier = modifier) {
        EditCategoryDialogSubCategoryName(subCategory)
        val countText = stringResource(R.string.groceries_count, groceriesCount)
        Text(text = countText, style = MaterialTheme.typography.bodySmall, color = SuperCartColors.darkGray)
    }
}

@Composable
private fun EditCategoryDialogSubCategoryName(subCategory: SubCategory) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val name = localizedSubCategoryDisplayName(subCategory.name)
        Text(text = name, style = MaterialTheme.typography.bodyLarge, color = SuperCartColors.black)
        if (subCategory.protected) EditCategoryDialogProtectedLabel()
    }
}

@Composable
private fun EditCategoryDialogProtectedLabel() {
    Spacer(modifier = Modifier.width(SuperCartSpacing.xs))
    val label = stringResource(R.string.protected_label)
    Text(text = label, style = MaterialTheme.typography.bodySmall, color = SuperCartColors.gray)
}

@Composable
private fun EditCategoryDialogSubCategoryActions(subCategory: SubCategory, onEdit: () -> Unit, onDelete: () -> Unit) {
    val protectedDescription = stringResource(R.string.subcategory_protected)
    val deleteDescription = stringResource(R.string.delete_subcategory)
    Row(Modifier, Arrangement.spacedBy(SuperCartSpacing.xs), Alignment.CenterVertically) {
        IconButton(onClick = onEdit) { EditCategoryDialogGreenIcon(Icons.Default.Edit, R.string.edit_subcategory) }
        EditCategoryDialogDeleteIcon(subCategory.protected, protectedDescription, deleteDescription, onDelete)
    }
}

@Composable
private fun EditCategoryDialogGreenIcon(icon: ImageVector, descriptionRes: Int) {
    Icon(imageVector = icon, contentDescription = stringResource(descriptionRes), tint = SuperCartColors.primaryGreen)
}

private fun editCategoryDialogAddSubCategory(categoryUuid: String, name: String, scope: CoroutineScope) {
    val subCategory = SubCategory(
        categoryId = categoryUuid, name = name.trim(), protected = false,
        lastUpdate = LocalDateTime.now(), deleted = false,
    )
    DataManagerObject.addSubCategory(categoryUuid, subCategory)
    editCategoryDialogSave(scope, "Saved new sub-category to DataStore")
}

private fun editCategoryDialogDeleteSubCategory(categoryUuid: String, subCategory: SubCategory, scope: CoroutineScope) {
    if (subCategory.protected) return
    DataManagerObject.deleteSubCategory(categoryUuid, subCategory.uuid)
    editCategoryDialogSave(scope, "Saved sub-category deletion to DataStore")
}

internal fun editCategoryDialogSave(scope: CoroutineScope, logMessage: String) {
    scope.launch {
        DataStoreManager.saveDataGlobally()
        android.util.Log.d("EditCategoryDialog", logMessage)
    }
}
