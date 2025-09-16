package com.example.supercart2.models

data class CategoryWithSubCategories(
    val category: Category,
    val subCategories: MutableList<SubCategoryWithGroceries>
)

data class SubCategoryWithGroceries(
    val subCategory: SubCategory,
    val groceries: MutableList<Grocery>
)
