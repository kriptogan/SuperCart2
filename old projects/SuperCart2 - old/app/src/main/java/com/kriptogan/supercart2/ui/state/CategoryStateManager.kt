package com.kriptogan.supercart2.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import com.kriptogan.supercart2.classes.Category
import com.kriptogan.supercart2.classes.CategoryDataManager
import com.kriptogan.supercart2.classes.FirebaseManager
import com.kriptogan.supercart2.classes.LocalStorageManager
import com.kriptogan.supercart2.classes.SubCategory
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope

@Composable
fun rememberCategoryState(
    localStorageManager: LocalStorageManager,
    firebaseManager: FirebaseManager
): CategoryStateManager {
    val coroutineScope = rememberCoroutineScope()
    val categoryDataManager = remember { CategoryDataManager(localStorageManager, firebaseManager, coroutineScope) }
    
    return remember {
        CategoryStateManager(
            localStorageManager = localStorageManager,
            firebaseManager = firebaseManager,
            categoryDataManager = categoryDataManager,
            coroutineScope = coroutineScope
        )
    }
}

class CategoryStateManager(
    private val localStorageManager: LocalStorageManager,
    private val firebaseManager: FirebaseManager,
    private val categoryDataManager: CategoryDataManager,
    private val coroutineScope: CoroutineScope
) {
    var categories by mutableStateOf(categoryDataManager.getCategoriesSorted())
    var showEditCategoryDialog by mutableStateOf(false)
    var categoryToEdit by mutableStateOf<Category?>(null)

    init {
        refreshCategories()
    }

    fun refreshCategories() {
        categories = categoryDataManager.getCategoriesSorted()
    }

    val onSaveCategory: (Category) -> Unit = { category ->
        categoryDataManager.addCategory(category)
        refreshCategories()
    }

    val onUpdateCategory: (Category) -> Unit = { category ->
        categoryDataManager.updateCategory(category)
        refreshCategories()
    }

    val onDeleteCategory: (String) -> Unit = { categoryId ->
        categoryDataManager.deleteCategory(categoryId)
        refreshCategories()
    }

    val onDeleteAllCategories: () -> Unit = {
        coroutineScope.launch {
            try {
                // Clear all data from the data manager
                val allCategories = categoryDataManager.getCategoriesSorted()
                allCategories.forEach { categoryWithSubs ->
                    categoryDataManager.deleteCategory(categoryWithSubs.category.uuid)
                }
                
                // Also clear from Firebase using individual delete methods
                val allSubCategories = categoryDataManager.getAllGroceries().map { it.subCategoryId }.distinct().mapNotNull { categoryDataManager.findSubCategory(it) }
                val allGroceries = categoryDataManager.getAllGroceries()
                
                // Delete all groceries first
                allGroceries.forEach { grocery ->
                    try {
                        firebaseManager.deleteGrocery(grocery.uuid)
                    } catch (e: Exception) {
                        // Continue with other deletions even if one fails
                    }
                }
                
                // Delete all sub-categories
                allSubCategories.forEach { subCategoryWithGroceries ->
                    try {
                        firebaseManager.deleteSubCategory(subCategoryWithGroceries.subCategory.uuid)
                    } catch (e: Exception) {
                        // Continue with other deletions even if one fails
                    }
                }
                
                // Delete all categories
                allCategories.forEach { categoryWithSubs ->
                    try {
                        firebaseManager.deleteCategory(categoryWithSubs.category.uuid)
                    } catch (e: Exception) {
                        // Continue with other deletions even if one fails
                    }
                }
                
                refreshCategories()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    val onShowEditDialog: (Category) -> Unit = { category ->
        categoryToEdit = category
        showEditCategoryDialog = true
    }

    val onHideEditDialog: () -> Unit = {
        showEditCategoryDialog = false
        categoryToEdit = null
    }

    // Sub-category operations
    val onAddSubCategory: (SubCategory) -> Unit = { subCategory ->
        categoryDataManager.addSubCategory(subCategory)
        refreshCategories()
    }

    val onUpdateSubCategory: (SubCategory) -> Unit = { subCategory ->
        categoryDataManager.updateSubCategory(subCategory)
        refreshCategories()
    }

    val onDeleteSubCategory: (String) -> Unit = { subCategoryId ->
        categoryDataManager.deleteSubCategory(subCategoryId)
        refreshCategories()
    }

    // Grocery operations
    val onAddGrocery: (com.kriptogan.supercart2.classes.Grocery) -> Unit = { grocery ->
        categoryDataManager.addGrocery(grocery)
        refreshCategories()
    }

    val onUpdateGrocery: (com.kriptogan.supercart2.classes.Grocery) -> Unit = { grocery ->
        categoryDataManager.updateGrocery(grocery)
        refreshCategories()
    }

    val onDeleteGrocery: (String) -> Unit = { groceryId ->
        categoryDataManager.deleteGrocery(groceryId)
        refreshCategories()
    }

    // Getter methods for UI components
    fun getCategoriesList() = categoryDataManager.getCategoriesSorted()
    fun getSubCategories() = categoryDataManager.getAllGroceries().map { it.subCategoryId }.distinct().mapNotNull { categoryDataManager.findSubCategory(it) }
    fun getGroceries() = categoryDataManager.getAllGroceries()
    fun getGroceriesForSubCategory(subCategoryId: String) = categoryDataManager.getGroceriesForSubCategory(subCategoryId)
}
