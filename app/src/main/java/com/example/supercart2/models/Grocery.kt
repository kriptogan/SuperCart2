package com.example.supercart2.models

import java.util.UUID

data class Grocery(
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val subCategoryId: String,
    val inShoppingList: Boolean = false,
    val isBought: Boolean = false
)
