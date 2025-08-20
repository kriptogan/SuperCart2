package com.kriptogan.supercart2.classes

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

data class CategoryWithSubCategories(
    val category: Category,
    val subCategories: MutableList<SubCategoryWithGroceries>
) {
    fun getGroceriesCount(): Int = subCategories.sumOf { it.groceries.size }
}

data class SubCategoryWithGroceries(
    val subCategory: SubCategory,
    val groceries: MutableList<Grocery>
) {
    fun getGroceriesCount(): Int = groceries.size
}

class CategoryDataManager(
    private val localStorageManager: LocalStorageManager,
    private val firebaseManager: FirebaseManager,
    private val coroutineScope: CoroutineScope
) {
    private var _categories = mutableListOf<CategoryWithSubCategories>()
    val categories: List<CategoryWithSubCategories> get() = _categories.sortedBy { it.category.viewOrder }

    init {
        loadFromLocalStorage()
    }

    private fun loadFromLocalStorage() {
        val localCategories = localStorageManager.getCategories()
        val localSubCategories = localStorageManager.getSubCategories()
        val localGroceries = localStorageManager.getGroceries()

        _categories.clear()
        
        localCategories.forEach { category ->
            val categorySubCategories = localSubCategories
                .filter { it.categoryId == category.uuid }
                .map { subCategory ->
                    val subCategoryGroceries = localGroceries.filter { it.subCategoryId == subCategory.uuid }
                    SubCategoryWithGroceries(subCategory, subCategoryGroceries.toMutableList())
                }
                .sortedBy { it.subCategory.name }
                .toMutableList()
            
            _categories.add(CategoryWithSubCategories(category, categorySubCategories))
        }
    }

    // Category operations
    fun addCategory(category: Category) {
        val newCategoryWithSubs = CategoryWithSubCategories(
            category = category,
            subCategories = mutableListOf()
        )
        _categories.add(newCategoryWithSubs)
        
        // Create a new list reference to trigger UI updates
        _categories = _categories.toMutableList()
        
        saveToStorage()
    }

    fun updateCategory(category: Category) {
        val index = _categories.indexOfFirst { it.category.uuid == category.uuid }
        if (index != -1) {
            val existingSubCategories = _categories[index].subCategories
            _categories[index] = CategoryWithSubCategories(category, existingSubCategories)
            
            // Create a new list reference to trigger UI updates
            _categories = _categories.toMutableList()
            
            saveToStorage()
        }
    }

    fun deleteCategory(categoryId: String) {
        _categories.removeAll { it.category.uuid == categoryId }
        
        // Create a new list reference to trigger UI updates
        _categories = _categories.toMutableList()
        
        saveToStorage()
    }

    // Sub-category operations
    fun addSubCategory(subCategory: SubCategory) {
        val categoryIndex = _categories.indexOfFirst { it.category.uuid == subCategory.categoryId }
        if (categoryIndex != -1) {
            val newSubCategoryWithGroceries = SubCategoryWithGroceries(
                subCategory = subCategory,
                groceries = mutableListOf()
            )
            _categories[categoryIndex].subCategories.add(newSubCategoryWithGroceries)
            _categories[categoryIndex].subCategories.sortBy { it.subCategory.name }
            
            // Create a new list reference to trigger UI updates
            _categories = _categories.toMutableList()
            
            saveToStorage()
        }
    }

    fun updateSubCategory(subCategory: SubCategory) {
        val categoryIndex = _categories.indexOfFirst { it.category.uuid == subCategory.categoryId }
        if (categoryIndex != -1) {
            val subCategoryIndex = _categories[categoryIndex].subCategories.indexOfFirst { it.subCategory.uuid == subCategory.uuid }
            if (subCategoryIndex != -1) {
                val existingGroceries = _categories[categoryIndex].subCategories[subCategoryIndex].groceries
                _categories[categoryIndex].subCategories[subCategoryIndex] = SubCategoryWithGroceries(
                    subCategory = subCategory,
                    groceries = existingGroceries
                )
                _categories[categoryIndex].subCategories.sortBy { it.subCategory.name }
                
                // Create a new list reference to trigger UI updates
                _categories = _categories.toMutableList()
                
                saveToStorage()
            }
        }
    }

    fun deleteSubCategory(subCategoryId: String) {
        for (category in _categories) {
            val subCategoryIndex = category.subCategories.indexOfFirst { it.subCategory.uuid == subCategoryId }
            if (subCategoryIndex != -1) {
                category.subCategories.removeAt(subCategoryIndex)
                
                // Create a new list reference to trigger UI updates
                _categories = _categories.toMutableList()
                
                saveToStorage()
                break
            }
        }
    }

    // Grocery operations
    fun addGrocery(grocery: Grocery) {
        for (category in _categories) {
            val subCategoryIndex = category.subCategories.indexOfFirst { it.subCategory.uuid == grocery.subCategoryId }
            if (subCategoryIndex != -1) {
                category.subCategories[subCategoryIndex].groceries.add(grocery)
                
                // Create a new list reference to trigger UI updates
                _categories = _categories.toMutableList()
                
                saveToStorage()
                break
            }
        }
    }

    fun updateGrocery(grocery: Grocery) {
        for (category in _categories) {
            val subCategoryIndex = category.subCategories.indexOfFirst { it.subCategory.uuid == grocery.subCategoryId }
            if (subCategoryIndex != -1) {
                val groceryIndex = category.subCategories[subCategoryIndex].groceries.indexOfFirst { it.uuid == grocery.uuid }
                if (groceryIndex != -1) {
                    category.subCategories[subCategoryIndex].groceries[groceryIndex] = grocery
                    
                    // Create a new list reference to trigger UI updates
                    _categories = _categories.toMutableList()
                    
                    saveToStorage()
                    break
                }
            }
        }
    }

    fun deleteGrocery(groceryId: String) {
        for (category in _categories) {
            for (subCategory in category.subCategories) {
                val groceryIndex = subCategory.groceries.indexOfFirst { it.uuid == groceryId }
                if (groceryIndex != -1) {
                    subCategory.groceries.removeAt(groceryIndex)
                    
                    // Create a new list reference to trigger UI updates
                    _categories = _categories.toMutableList()
                    
                    saveToStorage()
                    return
                }
            }
        }
    }

    // Search methods
    fun findCategory(categoryId: String): CategoryWithSubCategories? {
        return _categories.find { it.category.uuid == categoryId }
    }

    fun findSubCategory(subCategoryId: String): SubCategoryWithGroceries? {
        for (category in _categories) {
            val subCategory = category.subCategories.find { it.subCategory.uuid == subCategoryId }
            if (subCategory != null) return subCategory
        }
        return null
    }

    fun findGrocery(groceryId: String): Grocery? {
        for (category in _categories) {
            for (subCategory in category.subCategories) {
                val grocery = subCategory.groceries.find { it.uuid == groceryId }
                if (grocery != null) return grocery
            }
        }
        return null
    }

    // Get filtered lists
    fun getCategoriesSorted(): List<CategoryWithSubCategories> = categories

    fun getSubCategoriesForCategory(categoryId: String): List<SubCategoryWithGroceries> {
        return findCategory(categoryId)?.subCategories ?: emptyList()
    }

    fun getGroceriesForSubCategory(subCategoryId: String): List<Grocery> {
        return findSubCategory(subCategoryId)?.groceries ?: emptyList()
    }

    fun getAllGroceries(): List<Grocery> {
        return _categories.flatMap { category ->
            category.subCategories.flatMap { it.groceries }
        }
    }

    private fun saveToStorage() {
        // Save to local storage first
        val allCategories = _categories.map { it.category }
        val allSubCategories = _categories.flatMap { it.subCategories.map { sub -> sub.subCategory } }
        val allGroceries = _categories.flatMap { it.subCategories.flatMap { sub -> sub.groceries } }

        localStorageManager.saveCategories(allCategories)
        localStorageManager.saveSubCategories(allSubCategories)
        localStorageManager.saveGroceries(allGroceries)

        // Then save to Firebase
        coroutineScope.launch {
            try {
                allCategories.forEach { firebaseManager.saveCategory(it) }
                allSubCategories.forEach { firebaseManager.saveSubCategory(it) }
                allGroceries.forEach { firebaseManager.saveGrocery(it) }
            } catch (e: Exception) {
                // Firebase failure doesn't affect local state
            }
        }
    }

    fun refreshFromLocalStorage() {
        loadFromLocalStorage()
    }
}
