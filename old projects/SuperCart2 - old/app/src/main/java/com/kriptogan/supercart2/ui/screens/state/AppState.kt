package com.kriptogan.supercart2.ui.screens.state

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import com.kriptogan.supercart2.classes.Category
import com.kriptogan.supercart2.classes.FirebaseManager
import com.kriptogan.supercart2.classes.LocalStorageManager
import kotlinx.coroutines.launch

/**
 * Simple AppState that follows the KISS principle:
 * - Single source of truth for categories
 * - Direct state updates
 * - No complex callback mechanisms
 */
@Composable
fun rememberAppState(
    context: Context,
    firebaseManager: FirebaseManager
): AppState {
    val localStorageManager = remember { LocalStorageManager(context) }
    val coroutineScope = rememberCoroutineScope()
    
    // Single source of truth for categories - follows the rules
    var categories by remember { mutableStateOf(localStorageManager.getCategories()) }
    
    return remember {
        AppState(
            localStorageManager = localStorageManager,
            firebaseManager = firebaseManager,
            coroutineScope = coroutineScope,
            categories = categories,
            onCategoriesUpdated = { newCategories -> 
                categories = newCategories 
            }
        )
    }
}

class AppState(
    private val localStorageManager: LocalStorageManager,
    private val firebaseManager: FirebaseManager,
    private val coroutineScope: kotlinx.coroutines.CoroutineScope,
    private val categories: List<Category>,
    private val onCategoriesUpdated: (List<Category>) -> Unit
) {
    
    /**
     * Get current categories - direct access to state
     */
    fun getCategories(): List<Category> = categories
    
    /**
     * Save a new category - follows the working pattern from MainActivity
     */
    fun saveCategory(category: Category) {
        // Step 1: Save to local storage
        localStorageManager.addCategory(category)
        
        // Step 2: Update state directly (single source of truth)
        val updatedCategories = localStorageManager.getCategories()
        onCategoriesUpdated(updatedCategories)
        
        // Step 3: Save to Firebase asynchronously (don't wait)
        coroutineScope.launch {
            try {
                firebaseManager.saveCategory(category)
            } catch (e: Exception) {
                // Firebase failure doesn't affect local state
            }
        }
    }
    
    /**
     * Delete all categories - follows the working pattern
     */
    fun deleteAllCategories() {
        coroutineScope.launch {
            try {
                // Get existing categories before clearing
                val existingCategories = localStorageManager.getCategories()
                
                // Clear from Firebase first
                existingCategories.forEach { category ->
                    firebaseManager.deleteCategory(category.uuid)
                }
                
                // Then clear from local storage
                localStorageManager.clearAllData()
                
                // Update state directly
                onCategoriesUpdated(emptyList())
            } catch (e: Exception) {
                // Handle error if needed
            }
        }
    }
    
    /**
     * Sync with Firebase - follows the working pattern
     */
    fun syncWithFirebase() {
        coroutineScope.launch {
            try {
                val firebaseCategories = firebaseManager.getCategories()
                val localCategories = localStorageManager.getCategories()
                
                // Merge and deduplicate categories
                val mergedCategories = (localCategories + firebaseCategories)
                    .distinctBy { it.uuid }
                    .sortedBy { it.viewOrder }
                
                // Update local storage with merged data
                localStorageManager.saveCategories(mergedCategories)
                
                // Update state directly
                onCategoriesUpdated(mergedCategories)
                
            } catch (e: Exception) {
                // If Firebase fails, keep local categories
                onCategoriesUpdated(localStorageManager.getCategories())
            }
        }
    }
    
    /**
     * Refresh categories from local storage
     */
    fun refreshCategories() {
        val currentCategories = localStorageManager.getCategories()
        onCategoriesUpdated(currentCategories)
    }
    
    /**
     * Get localStorageManager for components that need it
     */
    fun getLocalStorageManager() = localStorageManager
    
    /**
     * Get FirebaseManager for components that need it
     */
    fun getFirebaseManager() = firebaseManager
}
