package com.example.supercart2.ui.screens

import com.example.supercart2.data.CategoryWithSubCategories
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.models.Grocery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Applies a grocery reported by GroceryCreationDialog's onGroceryCreated: adds it when [groceryToEdit] is null,
 * otherwise replaces it in place or moves it to its new sub-category. Saves afterwards.
 * Quirks kept on purpose: edits don't stamp lastUpdate, a move doesn't call updateData(), a move to an unknown
 * sub-category loses the grocery and a move from an unknown one duplicates it.
 */
internal fun homeScreenApplyGroceryResult(groceryToEdit: Grocery?, newGrocery: Grocery, scope: CoroutineScope) {
    when {
        groceryToEdit == null -> homeScreenAddGrocery(newGrocery, scope)
        groceryToEdit.subCategoryId != newGrocery.subCategoryId -> homeScreenMoveGrocery(groceryToEdit, newGrocery)
        else -> homeScreenReplaceGrocery(groceryToEdit, newGrocery)
    }
    if (groceryToEdit != null) homeScreenSave(scope)
}

private fun homeScreenAddGrocery(grocery: Grocery, scope: CoroutineScope) {
    if (homeScreenLocate(grocery.categoryId, grocery.subCategoryId) == null) return
    DataManagerObject.addGrocery(grocery)
    homeScreenSave(scope)
}

private fun homeScreenMoveGrocery(original: Grocery, moved: Grocery) {
    homeScreenEditGroceries(original.categoryId, original.subCategoryId) { groceries ->
        groceries.removeAll { it.uuid == original.uuid }
        true
    }
    homeScreenEditGroceries(moved.categoryId, moved.subCategoryId) { it.add(moved) }
}

/** Replaces [original] inside the sub-category named by [updated] (looked up under updated's category id). */
private fun homeScreenReplaceGrocery(original: Grocery, updated: Grocery) {
    val replaced = homeScreenEditGroceries(updated.categoryId, updated.subCategoryId) { groceries ->
        val index = groceries.indexOfFirst { it.uuid == original.uuid }
        if (index != -1) groceries[index] = updated
        index != -1
    }
    if (replaced) DataManagerObject.updateData()
}

private fun homeScreenSave(scope: CoroutineScope) {
    scope.launch { DataStoreManager.saveDataGlobally() }
}

/** (category index, sub-category index) in [DataManagerObject.categories], or null when either is missing. */
private fun homeScreenLocate(categoryId: String, subCategoryId: String): Pair<Int, Int>? {
    val categoryIndex = DataManagerObject.categories.indexOfFirst { it.category.uuid == categoryId }
    if (categoryIndex == -1) return null
    val subCategories = DataManagerObject.categories[categoryIndex].subCategories
    val subCategoryIndex = subCategories.indexOfFirst { it.subCategory.uuid == subCategoryId }
    return if (subCategoryIndex == -1) null else categoryIndex to subCategoryIndex
}

/**
 * Runs [edit] on a copy of one sub-category's groceries and, when it returns true, writes the rebuilt
 * category back. Returns whether anything was written.
 */
private fun homeScreenEditGroceries(
    categoryId: String,
    subCategoryId: String,
    edit: (MutableList<Grocery>) -> Boolean,
): Boolean {
    val (categoryIndex, subCategoryIndex) = homeScreenLocate(categoryId, subCategoryId) ?: return false
    val category = DataManagerObject.categories[categoryIndex]
    val groceries = category.subCategories[subCategoryIndex].groceries.toMutableList()
    if (!edit(groceries)) return false
    DataManagerObject.categories[categoryIndex] = category.homeScreenWithGroceries(subCategoryIndex, groceries)
    return true
}

private fun CategoryWithSubCategories.homeScreenWithGroceries(index: Int, groceries: MutableList<Grocery>) =
    copy(subCategories = subCategories.toMutableList().apply { set(index, this[index].copy(groceries = groceries)) })
