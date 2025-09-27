package com.example.supercart2.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

object FirebaseManager {
    private val db = FirebaseFirestore.getInstance()
    private const val CATEGORIES_COLLECTION = "categories"
    
    // Simple connection test
    suspend fun testConnection(): Boolean {
        return try {
            db.collection(CATEGORIES_COLLECTION).limit(1).get().await()
            true
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Connection test failed", e)
            false
        }
    }

    // Upload all data from DataManagerObject
    suspend fun uploadData() {
        try {
            val batch = db.batch()
            DataManagerObject.categories.forEach { category ->
                val docRef = db.collection(CATEGORIES_COLLECTION).document(category.category.uuid)
                batch.set(docRef, category, SetOptions.merge())
            }
            batch.commit().await()
            android.util.Log.d("FirebaseManager", "Data uploaded successfully")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error uploading data", e)
            throw e
        }
    }

    // Download data to DataManagerObject
    suspend fun downloadData() {
        try {
            val snapshot = db.collection(CATEGORIES_COLLECTION)
                .get()
                .await()
            
            val categories = snapshot.toObjects(CategoryWithSubCategories::class.java)
            
            DataManagerObject.categories.clear()
            DataManagerObject.categories.addAll(categories)
            
            // Save to local storage
            DataStoreManager.saveDataGlobally()
            
            android.util.Log.d("FirebaseManager", "Data downloaded successfully")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error downloading data", e)
            throw e
        }
    }

    // Listen for remote changes
    fun startListening() {
        db.collection(CATEGORIES_COLLECTION)
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
    }
}
