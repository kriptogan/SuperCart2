package com.kriptogan.supercart2.classes

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.tasks.await

/**
 * Manages all Firebase operations for the SuperCart app
 * Handles Firestore database operations, authentication, and analytics
 */
class FirebaseManager {
    
    // Firebase instances
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    
    // Collection names
    companion object {
        const val CATEGORIES_COLLECTION = "categories"
        const val SUBCATEGORIES_COLLECTION = "subcategories"
        const val GROCERIES_COLLECTION = "groceries"
        const val USERS_COLLECTION = "users"
        const val SHARING_GROUPS_COLLECTION = "sharing_groups"
    }
    
    /**
     * Initialize Firebase connection
     */
    fun initialize() {
        // Enable offline persistence
        firestore.firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
    }
    
    /**
     * Check if Firebase is connected
     */
    fun isConnected(): Boolean {
        return try {
            firestore.app != null
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Save category to Firestore
     */
    suspend fun saveCategory(category: Category): Boolean {
        return try {
            firestore.collection(CATEGORIES_COLLECTION)
                .document(category.uuid)
                .set(category)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get all categories from Firestore
     */
    suspend fun getCategories(): List<Category> {
        return try {
            val snapshot = firestore.collection(CATEGORIES_COLLECTION)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Category::class.java)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Update category in Firestore
     */
    suspend fun updateCategory(category: Category): Boolean {
        return try {
            firestore.collection(CATEGORIES_COLLECTION)
                .document(category.uuid)
                .set(category)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Delete category from Firestore
     */
    suspend fun deleteCategory(categoryId: String): Boolean {
        return try {
            firestore.collection(CATEGORIES_COLLECTION)
                .document(categoryId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Save sub-category to Firestore
     */
    suspend fun saveSubCategory(subCategory: SubCategory): Boolean {
        return try {
            firestore.collection(SUBCATEGORIES_COLLECTION)
                .document(subCategory.uuid)
                .set(subCategory)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get all sub-categories from Firestore
     */
    suspend fun getSubCategories(): List<SubCategory> {
        return try {
            val snapshot = firestore.collection(SUBCATEGORIES_COLLECTION)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(SubCategory::class.java)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get sub-categories by category ID from Firestore
     */
    suspend fun getSubCategoriesByCategoryId(categoryId: String): List<SubCategory> {
        return try {
            val snapshot = firestore.collection(SUBCATEGORIES_COLLECTION)
                .whereEqualTo("categoryId", categoryId)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(SubCategory::class.java)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Update sub-category in Firestore
     */
    suspend fun updateSubCategory(subCategory: SubCategory): Boolean {
        return try {
            firestore.collection(SUBCATEGORIES_COLLECTION)
                .document(subCategory.uuid)
                .set(subCategory)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Delete sub-category from Firestore
     */
    suspend fun deleteSubCategory(subCategoryId: String): Boolean {
        return try {
            firestore.collection(SUBCATEGORIES_COLLECTION)
                .document(subCategoryId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
