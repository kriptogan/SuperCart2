package com.example.supercart2.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.supercart2.models.Category
import com.example.supercart2.models.SubCategory
import com.example.supercart2.models.Grocery

object DataManagerObject {
    val categories: SnapshotStateList<CategoryWithSubCategories> = mutableStateListOf()
    
    // Version counter to force recomposition
    private var _version by mutableStateOf(0)
    val version: Int get() = _version
    
    private fun notifyUpdate() {
        _version++
        android.util.Log.d("datastore test", "Data updated, version: $_version")
    }
    
    // Get categories sorted by viewOrder for consistent display order
    fun getSortedCategories(): List<CategoryWithSubCategories> {
        return categories.sortedBy { it.category.viewOrder }
    }
    
    // Helper function to update a grocery item
    fun updateGrocery(groceryUuid: String, update: (Grocery) -> Grocery) {
        categories.forEach { category ->
            category.subCategories.forEach { subCategory ->
                val index = subCategory.groceries.indexOfFirst { it.uuid == groceryUuid }
                if (index != -1) {
                    val oldGrocery = subCategory.groceries[index]
                    val newGrocery = update(oldGrocery)
                    subCategory.groceries[index] = newGrocery
                    updateData()
                    return
                }
            }
        }
    }

    // Helper function to toggle shopping list status
    fun toggleShoppingListStatus(groceryUuid: String) {
        updateGrocery(groceryUuid) { it.copy(inShoppingList = !it.inShoppingList) }
    }

    // Helper function to toggle bought status
    fun toggleBoughtStatus(groceryUuid: String) {
        updateGrocery(groceryUuid) { it.copy(isBought = !it.isBought) }
    }

    // Category Management Helpers
    fun updateCategory(categoryUuid: String, update: (Category) -> Category) {
        val index = categories.indexOfFirst { it.category.uuid == categoryUuid }
        if (index != -1) {
            val categoryWithSubs = categories[index]
            categories[index] = categoryWithSubs.copy(
                category = update(categoryWithSubs.category)
            )
            updateData()
        }
    }

    fun swapCategoryOrder(category1Uuid: String, category2Uuid: String) {
        val index1 = categories.indexOfFirst { it.category.uuid == category1Uuid }
        val index2 = categories.indexOfFirst { it.category.uuid == category2Uuid }
        
        if (index1 != -1 && index2 != -1) {
            val cat1 = categories[index1]
            val cat2 = categories[index2]
            
            // Swap their view orders
            val tempOrder = cat1.category.viewOrder
            updateCategory(cat1.category.uuid) { it.copy(viewOrder = cat2.category.viewOrder) }
            updateCategory(cat2.category.uuid) { it.copy(viewOrder = tempOrder) }
        }
    }

    fun addCategory(category: Category, subCategories: List<SubCategoryWithGroceries> = emptyList()) {
        categories.add(CategoryWithSubCategories(
            category = category,
            subCategories = subCategories.toMutableList()
        ))
        updateData()
    }

    fun deleteCategory(categoryUuid: String) {
        categories.removeAll { it.category.uuid == categoryUuid }
        updateData()
    }

    // Sub-Category Management Helpers
    fun updateSubCategory(categoryUuid: String, subCategoryUuid: String, update: (SubCategory) -> SubCategory) {
        val categoryIndex = categories.indexOfFirst { it.category.uuid == categoryUuid }
        if (categoryIndex != -1) {
            val categoryWithSubs = categories[categoryIndex]
            val subCategoryIndex = categoryWithSubs.subCategories.indexOfFirst { 
                it.subCategory.uuid == subCategoryUuid 
            }
            
            if (subCategoryIndex != -1) {
                val updatedSubCategory = update(categoryWithSubs.subCategories[subCategoryIndex].subCategory)
                categoryWithSubs.subCategories[subCategoryIndex] = SubCategoryWithGroceries(
                    subCategory = updatedSubCategory,
                    groceries = categoryWithSubs.subCategories[subCategoryIndex].groceries
                )
                updateData()
            }
        }
    }

    fun addSubCategory(categoryUuid: String, subCategory: SubCategory) {
        val categoryIndex = categories.indexOfFirst { it.category.uuid == categoryUuid }
        if (categoryIndex != -1) {
            categories[categoryIndex].subCategories.add(
                SubCategoryWithGroceries(
                    subCategory = subCategory,
                    groceries = mutableListOf()
                )
            )
            updateData()
        }
    }

    fun deleteSubCategory(categoryUuid: String, subCategoryUuid: String) {
        val categoryIndex = categories.indexOfFirst { it.category.uuid == categoryUuid }
        if (categoryIndex != -1) {
            categories[categoryIndex].subCategories.removeAll { 
                it.subCategory.uuid == subCategoryUuid 
            }
            updateData()
        }
    }

    fun moveGroceriesToSubCategory(
        sourceSubCategoryUuid: String,
        targetSubCategoryUuid: String,
        groceryUuids: List<String>
    ) {
        // Find source and target sub-categories
        var sourceGroceries: MutableList<Grocery>? = null
        var targetSubCategory: SubCategoryWithGroceries? = null

        categories.forEach { category ->
            category.subCategories.forEach { subCategory ->
                if (subCategory.subCategory.uuid == sourceSubCategoryUuid) {
                    sourceGroceries = subCategory.groceries
                }
                if (subCategory.subCategory.uuid == targetSubCategoryUuid) {
                    targetSubCategory = subCategory
                }
            }
        }

        if (sourceGroceries != null && targetSubCategory != null) {
            // Find groceries to move
            val groceriesToMove = sourceGroceries!!.filter { it.uuid in groceryUuids }
            
            // Update their category and sub-category IDs
            val updatedGroceries = groceriesToMove.map { grocery ->
                grocery.copy(
                    categoryId = targetSubCategory!!.subCategory.categoryId,
                    subCategoryId = targetSubCategory!!.subCategory.uuid
                )
            }

            // Remove from source and add to target
            sourceGroceries!!.removeAll { it.uuid in groceryUuids }
            targetSubCategory!!.groceries.addAll(updatedGroceries)
            
            updateData()
        }
    }

    // Grocery Management Helpers
    fun addGrocery(grocery: Grocery) {
        categories.forEach { category ->
            if (category.category.uuid == grocery.categoryId) {
                category.subCategories.forEach { subCategory ->
                    if (subCategory.subCategory.uuid == grocery.subCategoryId) {
                        subCategory.groceries.add(grocery)
                        updateData()
                        return
                    }
                }
            }
        }
    }

    fun updateGroceryLocation(
        groceryUuid: String,
        newCategoryId: String,
        newSubCategoryId: String
    ) {
        var groceryToMove: Grocery? = null
        var sourceSubCategory: SubCategoryWithGroceries? = null

        // Find the grocery and its current location
        categories.forEach { category ->
            category.subCategories.forEach { subCategory ->
                val index = subCategory.groceries.indexOfFirst { it.uuid == groceryUuid }
                if (index != -1) {
                    groceryToMove = subCategory.groceries[index]
                    sourceSubCategory = subCategory
                }
            }
        }

        if (groceryToMove != null && sourceSubCategory != null) {
            // Remove from current location
            sourceSubCategory!!.groceries.removeAll { it.uuid == groceryUuid }

            // Update location and add to new location
            val updatedGrocery = groceryToMove!!.copy(
                categoryId = newCategoryId,
                subCategoryId = newSubCategoryId
            )

            // Add to new location
            categories.forEach { category ->
                if (category.category.uuid == newCategoryId) {
                    category.subCategories.forEach { subCategory ->
                        if (subCategory.subCategory.uuid == newSubCategoryId) {
                            subCategory.groceries.add(updatedGrocery)
                        }
                    }
                }
            }

            updateData()
        }
    }

    fun deleteGrocery(groceryUuid: String) {
        categories.forEach { category ->
            category.subCategories.forEach { subCategory ->
                if (subCategory.groceries.removeAll { it.uuid == groceryUuid }) {
                    updateData()
                    return
                }
            }
        }
    }

    // Call this after any data modification
    fun updateData() {
        notifyUpdate()
    }
}

data class CategoryWithSubCategories(
    val category: Category,
    val subCategories: MutableList<SubCategoryWithGroceries>
) {
    fun getGroceriesCount(): Int = subCategories.sumOf { it.groceries.size }
}

data class SubCategoryWithGroceries(
    val subCategory: SubCategory,
    val groceries: MutableList<Grocery>
)
