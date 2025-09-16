package com.example.supercart2.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.supercart2.models.Category
import com.example.supercart2.models.SubCategory
import com.example.supercart2.models.Grocery

object DataManagerObject {
    val categories: SnapshotStateList<CategoryWithSubCategories> = mutableStateListOf()
    
    // Version counter to force recomposition
    private var _version by mutableStateOf(0)
    val version: Int get() = _version
    
    private fun notifyUpdate() {
        _version++
        android.util.Log.d("datastore test", "Data updated, version: $_version")
    }
    
    // Get categories sorted by viewOrder for consistent display order
    fun getSortedCategories(): List<CategoryWithSubCategories> {
        return categories.sortedBy { it.category.viewOrder }
    }
    
    // Call this after any data modification
    fun updateData() {
        notifyUpdate()
    }
}

data class CategoryWithSubCategories(
    val category: Category,
    val subCategories: MutableList<SubCategoryWithGroceries>
) {
    fun getGroceriesCount(): Int = subCategories.sumOf { it.groceries.size }
}

data class SubCategoryWithGroceries(
    val subCategory: SubCategory,
    val groceries: MutableList<Grocery>
)
