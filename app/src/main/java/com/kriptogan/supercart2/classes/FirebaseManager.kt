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
}
