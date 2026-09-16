package com.example.supercart2.ui.components

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.example.supercart2.R
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.data.SubCategoryWithGroceries
import com.example.supercart2.models.Category
import com.example.supercart2.models.SubCategory
import com.example.supercart2.utils.localizedCategoryDisplayName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun CategorySelectionDialog(
    onDismiss: () -> Unit,
    onCategorySelected: (Category) -> Unit,
    onNewCategoryCreated: (Category, SubCategory) -> Unit,
    selectedCategoryId: String? = null
) {
    val scope = rememberCoroutineScope()
    var showCreateCategory by remember { mutableStateOf(false) }
    CategoryListAlert(selectedCategoryId, onDismiss, onCategorySelected, onNewCategory = { showCreateCategory = true })
    if (showCreateCategory) {
        CreateCategoryForSelection(scope, onClose = { showCreateCategory = false }, onNewCategoryCreated)
    }
}

@Composable
private fun CategoryListAlert(
    selectedCategoryId: String?, onDismiss: () -> Unit,
    onCategorySelected: (Category) -> Unit, onNewCategory: () -> Unit,
) {
    val sortedCategories = DataManagerObject.getSortedCategories()
    val title = stringResource(R.string.select_category)
    CategorySelectionListAlert(title, stringResource(R.string.new_category), onDismiss, onNewCategory) {
        items(sortedCategories) { node -> CategoryRow(node.category, selectedCategoryId, onCategorySelected) }
    }
}

@Composable
private fun CategoryRow(category: Category, selectedCategoryId: String?, onCategorySelected: (Category) -> Unit) {
    CategorySelectionListRow(localizedCategoryDisplayName(category.name), selectedCategoryId == category.uuid) {
        onCategorySelected(category)
    }
}

/** The created category is stored (and saved) before [onNewCategoryCreated] is called, then the dialog closes. */
@Composable
private fun CreateCategoryForSelection(
    scope: CoroutineScope, onClose: () -> Unit, onNewCategoryCreated: (Category, SubCategory) -> Unit,
) {
    CreateCategoryDialog(onDismiss = onClose) { category, general ->
        addCategoryAndSave(category, general, scope)
        onNewCategoryCreated(category, general)
        onClose()
    }
}

/** Adds the category with its single sub-category (bumping the version) and saves in the background. */
private fun addCategoryAndSave(category: Category, general: SubCategory, scope: CoroutineScope) {
    val generalNode = SubCategoryWithGroceries(subCategory = general, groceries = mutableListOf())
    DataManagerObject.addCategory(category, listOf(generalNode))
    scope.launch { DataStoreManager.saveDataGlobally() }
}
