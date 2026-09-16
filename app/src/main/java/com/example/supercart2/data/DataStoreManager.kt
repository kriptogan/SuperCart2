package com.example.supercart2.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.supercart2.models.Category
import com.example.supercart2.models.SubCategory
import com.example.supercart2.models.Grocery
import com.example.supercart2.models.Store

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "supercart_data")

private typealias CategoryOrders = Map<String, List<String>>
private typealias SubCategoryOrders = Map<String, Map<String, List<String>>>

/**
 * Everything read by loadData, fully parsed before any in-memory state is replaced.
 * Entities are normalized: fields missing from older saves get their defaults instead of null.
 */
private class StoredData(
    categories: List<Category>,
    subCategories: List<SubCategory>,
    groceries: List<Grocery>,
    stores: List<Store>,
    val hiddenStoreIds: List<String>,
    val storeCategoryOrders: CategoryOrders,
    val storeSubCategoryOrders: SubCategoryOrders,
) {
    val categories = categories.map { it.normalized() }
    val subCategories = subCategories.map { it.normalized() }
    val groceries = groceries.map { it.normalized() }
    val stores = stores.map { it.normalized() }
}

object DataStoreManager {
    private val CATEGORIES_KEY = stringPreferencesKey("categories_flat")
    private val SUBCATEGORIES_KEY = stringPreferencesKey("subcategories_flat")
    private val GROCERIES_KEY = stringPreferencesKey("groceries_flat")
    private val STORES_KEY = stringPreferencesKey("stores_flat")
    private val HIDDEN_STORES_KEY = stringPreferencesKey("hidden_stores")
    private val STORE_VIEW_MODE_KEY = stringPreferencesKey("store_view_mode")
    private val GROUP_CODE_KEY = stringPreferencesKey("group_code")
    private val STORE_CATEGORY_ORDERS_KEY = stringPreferencesKey("store_category_orders")
    private val STORE_SUBCATEGORY_ORDERS_KEY = stringPreferencesKey("store_subcategory_orders")

    private val gson: Gson by lazy { gsonBuilder().create() }

    private inline fun <reified T> Preferences.readJson(key: Preferences.Key<String>): T? =
        this[key]?.let { gson.fromJson<T>(it, object : TypeToken<T>() {}.type) }

    private inline fun <reified T> Preferences.readList(key: Preferences.Key<String>): List<T> =
        readJson<List<T>>(key) ?: emptyList()

    // Store orders are optional extras: a corrupt value falls back to empty instead of failing the load
    private inline fun <reified T : Map<*, *>> Preferences.readOrders(key: Preferences.Key<String>): T? =
        try {
            readJson<T>(key)
        } catch (e: Exception) {
            android.util.Log.e("DataStoreManager", "Error loading ${key.name}", e)
            null
        }

    // Global context reference for saving data from anywhere
    internal var globalContext: Context? = null

    fun setGlobalContext(context: Context) {
        globalContext = context
    }

    suspend fun saveDataGlobally() {
        globalContext?.let { saveData(it) }
            ?: android.util.Log.w("DataStoreManager", "Global context not set, cannot save data")
    }

    suspend fun saveData(context: Context) {
        val json = currentDataAsJson()
        context.dataStore.edit { preferences -> json.forEach { (key, value) -> preferences[key] = value } }
    }

    // Snapshot of the in-memory data (flattened, including deleted items) as JSON per storage key
    private fun currentDataAsJson(): Map<Preferences.Key<String>, String> {
        val (categories, subCategories, groceries) = DataConverter.flattenCategories(DataManagerObject.categories)
        val flatValues = mapOf<Preferences.Key<String>, Any>(
            CATEGORIES_KEY to categories, SUBCATEGORIES_KEY to subCategories, GROCERIES_KEY to groceries,
        )
        return (flatValues + storeValues()).mapValues { (_, value) -> gson.toJson(value) }
    }

    private fun storeValues(): Map<Preferences.Key<String>, Any> = mapOf(
        STORES_KEY to DataManagerObject.stores.toList(),
        HIDDEN_STORES_KEY to DataManagerObject.hiddenStoreIds.toList(),
        STORE_CATEGORY_ORDERS_KEY to DataManagerObject.storeCategoryOrders.toMap(),
        STORE_SUBCATEGORY_ORDERS_KEY to DataManagerObject.storeSubCategoryOrders.toMap(),
    )

    suspend fun loadData(context: Context) {
        try {
            loadAndInitialize(context)
        } catch (e: Exception) {
            android.util.Log.e("DataStoreManager", "Error loading data", e)
            DataInitializer.initializeDefaultData(context)
        }
    }

    private suspend fun loadAndInitialize(context: Context) {
        applyStoredData(context.dataStore.data.first().toStoredData())
        // Initialize default data if no categories exist
        DataInitializer.initializeDefaultData(context)
    }

    private fun Preferences.toStoredData() = StoredData(
        readList(CATEGORIES_KEY), readList(SUBCATEGORIES_KEY), readList(GROCERIES_KEY),
        readList(STORES_KEY), readList(HIDDEN_STORES_KEY),
        readOrders<CategoryOrders>(STORE_CATEGORY_ORDERS_KEY) ?: emptyMap(),
        readOrders<SubCategoryOrders>(STORE_SUBCATEGORY_ORDERS_KEY) ?: emptyMap(),
    )

    // Replaces the in-memory data (dropping deleted stores), unless the relationships are invalid
    private fun applyStoredData(data: StoredData) {
        if (!DataConverter.validateRelationships(data.categories, data.subCategories, data.groceries)) {
            android.util.Log.e("DataStoreManager", "Invalid data relationships detected")
            return
        }
        DataManagerObject.replaceCategoriesAndStores(data.categories, data.subCategories, data.groceries, data.stores)
        replaceOrders(data.hiddenStoreIds, data.storeCategoryOrders, data.storeSubCategoryOrders)
    }

    suspend fun saveStoreViewMode(context: Context, isStoreView: Boolean) {
        try {
            context.dataStore.edit { it[STORE_VIEW_MODE_KEY] = if (isStoreView) "STORE" else "CATEGORY" }
        } catch (e: Exception) {
            android.util.Log.e("DataStoreManager", "Error saving store view mode", e)
        }
    }

    // Defaults to category view
    suspend fun loadStoreViewMode(context: Context): Boolean = try {
        context.dataStore.data.first()[STORE_VIEW_MODE_KEY] == "STORE"
    } catch (e: Exception) {
        android.util.Log.e("DataStoreManager", "Error loading store view mode", e)
        false
    }

    // ========== Group Code Management ==========

    suspend fun saveGroupCode(context: Context, groupCode: String) {
        context.dataStore.edit { it[GROUP_CODE_KEY] = groupCode }
    }

    /** Returns null if no group code is set (or it can't be read). */
    suspend fun loadGroupCode(context: Context): String? = try {
        context.dataStore.data.first()[GROUP_CODE_KEY]
    } catch (e: Exception) {
        android.util.Log.e("DataStoreManager", "Error loading group code", e)
        null
    }

    suspend fun clearGroupCode(context: Context) {
        context.dataStore.edit { it.remove(GROUP_CODE_KEY) }
    }

    suspend fun hasGroupCode(context: Context): Boolean = loadGroupCode(context) != null
}

/**
 * Replaces categories (nested from the flat lists, hiding deleted items) and stores (without deleted ones)
 * in memory. Shared by DataStoreManager.loadData and BackupManager restore.
 */
internal fun DataManagerObject.replaceCategoriesAndStores(
    categories: List<Category>,
    subCategories: List<SubCategory>,
    groceries: List<Grocery>,
    stores: List<Store>,
) {
    this.categories.replaceWith(DataConverter.buildNestedStructure(categories, subCategories, groceries))
    this.stores.replaceWith(stores.filter { !it.deleted })
}

/** Replaces hidden store ids and per-store orders in memory. Shared by loadData and BackupManager restore. */
internal fun replaceOrders(
    hiddenStoreIds: List<String>,
    categoryOrders: Map<String, List<String>>,
    subCategoryOrders: Map<String, Map<String, List<String>>>,
) {
    DataManagerObject.hiddenStoreIds.replaceWith(hiddenStoreIds)
    DataManagerObject.storeCategoryOrders.replaceWith(categoryOrders)
    DataManagerObject.storeSubCategoryOrders.replaceWith(subCategoryOrders)
}

private fun <T> MutableList<T>.replaceWith(items: Collection<T>) {
    clear()
    addAll(items)
}

private fun <K, V> MutableMap<K, V>.replaceWith(items: Map<K, V>) {
    clear()
    putAll(items)
}
