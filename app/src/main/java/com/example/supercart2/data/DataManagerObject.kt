package com.example.supercart2.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.supercart2.models.Category
import com.example.supercart2.models.SubCategory
import com.example.supercart2.models.Grocery

object DataManagerObject {
    val categories: SnapshotStateList<CategoryWithSubCategories> = mutableStateListOf()
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
