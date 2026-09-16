package com.example.supercart2.data

import com.example.supercart2.models.Category
import com.example.supercart2.models.Grocery
import com.example.supercart2.models.Store
import com.example.supercart2.models.SubCategory
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import com.google.protobuf.Timestamp as ProtoTimestamp

/** How one entity type is identified, compared and (de)serialized in its Firestore collection. */
internal class FirebaseManagerCodec<T>(
    val collection: String,
    val label: String,
    val uuid: (T) -> String,
    val name: (T) -> String,
    val lastUpdate: (T) -> LocalDateTime,
    val toMap: (T) -> Map<String, Any?>,
    val fromDocument: (DocumentSnapshot) -> T,
)

internal object FirebaseManagerCodecs {
    val categories = FirebaseManagerCodec(
        FirebaseManager.CATEGORIES_COLLECTION, "category", Category::uuid, Category::name, Category::lastUpdate,
        FirebaseManagerMappers::categoryToMap, FirebaseManagerMappers::documentToCategory,
    )
    val subCategories = FirebaseManagerCodec(
        FirebaseManager.SUBCATEGORIES_COLLECTION, "subcategory", SubCategory::uuid, SubCategory::name,
        SubCategory::lastUpdate, FirebaseManagerMappers::subCategoryToMap,
        FirebaseManagerMappers::documentToSubCategory,
    )
    val groceries = FirebaseManagerCodec(
        FirebaseManager.GROCERIES_COLLECTION, "grocery", Grocery::uuid, Grocery::name, Grocery::lastUpdate,
        FirebaseManagerMappers::groceryToMap, FirebaseManagerMappers::documentToGrocery,
    )
    val stores = FirebaseManagerCodec(
        FirebaseManager.STORES_COLLECTION, "store", Store::uuid, Store::name, Store::lastUpdate,
        FirebaseManagerMappers::storeToMap, FirebaseManagerMappers::documentToStore,
    )
}

/**
 * Pure conversions between model entities and Firestore document data.
 * Written dates are ISO strings; read dates may be ISO strings or Timestamps.
 */
internal object FirebaseManagerMappers {

    // ---------- entity -> map ----------

    fun categoryToMap(category: Category): Map<String, Any?> = mapOf(
        "uuid" to category.uuid, "name" to category.name, "default" to category.default,
        "viewOrder" to category.viewOrder, "protected" to category.protected,
        "lastUpdate" to formatDateTime(category.lastUpdate), "deleted" to category.deleted,
    )

    fun subCategoryToMap(subCategory: SubCategory): Map<String, Any?> = mapOf(
        "uuid" to subCategory.uuid, "categoryId" to subCategory.categoryId, "name" to subCategory.name,
        "protected" to subCategory.protected, "lastUpdate" to formatDateTime(subCategory.lastUpdate),
        "deleted" to subCategory.deleted,
    )

    fun groceryToMap(grocery: Grocery): Map<String, Any?> = groceryItemFields(grocery) + groceryHistoryFields(grocery)

    private fun groceryItemFields(grocery: Grocery): Map<String, Any?> = mapOf(
        "uuid" to grocery.uuid, "name" to grocery.name, "categoryId" to grocery.categoryId,
        "subCategoryId" to grocery.subCategoryId, "expirationDate" to grocery.expirationDate?.let(::formatDate),
        "inShoppingList" to grocery.inShoppingList, "isBought" to grocery.isBought,
    )

    private fun groceryHistoryFields(grocery: Grocery): Map<String, Any?> = mapOf(
        "buyEvents" to grocery.buyEvents.map(::formatDate), "imageUUID" to grocery.imageUUID,
        "averageBuyDays" to grocery.averageBuyDays, "storeIds" to grocery.storeIds,
        "lastUpdate" to formatDateTime(grocery.lastUpdate), "deleted" to grocery.deleted,
    )

    /** Known issue: Store.address is not synced. */
    fun storeToMap(store: Store): Map<String, Any?> = mapOf(
        "uuid" to store.uuid, "name" to store.name, "viewOrder" to store.viewOrder,
        "lastUpdate" to formatDateTime(store.lastUpdate), "deleted" to store.deleted,
    )

    fun storeCategoryOrdersToMap(orders: Map<String, List<String>>): Map<String, Any> =
        mapOf("orders" to orders.toMap())

    // ---------- document -> entity ----------

    fun documentToCategory(doc: DocumentSnapshot): Category = FirebaseManagerDocument(doc, "Category").toCategory()

    fun documentToSubCategory(doc: DocumentSnapshot): SubCategory =
        FirebaseManagerDocument(doc, "SubCategory").toSubCategory()

    fun documentToGrocery(doc: DocumentSnapshot): Grocery = FirebaseManagerDocument(doc, "Grocery").toGrocery()

    fun documentToStore(doc: DocumentSnapshot): Store = FirebaseManagerDocument(doc, "Store").toStore()

    private fun FirebaseManagerDocument.toCategory() = Category(
        uuid = id, name = required("name"), default = flag("default"), viewOrder = int("viewOrder") ?: 0,
        protected = flag("protected"), lastUpdate = dateTime("lastUpdate"), deleted = flag("deleted"),
    )

    private fun FirebaseManagerDocument.toSubCategory() = SubCategory(
        uuid = id, categoryId = required("categoryId"), name = required("name"), protected = flag("protected"),
        lastUpdate = dateTime("lastUpdate"), deleted = flag("deleted"),
    )

    private fun FirebaseManagerDocument.toGrocery() = Grocery(uuid = id, name = required("name"),
        categoryId = required("categoryId"), subCategoryId = required("subCategoryId"), isBought = flag("isBought"),
        expirationDate = date("expirationDate"), inShoppingList = flag("inShoppingList"),
        buyEvents = dates("buyEvents"), imageUUID = string("imageUUID"), averageBuyDays = int("averageBuyDays"),
        storeIds = strings("storeIds"), lastUpdate = dateTime("lastUpdate"), deleted = flag("deleted"),
    )

    /** Known issue: Store.address is not synced. */
    private fun FirebaseManagerDocument.toStore() = Store(
        uuid = id, name = required("name"), viewOrder = int("viewOrder") ?: 0,
        lastUpdate = dateTime("lastUpdate"), deleted = flag("deleted"),
    )

    /** storeId -> categoryIds from the `orders` field; missing documents and malformed entries are ignored. */
    fun documentToStoreCategoryOrders(doc: DocumentSnapshot?): Map<String, List<String>> {
        if (doc == null || !doc.exists()) return emptyMap()
        val orders = doc.data?.get("orders") as? Map<*, *> ?: return emptyMap()
        return orders.mapNotNull { (key, value) -> orderEntry(key, value) }.toMap()
    }

    private fun orderEntry(key: Any?, value: Any?): Pair<String, List<String>>? {
        val storeId = key as? String ?: return null
        val categoryIds = (value as? List<*>)?.mapNotNull { it as? String } ?: return null
        return storeId to categoryIds
    }

    // ---------- dates ----------

    private fun formatDateTime(value: LocalDateTime): String = value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

    private fun formatDate(value: LocalDate): String = value.format(DateTimeFormatter.ISO_LOCAL_DATE)

    /** Timestamp, ISO string or protobuf Timestamp; anything else (including a missing value) means now. */
    fun toLocalDateTime(value: Any?): LocalDateTime = when (value) {
        is Timestamp -> value.atSystemZone().toLocalDateTime()
        is String -> LocalDateTime.parse(value)
        is ProtoTimestamp -> value.atSystemZone().toLocalDateTime()
        else -> LocalDateTime.now()
    }

    /** ISO string or Timestamp; anything else is null. */
    fun toLocalDate(value: Any?): LocalDate? = when (value) {
        is String -> LocalDate.parse(value)
        is Timestamp -> value.atSystemZone().toLocalDate()
        else -> null
    }

    private fun Timestamp.atSystemZone(): ZonedDateTime = toDate().toInstant().atZone(ZoneId.systemDefault())

    private fun ProtoTimestamp.atSystemZone(): ZonedDateTime =
        Instant.ofEpochMilli(seconds * 1000 + nanos / 1_000_000).atZone(ZoneId.systemDefault())
}

/** Typed, lenient reads of a Firestore document; throws when it has no data or lacks a required string. */
internal class FirebaseManagerDocument(private val doc: DocumentSnapshot, private val entity: String) {
    private val data: Map<String, Any> = doc.data ?: throw IllegalStateException("Document ${doc.id} has no data")

    val id: String get() = doc.id

    fun required(key: String): String =
        data[key] as? String ?: throw IllegalStateException("$entity ${doc.id} missing $key")

    fun string(key: String): String? = data[key] as? String

    fun flag(key: String): Boolean = data[key] as? Boolean ?: false

    fun int(key: String): Int? = (data[key] as? Long)?.toInt()

    fun strings(key: String): List<String> = (data[key] as? List<*>)?.filterIsInstance<String>() ?: emptyList()

    fun dateTime(key: String): LocalDateTime = FirebaseManagerMappers.toLocalDateTime(data[key])

    fun date(key: String): LocalDate? = FirebaseManagerMappers.toLocalDate(data[key])

    fun dates(key: String): List<LocalDate> =
        (data[key] as? List<*>)?.mapNotNull { FirebaseManagerMappers.toLocalDate(it) } ?: emptyList()
}
