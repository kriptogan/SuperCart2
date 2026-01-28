package com.example.supercart2.data

import android.content.Context
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import java.io.File

object FirebaseStorageManager {
    // Initialize Firebase Storage with explicit bucket URL
    private val storage: FirebaseStorage by lazy {
        try {
            // Use the bucket URL from google-services.json
            val instance = FirebaseStorage.getInstance("gs://supercart2-58caf.firebasestorage.app")
            android.util.Log.d("FirebaseStorageManager", "✅ Storage initialized with bucket: ${instance.reference.bucket}")
            instance
        } catch (e: Exception) {
            android.util.Log.e("FirebaseStorageManager", "❌ Failed to initialize Storage", e)
            // Fallback to default instance
            FirebaseStorage.getInstance()
        }
    }
    
    private val storageRef: StorageReference
        get() = storage.reference
    
    private const val IMAGES_PATH = "grocery_images"
    
    /**
     * Test if Firebase Storage is properly initialized and accessible
     */
    suspend fun testStorageConnection(): Boolean {
        return try {
            android.util.Log.d("FirebaseStorageManager", "🔍 Testing Storage connection...")
            android.util.Log.d("FirebaseStorageManager", "Bucket: ${storageRef.bucket}")
            android.util.Log.d("FirebaseStorageManager", "Path: ${storageRef.path}")
            
            // Try to get metadata of the root (should work even if empty)
            val listResult = storageRef.listAll().await()
            android.util.Log.d("FirebaseStorageManager", "✅ Storage is accessible! Found ${listResult.items.size} items")
            true
        } catch (e: com.google.firebase.storage.StorageException) {
            when (e.errorCode) {
                com.google.firebase.storage.StorageException.ERROR_BUCKET_NOT_FOUND -> {
                    android.util.Log.e("FirebaseStorageManager", "❌ Storage bucket not found! Storage might not be enabled in Firebase Console.", e)
                }
                else -> {
                    android.util.Log.e("FirebaseStorageManager", "❌ Storage connection test failed (code: ${e.errorCode})", e)
                }
            }
            false
        } catch (e: Exception) {
            android.util.Log.e("FirebaseStorageManager", "❌ Storage connection test failed", e)
            false
        }
    }
    
    /**
     * Upload a single image to Firebase Storage
     * Returns true if successful
     */
    suspend fun uploadImage(imageFile: File, uuid: String): Boolean {
        return try {
            val imageRef = storageRef.child("$IMAGES_PATH/$uuid.jpg")
            android.util.Log.d("FirebaseStorageManager", "Uploading to: ${imageRef.path} in bucket: ${imageRef.bucket}")
            
            imageRef.putFile(android.net.Uri.fromFile(imageFile)).await()
            
            android.util.Log.d("FirebaseStorageManager", "Uploaded image: $uuid")
            true
        } catch (e: com.google.firebase.storage.StorageException) {
            when (e.errorCode) {
                com.google.firebase.storage.StorageException.ERROR_OBJECT_NOT_FOUND -> {
                    android.util.Log.e("FirebaseStorageManager", "❌ Firebase Storage bucket not found! Please enable Storage in Firebase Console.", e)
                }
                com.google.firebase.storage.StorageException.ERROR_NOT_AUTHENTICATED -> {
                    android.util.Log.e("FirebaseStorageManager", "❌ Not authenticated to upload images", e)
                }
                com.google.firebase.storage.StorageException.ERROR_NOT_AUTHORIZED -> {
                    android.util.Log.e("FirebaseStorageManager", "❌ Not authorized to upload images. Check Storage Rules.", e)
                }
                else -> {
                    android.util.Log.e("FirebaseStorageManager", "❌ Error uploading image: $uuid (code: ${e.errorCode})", e)
                }
            }
            false
        } catch (e: Exception) {
            android.util.Log.e("FirebaseStorageManager", "❌ Error uploading image: $uuid", e)
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
            // Test Storage connection first
            if (!testStorageConnection()) {
                android.util.Log.e("FirebaseStorageManager", "❌ Storage connection test failed. Aborting upload.")
                android.util.Log.e("FirebaseStorageManager", "📖 Please enable Firebase Storage in Firebase Console:")
                android.util.Log.e("FirebaseStorageManager", "   1. Go to https://console.firebase.google.com/")
                android.util.Log.e("FirebaseStorageManager", "   2. Select project: supercart2-58caf")
                android.util.Log.e("FirebaseStorageManager", "   3. Click 'Storage' in left menu")
                android.util.Log.e("FirebaseStorageManager", "   4. Click 'Get Started' button")
                return 0
            }
            
            // Get all groceries with images
            val groceriesWithImages = DataManagerObject.categories
                .flatMap { it.subCategories }
                .flatMap { it.groceries }
                .filter { it.imageUUID != null && !it.deleted }
            
            android.util.Log.d("FirebaseStorageManager", "📤 Found ${groceriesWithImages.size} groceries with images")
            
            groceriesWithImages.forEach { grocery ->
                val uuid = grocery.imageUUID ?: return@forEach
                val localFile = ImageManager.getLocalImageFile(uuid, context)
                
                android.util.Log.d("FirebaseStorageManager", "Processing image $uuid, local file exists: ${localFile?.exists()}")
                
                if (localFile != null && localFile.exists()) {
                    if (uploadImage(localFile, uuid)) {
                        uploadedCount++
                        android.util.Log.d("FirebaseStorageManager", "✅ Uploaded image $uuid ($uploadedCount/${groceriesWithImages.size})")
                    } else {
                        android.util.Log.w("FirebaseStorageManager", "❌ Failed to upload image $uuid")
                    }
                } else {
                    android.util.Log.w("FirebaseStorageManager", "⚠️ Local file not found for image $uuid")
                }
            }
            
            android.util.Log.d("FirebaseStorageManager", "📤 Batch upload complete: $uploadedCount/${groceriesWithImages.size} images uploaded")
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
            // Test Storage connection first
            if (!testStorageConnection()) {
                android.util.Log.e("FirebaseStorageManager", "❌ Storage connection test failed. Aborting download.")
                return 0
            }
            
            // Get all groceries with images
            val groceriesWithImages = DataManagerObject.categories
                .flatMap { it.subCategories }
                .flatMap { it.groceries }
                .filter { it.imageUUID != null && !it.deleted }
            
            android.util.Log.d("FirebaseStorageManager", "📥 Found ${groceriesWithImages.size} groceries with images")
            
            var skippedCount = 0
            groceriesWithImages.forEach { grocery ->
                val uuid = grocery.imageUUID ?: return@forEach
                val localFile = ImageManager.getLocalImageFile(uuid, context)
                
                // Only download if we don't have it locally
                if (localFile == null || !localFile.exists()) {
                    android.util.Log.d("FirebaseStorageManager", "Downloading missing image $uuid...")
                    if (downloadImage(uuid, context) != null) {
                        downloadedCount++
                        android.util.Log.d("FirebaseStorageManager", "✅ Downloaded image $uuid ($downloadedCount downloaded)")
                    } else {
                        android.util.Log.w("FirebaseStorageManager", "❌ Failed to download image $uuid")
                    }
                } else {
                    skippedCount++
                    android.util.Log.d("FirebaseStorageManager", "⏭️ Skipped $uuid (already exists locally)")
                }
            }
            
            android.util.Log.d("FirebaseStorageManager", "📥 Batch download complete: $downloadedCount downloaded, $skippedCount skipped")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseStorageManager", "Error in batch download", e)
        }
        
        return downloadedCount
    }
}
