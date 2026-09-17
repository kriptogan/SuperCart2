package com.example.supercart2.data

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.tasks.await
import com.example.supercart2.data.FirebaseManagerCodecs as Codecs
import com.example.supercart2.data.FirebaseManagerMappers as Mappers

/**
 * Syncs the local data with Firestore under `groups/{groupCode}/...`, scoped by the saved group code.
 * Upload = per-item last-write-wins; download = replace all local data.
 */
object FirebaseManager {
    val db = FirebaseFirestore.getInstance()

    const val CATEGORIES_COLLECTION = "categories"
    const val SUBCATEGORIES_COLLECTION = "subcategories"
    const val GROCERIES_COLLECTION = "groceries"
    const val STORES_COLLECTION = "stores"
    const val MEALS_COLLECTION = "meals"
    const val STORE_CATEGORY_ORDERS_COLLECTION = "storeCategoryOrders"

    private const val TAG = "FirebaseManager"
    private const val ORDERS_DOCUMENT = "orders"

    private fun getGroupCollection(groupCode: String, collection: String): CollectionReference =
        db.collection("groups").document(groupCode).collection(collection)

    private fun ordersDocument(groupCode: String): DocumentReference =
        getGroupCollection(groupCode, STORE_CATEGORY_ORDERS_COLLECTION).document(ORDERS_DOCUMENT)

    /** True when the group has at least one category; false when it has none or Firestore fails. */
    suspend fun checkGroupExists(groupCode: String): Boolean = recoverWith(false, "Error checking group existence") {
        Log.d(TAG, "Checking if group exists: $groupCode")
        val exists = !getGroupCollection(groupCode, CATEGORIES_COLLECTION).limit(1).get().await().isEmpty
        Log.d(TAG, "Group exists: $exists")
        exists
    }

    // ---------- upload ----------

    /** Uploads new/newer items and the store category orders in one batch, then the images (failures ignored). */
    suspend fun uploadData(): Unit = rethrowLogged("Error uploading data") {
        val (context, groupCode) = requireGroup("upload")
        Log.d(TAG, "Uploading with conflict resolution to group: $groupCode")
        commitUpload(groupCode)
        transferImages("upload") { FirebaseStorageManager.batchUploadImages(context) }
    }

    private suspend fun commitUpload(groupCode: String) {
        val uploads = localUploads(groupCode)
        val batch = db.batch()
        uploads.forEach { it.queueNewer(batch) }
        queueStoreCategoryOrders(batch, groupCode)
        batch.commit().await()
    }

    /**
     * Fetches every collection's Firebase items (all before writing anything) next to the local ones.
     * A malformed Firebase document is skipped, so the local item with its uuid (if any) overwrites it.
     */
    private suspend fun localUploads(groupCode: String): List<FirebaseManagerUpload<*>> {
        val (categories, subCategories, groceries) = DataConverter.flattenCategories(DataManagerObject.categories)
        return listOf(
            upload(groupCode, Codecs.categories, categories), upload(groupCode, Codecs.subCategories, subCategories),
            upload(groupCode, Codecs.groceries, groceries), upload(groupCode, Codecs.stores, DataManagerObject.stores.toList()),
            upload(groupCode, Codecs.meals, DataManagerObject.meals.toList()),
        )
    }

    /** All store category and sub-category orders live in one document, merged into what Firebase already has. */
    private fun queueStoreCategoryOrders(batch: WriteBatch, groupCode: String) {
        val data = Mappers.storeCategoryOrdersToMap(
            DataManagerObject.storeCategoryOrders, DataManagerObject.storeSubCategoryOrders,
        )
        batch.set(ordersDocument(groupCode), data, SetOptions.merge())
    }

    private suspend fun <T> upload(groupCode: String, codec: FirebaseManagerCodec<T>, items: List<T>) =
        FirebaseManagerUpload(getGroupCollection(groupCode, codec.collection), codec, items, fetchAll(groupCode, codec))

    // ---------- download ----------

    /**
     * Replaces ALL local data with the group's data, saves, then downloads images. Malformed documents and items
     * whose parents are missing are logged and left out instead of failing the download.
     */
    suspend fun downloadData(): Unit = rethrowLogged("Error downloading data") {
        val (context, groupCode) = requireGroup("download")
        Log.d(TAG, "Downloading data from group: $groupCode")
        replaceLocalData(fetchGroup(groupCode))
        transferImages("download") { FirebaseStorageManager.batchDownloadImages(context) }
    }

    private suspend fun fetchGroup(groupCode: String): FirebaseManagerDownload {
        val categories = fetchAll(groupCode, Codecs.categories)
        val subCategories = fetchAll(groupCode, Codecs.subCategories)
        val groceries = fetchAll(groupCode, Codecs.groceries)
        val stores = fetchAll(groupCode, Codecs.stores)
        return FirebaseManagerDownload(categories, subCategories, groceries, stores, fetchOrdersDocument(groupCode))
    }

    /** Fetch failures are thrown; malformed documents are logged and skipped. */
    private suspend fun <T> fetchAll(groupCode: String, codec: FirebaseManagerCodec<T>): List<T> =
        getGroupCollection(groupCode, codec.collection).get().await().documents.mapNotNull { decodeOrSkip(codec, it) }

    private fun <T> decodeOrSkip(codec: FirebaseManagerCodec<T>, doc: DocumentSnapshot): T? =
        recoverWith(null, "⚠️ Skipping malformed ${codec.label} document ${doc.id}") { codec.fromDocument(doc) }

    private suspend fun fetchOrdersDocument(groupCode: String): DocumentSnapshot? = try {
        ordersDocument(groupCode).get().await()
    } catch (e: Exception) {
        Log.w(TAG, "No store category orders found (this is OK for new groups)", e)
        null
    }

    private suspend fun replaceLocalData(download: FirebaseManagerDownload) {
        Log.d(TAG, "Downloaded: ${download.summary()}")
        download.replaceLocalData()
        DataManagerObject.updateData()
        DataStoreManager.saveDataGlobally()
        Log.d(TAG, "✅ Data downloaded and replaced successfully")
    }

    // ---------- helpers ----------

    /** The global context and its group code; throws IllegalStateException when either is missing. */
    private suspend fun requireGroup(action: String): Pair<Context, String> {
        val context = DataStoreManager.globalContext ?: missingGroupCode(action)
        val groupCode = DataStoreManager.loadGroupCode(context) ?: missingGroupCode(action)
        return context to groupCode
    }

    private fun missingGroupCode(action: String): Nothing {
        Log.e(TAG, "❌ Cannot $action: No group code set")
        throw IllegalStateException("Group code required for $action")
    }

    /** Image sync failures are logged but never fail the data sync. */
    private inline fun transferImages(action: String, transfer: () -> Int): Unit =
        recoverWith(Unit, "❌ Error during image $action") {
            Log.d(TAG, "Starting image $action...")
            Log.d(TAG, "✅ Image $action complete: ${transfer()} images")
        }

    private inline fun <T> recoverWith(fallback: T, errorMessage: String, block: () -> T): T =
        try {
            block()
        } catch (e: Exception) {
            Log.e(TAG, errorMessage, e)
            fallback
        }

    private inline fun <T> rethrowLogged(errorMessage: String, block: () -> T): T =
        try {
            block()
        } catch (e: Exception) {
            Log.e(TAG, errorMessage, e)
            throw e
        }
}
