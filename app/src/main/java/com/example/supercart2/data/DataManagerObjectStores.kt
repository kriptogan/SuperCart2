package com.example.supercart2.data

import com.example.supercart2.models.Store
import com.example.supercart2.ui.components.StoreWithCategories

// Per-store ordering helpers for DataManagerObject.

/** Items whose id appears in [ids] first (in that order), then the rest via [sortRest]. */
private fun <T> List<T>.orderedBy(
    ids: List<String>,
    idOf: (T) -> String,
    sortRest: (List<T>) -> List<T> = { it },
): List<T> {
    val remaining = toMutableList()
    val ordered = ids.mapNotNull { id -> remaining.find { idOf(it) == id }?.also { remaining.remove(it) } }
    return ordered + sortRest(remaining)
}

// Store's custom category order first, remaining categories by global viewOrder
internal fun List<CategoryWithSubCategories>.inStoreCategoryOrder(storeId: String) =
    orderedBy(DataManagerObject.getStoreCategoryOrder(storeId).orEmpty(), { it.category.uuid }) { rest ->
        rest.sortedBy { it.category.viewOrder }
    }

/** This category with sub-categories in the store's custom order; unchanged when the store has none for it. */
internal fun CategoryWithSubCategories.inStoreSubCategoryOrder(storeId: String): CategoryWithSubCategories {
    val subOrder = DataManagerObject.getStoreSubCategoryOrder(storeId, category.uuid) ?: return this
    val ordered = subCategories.orderedBy(subOrder, { it.subCategory.uuid })
    return CategoryWithSubCategories(category, ordered.toMutableList())
}

/** The groceries linked to [store], in the store's category order, or null when there are none. */
internal fun List<CategoryWithSubCategories>.groupForStore(store: Store): StoreWithCategories? =
    filterGroceries { store.uuid in it.storeIds }
        .inStoreCategoryOrder(store.uuid)
        .takeIf { it.isNotEmpty() }
        ?.let { StoreWithCategories(store = store, categories = it) }
