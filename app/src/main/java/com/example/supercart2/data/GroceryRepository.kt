package com.example.supercart2.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.supercart2.models.Category
import com.example.supercart2.models.CategoryWithSubCategories
import com.example.supercart2.models.Grocery
import com.example.supercart2.models.SubCategory
import com.example.supercart2.models.SubCategoryWithGroceries
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object GroceryRepository {
    private lateinit var dataStoreManager: DataStoreManager
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    // Private mutable state
    private val _categories = mutableStateListOf<CategoryWithSubCategories>()
    
    // Version counter to force recomposition
    private var _version by mutableStateOf(0)
    val version: Int get() = _version
    
    private fun notifyUpdate() {
        _version++
        android.util.Log.d("datastore test", "Repository updated, version: $_version")
    }
    
    fun initialize(context: Context) {
        android.util.Log.d("datastore test", "Initializing GroceryRepository")
        dataStoreManager = DataStoreManager(context)
        // Load initial data
        coroutineScope.launch {
            android.util.Log.d("datastore test", "Loading initial data in GroceryRepository")
            val savedData = dataStoreManager.loadData()
            _categories.clear()
            _categories.addAll(savedData)
            notifyUpdate()
            android.util.Log.d("datastore test", "Initial data loaded, categories count: ${_categories.size}")
        }
    }
    
    // Public immutable state
    val categories: List<CategoryWithSubCategories> get() = _categories
    
    // Sorted categories for consistent display
    val sortedCategories: List<CategoryWithSubCategories>
        get() = _categories.sortedBy { it.category.viewOrder }
    
    // Category operations
    private fun saveData() {
        coroutineScope.launch {
            android.util.Log.d("datastore test", "Saving data in GroceryRepository, categories count: ${_categories.size}")
            dataStoreManager.saveData(_categories.toList())
        }
    }

    fun addCategory(category: Category) {
        _categories.add(CategoryWithSubCategories(
            category = category,
            subCategories = mutableListOf<SubCategoryWithGroceries>()
        ))
        _categories.sortBy { it.category.viewOrder }
        saveData()
    }
    
    fun updateCategory(category: Category) {
        val index = _categories.indexOfFirst { it.category.uuid == category.uuid }
        if (index != -1) {
            _categories[index] = _categories[index].copy(category = category)
            _categories.sortBy { it.category.viewOrder }
            saveData()
        }
    }
    
    fun deleteCategory(categoryId: String) {
        _categories.removeAll { it.category.uuid == categoryId }
        saveData()
    }
    
    // SubCategory operations
    fun addSubCategory(subCategory: SubCategory) {
        val categoryIndex = _categories.indexOfFirst { it.category.uuid == subCategory.categoryId }
        if (categoryIndex != -1) {
            _categories[categoryIndex].subCategories.add(
                SubCategoryWithGroceries(
                    subCategory = subCategory,
                    groceries = mutableListOf<Grocery>()
                )
            )
            // Sort subcategories by viewOrder
            _categories[categoryIndex].subCategories.sortBy { it.subCategory.viewOrder }
            saveData()
        }
    }
    
    fun updateSubCategory(subCategory: SubCategory) {
        val categoryIndex = _categories.indexOfFirst { it.category.uuid == subCategory.categoryId }
        if (categoryIndex != -1) {
            val subCategoryIndex = _categories[categoryIndex].subCategories
                .indexOfFirst { it.subCategory.uuid == subCategory.uuid }
            if (subCategoryIndex != -1) {
                val currentGroceries = _categories[categoryIndex]
                    .subCategories[subCategoryIndex].groceries
                _categories[categoryIndex].subCategories[subCategoryIndex] =
                    SubCategoryWithGroceries(subCategory, currentGroceries)
                // Sort subcategories by viewOrder
                _categories[categoryIndex].subCategories.sortBy { it.subCategory.viewOrder }
                saveData()
            }
        }
    }
    
    fun deleteSubCategory(categoryId: String, subCategoryId: String) {
        val categoryIndex = _categories.indexOfFirst { it.category.uuid == categoryId }
        if (categoryIndex != -1) {
            _categories[categoryIndex].subCategories.removeAll { 
                it.subCategory.uuid == subCategoryId 
            }
            saveData()
        }
    }
    
    // Grocery operations
    fun addGrocery(grocery: Grocery) {
        // Find the subcategory across all categories
        _categories.forEach { category ->
            val subCategoryIndex = category.subCategories
                .indexOfFirst { it.subCategory.uuid == grocery.subCategoryId }
            if (subCategoryIndex != -1) {
                category.subCategories[subCategoryIndex].groceries.add(grocery)
                saveData()
                return
            }
        }
    }
    
    fun updateGrocery(grocery: Grocery) {
        // Find the subcategory across all categories
        _categories.forEach { category ->
            val subCategoryIndex = category.subCategories
                .indexOfFirst { it.subCategory.uuid == grocery.subCategoryId }
            if (subCategoryIndex != -1) {
                val groceryIndex = category
                    .subCategories[subCategoryIndex].groceries
                    .indexOfFirst { it.uuid == grocery.uuid }
                if (groceryIndex != -1) {
                    category
                        .subCategories[subCategoryIndex]
                        .groceries[groceryIndex] = grocery
                    saveData()
                    return
                }
            }
        }
    }
    
    fun deleteGrocery(categoryId: String, subCategoryId: String, groceryId: String) {
        val categoryIndex = _categories.indexOfFirst { it.category.uuid == categoryId }
        if (categoryIndex != -1) {
            val subCategoryIndex = _categories[categoryIndex].subCategories
                .indexOfFirst { it.subCategory.uuid == subCategoryId }
            if (subCategoryIndex != -1) {
                _categories[categoryIndex]
                    .subCategories[subCategoryIndex]
                    .groceries
                    .removeAll { it.uuid == groceryId }
                saveData()
            }
        }
    }
    
    // Shopping list operations
    fun toggleGroceryInShoppingList(groceryId: String, inShoppingList: Boolean) {
        android.util.Log.d("datastore test", "Toggling shopping list status for grocery $groceryId to $inShoppingList")
        _categories.forEach { category ->
            category.subCategories.forEach { subCategory ->
                val grocery = subCategory.groceries.find { it.uuid == groceryId }
                if (grocery != null) {
                    val index = subCategory.groceries.indexOf(grocery)
                    subCategory.groceries[index] = grocery.copy(inShoppingList = inShoppingList)
                    android.util.Log.d("datastore test", "Successfully toggled shopping list status")
                    notifyUpdate()
                    saveData()
                    return
                }
            }
        }
        android.util.Log.d("datastore test", "Grocery $groceryId not found")
    }
    
    fun toggleGroceryBoughtStatus(groceryId: String, isBought: Boolean) {
        _categories.forEach { category ->
            category.subCategories.forEach { subCategory ->
                val grocery = subCategory.groceries.find { it.uuid == groceryId }
                if (grocery != null) {
                    val index = subCategory.groceries.indexOf(grocery)
                    subCategory.groceries[index] = grocery.copy(isBought = isBought)
                    saveData()
                    return
                }
            }
        }
    }
    
    // Query operations
    fun getShoppingList(): List<Grocery> {
        return _categories.flatMap { category ->
            category.subCategories.flatMap { subCategory ->
                subCategory.groceries.filter { it.inShoppingList }
            }
        }
    }
    
    fun getBoughtItems(): List<Grocery> {
        return _categories.flatMap { category ->
            category.subCategories.flatMap { subCategory ->
                subCategory.groceries.filter { it.isBought }
            }
        }
    }
    
    fun searchGroceries(query: String): List<Grocery> {
        return _categories.flatMap { category ->
            category.subCategories.flatMap { subCategory ->
                subCategory.groceries.filter { 
                    it.name.contains(query, ignoreCase = true)
                }
            }
        }
    }
    
    fun createMockData() {
        // Clear existing data
        _categories.clear()
        
        // Create and add category
        val category = Category(
            uuid = "1234",
            name = "first category",
            viewOrder = 1
        )
        addCategory(category)
        
        // Create and add subcategory
        val subCategory = SubCategory(
            uuid = "2345",
            name = "General",
            categoryId = category.uuid,
            viewOrder = 1
        )
        addSubCategory(subCategory)
        
        // Create and add grocery
        val grocery = Grocery(
            uuid = "3456",
            name = "1st item",
            subCategoryId = subCategory.uuid,
            date = null,
            inShoppingList = false,
            isBought = false,
            imageId = null
        )
        addGrocery(grocery)
    }
    
    fun clearAllData() {
        android.util.Log.d("datastore test", "Clearing all data from repository and DataStore")
        coroutineScope.launch {
            dataStoreManager.clearData()
            _categories.clear()
        }
        android.util.Log.d("datastore test", "All data cleared")
    }
}
