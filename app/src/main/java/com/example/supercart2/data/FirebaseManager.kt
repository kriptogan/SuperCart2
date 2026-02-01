package com.example.supercart2.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import com.example.supercart2.models.Category
import com.example.supercart2.models.SubCategory
import com.example.supercart2.models.Grocery
import com.example.supercart2.models.Store
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

object FirebaseManager {
    val db = FirebaseFirestore.getInstance() // Made public
    
    // Made collections public
    const val CATEGORIES_COLLECTION = "categories"
    const val SUBCATEGORIES_COLLECTION = "subcategories"
    const val GROCERIES_COLLECTION = "groceries"
    const val STORES_COLLECTION = "stores"
    
    /**
     * Get Firestore collection reference for a group
     */
    private fun getGroupCollection(groupCode: String, collection: String): com.google.firebase.firestore.CollectionReference {
        return db.collection("groups")
            .document(groupCode)
            .collection(collection)
    }
    
    /**
     * Check if a group exists in Firestore
     * Returns true if group has at least one document
     */
    suspend fun checkGroupExists(groupCode: String): Boolean {
        return try {
            android.util.Log.d("FirebaseManager", "Checking if group exists: $groupCode")
            
            // Check if any categories exist in this group
            val snapshot = getGroupCollection(groupCode, CATEGORIES_COLLECTION)
                .limit(1)
                .get()
                .await()
            
            val exists = !snapshot.isEmpty
            android.util.Log.d("FirebaseManager", "Group exists: $exists")
            exists
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error checking group existence", e)
            false
        }
    }
    
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

    // Helper function to convert Category to Map for Firestore (with date serialization)
    private fun categoryToMap(category: Category): Map<String, Any?> {
        return mapOf(
            "uuid" to category.uuid,
            "name" to category.name,
            "default" to category.default,
            "viewOrder" to category.viewOrder,
            "protected" to category.protected,
            "lastUpdate" to category.lastUpdate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            "deleted" to category.deleted
        )
    }
    
    // Helper function to convert SubCategory to Map for Firestore (with date serialization)
    private fun subCategoryToMap(subCategory: SubCategory): Map<String, Any?> {
        return mapOf(
            "uuid" to subCategory.uuid,
            "categoryId" to subCategory.categoryId,
            "name" to subCategory.name,
            "protected" to subCategory.protected,
            "lastUpdate" to subCategory.lastUpdate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            "deleted" to subCategory.deleted
        )
    }
    
    // Helper function to convert Grocery to Map for Firestore (with date serialization)
    private fun groceryToMap(grocery: Grocery): Map<String, Any?> {
        return mapOf(
            "uuid" to grocery.uuid,
            "name" to grocery.name,
            "categoryId" to grocery.categoryId,
            "subCategoryId" to grocery.subCategoryId,
            "expirationDate" to (grocery.expirationDate?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: null),
            "inShoppingList" to grocery.inShoppingList,
            "isBought" to grocery.isBought,
            "buyEvents" to grocery.buyEvents.map { it.format(DateTimeFormatter.ISO_LOCAL_DATE) },
            "imageUUID" to grocery.imageUUID,
            "averageBuyDays" to grocery.averageBuyDays,
            "storeIds" to grocery.storeIds,
            "lastUpdate" to grocery.lastUpdate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            "deleted" to grocery.deleted
        )
    }
    
    // Helper function to convert Store to Map for Firestore (with date serialization)
    private fun storeToMap(store: Store): Map<String, Any?> {
        return mapOf(
            "uuid" to store.uuid,
            "name" to store.name,
            "viewOrder" to store.viewOrder,
            "lastUpdate" to store.lastUpdate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            "deleted" to store.deleted
        )
    }

    suspend fun uploadData() {
        try {
            // Get group code
            val context = DataStoreManager.globalContext
            val groupCode = context?.let { DataStoreManager.loadGroupCode(it) }
            
            if (groupCode == null) {
                android.util.Log.e("FirebaseManager", "❌ Cannot upload: No group code set")
                throw IllegalStateException("Group code required for upload")
            }
            
            android.util.Log.d("FirebaseManager", "Uploading ALL local data to group: $groupCode")
            
            // Convert nested structure to flat lists
            val (categories, subCategories, groceries) = DataConverter.flattenCategories(
                DataManagerObject.categories
            )
            val stores = DataManagerObject.stores.toList()
            
            android.util.Log.d("FirebaseManager", "Uploading: ${categories.size} categories, " +
                "${subCategories.size} subcategories, ${groceries.size} groceries, " +
                "${stores.size} stores")
            
            // Create batch operation
            val batch = db.batch()
            
            // Upload ALL items (including those with deleted: true)
            categories.forEach { category ->
                val docRef = getGroupCollection(groupCode, CATEGORIES_COLLECTION)
                    .document(category.uuid)
                batch.set(docRef, categoryToMap(category))
            }
            
            subCategories.forEach { subCategory ->
                val docRef = getGroupCollection(groupCode, SUBCATEGORIES_COLLECTION)
                    .document(subCategory.uuid)
                batch.set(docRef, subCategoryToMap(subCategory))
            }
            
            groceries.forEach { grocery ->
                val docRef = getGroupCollection(groupCode, GROCERIES_COLLECTION)
                    .document(grocery.uuid)
                batch.set(docRef, groceryToMap(grocery))
            }
            
            stores.forEach { store ->
                val docRef = getGroupCollection(groupCode, STORES_COLLECTION)
                    .document(store.uuid)
                batch.set(docRef, storeToMap(store))
            }
            
            // Commit batch
            batch.commit().await()
            
            android.util.Log.d("FirebaseManager", "✅ Data uploaded successfully to group: $groupCode")
            
            // Batch upload images to Firebase Storage
            if (context != null) {
                try {
                    android.util.Log.d("FirebaseManager", "Starting image upload...")
                    val uploadedCount = FirebaseStorageManager.batchUploadImages(context)
                    android.util.Log.d("FirebaseManager", "✅ Uploaded $uploadedCount images")
                } catch (e: Exception) {
                    android.util.Log.e("FirebaseManager", "❌ Error uploading images", e)
                    // Don't throw - allow data upload to succeed even if images fail
                }
            } else {
                android.util.Log.w("FirebaseManager", "⚠️ Cannot upload images: context is null")
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error uploading data", e)
            throw e
        }
    }

    // Helper function to convert Firestore Timestamp to LocalDateTime
    private fun timestampToLocalDateTime(timestamp: Any?): LocalDateTime {
        return when (timestamp) {
            is Timestamp -> timestamp.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
            is String -> LocalDateTime.parse(timestamp)
            is com.google.protobuf.Timestamp -> {
                val millis = timestamp.seconds * 1000 + timestamp.nanos / 1_000_000
                java.time.Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime()
            }
            else -> LocalDateTime.now()
        }
    }
    
    // Helper function to convert Firestore value to LocalDate
    private fun toLocalDate(value: Any?): LocalDate? {
        if (value == null) return null
        return when (value) {
            is String -> LocalDate.parse(value)
            is Timestamp -> value.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            else -> null
        }
    }
    
    // Helper function to convert Firestore value to List<LocalDate>
    private fun toLocalDateList(value: Any?): List<LocalDate> {
        if (value == null) return emptyList()
        return when (value) {
            is List<*> -> value.mapNotNull { item ->
                when (item) {
                    is String -> LocalDate.parse(item)
                    is Timestamp -> item.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                    else -> null
                }
            }
            else -> emptyList()
        }
    }
    
    // Helper function to deserialize Category from Firestore document
    private fun documentToCategory(doc: DocumentSnapshot): Category {
        val data = doc.data ?: throw IllegalStateException("Document ${doc.id} has no data")
        return Category(
            uuid = doc.id,
            name = data["name"] as? String ?: throw IllegalStateException("Category ${doc.id} missing name"),
            default = (data["default"] as? Boolean) ?: false,
            viewOrder = (data["viewOrder"] as? Long)?.toInt() ?: 0,
            protected = (data["protected"] as? Boolean) ?: false,
            lastUpdate = timestampToLocalDateTime(data["lastUpdate"]),
            deleted = (data["deleted"] as? Boolean) ?: false
        )
    }
    
    // Helper function to deserialize SubCategory from Firestore document
    private fun documentToSubCategory(doc: DocumentSnapshot): SubCategory {
        val data = doc.data ?: throw IllegalStateException("Document ${doc.id} has no data")
        return SubCategory(
            uuid = doc.id,
            categoryId = data["categoryId"] as? String ?: throw IllegalStateException("SubCategory ${doc.id} missing categoryId"),
            name = data["name"] as? String ?: throw IllegalStateException("SubCategory ${doc.id} missing name"),
            protected = (data["protected"] as? Boolean) ?: false,
            lastUpdate = timestampToLocalDateTime(data["lastUpdate"]),
            deleted = (data["deleted"] as? Boolean) ?: false
        )
    }
    
    // Helper function to deserialize Grocery from Firestore document
    private fun documentToGrocery(doc: DocumentSnapshot): Grocery {
        val data = doc.data ?: throw IllegalStateException("Document ${doc.id} has no data")
        
        // Handle storeIds as List<String>
        val storeIds = when (val value = data["storeIds"]) {
            is List<*> -> value.filterIsInstance<String>()
            else -> emptyList()
        }
        
        return Grocery(
            uuid = doc.id,
            name = data["name"] as? String ?: throw IllegalStateException("Grocery ${doc.id} missing name"),
            categoryId = data["categoryId"] as? String ?: throw IllegalStateException("Grocery ${doc.id} missing categoryId"),
            subCategoryId = data["subCategoryId"] as? String ?: throw IllegalStateException("Grocery ${doc.id} missing subCategoryId"),
            expirationDate = toLocalDate(data["expirationDate"]),
            inShoppingList = (data["inShoppingList"] as? Boolean) ?: false,
            isBought = (data["isBought"] as? Boolean) ?: false,
            buyEvents = toLocalDateList(data["buyEvents"]),
            imageUUID = data["imageUUID"] as? String,
            averageBuyDays = (data["averageBuyDays"] as? Long)?.toInt(),
            storeIds = storeIds,
            lastUpdate = timestampToLocalDateTime(data["lastUpdate"]),
            deleted = (data["deleted"] as? Boolean) ?: false
        )
    }
    
    // Helper function to deserialize Store from Firestore document
    private fun documentToStore(doc: DocumentSnapshot): Store {
        val data = doc.data ?: throw IllegalStateException("Document ${doc.id} has no data")
        return Store(
            uuid = doc.id,
            name = data["name"] as? String ?: throw IllegalStateException("Store ${doc.id} missing name"),
            viewOrder = (data["viewOrder"] as? Long)?.toInt() ?: 0,
            lastUpdate = timestampToLocalDateTime(data["lastUpdate"]),
            deleted = (data["deleted"] as? Boolean) ?: false
        )
    }

    suspend fun downloadData() {
        try {
            // Get group code
            val context = DataStoreManager.globalContext
            val groupCode = context?.let { DataStoreManager.loadGroupCode(it) }
            
            if (groupCode == null) {
                android.util.Log.e("FirebaseManager", "❌ Cannot download: No group code set")
                throw IllegalStateException("Group code required for download")
            }
            
            android.util.Log.d("FirebaseManager", "Downloading data from group: $groupCode")
            
            // Download from group-based paths
            val categoriesSnapshot = getGroupCollection(groupCode, CATEGORIES_COLLECTION).get().await()
            val downloadedCategories = categoriesSnapshot.documents.map { documentToCategory(it) }
            
            val subCategoriesSnapshot = getGroupCollection(groupCode, SUBCATEGORIES_COLLECTION).get().await()
            val downloadedSubCategories = subCategoriesSnapshot.documents.map { documentToSubCategory(it) }
            
            val groceriesSnapshot = getGroupCollection(groupCode, GROCERIES_COLLECTION).get().await()
            val downloadedGroceries = groceriesSnapshot.documents.map { documentToGrocery(it) }
            
            val storesSnapshot = getGroupCollection(groupCode, STORES_COLLECTION).get().await()
            val downloadedStores = storesSnapshot.documents.map { documentToStore(it) }
            
            android.util.Log.d("FirebaseManager", "Downloaded: ${downloadedCategories.size} categories, " +
                "${downloadedSubCategories.size} subcategories, ${downloadedGroceries.size} groceries, " +
                "${downloadedStores.size} stores")
            
            // Validate relationships
            if (DataConverter.validateRelationships(downloadedCategories, downloadedSubCategories, downloadedGroceries)) {
                // Build nested structure
                val nestedCategories = DataConverter.buildNestedStructure(
                    downloadedCategories,
                    downloadedSubCategories,
                    downloadedGroceries
                )
                
                // Replace ALL local data with downloaded data (including deleted items)
                DataManagerObject.categories.clear()
                DataManagerObject.categories.addAll(nestedCategories)
                
                DataManagerObject.stores.clear()
                DataManagerObject.stores.addAll(downloadedStores)  // Keep deleted items too
                
                DataManagerObject.updateData()
                DataStoreManager.saveDataGlobally()
                
                android.util.Log.d("FirebaseManager", "✅ Data downloaded and replaced successfully")
                
                // Batch download images from Firebase Storage
                if (context != null) {
                    try {
                        android.util.Log.d("FirebaseManager", "Starting image download...")
                        val downloadedCount = FirebaseStorageManager.batchDownloadImages(context)
                        android.util.Log.d("FirebaseManager", "✅ Downloaded $downloadedCount images")
                    } catch (e: Exception) {
                        android.util.Log.e("FirebaseManager", "❌ Error downloading images", e)
                        // Don't throw - allow data download to succeed even if images fail
                    }
                } else {
                    android.util.Log.w("FirebaseManager", "⚠️ Cannot download images: context is null")
                }
            } else {
                throw IllegalStateException("Invalid data relationships in downloaded data")
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