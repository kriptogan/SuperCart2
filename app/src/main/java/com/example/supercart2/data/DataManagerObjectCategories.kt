package com.example.supercart2.data

import com.example.supercart2.models.Category
import com.example.supercart2.models.SubCategory
import java.time.LocalDateTime

// Category / sub-category helpers for DataManagerObject.

/** A copy without deleted sub-categories and groceries (new lists, the source is not aliased). */
internal fun CategoryWithSubCategories.withoutDeletedChildren(): CategoryWithSubCategories = copy(
    subCategories = subCategories
        .filter { !it.subCategory.deleted }
        .map { it.copy(groceries = it.groceries.filterNot { grocery -> grocery.deleted }.toMutableList()) }
        .toMutableList()
)

/** A copy with [update] applied to the category and lastUpdate stamped. */
internal fun CategoryWithSubCategories.withUpdatedCategory(update: (Category) -> Category) =
    copy(category = update(category).copy(lastUpdate = LocalDateTime.now()))

/** A copy with [update] applied to the sub-category and lastUpdate stamped. */
internal fun SubCategoryWithGroceries.withUpdatedSubCategory(update: (SubCategory) -> SubCategory) =
    copy(subCategory = update(subCategory).copy(lastUpdate = LocalDateTime.now()))

/** A copy with an empty [subCategory] appended. */
internal fun CategoryWithSubCategories.withAddedSubCategory(subCategory: SubCategory) =
    copy(subCategories = (subCategories + SubCategoryWithGroceries(subCategory, mutableListOf())).toMutableList())

/** Removes [categoryUuid] from every store's category order. */
internal fun MutableMap<String, List<String>>.removeCategoryFromAll(categoryUuid: String) {
    forEach { (storeId, categoryIds) -> this[storeId] = categoryIds.filter { it != categoryUuid } }
}
