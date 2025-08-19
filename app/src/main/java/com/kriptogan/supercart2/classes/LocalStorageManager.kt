package com.kriptogan.supercart2.classes

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kriptogan.supercart2.classes.Grocery

/**
 * Manages local storage operations for the SuperCart app
 * Handles storing and retrieving data locally before syncing to Firestore
 */
class LocalStorageManager(context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "SuperCart2Data",
        Context.MODE_PRIVATE
    )
    
    private val gson = Gson()
    
    companion object {
        private const val CATEGORIES_KEY = "categories"
        private const val GROCERIES_KEY = "groceries"
        private const val SUBCATEGORIES_KEY = "subcategories"
        private const val GROUPS_KEY = "groups"
    }
    
    /**
     * Save categories to local storage
     */
    fun saveCategories(categories: List<Category>) {
        val json = gson.toJson(categories)
        sharedPreferences.edit().putString(CATEGORIES_KEY, json).apply()
    }
    
    /**
     * Get categories from local storage
     */
    fun getCategories(): List<Category> {
        val json = sharedPreferences.getString(CATEGORIES_KEY, "[]")
        val type = object : TypeToken<List<Category>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Add a new category to local storage
     */
    fun addCategory(category: Category) {
        val currentCategories = getCategories().toMutableList()
        currentCategories.add(category)
        saveCategories(currentCategories)
    }
    
    /**
     * Update an existing category in local storage
     */
    fun updateCategory(updatedCategory: Category) {
        val currentCategories = getCategories().toMutableList()
        val index = currentCategories.indexOfFirst { it.uuid == updatedCategory.uuid }
        if (index != -1) {
            currentCategories[index] = updatedCategory
            saveCategories(currentCategories)
        }
    }
    
    /**
     * Delete a category from local storage
     */
    fun deleteCategory(categoryId: String) {
        val currentCategories = getCategories().toMutableList()
        currentCategories.removeAll { it.uuid == categoryId }
        saveCategories(currentCategories)
    }
    
    /**
     * Clear all local data
     */
    fun clearAllData() {
        sharedPreferences.edit().clear().apply()
    }
    
    // ===== GROCERY METHODS =====
    
    /**
     * Save groceries to local storage
     */
    fun saveGroceries(groceries: List<Grocery>) {
        val json = gson.toJson(groceries)
        sharedPreferences.edit().putString(GROCERIES_KEY, json).apply()
    }
    
    /**
     * Get groceries from local storage
     */
    fun getGroceries(): List<Grocery> {
        val json = sharedPreferences.getString(GROCERIES_KEY, "[]")
        val type = object : TypeToken<List<Grocery>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Add a new grocery to local storage
     */
    fun addGrocery(grocery: Grocery) {
        val currentGroceries = getGroceries().toMutableList()
        currentGroceries.add(grocery)
        saveGroceries(currentGroceries)
    }
    
    /**
     * Update an existing grocery in local storage
     */
    fun updateGrocery(updatedGrocery: Grocery) {
        val currentGroceries = getGroceries().toMutableList()
        val index = currentGroceries.indexOfFirst { it.uuid == updatedGrocery.uuid }
        if (index != -1) {
            currentGroceries[index] = updatedGrocery
            saveGroceries(currentGroceries)
        }
    }
    
    /**
     * Delete a grocery from local storage
     */
    fun deleteGrocery(groceryId: String) {
        val currentGroceries = getGroceries().toMutableList()
        currentGroceries.removeAll { it.uuid == groceryId }
        saveGroceries(currentGroceries)
    }
    
    /**
     * Clear all groceries from local storage
     */
    fun clearAllGroceries() {
        sharedPreferences.edit().remove(GROCERIES_KEY).apply()
    }
}
