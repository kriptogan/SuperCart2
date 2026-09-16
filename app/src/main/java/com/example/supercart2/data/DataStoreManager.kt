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
        val (categories, subCategories, groceries) = DataConverter.flattenCategories(DataManagerObject.categories)
        val stores = DataManagerObject.stores.toList()
        val hiddenStoreIds = DataManagerObject.hiddenStoreIds.toList()
        val storeCategoryOrders = DataManagerObject.storeCategoryOrders.toMap()
        val storeSubCategoryOrders = DataManagerObject.storeSubCategoryOrders.toMap()

        context.dataStore.edit { preferences ->
            preferences[CATEGORIES_KEY] = gson.toJson(categories)
            preferences[SUBCATEGORIES_KEY] = gson.toJson(subCategories)
            preferences[GROCERIES_KEY] = gson.toJson(groceries)
            preferences[STORES_KEY] = gson.toJson(stores)
            preferences[HIDDEN_STORES_KEY] = gson.toJson(hiddenStoreIds)
            preferences[STORE_CATEGORY_ORDERS_KEY] = gson.toJson(storeCategoryOrders)
            preferences[STORE_SUBCATEGORY_ORDERS_KEY] = gson.toJson(storeSubCategoryOrders)
        }
    }

    suspend fun loadData(context: Context) {
        try {
            val preferences = context.dataStore.data.first()

            val categories = preferences.readJson<List<Category>>(CATEGORIES_KEY) ?: emptyList()
            val subCategories = preferences.readJson<List<SubCategory>>(SUBCATEGORIES_KEY) ?: emptyList()
            val groceries = preferences.readJson<List<Grocery>>(GROCERIES_KEY) ?: emptyList()
            val stores = preferences.readJson<List<Store>>(STORES_KEY) ?: emptyList()
            val hiddenStoreIds = preferences.readJson<List<String>>(HIDDEN_STORES_KEY) ?: emptyList()
            val storeCategoryOrders =
                preferences.readOrders<Map<String, List<String>>>(STORE_CATEGORY_ORDERS_KEY) ?: emptyMap()
            val storeSubCategoryOrders =
                preferences.readOrders<Map<String, Map<String, List<String>>>>(STORE_SUBCATEGORY_ORDERS_KEY) ?: emptyMap()

            if (DataConverter.validateRelationships(categories, subCategories, groceries)) {
                DataManagerObject.categories.clear()
                DataManagerObject.categories.addAll(DataConverter.buildNestedStructure(categories, subCategories, groceries))

                DataManagerObject.stores.clear()
                DataManagerObject.stores.addAll(stores.filter { !it.deleted })

                DataManagerObject.hiddenStoreIds.clear()
                DataManagerObject.hiddenStoreIds.addAll(hiddenStoreIds)

                DataManagerObject.storeCategoryOrders.clear()
                DataManagerObject.storeCategoryOrders.putAll(storeCategoryOrders)

                DataManagerObject.storeSubCategoryOrders.clear()
                DataManagerObject.storeSubCategoryOrders.putAll(storeSubCategoryOrders)
            } else {
                android.util.Log.e("DataStoreManager", "Invalid data relationships detected")
            }

            // Initialize default data if no categories exist
            DataInitializer.initializeDefaultData(context)
        } catch (e: Exception) {
            android.util.Log.e("DataStoreManager", "Error loading data", e)
            DataInitializer.initializeDefaultData(context)
        }
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
