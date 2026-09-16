package com.example.supercart2.data

import com.example.supercart2.models.Category
import com.example.supercart2.models.SubCategory

object DataInitializer {

    /** Creates and saves the default categories, only when there are no categories at all. */
    suspend fun initializeDefaultData(context: android.content.Context) {
        if (DataManagerObject.categories.isNotEmpty()) return
        createDefaultCategories()
        DataStoreManager.saveData(context)
    }

    // Names are stored in English and translated at display time (see LocalizedCategoryNames).
    // "Other" is the protected fallback category; every default "General" sub-category is protected.
    private fun createDefaultCategories() {
        val defaults = listOf("Other", "Vegetables", "Fruits").mapIndexed { index, name ->
            val category = Category(name = name, default = true, viewOrder = index + 1, protected = name == "Other")
            val general = SubCategory(categoryId = category.uuid, name = "General", protected = true)
            CategoryWithSubCategories(category, mutableListOf(SubCategoryWithGroceries(general, mutableListOf())))
        }
        DataManagerObject.categories.addAll(defaults)
    }
}
