package com.example.supercart2.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.supercart2.data.CategoryWithSubCategories
import com.example.supercart2.data.DataManagerObject
import java.util.Collections

/** Unsaved category order (UUIDs) and per-category sub-category orders shown by [StoreCategoryOrderDialog]. */
internal class StoreCategoryOrderDialogState(storeId: String, current: List<CategoryWithSubCategories>) {
    var categoryOrder by mutableStateOf(current.map { it.category.uuid })
        private set
    var subOrders by mutableStateOf(storeCategoryOrderDialogSubOrders(storeId, current))
        private set

    fun reset(storeId: String, current: List<CategoryWithSubCategories>) {
        categoryOrder = current.map { it.category.uuid }
        subOrders = storeCategoryOrderDialogSubOrders(storeId, current)
    }

    fun categoryMoves(index: Int) = StoreCategoryOrderDialogMoves(
        canMoveUp = index > 0, canMoveDown = index < categoryOrder.size - 1,
        onMoveUp = { moveCategory(index, -1) }, onMoveDown = { moveCategory(index, 1) },
    )

    /** Moves for a sub-category row; [subOrder] is the order being displayed for [categoryId]. */
    fun subCategoryMoves(categoryId: String, subOrder: List<String>, index: Int) = StoreCategoryOrderDialogMoves(
        canMoveUp = index > 0, canMoveDown = index < subOrder.size - 1,
        onMoveUp = { moveSubCategory(categoryId, subOrder, index, -1) },
        onMoveDown = { moveSubCategory(categoryId, subOrder, index, 1) },
    )

    /** Swaps the category at [index] with its neighbour at [index] + [delta], if there is one. */
    fun moveCategory(index: Int, delta: Int) {
        if (index + delta !in categoryOrder.indices) return
        categoryOrder = categoryOrder.storeCategoryOrderDialogSwapped(index, delta)
    }

    /** Swaps within [subOrder] (the order displayed when the button was composed) and stores it for [categoryId]. */
    fun moveSubCategory(categoryId: String, subOrder: List<String>, index: Int, delta: Int) {
        if (index + delta !in subOrder.indices) return
        val swapped = subOrder.storeCategoryOrderDialogSwapped(index, delta)
        subOrders = subOrders.toMutableMap().apply { put(categoryId, swapped) }
    }
}

/** Categories in the store's custom order (unknown ids ignored), then the rest by viewOrder. */
internal fun storeCategoryOrderDialogCurrentOrder(
    storeId: String, allCategories: List<CategoryWithSubCategories>,
): List<CategoryWithSubCategories> {
    val customOrder = DataManagerObject.getStoreCategoryOrder(storeId)
        ?: return allCategories.sortedBy { it.category.viewOrder }
    val unordered = allCategories.toMutableList()
    val ordered = customOrder.mapNotNull { id -> unordered.find { it.category.uuid == id }?.also(unordered::remove) }
    return ordered + unordered.sortedBy { it.category.viewOrder }
}

/** The store's custom sub-category order per category, defaulting to the category's own order. */
private fun storeCategoryOrderDialogSubOrders(
    storeId: String, categories: List<CategoryWithSubCategories>,
): Map<String, List<String>> = categories.associate { group ->
    val categoryId = group.category.uuid
    val customOrder = DataManagerObject.getStoreSubCategoryOrder(storeId, categoryId)
    categoryId to (customOrder ?: group.subCategories.map { it.subCategory.uuid })
}

private fun List<String>.storeCategoryOrderDialogSwapped(index: Int, delta: Int): List<String> =
    toMutableList().apply { Collections.swap(this, index, index + delta) }
