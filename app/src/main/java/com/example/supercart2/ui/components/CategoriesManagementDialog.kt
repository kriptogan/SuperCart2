package com.example.supercart2.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.supercart2.R
import com.example.supercart2.data.CategoryWithSubCategories
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.data.SubCategoryWithGroceries
import com.example.supercart2.models.Category
import com.example.supercart2.models.SubCategory
import com.example.supercart2.ui.components.common.DialogIconButton
import com.example.supercart2.ui.components.common.DialogTitle
import com.example.supercart2.ui.components.common.primaryButtonColors
import com.example.supercart2.ui.components.common.secondaryButtonColors
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun CategoriesManagementDialog(
    onDismiss: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    val scope = rememberCoroutineScope()
    if (showCreateDialog) CategoriesManagementDialogCreate(scope) { showCreateDialog = false }
    editingCategory?.let { CategoriesManagementDialogEdit(it, scope) { editingCategory = null } }
    CategoriesManagementDialogAlert(scope, onDismiss, { editingCategory = it }) { showCreateDialog = true }
}

@Composable
private fun CategoriesManagementDialogCreate(scope: CoroutineScope, onClose: () -> Unit) {
    CreateCategoryDialog(onDismiss = onClose) { newCategory, generalSubCategory ->
        val general = SubCategoryWithGroceries(subCategory = generalSubCategory, groceries = mutableListOf())
        DataManagerObject.addCategory(category = newCategory, subCategories = listOf(general))
        scope.launch { DataStoreManager.saveDataGlobally() }
    }
}

@Composable
private fun CategoriesManagementDialogEdit(category: Category, scope: CoroutineScope, onClose: () -> Unit) {
    val subCategories = remember(DataManagerObject.version) { categoriesManagementDialogSubCategoriesOf(category.uuid) }
    val handlers = CategoriesManagementDialogEditHandlers(category.uuid, scope, onClose)
    EditCategoryDialog(
        category, subCategories, onClose, handlers::categoryUpdated, handlers::categoryDeleted,
        handlers::subCategoryCreated, handlers::subCategoryUpdated, handlers::subCategoryDeleted,
    )
}

private fun categoriesManagementDialogSubCategoriesOf(categoryUuid: String): List<SubCategoryWithGroceries> =
    DataManagerObject.categories.find { it.category.uuid == categoryUuid }?.subCategories ?: emptyList()

@Composable
private fun CategoriesManagementDialogAlert(
    scope: CoroutineScope, onDismiss: () -> Unit, onEdit: (Category) -> Unit, onCreate: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss, modifier = Modifier.fillMaxSize(), containerColor = SuperCartColors.lightGreen,
        title = { DialogTitle(stringResource(R.string.categories_management)) },
        text = { CategoriesManagementDialogList(scope, onEdit) },
        confirmButton = { CategoriesManagementDialogButtons(onDismiss, onCreate) },
    )
}

@Composable
private fun CategoriesManagementDialogList(scope: CoroutineScope, onEdit: (Category) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            val sortedCategories = DataManagerObject.getSortedCategories()
            items(sortedCategories) { CategoriesManagementDialogItem(sortedCategories, it, scope, onEdit) }
        }
    }
}

@Composable
private fun CategoriesManagementDialogItem(
    sorted: List<CategoryWithSubCategories>, item: CategoryWithSubCategories, scope: CoroutineScope,
    onEdit: (Category) -> Unit,
) {
    CategoriesManagementDialogCard(item, categoriesManagementDialogMoves(sorted, item, scope)) { onEdit(item.category) }
}

/** Moves within the sorted (displayed) list: the first can't go up, the last can't go down. */
private fun categoriesManagementDialogMoves(
    sorted: List<CategoryWithSubCategories>, item: CategoryWithSubCategories, scope: CoroutineScope,
): CategoriesManagementDialogMoves {
    val index = sorted.indexOf(item)
    return CategoriesManagementDialogMoves(
        canMoveUp = index > 0, canMoveDown = index < sorted.size - 1,
        onMoveUp = { categoriesManagementDialogSwap(item, sorted.getOrNull(index - 1), scope) },
        onMoveDown = { categoriesManagementDialogSwap(item, sorted.getOrNull(index + 1), scope) },
    )
}

/** Swaps the viewOrder of [item] with its neighbour (when both still exist) and saves. */
private fun categoriesManagementDialogSwap(
    item: CategoryWithSubCategories, neighbour: CategoryWithSubCategories?, scope: CoroutineScope,
) {
    val other = neighbour?.category ?: return
    if (!categoriesManagementDialogExists(item.category.uuid) || !categoriesManagementDialogExists(other.uuid)) return
    DataManagerObject.swapCategoryOrder(item.category.uuid, other.uuid)
    scope.launch { DataStoreManager.saveDataGlobally() }
}

private fun categoriesManagementDialogExists(categoryUuid: String) =
    DataManagerObject.categories.any { it.category.uuid == categoryUuid }

@Composable
private fun CategoriesManagementDialogButtons(onDismiss: () -> Unit, onCreate: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SuperCartSpacing.sm)) {
        DialogIconButton(Icons.Default.Close, stringResource(R.string.cancel), secondaryButtonColors(),
            Modifier.weight(1f), onClick = onDismiss)
        DialogIconButton(Icons.Default.Add, stringResource(R.string.create_new_category), primaryButtonColors(),
            Modifier.weight(1f), onClick = onCreate)
    }
}
