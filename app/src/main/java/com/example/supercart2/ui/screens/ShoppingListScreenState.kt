package com.example.supercart2.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.supercart2.data.CategoryWithSubCategories
import com.example.supercart2.data.filterGroceries
import com.example.supercart2.models.Grocery

/** UI state of [ShoppingListScreen]: search, expansion, view mode and which dialog is open. */
internal class ShoppingListScreenState {
    var showCategoriesManagement by mutableStateOf(false)
    var showStoresManagement by mutableStateOf(false)
    var showGroceryCreation by mutableStateOf(false)
    var showFinishConfirmation by mutableStateOf(false)
    var showImportGroceries by mutableStateOf(false)
    var showSettings by mutableStateOf(false)
    var showGroupManagement by mutableStateOf(false)
    var showHideStoresDialog by mutableStateOf(false)
    var searchQuery by mutableStateOf("")
    var isAllExpanded by mutableStateOf(false)
    var viewMode by mutableStateOf(ShoppingListViewMode.CATEGORY)
    var groceryToEdit by mutableStateOf<Grocery?>(null)
    var isEditMode by mutableStateOf(false)

    val isStoreView: Boolean
        get() = viewMode == ShoppingListViewMode.STORE

    fun toggleViewMode() {
        viewMode = if (isStoreView) ShoppingListViewMode.CATEGORY else ShoppingListViewMode.STORE
    }

    /** Opens the grocery dialog to edit [grocery]. */
    fun editGrocery(grocery: Grocery) {
        groceryToEdit = grocery
        isEditMode = true
        showGroceryCreation = true
    }

    /** Opens the grocery dialog to create a new grocery. */
    fun createGrocery() {
        isEditMode = false
        groceryToEdit = null
        showGroceryCreation = true
    }

    fun dismissGroceryDialog() {
        showGroceryCreation = false
        isEditMode = false
        groceryToEdit = null
    }

    companion object {
        fun viewModeFor(isStoreView: Boolean): ShoppingListViewMode =
            if (isStoreView) ShoppingListViewMode.STORE else ShoppingListViewMode.CATEGORY
    }
}

/** The shopping list split into "to buy" and "already bought" trees (only groceries in the list). */
internal data class ShoppingListScreenLists(
    val toBuy: List<CategoryWithSubCategories>,
    val bought: List<CategoryWithSubCategories>,
) {
    val toBuyCount: Int
        get() = toBuy.sumOf { it.getGroceriesCount() }

    val boughtCount: Int
        get() = bought.sumOf { it.getGroceriesCount() }

    /** Bought groceries in display order, each with its (untranslated) category name. */
    val boughtEntries: List<Pair<Grocery, String>>
        get() = bought.flatMap { category ->
            category.subCategories.flatMap { sub -> sub.groceries.map { it to category.category.name } }
        }

    companion object {
        fun from(categories: List<CategoryWithSubCategories>, query: String) = ShoppingListScreenLists(
            toBuy = categories.filterGroceries { it.inShoppingList && !it.isBought && matches(it, query) },
            bought = categories.filterGroceries { it.inShoppingList && it.isBought && matches(it, query) },
        )

        private fun matches(grocery: Grocery, query: String) =
            query.isEmpty() || grocery.name.contains(query, ignoreCase = true)
    }
}
