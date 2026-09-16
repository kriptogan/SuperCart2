package com.example.supercart2.ui.screens

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.supercart2.data.CategoryWithSubCategories
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.filterGroceries
import com.example.supercart2.models.Grocery
import com.example.supercart2.models.Store

/** What the store detail shows: the store, its to-buy tree and (buying mode only) its bought tree. */
internal class StoresScreenDetailModel(
    val storeId: String,
    val mode: StoreMode,
    val store: Store?,
    val toBuy: List<CategoryWithSubCategories>,
    val bought: List<CategoryWithSubCategories>,
) {
    val toBuyCount: Int = storesScreenGroceryCount(toBuy)
    val boughtCount: Int = storesScreenGroceryCount(bought)
}

private fun Grocery.storesScreenMatches(storeId: String, query: String): Boolean =
    storeId in storeIds && (query.isEmpty() || name.contains(query, ignoreCase = true))

/** Buying mode: unbought shopping-list groceries of the store. Edit mode: every grocery linked to the store. */
internal fun storesScreenToBuyCategories(
    categories: List<CategoryWithSubCategories>, storeId: String, mode: StoreMode, query: String,
): List<CategoryWithSubCategories> = categories.filterGroceries {
    it.storesScreenMatches(storeId, query) && (mode == StoreMode.Edit || it.inShoppingList && !it.isBought)
}

internal fun storesScreenBoughtCategories(
    categories: List<CategoryWithSubCategories>, storeId: String, query: String,
): List<CategoryWithSubCategories> = categories.filterGroceries {
    it.inShoppingList && it.isBought && it.storesScreenMatches(storeId, query)
}

internal fun storesScreenGroceryCount(categories: List<CategoryWithSubCategories>): Int =
    categories.sumOf { category -> category.subCategories.sumOf { it.groceries.size } }

/** Every grocery of the tree, in order, paired with its category name. */
internal fun storesScreenBoughtEntries(categories: List<CategoryWithSubCategories>): List<Pair<String, Grocery>> =
    categories.flatMap { category ->
        category.subCategories.flatMap { sub -> sub.groceries.map { category.category.name to it } }
    }

/** Height of the fixed header that the scrolling content must start below. */
internal fun storesScreenHeaderTopPadding(mode: StoreMode, boughtCount: Int): Dp = when {
    mode == StoreMode.Edit -> 100.dp
    boughtCount > 0 -> 248.dp
    else -> 196.dp
}

/** Replaces [original] by [updated], moving it first when its category or sub-category changed. */
internal fun storesScreenApplyGroceryEdit(original: Grocery, updated: Grocery) {
    if (original.categoryId != updated.categoryId || original.subCategoryId != updated.subCategoryId) {
        DataManagerObject.updateGroceryLocation(original.uuid, updated.categoryId, updated.subCategoryId)
    }
    DataManagerObject.updateGrocery(original.uuid) { updated }
}
