package com.example.supercart2.models

import java.time.LocalDate
import java.util.UUID

data class Grocery(
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val subCategoryId: String,
    val date: LocalDate? = null,
    val inShoppingList: Boolean = false,
    val isBought: Boolean = false,
    val imageId: Int? = null
)
