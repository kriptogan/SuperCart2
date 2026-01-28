package com.example.supercart2.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ImageManager {
    private const val MAX_IMAGE_SIZE_BYTES = 200_000 // 200KB
    private const val IMAGES_DIR = "images"
    
    /**
     * Compress image from URI to target size (200KB max)
     * Returns the compressed file or null on error
     */
    suspend fun compressAndSaveImage(
        uri: Uri,
        uuid: String,
        context: Context
    ): File? {
        return try {
            // Load bitmap from URI
            val bitmap = loadBitmapFromUri(uri, context) ?: return null
            
            // Correct orientation based on EXIF
            val orientedBitmap = correctOrientation(bitmap, uri, context)
            
            // Compress to target size
            val compressedBytes = compressToTarget(orientedBitmap, MAX_IMAGE_SIZE_BYTES)
            
            // Save to local cache
            val imageFile = getImageFile(uuid, context)
            FileOutputStream(imageFile).use { it.write(compressedBytes) }
            
            android.util.Log.d("ImageManager", "Image saved: ${imageFile.absolutePath}, size: ${compressedBytes.size} bytes")
            imageFile
        } catch (e: Exception) {
            android.util.Log.e("ImageManager", "Error compressing image", e)
            null
        }
    }
    
    /**
     * Get local image file for a grocery UUID
     */
    fun getLocalImageFile(uuid: String, context: Context): File? {
        val file = getImageFile(uuid, context)
        return if (file.exists()) file else null
    }
    
    /**
     * Delete local image file
     */
    fun deleteLocalImage(uuid: String, context: Context): Boolean {
        return try {
            val file = getImageFile(uuid, context)
            val deleted = file.delete()
            if (deleted) {
                android.util.Log.d("ImageManager", "Deleted local image: $uuid")
            }
            deleted
        } catch (e: Exception) {
            android.util.Log.e("ImageManager", "Error deleting image", e)
            false
        }
    }
    
    /**
     * Get images directory, create if doesn't exist
     */
    private fun getImagesDir(context: Context): File {
        val dir = File(context.cacheDir, IMAGES_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
    
    /**
     * Get file path for image UUID
     */
    fun getImageFile(uuid: String, context: Context): File {
        return File(getImagesDir(context), "$uuid.jpg")
    }
    
    /**
     * Load bitmap from URI with sampling for memory efficiency
     */
    private fun loadBitmapFromUri(uri: Uri, context: Context): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageManager", "Error loading bitmap", e)
            null
        }
    }
    
    /**
     * Correct image orientation based on EXIF data
     */
    private fun correctOrientation(bitmap: Bitmap, uri: Uri, context: Context): Bitmap {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                    else -> bitmap
                }
            } ?: bitmap
        } catch (e: Exception) {
            android.util.Log.e("ImageManager", "Error correcting orientation", e)
            bitmap
        }
    }
    
    /**
     * Rotate bitmap by degrees
     */
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    
    /**
     * Compress bitmap to target size using iterative quality reduction
     */
    private fun compressToTarget(bitmap: Bitmap, maxSizeBytes: Int): ByteArray {
        var quality = 90
        var output: ByteArray
        
        do {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            output = stream.toByteArray()
            
            android.util.Log.d("ImageManager", "Compressed at quality $quality: ${output.size} bytes")
            quality -= 10
        } while (output.size > maxSizeBytes && quality > 10)
        
        return output
    }
    
    /**
     * Get all local image UUIDs
     */
    fun getAllLocalImageUUIDs(context: Context): List<String> {
        return try {
            getImagesDir(context).listFiles()
                ?.mapNotNull { it.nameWithoutExtension }
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Clean up orphaned images (images not linked to any grocery)
     */
    fun cleanupOrphanedImages(context: Context) {
        try {
            val allImageUUIDs = getAllLocalImageUUIDs(context)
            val groceryImageUUIDs = DataManagerObject.categories
                .flatMap { it.subCategories }
                .flatMap { it.groceries }
                .mapNotNull { it.imageUUID }
                .toSet()
            
            val orphanedUUIDs = allImageUUIDs.filterNot { groceryImageUUIDs.contains(it) }
            
            orphanedUUIDs.forEach { uuid ->
                deleteLocalImage(uuid, context)
                android.util.Log.d("ImageManager", "Cleaned up orphaned image: $uuid")
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageManager", "Error cleaning orphaned images", e)
        }
    }
}
