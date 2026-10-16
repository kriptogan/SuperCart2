package com.example.supercart2.models

import java.time.LocalDateTime
import java.util.UUID

/** An ingredient of a meal: a reference to an existing grocery plus a free-text quantity (e.g. "500 g"). */
data class MealIngredient(
    val groceryId: String,
    val quantity: String = "",
)

/** A named list of ingredients the user can add to the shopping list in one go. Soft-deleted like other entities. */
data class Meal(
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val ingredients: List<MealIngredient> = emptyList(),
    val lastUpdate: LocalDateTime = LocalDateTime.now(),
    val deleted: Boolean = false,
)
