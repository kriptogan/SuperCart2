package com.example.supercart2.data

import android.util.Log
import com.example.supercart2.models.Category
import com.example.supercart2.models.Grocery
import com.example.supercart2.models.Meal
import com.example.supercart2.models.Store
import com.example.supercart2.models.SubCategory
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
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

/**
 * Everything fetched from a group, ready to replace the local data. Sub-categories and groceries whose parents
 * are missing (e.g. skipped as malformed) are logged and dropped, so the remaining data has valid relationships.
 */
internal class FirebaseManagerDownload(
    private val categories: List<Category>,
    subCategories: List<SubCategory>,
    groceries: List<Grocery>,
    private val stores: List<Store>,
    private val meals: List<Meal>,
    ordersDocument: DocumentSnapshot?,
) {
    private val categoryIds = categories.map { it.uuid }.toSet()
    private val subCategories = subCategories.keepLinked("subcategory") { it.categoryId in categoryIds }
    private val subCategoryIds = this.subCategories.map { it.uuid }.toSet()
    private val groceries =
        groceries.keepLinked("grocery") { it.categoryId in categoryIds && it.subCategoryId in subCategoryIds }
    private val storeCategoryOrders = FirebaseManagerMappers.documentToStoreCategoryOrders(ordersDocument)
    private val storeSubCategoryOrders = FirebaseManagerMappers.documentToStoreSubCategoryOrders(ordersDocument)

    fun summary(): String = "${categories.size} categories, ${subCategories.size} subcategories, " +
        "${groceries.size} groceries, ${stores.size} stores, ${meals.size} meals, " +
        "${storeCategoryOrders.size} store category orders, " +
        "${storeSubCategoryOrders.size} store sub-category orders"

    private fun <T> List<T>.keepLinked(label: String, isLinked: (T) -> Boolean): List<T> {
        val (linked, orphans) = partition(isLinked)
        orphans.forEach { Log.w(TAG, "⚠️ Dropped downloaded $label with a missing parent: $it") }
        return linked
    }

    /**
     * Replaces categories, stores, meals and the per-store orders. Like DataStoreManager.loadData, deleted stores
     * are dropped, and buildNestedStructure drops deleted categories, sub-categories and groceries.
     * Meals are kept as downloaded: deleted meals stay (so deletions sync) and unknown grocery ids are kept.
     */
    fun replaceLocalData() {
        val nested = DataConverter.buildNestedStructure(categories, subCategories, groceries)
        DataManagerObject.categories.replaceWith(nested)
        DataManagerObject.stores.replaceWith(stores.filter { !it.deleted })
        DataManagerObject.meals.replaceWith(meals)
        DataManagerObject.storeCategoryOrders.replaceWith(storeCategoryOrders)
        DataManagerObject.storeSubCategoryOrders.replaceWith(storeSubCategoryOrders)
    }

    private fun <T> MutableList<T>.replaceWith(newItems: List<T>) {
        clear()
        addAll(newItems)
    }

    private fun <K, V> MutableMap<K, V>.replaceWith(newEntries: Map<K, V>) {
        clear()
        putAll(newEntries)
    }

    private companion object {
        const val TAG = "FirebaseManager"
    }
}
