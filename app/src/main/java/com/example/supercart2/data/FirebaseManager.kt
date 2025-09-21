package com.example.supercart2.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.example.supercart2.models.Category
import com.example.supercart2.models.SubCategory
import com.example.supercart2.models.Grocery
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot

object FirebaseManager {
    private val db = FirebaseFirestore.getInstance()
    private const val CATEGORIES_COLLECTION = "categories"
    private const val GROCERIES_COLLECTION = "groceries"
    
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

    // Category Operations
    suspend fun saveCategory(category: CategoryWithSubCategories) {
        try {
            db.collection(CATEGORIES_COLLECTION)
                .document(category.category.uuid)
                .set(category, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error saving category", e)
            throw e
        }
    }

    suspend fun saveAllCategories(categories: List<CategoryWithSubCategories>) {
        try {
            val batch = db.batch()
            categories.forEach { category ->
                val docRef = db.collection(CATEGORIES_COLLECTION).document(category.category.uuid)
                batch.set(docRef, category, SetOptions.merge())
            }
            batch.commit().await()
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error saving all categories", e)
            throw e
        }
    }

    suspend fun getCategory(uuid: String): CategoryWithSubCategories? {
        return try {
            val doc = db.collection(CATEGORIES_COLLECTION)
                .document(uuid)
                .get()
                .await()
            doc.toObject(CategoryWithSubCategories::class.java)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error getting category", e)
            null
        }
    }

    suspend fun getAllCategories(): List<CategoryWithSubCategories> {
        return try {
            db.collection(CATEGORIES_COLLECTION)
                .get()
                .await()
                .toObjects(CategoryWithSubCategories::class.java)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error getting all categories", e)
            emptyList()
        }
    }

    suspend fun deleteCategory(uuid: String) {
        try {
            db.collection(CATEGORIES_COLLECTION)
                .document(uuid)
                .delete()
                .await()
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error deleting category", e)
            throw e
        }
    }

    // Grocery Operations
    suspend fun saveGrocery(grocery: Grocery) {
        try {
            db.collection(GROCERIES_COLLECTION)
                .document(grocery.uuid)
                .set(grocery, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error saving grocery", e)
            throw e
        }
    }

    suspend fun getGrocery(uuid: String): Grocery? {
        return try {
            val doc = db.collection(GROCERIES_COLLECTION)
                .document(uuid)
                .get()
                .await()
            doc.toObject(Grocery::class.java)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error getting grocery", e)
            null
        }
    }

    suspend fun deleteGrocery(uuid: String) {
        try {
            db.collection(GROCERIES_COLLECTION)
                .document(uuid)
                .delete()
                .await()
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error deleting grocery", e)
            throw e
        }
    }

    // Sync Operations
    suspend fun syncWithFirebase() {
        try {
            // Get all data from Firebase
            val firebaseCategories = getAllCategories()
            
            // Update local data
            DataManagerObject.categories.clear()
            DataManagerObject.categories.addAll(firebaseCategories)
            
            // Save to DataStore
            DataStoreManager.saveDataGlobally()
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error syncing with Firebase", e)
            throw e
        }
    }

    suspend fun uploadLocalData() {
        try {
            // Get all local categories
            val localCategories = DataManagerObject.categories.toList()
            
            // Upload to Firebase
            saveAllCategories(localCategories)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error uploading local data", e)
            throw e
        }
    }

    // Real-time updates
    fun observeCategories(): Flow<List<CategoryWithSubCategories>> = flow {
        try {
            val snapshot = db.collection(CATEGORIES_COLLECTION)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        android.util.Log.e("FirebaseManager", "Listen failed", e)
                        return@addSnapshotListener
                    }

                    snapshot?.let {
                        val categories = it.toObjects(CategoryWithSubCategories::class.java)
                        DataManagerObject.categories.clear()
                        DataManagerObject.categories.addAll(categories)
                    }
                }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error observing categories", e)
        }
    }
}
