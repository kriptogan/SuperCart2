package com.kriptogan.supercart2.classes

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

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
     * Save sub-categories to local storage
     */
    fun saveSubCategories(subCategories: List<SubCategory>) {
        val json = gson.toJson(subCategories)
        sharedPreferences.edit().putString(SUBCATEGORIES_KEY, json).apply()
    }
    
    /**
     * Get sub-categories from local storage
     */
    fun getSubCategories(): List<SubCategory> {
        val json = sharedPreferences.getString(SUBCATEGORIES_KEY, "[]")
        val type = object : TypeToken<List<SubCategory>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get sub-categories by category ID
     */
    fun getSubCategoriesByCategoryId(categoryId: String): List<SubCategory> {
        return getSubCategories().filter { it.categoryId == categoryId }
    }
    
    /**
     * Add a new sub-category to local storage
     */
    fun addSubCategory(subCategory: SubCategory) {
        val currentSubCategories = getSubCategories().toMutableList()
        currentSubCategories.add(subCategory)
        saveSubCategories(currentSubCategories)
    }
    
    /**
     * Update an existing sub-category in local storage
     */
    fun updateSubCategory(updatedSubCategory: SubCategory) {
        val currentSubCategories = getSubCategories().toMutableList()
        val index = currentSubCategories.indexOfFirst { it.uuid == updatedSubCategory.uuid }
        if (index != -1) {
            currentSubCategories[index] = updatedSubCategory
            saveSubCategories(currentSubCategories)
        }
    }
    
    /**
     * Delete a sub-category from local storage
     */
    fun deleteSubCategory(subCategoryId: String) {
        val currentSubCategories = getSubCategories().toMutableList()
        currentSubCategories.removeAll { it.uuid == subCategoryId }
        saveSubCategories(currentSubCategories)
    }
    
    /**
     * Clear all local data
     */
    fun clearAllData() {
        sharedPreferences.edit().clear().apply()
    }
}
