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
            // Add category
            categories.add(categoryWithSubs.category)
            
            // Add subcategories and their groceries
            categoryWithSubs.subCategories.forEach { subCategoryWithGroceries ->
                subCategories.add(subCategoryWithGroceries.subCategory)
                groceries.addAll(subCategoryWithGroceries.groceries)
            }
        }
        
        return Triple(categories, subCategories, groceries)
    }
    
    /**
     * Rebuilds nested structure from flat lists
     */
    fun buildNestedStructure(
        categories: List<Category>,
        subCategories: List<SubCategory>,
        groceries: List<Grocery>
    ): List<CategoryWithSubCategories> {
        return categories.map { category ->
            // Find all subcategories for this category
            val categorySubCategories = subCategories.filter { it.categoryId == category.uuid }
            
            // Create CategoryWithSubCategories
            CategoryWithSubCategories(
                category = category,
                subCategories = categorySubCategories.map { subCategory ->
                    // Find all groceries for this subcategory
                    val subCategoryGroceries = groceries.filter { 
                        it.categoryId == category.uuid && 
                        it.subCategoryId == subCategory.uuid 
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
        // Get all category IDs
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
