package com.example.supercart2.data

import com.example.supercart2.models.Category
import com.example.supercart2.models.SubCategory
import com.example.supercart2.models.Grocery

/**
 * Utility object for converting between flat and nested data structures.
 * - Flat structure: Separate lists of categories, subcategories, and groceries
 * - Nested structure: List of CategoryWithSubCategories used by DataManagerObject
 */
object DataConverter {

    /**
     * Converts nested structure to flat lists.
     * For Firebase sync, includes deleted items. Without deleted items, children of deleted parents are dropped too.
     */
    fun flattenCategories(
        nestedData: List<CategoryWithSubCategories>,
        includeDeleted: Boolean = true
    ): Triple<List<Category>, List<SubCategory>, List<Grocery>> {
        val keptCategories = nestedData.filter { includeDeleted || !it.category.deleted }
        val keptSubCategories = keptCategories.flatMap { it.subCategories }
            .filter { includeDeleted || !it.subCategory.deleted }
        val keptGroceries = keptSubCategories.flatMap { sub -> sub.groceries.filter { includeDeleted || !it.deleted } }
        return Triple(keptCategories.map { it.category }, keptSubCategories.map { it.subCategory }, keptGroceries)
    }

    /**
     * Rebuilds nested structure from flat lists.
     * For UI display, filters out deleted items by default.
     */
    fun buildNestedStructure(
        categories: List<Category>,
        subCategories: List<SubCategory>,
        groceries: List<Grocery>,
        includeDeleted: Boolean = false
    ): List<CategoryWithSubCategories> =
        categories.filter { includeDeleted || !it.deleted }.map { category ->
            val nested = subCategories
                .filter { it.categoryId == category.uuid && (includeDeleted || !it.deleted) }
                .map { nestSubCategory(category, it, groceries, includeDeleted) }
            CategoryWithSubCategories(category = category, subCategories = nested.toMutableList())
        }

    // A grocery belongs to a sub-category only when both its categoryId and subCategoryId match
    private fun nestSubCategory(
        category: Category,
        subCategory: SubCategory,
        groceries: List<Grocery>,
        includeDeleted: Boolean,
    ) = SubCategoryWithGroceries(
        subCategory = subCategory,
        groceries = groceries.filter {
            it.categoryId == category.uuid && it.subCategoryId == subCategory.uuid && (includeDeleted || !it.deleted)
        }.toMutableList()
    )

    /**
     * Validates relationships between categories, subcategories, and groceries
     * (deleted items count as valid parents).
     */
    fun validateRelationships(
        categories: List<Category>,
        subCategories: List<SubCategory>,
        groceries: List<Grocery>
    ): Boolean {
        val categoryIds = categories.map { it.uuid }.toSet()
        if (!subCategories.all { categoryIds.contains(it.categoryId) }) return false
        val subCategoryIds = subCategories.map { it.uuid }.toSet()
        return groceries.all { categoryIds.contains(it.categoryId) && subCategoryIds.contains(it.subCategoryId) }
    }
}
