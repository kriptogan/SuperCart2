package com.kriptogan.supercart2.ui.state

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.Composable
import com.kriptogan.supercart2.classes.Category
import com.kriptogan.supercart2.classes.FirebaseManager
import com.kriptogan.supercart2.classes.LocalStorageManager
import kotlinx.coroutines.launch

@Composable
fun rememberCategoryState(
    context: Context,
    firebaseManager: FirebaseManager
): CategoryState {
    val localStorageManager = LocalStorageManager(context)
    var categories by remember { mutableStateOf(localStorageManager.getCategories()) }
    var showEditCategoryDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    return CategoryState(
        categories = categories,
        showEditCategoryDialog = showEditCategoryDialog,
        categoryToEdit = categoryToEdit,
        localStorageManager = localStorageManager,
        onCategoriesUpdate = { newCategories -> categories = newCategories },
        onShowEditDialog = { category -> 
            categoryToEdit = category
            showEditCategoryDialog = true
        },
        onHideEditDialog = { 
            showEditCategoryDialog = false
            categoryToEdit = null
        },
        onDeleteAllCategories = {
            coroutineScope.launch {
                try {
                    val existingCategories = localStorageManager.getCategories()
                    existingCategories.forEach { category ->
                        firebaseManager.deleteCategory(category.uuid)
                    }
                    localStorageManager.clearAllData()
                    categories = emptyList()
                } catch (e: Exception) {
                    // Handle error if needed
                }
            }
        },
        onSaveCategory = { category ->
            localStorageManager.addCategory(category)
            categories = localStorageManager.getCategories()
            
            coroutineScope.launch {
                try {
                    firebaseManager.saveCategory(category)
                } catch (e: Exception) {
                    // Firebase failure doesn't affect local state
                }
            }
        },
        onUpdateCategory = { updatedCategory ->
            localStorageManager.updateCategory(updatedCategory)
            categories = localStorageManager.getCategories()
            
            coroutineScope.launch {
                try {
                    firebaseManager.saveCategory(updatedCategory)
                } catch (e: Exception) {
                    // Firebase failure doesn't affect local state
                }
            }
        }
    )
}

data class CategoryState(
    val categories: List<Category>,
    val showEditCategoryDialog: Boolean,
    val categoryToEdit: Category?,
    val localStorageManager: LocalStorageManager,
    val onCategoriesUpdate: (List<Category>) -> Unit,
    val onShowEditDialog: (Category) -> Unit,
    val onHideEditDialog: () -> Unit,
    val onDeleteAllCategories: () -> Unit,
    val onSaveCategory: (Category) -> Unit,
    val onUpdateCategory: (Category) -> Unit
)
