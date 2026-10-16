package com.example.supercart2.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import java.io.File

/** Grocery photos in Firebase Storage under `grocery_images/{uuid}.jpg`. Failures are logged, never thrown. */
object FirebaseStorageManager {
    private const val TAG = "FirebaseStorageManager"
    private const val BUCKET_URL = "gs://supercart2-58caf.firebasestorage.app" // from google-services.json
    private const val IMAGES_PATH = "grocery_images"

    private val STORAGE_ERROR_HINTS = mapOf(
        StorageException.ERROR_BUCKET_NOT_FOUND to "Bucket not found! Is Storage enabled in Firebase Console?",
        StorageException.ERROR_OBJECT_NOT_FOUND to "Object or bucket not found! Is Storage enabled?",
        StorageException.ERROR_NOT_AUTHENTICATED to "Not authenticated.",
        StorageException.ERROR_NOT_AUTHORIZED to "Not authorized. Check Storage Rules.",
    )

    private val storage: FirebaseStorage by lazy { createStorage() }

    private val storageRef: StorageReference
        get() = storage.reference

    private fun createStorage(): FirebaseStorage = try {
        FirebaseStorage.getInstance(BUCKET_URL).also { Log.d(TAG, "✅ Storage bucket: ${it.reference.bucket}") }
    } catch (e: Exception) {
        Log.e(TAG, "❌ Failed to initialize Storage, falling back to the default instance", e)
        FirebaseStorage.getInstance()
    }

    private fun imageRef(uuid: String): StorageReference = storageRef.child("$IMAGES_PATH/$uuid.jpg")

    /** True when the bucket root can be listed. */
    suspend fun testStorageConnection(): Boolean = orOnError(false, "❌ Storage connection test failed") {
        Log.d(TAG, "🔍 Testing Storage connection (bucket: ${storageRef.bucket}, path: ${storageRef.path})")
        val listResult = storageRef.listAll().await()
        Log.d(TAG, "✅ Storage is accessible! Found ${listResult.items.size} items")
        true
    }

    /** Uploads a single image; true if successful. */
    suspend fun uploadImage(imageFile: File, uuid: String): Boolean = orOnError(false, "❌ Error uploading $uuid") {
        val imageRef = imageRef(uuid)
        Log.d(TAG, "Uploading to: ${imageRef.path} in bucket: ${imageRef.bucket}")
        imageRef.putFile(Uri.fromFile(imageFile)).await()
        true
    }

    /** Downloads a single image into the local image cache; returns that file, or null on error. */
    suspend fun downloadImage(uuid: String, context: Context): File? = orOnError(null, "Error downloading $uuid") {
        val imageRef = imageRef(uuid)
        val localFile = ImageManager.getImageFile(uuid, context)
        imageRef.getFile(localFile).await()
        localFile
    }

    /** Uploads the local images of all visible groceries; returns how many succeeded. */
    suspend fun batchUploadImages(context: Context): Int = countSuccesses("upload") { uploadAll(context, it) }

    /** Downloads the images of visible groceries that have no local copy; returns how many succeeded. */
    suspend fun batchDownloadImages(context: Context): Int = countSuccesses("download") { downloadAll(context, it) }

    private suspend fun uploadAll(context: Context, results: MutableList<Boolean>) {
        if (storageUnavailable("upload")) return
        val imageUUIDs = groceryImageUUIDs()
        Log.d(TAG, "📤 Found ${imageUUIDs.size} groceries with images")
        imageUUIDs.forEach { uuid -> results += uploadIfLocal(context, uuid) }
        Log.d(TAG, "📤 Batch upload complete: ${results.count { it }}/${imageUUIDs.size} images uploaded")
    }

    private suspend fun uploadIfLocal(context: Context, uuid: String): Boolean {
        val localFile = ImageManager.getLocalImageFile(uuid, context)
        if (localFile == null || !localFile.exists()) {
            Log.w(TAG, "⚠️ Local file not found for image $uuid")
            return false
        }
        return uploadImage(localFile, uuid).also { if (!it) Log.w(TAG, "❌ Failed to upload image $uuid") }
    }

    private suspend fun downloadAll(context: Context, results: MutableList<Boolean>) {
        if (storageUnavailable("download")) return
        val imageUUIDs = groceryImageUUIDs()
        Log.d(TAG, "📥 Found ${imageUUIDs.size} groceries with images")
        imageUUIDs.forEach { uuid -> downloadIfMissing(context, uuid)?.let { results += it } }
        Log.d(TAG, "📥 Batch download: ${results.count { it }} downloaded, ${imageUUIDs.size - results.size} skipped")
    }

    /** Checks the local copy right before downloading, so a uuid shared by several groceries is fetched once. */
    private suspend fun downloadIfMissing(context: Context, uuid: String): Boolean? {
        if (ImageManager.getLocalImageFile(uuid, context)?.exists() == true) return null // already exists locally
        val downloaded = downloadImage(uuid, context) != null
        if (!downloaded) Log.w(TAG, "❌ Failed to download image $uuid")
        return downloaded
    }

    /** Image uuids of visible groceries: the grocery, its sub-category and its category are all non-deleted. */
    private fun groceryImageUUIDs(): List<String> =
        DataConverter.flattenCategories(DataManagerObject.categories, includeDeleted = false).third
            .mapNotNull { it.imageUUID }

    private suspend fun storageUnavailable(action: String): Boolean {
        val unavailable = !testStorageConnection()
        if (unavailable) Log.e(TAG, "❌ Storage connection test failed. Aborting $action.")
        return unavailable
    }

    /** Runs a batch transfer that records one result per attempt; an unexpected error keeps the results so far. */
    private inline fun countSuccesses(action: String, transfer: (MutableList<Boolean>) -> Unit): Int {
        val results = mutableListOf<Boolean>()
        orOnError(Unit, "Error in batch $action") { transfer(results) }
        return results.count { it }
    }

    private inline fun <T> orOnError(fallback: T, errorMessage: String, block: () -> T): T =
        try {
            block()
        } catch (e: Exception) {
            Log.e(TAG, listOfNotNull(errorMessage, storageErrorHint(e)).joinToString(" "), e)
            fallback
        }

    private fun storageErrorHint(e: Exception): String? {
        val code = (e as? StorageException)?.errorCode ?: return null
        return STORAGE_ERROR_HINTS[code] ?: "(code: $code)"
    }
}
