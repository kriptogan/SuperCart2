package com.kriptogan.supercart2.classes

import java.time.LocalDateTime
import java.util.UUID

/**
 * Represents a sub-category within a main grocery category
 * @param uuid Unique identifier for the sub-category
 * @param categoryId Reference to the parent Category
 * @param name Display name of the sub-category
 * @param lastUpdate Last update timestamp in ISO string format
 */
data class SubCategory(
    val uuid: String = UUID.randomUUID().toString(),
    val categoryId: String,
    val name: String,
    val lastUpdate: String = LocalDateTime.now().toString()
)
