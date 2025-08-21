package com.example.supercart2.data

import com.example.supercart2.models.Category
import com.example.supercart2.models.SubCategory
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope

object DataInitializer {
    
    suspend fun initializeDefaultData(context: android.content.Context) {
        if (DataManagerObject.categories.isEmpty()) {
            android.util.Log.d("DataInitializer", "Creating default categories...")
            createDefaultCategories()
            android.util.Log.d("DataInitializer", "Default categories created. Count: ${DataManagerObject.categories.size}")
            // Save the default data to local storage
            DataStoreManager.saveData(context)
            android.util.Log.d("DataInitializer", "Default data saved to storage")
        } else {
            android.util.Log.d("DataInitializer", "Categories already exist. Count: ${DataManagerObject.categories.size}")
        }
    }
    
    private fun createDefaultCategories() {
        // Create "Other" category
        val otherCategory = Category(
            name = "Other",
            default = true,
            viewOrder = 1,
            protected = true
        )
        
        val otherSubCategory = SubCategory(
            categoryId = otherCategory.uuid,
            name = "General"
        )
        
        val otherCategoryWithSubs = CategoryWithSubCategories(
            category = otherCategory,
            subCategories = mutableListOf(
                SubCategoryWithGroceries(
                    subCategory = otherSubCategory,
                    groceries = mutableListOf()
                )
            )
        )
        
        // Create "Vegetables" category
        val vegetablesCategory = Category(
            name = "Vegetables",
            default = true,
            viewOrder = 2,
            protected = false
        )
        
        val vegetablesSubCategory = SubCategory(
            categoryId = vegetablesCategory.uuid,
            name = "General"
        )
        
        val vegetablesCategoryWithSubs = CategoryWithSubCategories(
            category = vegetablesCategory,
            subCategories = mutableListOf(
                SubCategoryWithGroceries(
                    subCategory = vegetablesSubCategory,
                    groceries = mutableListOf()
                )
            )
        )
        
        // Create "Fruits" category
        val fruitsCategory = Category(
            name = "Fruits",
            default = true,
            viewOrder = 3,
            protected = false
        )
        
        val fruitsSubCategory = SubCategory(
            categoryId = fruitsCategory.uuid,
            name = "General"
        )
        
        val fruitsCategoryWithSubs = CategoryWithSubCategories(
            category = fruitsCategory,
            subCategories = mutableListOf(
                SubCategoryWithGroceries(
                    subCategory = fruitsSubCategory,
                    groceries = mutableListOf()
                )
            )
        )
        
        // Add all categories to the data manager
        DataManagerObject.categories.addAll(
            listOf(otherCategoryWithSubs, vegetablesCategoryWithSubs, fruitsCategoryWithSubs)
        )
    }
}
