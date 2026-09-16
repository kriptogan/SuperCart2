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
            loadBitmapFromUri(uri, context)?.let { saveCompressed(correctOrientation(it, uri, context), uuid, context) }
        } catch (e: Exception) {
            android.util.Log.e("ImageManager", "Error compressing image", e)
            null
        }
    }

    // Compresses to the target size and writes it to the local cache
    private fun saveCompressed(bitmap: Bitmap, uuid: String, context: Context): File {
        val compressedBytes = compressToTarget(bitmap, MAX_IMAGE_SIZE_BYTES)
        val imageFile = getImageFile(uuid, context)
        FileOutputStream(imageFile).use { it.write(compressedBytes) }
        val sizeInfo = "size: ${compressedBytes.size} bytes"
        android.util.Log.d("ImageManager", "Image saved: ${imageFile.absolutePath}, $sizeInfo")
        return imageFile
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
            logIfDeleted(getImageFile(uuid, context).delete(), uuid)
        } catch (e: Exception) {
            android.util.Log.e("ImageManager", "Error deleting image", e)
            false
        }
    }

    private fun logIfDeleted(deleted: Boolean, uuid: String): Boolean {
        if (deleted) android.util.Log.d("ImageManager", "Deleted local image: $uuid")
        return deleted
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
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
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
            context.contentResolver.openInputStream(uri)?.use { rotateForExif(bitmap, ExifInterface(it)) } ?: bitmap
        } catch (e: Exception) {
            android.util.Log.e("ImageManager", "Error correcting orientation", e)
            bitmap
        }
    }

    private fun rotateForExif(bitmap: Bitmap, exif: ExifInterface): Bitmap =
        when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
            else -> bitmap
        }

    /**
     * Rotate bitmap by degrees
     */
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Compress bitmap to target size using iterative quality reduction (90, 80, ... down to 20 at most);
     * returns the last attempt when nothing fits.
     */
    private fun compressToTarget(bitmap: Bitmap, maxSizeBytes: Int): ByteArray {
        var output = ByteArray(0)
        for (quality in 90 downTo 20 step 10) {
            output = compressJpeg(bitmap, quality)
            if (output.size <= maxSizeBytes) break
        }
        return output
    }

    private fun compressJpeg(bitmap: Bitmap, quality: Int): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        val output = stream.toByteArray()
        android.util.Log.d("ImageManager", "Compressed at quality $quality: ${output.size} bytes")
        return output
    }
}
