package com.example.supercart2.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import com.example.supercart2.data.CategoryWithSubCategories
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.SubCategoryWithGroceries
import com.example.supercart2.data.filterGroceries
import com.example.supercart2.models.Grocery
import com.example.supercart2.models.hasAlert
import java.time.LocalDate

/** The dialogs HomeScreen can open (at most one at a time). */
internal enum class HomeScreenDialog { CATEGORIES, STORES, GROUP, SETTINGS, IMPORT, GROCERY }

/** UI state of HomeScreen: open dialog, search / expand / alert-filter state and the grocery being edited. */
internal class HomeScreenState {
    var openDialog by mutableStateOf<HomeScreenDialog?>(null)
    var searchQuery by mutableStateOf("")
    var isAllExpanded by mutableStateOf(false)
    var isAlertFilterActive by mutableStateOf(false)

    /** Grocery shown by the grocery dialog in edit mode; null means create mode. */
    var groceryToEdit by mutableStateOf<Grocery?>(null)

    fun opener(dialog: HomeScreenDialog): () -> Unit = { openDialog = dialog }

    fun toggleAlertFilter() {
        isAlertFilterActive = !isAlertFilterActive
    }

    fun closeDialog() {
        openDialog = null
    }

    fun startCreate() {
        groceryToEdit = null
        openDialog = HomeScreenDialog.GROCERY
    }

    fun startEdit(grocery: Grocery) {
        groceryToEdit = grocery
        openDialog = HomeScreenDialog.GROCERY
    }

    fun dismissGroceryDialog() {
        openDialog = null
        groceryToEdit = null
    }

    fun hasAlertItems(showAlerts: Boolean): Boolean =
        showAlerts && homeScreenAlertData(homeScreenSortedCategories()).isNotEmpty()

    /**
     * Categories to display: alert filter (when active and alerts are enabled), then search filter; without a
     * search, an empty result falls back to the full tree. Side effect kept from the original: while the alert
     * filter applies, every evaluation forces [isAllExpanded] to true.
     */
    fun visibleCategories(showAlerts: Boolean): List<CategoryWithSubCategories> {
        val applyAlertFilter = isAlertFilterActive && showAlerts
        if (applyAlertFilter) isAllExpanded = true
        var data = homeScreenSortedCategories().let { if (applyAlertFilter) homeScreenAlertData(it) else it }
        if (searchQuery.isNotBlank()) data = homeScreenFilterBySearch(data, searchQuery)
        return if (data.isEmpty() && searchQuery.isBlank()) homeScreenSortedCategories() else data
    }
}

/** Sorted, non-deleted categories; also reads [DataManagerObject.version] so composable callers track every change. */
private fun homeScreenSortedCategories(): List<CategoryWithSubCategories> =
    DataManagerObject.getSortedCategories().also { DataManagerObject.version }

private fun homeScreenAlertData(categories: List<CategoryWithSubCategories>): List<CategoryWithSubCategories> {
    val today = LocalDate.now()
    return categories.filterGroceries { it.hasAlert(today) }
}

/** Keeps only groceries whose name contains [query] (case-insensitive), dropping emptied (sub-)categories. */
private fun homeScreenFilterBySearch(
    categories: List<CategoryWithSubCategories>,
    query: String,
): List<CategoryWithSubCategories> {
    val lowerCaseQuery = query.lowercase()
    return categories.mapNotNull { category ->
        val subCategories = category.subCategories.mapNotNull { homeScreenMatchingSubCategory(it, lowerCaseQuery) }
        if (subCategories.isEmpty()) null else category.copy(subCategories = subCategories.toMutableStateList())
    }
}

private fun homeScreenMatchingSubCategory(
    subCategory: SubCategoryWithGroceries,
    lowerCaseQuery: String,
): SubCategoryWithGroceries? {
    val groceries = subCategory.groceries.filter { it.name.lowercase().contains(lowerCaseQuery) }
    return if (groceries.isEmpty()) null else subCategory.copy(groceries = groceries.toMutableStateList())
}
