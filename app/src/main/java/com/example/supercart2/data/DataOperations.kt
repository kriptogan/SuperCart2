package com.example.supercart2.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

/**
 * Handles data operations that need to be synchronized across both storage systems
 */
object DataOperations {
    
    /**
     * Deletes a category and all its related items from both local and Firebase storage
     */
    suspend fun deleteCategory(categoryId: String) {
        withContext(Dispatchers.IO) {
            try {
                // Delete from Firebase first (includes cascade deletes)
                FirebaseManager.deleteCategory(categoryId)
                
                // Update local data
                val updatedCategories = DataManagerObject.categories.filter { 
                    it.category.uuid != categoryId 
                }
                DataManagerObject.categories.clear()
                DataManagerObject.categories.addAll(updatedCategories)
                
                // Save to DataStore
                DataStoreManager.saveDataGlobally()
                
                android.util.Log.d("DataOperations", "Category deleted successfully from all storage systems")
            } catch (e: Exception) {
                android.util.Log.e("DataOperations", "Error deleting category", e)
                throw e
            }
        }
    }
    
    /**
     * Deletes a subcategory and all its groceries from both local and Firebase storage
     */
    suspend fun deleteSubCategory(subCategoryId: String) {
        withContext(Dispatchers.IO) {
            try {
                // Delete from Firebase first (includes cascade deletes)
                FirebaseManager.deleteSubCategory(subCategoryId)
                
                // Update local data
                DataManagerObject.categories.forEach { categoryWithSubs ->
                    categoryWithSubs.subCategories.removeAll { it.subCategory.uuid == subCategoryId }
                }
                
                // Save to DataStore
                DataStoreManager.saveDataGlobally()
                
                android.util.Log.d("DataOperations", "Subcategory deleted successfully from all storage systems")
            } catch (e: Exception) {
                android.util.Log.e("DataOperations", "Error deleting subcategory", e)
                throw e
            }
        }
    }
    
    /**
     * Deletes a grocery item from both local and Firebase storage
     */
    suspend fun deleteGrocery(groceryId: String) {
        withContext(Dispatchers.IO) {
            try {
                // Delete from Firebase
                FirebaseManager.deleteGrocery(groceryId)
                
                // Update local data
                DataManagerObject.categories.forEach { categoryWithSubs ->
                    categoryWithSubs.subCategories.forEach { subCategoryWithGroceries ->
                        subCategoryWithGroceries.groceries.removeAll { it.uuid == groceryId }
                    }
                }
                
                // Save to DataStore
                DataStoreManager.saveDataGlobally()
                
                android.util.Log.d("DataOperations", "Grocery deleted successfully from all storage systems")
            } catch (e: Exception) {
                android.util.Log.e("DataOperations", "Error deleting grocery", e)
                throw e
            }
        }
    }
    
    /**
     * Synchronizes data between local storage and Firebase
     */
    suspend fun syncData(uploadToFirebase: Boolean) {
        withContext(Dispatchers.IO) {
            try {
                if (uploadToFirebase) {
                    FirebaseManager.uploadData()
                } else {
                    FirebaseManager.downloadData()
                }
                android.util.Log.d("DataOperations", "Data sync completed successfully")
            } catch (e: Exception) {
                android.util.Log.e("DataOperations", "Error syncing data", e)
                throw e
            }
        }
    }
}