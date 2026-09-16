package com.example.supercart2.ui.components

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.example.supercart2.R
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.models.Category
import com.example.supercart2.models.SubCategory
import com.example.supercart2.utils.localizedSubCategoryDisplayName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Sub-category picker of [GroceryCreationDialog]: like SubCategorySelectionDialog, but only offers the category's
 * non-deleted sub-categories. Picking or creating one selects it in [form] and closes the picker.
 */
@Composable
internal fun GroceryCreationDialogSubCategoryPicker(
    form: GroceryCreationDialogForm, category: Category, scope: CoroutineScope, onClose: () -> Unit,
) {
    var showCreate by remember { mutableStateOf(false) }
    val onSelected = { picked: SubCategory -> form.subCategory = picked; onClose() }
    ActiveSubCategoryList(category, form.subCategory?.uuid, onClose, onSelected) { showCreate = true }
    if (showCreate) CreateSubCategoryForGrocery(category, scope, { showCreate = false }, onSelected)
}

@Composable
private fun ActiveSubCategoryList(
    category: Category, selectedId: String?, onDismiss: () -> Unit,
    onSelected: (SubCategory) -> Unit, onNew: () -> Unit,
) {
    val subCategories = groceryCreationDialogActiveSubCategories(category.uuid)
    val title = stringResource(R.string.select_sub_category)
    CategorySelectionListAlert(title, stringResource(R.string.new_sub_category), onDismiss, onNew) {
        items(subCategories) { ActiveSubCategoryRow(it, selectedId, onSelected) }
    }
}

@Composable
private fun ActiveSubCategoryRow(subCategory: SubCategory, selectedId: String?, onSelected: (SubCategory) -> Unit) {
    CategorySelectionListRow(localizedSubCategoryDisplayName(subCategory.name), selectedId == subCategory.uuid) {
        onSelected(subCategory)
    }
}

/** The new sub-category is stored (and saved) before [onCreated] is called, then the create dialog closes. */
@Composable
private fun CreateSubCategoryForGrocery(
    category: Category, scope: CoroutineScope, onClose: () -> Unit, onCreated: (SubCategory) -> Unit,
) {
    CreateSubCategoryDialog(onDismiss = onClose) { name ->
        onCreated(addActiveSubCategory(category.uuid, name, scope))
        onClose()
    }
}

private fun addActiveSubCategory(categoryId: String, name: String, scope: CoroutineScope): SubCategory {
    val subCategory = SubCategory(categoryId = categoryId, name = name, protected = false)
    DataManagerObject.addSubCategory(categoryId, subCategory)
    scope.launch { DataStoreManager.saveDataGlobally() }
    return subCategory
}
