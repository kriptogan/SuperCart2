package com.example.supercart2.data

import android.content.Context
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import java.io.File

object FirebaseStorageManager {
    private val storage = FirebaseStorage.getInstance()
    private val storageRef: StorageReference = storage.reference
    private const val IMAGES_PATH = "grocery_images"
    
    /**
     * Upload a single image to Firebase Storage
     * Returns true if successful
     */
    suspend fun uploadImage(imageFile: File, uuid: String): Boolean {
        return try {
            val imageRef = storageRef.child("$IMAGES_PATH/$uuid.jpg")
            imageRef.putFile(android.net.Uri.fromFile(imageFile)).await()
            
            android.util.Log.d("FirebaseStorageManager", "Uploaded image: $uuid")
            true
        } catch (e: Exception) {
            android.util.Log.e("FirebaseStorageManager", "Error uploading image: $uuid", e)
            false
        }
    }
    
    /**
     * Download a single image from Firebase Storage
     * Returns the local file or null on error
     */
    suspend fun downloadImage(uuid: String, context: Context): File? {
        return try {
            val imageRef = storageRef.child("$IMAGES_PATH/$uuid.jpg")
            val localFile = ImageManager.getImageFile(uuid, context)
            
            // Download to local file
            imageRef.getFile(localFile).await()
            
            android.util.Log.d("FirebaseStorageManager", "Downloaded image: $uuid")
            localFile
        } catch (e: Exception) {
            android.util.Log.e("FirebaseStorageManager", "Error downloading image: $uuid", e)
            null
        }
    }
    
    /**
     * Delete image from Firebase Storage
     */
    suspend fun deleteImage(uuid: String): Boolean {
        return try {
            val imageRef = storageRef.child("$IMAGES_PATH/$uuid.jpg")
            imageRef.delete().await()
            
            android.util.Log.d("FirebaseStorageManager", "Deleted image from Firebase: $uuid")
            true
        } catch (e: Exception) {
            android.util.Log.e("FirebaseStorageManager", "Error deleting image: $uuid", e)
            false
        }
    }
    
    /**
     * Check if image exists in Firebase Storage
     */
    suspend fun imageExists(uuid: String): Boolean {
        return try {
            val imageRef = storageRef.child("$IMAGES_PATH/$uuid.jpg")
            imageRef.metadata.await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Batch upload all local images that aren't in Firebase yet
     * Returns count of successfully uploaded images
     */
    suspend fun batchUploadImages(context: Context): Int {
        var uploadedCount = 0
        
        try {
            // Get all groceries with images
            val groceriesWithImages = DataManagerObject.categories
                .flatMap { it.subCategories }
                .flatMap { it.groceries }
                .filter { it.imageUUID != null && !it.deleted }
            
            groceriesWithImages.forEach { grocery ->
                val uuid = grocery.imageUUID ?: return@forEach
                val localFile = ImageManager.getLocalImageFile(uuid, context)
                
                if (localFile != null && localFile.exists()) {
                    if (uploadImage(localFile, uuid)) {
                        uploadedCount++
                    }
                }
            }
            
            android.util.Log.d("FirebaseStorageManager", "Batch upload complete: $uploadedCount images")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseStorageManager", "Error in batch upload", e)
        }
        
        return uploadedCount
    }
    
    /**
     * Batch download images for groceries that don't have local copies
     * Returns count of successfully downloaded images
     */
    suspend fun batchDownloadImages(context: Context): Int {
        var downloadedCount = 0
        
        try {
            // Get all groceries with images
            val groceriesWithImages = DataManagerObject.categories
                .flatMap { it.subCategories }
                .flatMap { it.groceries }
                .filter { it.imageUUID != null && !it.deleted }
            
            groceriesWithImages.forEach { grocery ->
                val uuid = grocery.imageUUID ?: return@forEach
                val localFile = ImageManager.getLocalImageFile(uuid, context)
                
                // Only download if we don't have it locally
                if (localFile == null || !localFile.exists()) {
                    if (downloadImage(uuid, context) != null) {
                        downloadedCount++
                    }
                }
            }
            
            android.util.Log.d("FirebaseStorageManager", "Batch download complete: $downloadedCount images")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseStorageManager", "Error in batch download", e)
        }
        
        return downloadedCount
    }
}
