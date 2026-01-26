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
     * Converts nested structure to flat lists
     */
    fun flattenCategories(nestedData: List<CategoryWithSubCategories>): Triple<List<Category>, List<SubCategory>, List<Grocery>> {
        val categories = mutableListOf<Category>()
        val subCategories = mutableListOf<SubCategory>()
        val groceries = mutableListOf<Grocery>()
        
        nestedData.forEach { categoryWithSubs ->
            // Add category if not deleted
            if (!categoryWithSubs.category.isDeleted) {
                categories.add(categoryWithSubs.category)
                
                // Add subcategories and their groceries if not deleted
                categoryWithSubs.subCategories.forEach { subCategoryWithGroceries ->
                    if (!subCategoryWithGroceries.subCategory.isDeleted) {
                        subCategories.add(subCategoryWithGroceries.subCategory)
                        groceries.addAll(subCategoryWithGroceries.groceries.filter { !it.isDeleted })
                    }
                }
            }
        }
        
        return Triple(categories, subCategories, groceries)
    }
    
    /**
     * Rebuilds nested structure from flat lists, filtering out deleted items
     */
    fun buildNestedStructure(
        categories: List<Category>,
        subCategories: List<SubCategory>,
        groceries: List<Grocery>
    ): List<CategoryWithSubCategories> {
        return categories
            .filter { !it.isDeleted }
            .map { category ->
                // Find all non-deleted subcategories for this category
                val categorySubCategories = subCategories
                    .filter { it.categoryId == category.uuid && !it.isDeleted }
                
                // Create CategoryWithSubCategories
                CategoryWithSubCategories(
                    category = category,
                    subCategories = categorySubCategories.map { subCategory ->
                        // Find all non-deleted groceries for this subcategory
                        val subCategoryGroceries = groceries.filter { 
                            it.categoryId == category.uuid && 
                            it.subCategoryId == subCategory.uuid &&
                            !it.isDeleted
                        }
                        
                        // Create SubCategoryWithGroceries
                        SubCategoryWithGroceries(
                            subCategory = subCategory,
                            groceries = subCategoryGroceries.toMutableList()
                        )
                    }.toMutableList()
                )
            }
    }
    
    /**
     * Validates relationships between categories, subcategories, and groceries
     */
    fun validateRelationships(
        categories: List<Category>,
        subCategories: List<SubCategory>,
        groceries: List<Grocery>
    ): Boolean {
        // Get all non-deleted category IDs
        val categoryIds = categories
            .filter { !it.isDeleted }
            .map { it.uuid }
            .toSet()
        
        // Validate non-deleted subcategories
        val validSubCategories = subCategories
            .filter { !it.isDeleted }
            .all { subCategory ->
                categoryIds.contains(subCategory.categoryId)
            }
        
        if (!validSubCategories) return false
        
        // Get all non-deleted subcategory IDs
        val subCategoryIds = subCategories
            .filter { !it.isDeleted }
            .map { it.uuid }
            .toSet()
        
        // Validate non-deleted groceries
        return groceries
            .filter { !it.isDeleted }
            .all { grocery ->
                categoryIds.contains(grocery.categoryId) &&
                subCategoryIds.contains(grocery.subCategoryId)
            }
    }
}