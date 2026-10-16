package com.example.supercart2.data

import com.example.supercart2.models.Grocery
import java.time.LocalDateTime

// Grocery-level helpers for DataManagerObject's category tree.
// Paths are indices: (category, sub-category) or (category, sub-category, grocery).

private fun List<CategoryWithSubCategories>.groceriesAt(path: Pair<Int, Int>): List<Grocery> =
    this[path.first].subCategories[path.second].groceries

private fun List<CategoryWithSubCategories>.subCategoryPaths(): List<Pair<Int, Int>> =
    flatMapIndexed { categoryIndex, category -> category.subCategories.indices.map { categoryIndex to it } }

/** Path of the first grocery with [groceryUuid], in tree order, or null. */
internal fun List<CategoryWithSubCategories>.findGroceryPath(groceryUuid: String): Triple<Int, Int, Int>? {
    subCategoryPaths().forEach { path ->
        val groceryIndex = groceriesAt(path).indexOfFirst { it.uuid == groceryUuid }
        if (groceryIndex != -1) return Triple(path.first, path.second, groceryIndex)
    }
    return null
}

/** Path of the first category with [categoryId], then its first sub-category with [subCategoryId], or null. */
internal fun List<CategoryWithSubCategories>.findSubCategoryPath(
    categoryId: String,
    subCategoryId: String,
): Pair<Int, Int>? {
    val categoryIndex = indexOfFirst { it.category.uuid == categoryId }
    if (categoryIndex == -1) return null
    val subCategoryIndex = this[categoryIndex].subCategories.indexOfFirst { it.subCategory.uuid == subCategoryId }
    return if (subCategoryIndex == -1) null else categoryIndex to subCategoryIndex
}

/** Path of the last sub-category (anywhere in the tree) containing a grocery with [groceryUuid]. */
internal fun List<CategoryWithSubCategories>.lastSubCategoryPathContaining(groceryUuid: String): Pair<Int, Int>? =
    subCategoryPaths().lastOrNull { path -> groceriesAt(path).any { it.uuid == groceryUuid } }

/** Path of the last sub-category [subCategoryId] under a category [categoryId]. */
internal fun List<CategoryWithSubCategories>.lastSubCategoryPath(
    categoryId: String,
    subCategoryId: String,
): Pair<Int, Int>? = subCategoryPaths().lastOrNull { (categoryIndex, subCategoryIndex) ->
    val category = this[categoryIndex]
    category.category.uuid == categoryId && category.subCategories[subCategoryIndex].subCategory.uuid == subCategoryId
}

/** Replaces the groceries of one sub-category, rebuilding its (current) category immutably. */
internal fun MutableList<CategoryWithSubCategories>.setGroceries(
    categoryIndex: Int,
    subCategoryIndex: Int,
    groceries: MutableList<Grocery>,
) {
    this[categoryIndex] = this[categoryIndex].withSubCategory(subCategoryIndex, groceries)
}

internal fun MutableList<CategoryWithSubCategories>.appendGrocery(
    categoryIndex: Int,
    subCategoryIndex: Int,
    grocery: Grocery,
) {
    val groceries = groceriesAt(categoryIndex to subCategoryIndex)
    setGroceries(categoryIndex, subCategoryIndex, (groceries + grocery).toMutableList())
}

internal fun MutableList<CategoryWithSubCategories>.removeGrocery(
    categoryIndex: Int,
    subCategoryIndex: Int,
    groceryUuid: String,
) {
    val groceries = groceriesAt(categoryIndex to subCategoryIndex)
    setGroceries(categoryIndex, subCategoryIndex, groceries.filterNot { it.uuid == groceryUuid }.toMutableList())
}

/** Moves the grocery [groceryUuid] from the [source] to the [target] sub-category, applying [relocate] to it. */
internal fun MutableList<CategoryWithSubCategories>.moveGrocery(
    groceryUuid: String,
    source: Pair<Int, Int>,
    target: Pair<Int, Int>,
    relocate: (Grocery) -> Grocery,
) {
    val moved = groceriesAt(source).first { it.uuid == groceryUuid }
    removeGrocery(source.first, source.second, groceryUuid)
    // appendGrocery re-reads the target: it may be the category (or sub-category) just rebuilt above
    appendGrocery(target.first, target.second, relocate(moved))
}

/** A copy whose sub-category at [index] has [groceries]; the source category is not modified. */
internal fun CategoryWithSubCategories.withSubCategory(index: Int, groceries: MutableList<Grocery>) =
    copy(subCategories = subCategories.toMutableList().apply { set(index, get(index).copy(groceries = groceries)) })

/** Image UUIDs of the groceries with [groceryUuid] (first match per sub-category). */
internal fun List<CategoryWithSubCategories>.imageUuidsOf(groceryUuid: String): List<String> =
    flatMap { it.subCategories }.mapNotNull { subCategory ->
        subCategory.groceries.find { it.uuid == groceryUuid }?.imageUUID
    }

/** Removes [storeUuid] from every grocery in this list, replacing changed items in place with lastUpdate stamped. */
internal fun MutableList<Grocery>.unlinkStore(storeUuid: String) {
    forEachIndexed { index, grocery ->
        if (storeUuid !in grocery.storeIds) return@forEachIndexed
        this[index] = grocery.copy(storeIds = grocery.storeIds.filter { it != storeUuid }, lastUpdate = LocalDateTime.now())
    }
}
