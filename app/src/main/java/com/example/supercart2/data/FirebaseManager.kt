package com.example.supercart2.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import com.example.supercart2.models.Category
import com.example.supercart2.models.SubCategory
import com.example.supercart2.models.Grocery

object FirebaseManager {
    val db = FirebaseFirestore.getInstance() // Made public
    
    // Made collections public
    const val CATEGORIES_COLLECTION = "categories"
    const val SUBCATEGORIES_COLLECTION = "subcategories"
    const val GROCERIES_COLLECTION = "groceries"
    
    // Test Firebase connection
    suspend fun testConnection(): Boolean {
        return try {
            db.collection(CATEGORIES_COLLECTION).limit(1).get().await()
            true
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Connection test failed", e)
            false
        }
    }

    suspend fun uploadData() {
        try {
            // Convert nested structure to flat lists
            val (categories, subCategories, groceries) = DataConverter.flattenCategories(DataManagerObject.categories)
            
            // Create a batch operation
            val batch = db.batch()
            
            // Upload categories
            categories.forEach { category ->
                val docRef = db.collection(CATEGORIES_COLLECTION).document(category.uuid)
                batch.set(docRef, category)
            }
            
            // Upload subcategories
            subCategories.forEach { subCategory ->
                val docRef = db.collection(SUBCATEGORIES_COLLECTION).document(subCategory.uuid)
                batch.set(docRef, subCategory)
            }
            
            // Upload groceries
            groceries.forEach { grocery ->
                val docRef = db.collection(GROCERIES_COLLECTION).document(grocery.uuid)
                batch.set(docRef, grocery)
            }
            
            // Execute the batch
            batch.commit().await()
            
            android.util.Log.d("FirebaseManager", "Data uploaded successfully")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error uploading data", e)
            throw e
        }
    }

    suspend fun downloadData() {
        try {
            // Download all collections
            val categories = db.collection(CATEGORIES_COLLECTION)
                .get()
                .await()
                .toObjects(Category::class.java)
            
            val subCategories = db.collection(SUBCATEGORIES_COLLECTION)
                .get()
                .await()
                .toObjects(SubCategory::class.java)
            
            val groceries = db.collection(GROCERIES_COLLECTION)
                .get()
                .await()
                .toObjects(Grocery::class.java)
            
            // Validate relationships
            if (DataConverter.validateRelationships(categories, subCategories, groceries)) {
                // Convert to nested structure
                val nestedData = DataConverter.buildNestedStructure(categories, subCategories, groceries)
                
                // Update DataManagerObject
                DataManagerObject.categories.clear()
                DataManagerObject.categories.addAll(nestedData)
                
                // Save to DataStore
                DataStoreManager.saveDataGlobally()
                
                android.util.Log.d("FirebaseManager", "Data downloaded and saved successfully")
            } else {
                throw IllegalStateException("Invalid data relationships detected in downloaded data")
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error downloading data", e)
            throw e
        }
    }

    suspend fun deleteCategory(categoryId: String) {
        try {
            // Get all subcategories for this category
            val subCategories = db.collection(SUBCATEGORIES_COLLECTION)
                .whereEqualTo("categoryId", categoryId)
                .get()
                .await()
            
            // Get all groceries for this category
            val groceries = db.collection(GROCERIES_COLLECTION)
                .whereEqualTo("categoryId", categoryId)
                .get()
                .await()
            
            // Create a batch operation
            val batch = db.batch()
            
            // Delete the category
            batch.delete(db.collection(CATEGORIES_COLLECTION).document(categoryId))
            
            // Delete all subcategories
            subCategories.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            
            // Delete all groceries
            groceries.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            
            // Execute the batch
            batch.commit().await()
            
            android.util.Log.d("FirebaseManager", "Category and related items deleted successfully")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error deleting category", e)
            throw e
        }
    }

    suspend fun deleteSubCategory(subCategoryId: String) {
        try {
            // Get all groceries for this subcategory
            val groceries = db.collection(GROCERIES_COLLECTION)
                .whereEqualTo("subCategoryId", subCategoryId)
                .get()
                .await()
            
            // Create a batch operation
            val batch = db.batch()
            
            // Delete the subcategory
            batch.delete(db.collection(SUBCATEGORIES_COLLECTION).document(subCategoryId))
            
            // Delete all groceries
            groceries.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            
            // Execute the batch
            batch.commit().await()
            
            android.util.Log.d("FirebaseManager", "Subcategory and related items deleted successfully")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error deleting subcategory", e)
            throw e
        }
    }

    suspend fun deleteGrocery(groceryId: String) {
        try {
            db.collection(GROCERIES_COLLECTION)
                .document(groceryId)
                .delete()
                .await()
            
            android.util.Log.d("FirebaseManager", "Grocery deleted successfully")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error deleting grocery", e)
            throw e
        }
    }
}