package com.kriptogan.supercart2.models

import java.util.UUID
import java.time.LocalDate

data class Grocery(
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val categoryId: String,
    val subCategoryId: String,
    val expirationDate: LocalDate? = null,
    val inShoppingList: Boolean = false,
    val isBought: Boolean = false,
    val buyEvents: List<LocalDate> = emptyList(),
    val imageUUID: String? = null,
    val averageBuyDays: Int? = null // null means not enough data to calculate
)
