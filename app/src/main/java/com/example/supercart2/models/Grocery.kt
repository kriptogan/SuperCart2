package com.example.supercart2.models

import java.util.UUID
import java.time.LocalDate

data class Grocery(
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val categoryId: String,
    val subCategoryId: String,
    val date: LocalDate = LocalDate.now(),
    val inShoppingList: Boolean = false,
    val isBought: Boolean = false
)
