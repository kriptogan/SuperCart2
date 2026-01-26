package com.example.supercart2.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime

/**
 * Handles data operations that need to be synchronized across both storage systems
 */
object DataOperations {
    
    /**
     * Marks a category as deleted and updates all related items
     */
    suspend fun deleteCategory(categoryId: String) {
        withContext(Dispatchers.IO) {
            try {
                // Find the category in DataManagerObject
                DataManagerObject.categories.find { it.category.uuid == categoryId }?.let { categoryWithSubs ->
                    // Create updated category with deleted = true and new lastUpdate
                    val updatedCategory = categoryWithSubs.category.copy(
                        deleted = true,
                        lastUpdate = LocalDateTime.now()
                    )
                    
                    // Update Firebase
                    FirebaseManager.db.collection(FirebaseManager.CATEGORIES_COLLECTION)
                        .document(categoryId)
                        .set(updatedCategory)
                        .await()
                    
                    // Update local data
                    val categoryIndex = DataManagerObject.categories.indexOfFirst { it.category.uuid == categoryId }
                    if (categoryIndex >= 0) {
                        DataManagerObject.categories[categoryIndex] = categoryWithSubs.copy(category = updatedCategory)
                    }
                    
                    // Save to DataStore
                    DataStoreManager.saveDataGlobally()
                }
                
                android.util.Log.d("DataOperations", "Category marked as deleted successfully")
            } catch (e: Exception) {
                android.util.Log.e("DataOperations", "Error marking category as deleted", e)
                throw e
            }
        }
    }
    
    /**
     * Marks a subcategory as deleted
     */
    suspend fun deleteSubCategory(subCategoryId: String) {
        withContext(Dispatchers.IO) {
            try {
                // Find the subcategory
                DataManagerObject.categories.forEach { categoryWithSubs ->
                    categoryWithSubs.subCategories.find { it.subCategory.uuid == subCategoryId }?.let { subCategoryWithGroceries ->
                        // Create updated subcategory
                        val updatedSubCategory = subCategoryWithGroceries.subCategory.copy(
                            deleted = true,
                            lastUpdate = LocalDateTime.now()
                        )
                        
                        // Update Firebase
                        FirebaseManager.db.collection(FirebaseManager.SUBCATEGORIES_COLLECTION)
                            .document(subCategoryId)
                            .set(updatedSubCategory)
                            .await()
                        
                        // Update local data
                        val subCategoryIndex = categoryWithSubs.subCategories.indexOfFirst { 
                            it.subCategory.uuid == subCategoryId 
                        }
                        if (subCategoryIndex >= 0) {
                            categoryWithSubs.subCategories[subCategoryIndex] = 
                                subCategoryWithGroceries.copy(subCategory = updatedSubCategory)
                        }
                        
                        // Save to DataStore
                        DataStoreManager.saveDataGlobally()
                        return@forEach
                    }
                }
                
                android.util.Log.d("DataOperations", "Subcategory marked as deleted successfully")
            } catch (e: Exception) {
                android.util.Log.e("DataOperations", "Error marking subcategory as deleted", e)
                throw e
            }
        }
    }
    
    /**
     * Marks a grocery item as deleted
     */
    suspend fun deleteGrocery(groceryId: String) {
        withContext(Dispatchers.IO) {
            try {
                // Find the grocery item
                DataManagerObject.categories.forEach { categoryWithSubs ->
                    categoryWithSubs.subCategories.forEach { subCategoryWithGroceries ->
                        subCategoryWithGroceries.groceries.find { it.uuid == groceryId }?.let { grocery ->
                            // Create updated grocery
                            val updatedGrocery = grocery.copy(
                                deleted = true,
                                lastUpdate = LocalDateTime.now()
                            )
                            
                            // Update Firebase
                            FirebaseManager.db.collection(FirebaseManager.GROCERIES_COLLECTION)
                                .document(groceryId)
                                .set(updatedGrocery)
                                .await()
                            
                            // Update local data
                            val groceryIndex = subCategoryWithGroceries.groceries.indexOfFirst { 
                                it.uuid == groceryId 
                            }
                            if (groceryIndex >= 0) {
                                subCategoryWithGroceries.groceries[groceryIndex] = updatedGrocery
                            }
                            
                            // Save to DataStore
                            DataStoreManager.saveDataGlobally()
                            return@forEach
                        }
                    }
                }
                
                android.util.Log.d("DataOperations", "Grocery marked as deleted successfully")
            } catch (e: Exception) {
                android.util.Log.e("DataOperations", "Error marking grocery as deleted", e)
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