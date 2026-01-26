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
     * For Firebase sync, includes deleted items.
     */
    fun flattenCategories(nestedData: List<CategoryWithSubCategories>, includeDeleted: Boolean = true): Triple<List<Category>, List<SubCategory>, List<Grocery>> {
        val categories = mutableListOf<Category>()
        val subCategories = mutableListOf<SubCategory>()
        val groceries = mutableListOf<Grocery>()
        
        nestedData.forEach { categoryWithSubs ->
            // Add category (filter by deleted status if not including deleted)
            if (includeDeleted || !categoryWithSubs.category.deleted) {
                categories.add(categoryWithSubs.category)
                
                // Add subcategories and their groceries
                categoryWithSubs.subCategories.forEach { subCategoryWithGroceries ->
                    if (includeDeleted || !subCategoryWithGroceries.subCategory.deleted) {
                        subCategories.add(subCategoryWithGroceries.subCategory)
                        
                        // Add groceries (filter by deleted status if not including deleted)
                        val filteredGroceries = if (includeDeleted) {
                            subCategoryWithGroceries.groceries
                        } else {
                            subCategoryWithGroceries.groceries.filter { !it.deleted }
                        }
                        groceries.addAll(filteredGroceries)
                    }
                }
            }
        }
        
        return Triple(categories, subCategories, groceries)
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
    ): List<CategoryWithSubCategories> {
        // Filter categories if not including deleted
        val filteredCategories = if (includeDeleted) categories else categories.filter { !it.deleted }
        
        return filteredCategories.map { category ->
            // Find all subcategories for this category
            val categorySubCategories = subCategories
                .filter { it.categoryId == category.uuid }
                .filter { includeDeleted || !it.deleted }
            
            // Create CategoryWithSubCategories
            CategoryWithSubCategories(
                category = category,
                subCategories = categorySubCategories.map { subCategory ->
                    // Find all groceries for this subcategory
                    val subCategoryGroceries = groceries.filter { 
                        it.categoryId == category.uuid && 
                        it.subCategoryId == subCategory.uuid &&
                        (includeDeleted || !it.deleted)
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
        // Get all category IDs (including deleted ones for validation)
        val categoryIds = categories.map { it.uuid }.toSet()
        
        // Validate subcategories
        val validSubCategories = subCategories.all { subCategory ->
            categoryIds.contains(subCategory.categoryId)
        }
        
        if (!validSubCategories) return false
        
        // Get all subcategory IDs
        val subCategoryIds = subCategories.map { it.uuid }.toSet()
        
        // Validate groceries
        return groceries.all { grocery ->
            categoryIds.contains(grocery.categoryId) &&
            subCategoryIds.contains(grocery.subCategoryId)
        }
    }
}