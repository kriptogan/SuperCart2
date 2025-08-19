package com.kriptogan.supercart2.classes

import java.time.LocalDateTime
import java.util.UUID

/**
 * Represents a grocery category in the SuperCart app
 * @param uuid Unique identifier for the category
 * @param name Display name of the category
 * @param default Whether this is a default category
 * @param viewOrder Order for display purposes
 * @param groupId Optional reference to a sharing group
 * @param lastUpdate Last update timestamp in ISO string format
 */
data class Category(
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val default: Boolean,
    val viewOrder: Int,
    val groupId: String? = null,
    val lastUpdate: String = LocalDateTime.now().toString()
)
