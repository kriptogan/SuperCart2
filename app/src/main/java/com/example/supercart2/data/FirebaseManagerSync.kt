package com.example.supercart2.data

import android.util.Log
import com.example.supercart2.models.Category
import com.example.supercart2.models.Grocery
import com.example.supercart2.models.Store
import com.example.supercart2.models.SubCategory
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.WriteBatch

/**
 * One collection's local [items], uploaded last-write-wins: an item is written only when Firebase doesn't
 * have it or has an older `lastUpdate` (strictly older; equal timestamps are skipped).
 */
internal class FirebaseManagerUpload<T>(
    private val collection: CollectionReference,
    private val codec: FirebaseManagerCodec<T>,
    private val items: List<T>,
    existing: List<T>,
) {
    private val existingByUuid = existing.associateBy(codec.uuid)

    fun queueNewer(batch: WriteBatch) {
        val (newer, skipped) = items.partition(::isNewerThanFirebase)
        newer.forEach { batch.set(collection.document(codec.uuid(it)), codec.toMap(it)) }
        skipped.forEach { Log.d(TAG, "⏭️ Skipped ${codec.label} ${codec.name(it)} (Firebase version is newer)") }
        Log.d(TAG, "${codec.collection}: ${newer.size} queued for upload, ${skipped.size} skipped")
    }

    private fun isNewerThanFirebase(item: T): Boolean {
        val remote = existingByUuid[codec.uuid(item)]
        return remote == null || codec.lastUpdate(item).isAfter(codec.lastUpdate(remote))
    }

    private companion object {
        const val TAG = "FirebaseManager"
    }
}

/** Everything fetched from a group, ready to replace the local data. */
internal class FirebaseManagerDownload(
    private val categories: List<Category>,
    private val subCategories: List<SubCategory>,
    private val groceries: List<Grocery>,
    private val stores: List<Store>,
    private val storeCategoryOrders: Map<String, List<String>>,
) {
    fun summary(): String = "${categories.size} categories, ${subCategories.size} subcategories, " +
        "${groceries.size} groceries, ${stores.size} stores, ${storeCategoryOrders.size} store category orders"

    fun hasValidRelationships(): Boolean = DataConverter.validateRelationships(categories, subCategories, groceries)

    /**
     * Replaces categories, stores and store category orders. Like DataStoreManager.loadData, deleted stores
     * are dropped, and buildNestedStructure drops deleted categories, sub-categories and groceries.
     */
    fun replaceLocalData() {
        val nested = DataConverter.buildNestedStructure(categories, subCategories, groceries)
        DataManagerObject.categories.replaceWith(nested)
        DataManagerObject.stores.replaceWith(stores.filter { !it.deleted })
        DataManagerObject.storeCategoryOrders.clear()
        DataManagerObject.storeCategoryOrders.putAll(storeCategoryOrders)
    }

    private fun <T> MutableList<T>.replaceWith(newItems: List<T>) {
        clear()
        addAll(newItems)
    }
}
