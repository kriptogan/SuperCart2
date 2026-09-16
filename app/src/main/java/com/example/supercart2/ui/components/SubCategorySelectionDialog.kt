package com.example.supercart2.ui.components

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.models.Category
import com.example.supercart2.models.SubCategory
import com.example.supercart2.utils.localizedSubCategoryDisplayName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun SubCategorySelectionDialog(
    onDismiss: () -> Unit,
    onSubCategorySelected: (SubCategory) -> Unit,
    onNewSubCategoryCreated: (SubCategory) -> Unit,
    selectedCategory: Category,
    selectedSubCategoryId: String? = null
) {
    val scope = rememberCoroutineScope()
    var showCreate by remember { mutableStateOf(false) }
    SubCategoryPicker(selectedCategory, selectedSubCategoryId, onDismiss, onSubCategorySelected) { showCreate = true }
    if (showCreate) {
        CreateSubCategoryForSelection(selectedCategory, scope, { showCreate = false }, onNewSubCategoryCreated)
    }
}

/** Lists the category's sub-categories in data order; deleted ones are not filtered out. */
@Composable
private fun SubCategoryPicker(
    category: Category, selectedSubCategoryId: String?, onDismiss: () -> Unit,
    onSubCategorySelected: (SubCategory) -> Unit, onNewSubCategory: () -> Unit,
) {
    val subCategories = DataManagerObject.categories.find { it.category.uuid == category.uuid }
        ?.subCategories?.map { it.subCategory } ?: emptyList()
    CategorySelectionListAlert("Select Sub-Category", "New Sub-Category", onDismiss, onNewSubCategory) {
        items(subCategories) { SubCategoryRow(it, selectedSubCategoryId, onSubCategorySelected) }
    }
}

@Composable
private fun SubCategoryRow(subCategory: SubCategory, selectedId: String?, onSelected: (SubCategory) -> Unit) {
    CategorySelectionListRow(localizedSubCategoryDisplayName(subCategory.name), selectedId == subCategory.uuid) {
        onSelected(subCategory)
    }
}

/** The new sub-category is stored (and saved) before [onNewSubCategoryCreated] is called, then the dialog closes. */
@Composable
private fun CreateSubCategoryForSelection(
    category: Category, scope: CoroutineScope, onClose: () -> Unit, onNewSubCategoryCreated: (SubCategory) -> Unit,
) {
    CreateSubCategoryDialog(onDismiss = onClose) { name ->
        onNewSubCategoryCreated(addSubCategoryAndSave(category.uuid, name, scope))
        onClose()
    }
}

private fun addSubCategoryAndSave(categoryId: String, name: String, scope: CoroutineScope): SubCategory {
    val subCategory = SubCategory(categoryId = categoryId, name = name, protected = false)
    DataManagerObject.addSubCategory(categoryId, subCategory)
    scope.launch { DataStoreManager.saveDataGlobally() }
    return subCategory
}
