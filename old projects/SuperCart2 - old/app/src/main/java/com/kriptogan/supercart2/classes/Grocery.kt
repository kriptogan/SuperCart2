package com.kriptogan.supercart2.classes

import java.time.LocalDateTime
import java.util.UUID

/**
 * Represents a grocery item in the SuperCart app
 * @param uuid Unique identifier for the grocery item
 * @param name Display name of the grocery item (שם המצרך)
 * @param subCategoryId Reference to the parent SubCategory (required)
 * @param expirationDate Expiration date as ISO string (תאריך תפוגה, optional)
 * @param lastTimeBoughtDays Days since last purchase (מספר ימים מאז הקנייה האחרונה, optional)
 * @param averageBuyingDays Average days between purchases (ממוצע ימים בין קניות, optional)
 * @param buyEvents List of purchase dates as ISO strings
 * @param inShoppingList Whether the item is in the shopping list
 * @param isBought Whether the item has been purchased
 * @param lastUpdate Last update timestamp in ISO string format
 */
data class Grocery(
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val subCategoryId: String, // Required field - every grocery must belong to a sub-category
    val expirationDate: String? = null,
    val lastTimeBoughtDays: Int? = null,
    val averageBuyingDays: Int? = null,
    val buyEvents: List<String> = emptyList(),
    val inShoppingList: Boolean = false,
    val isBought: Boolean = false,
    val lastUpdate: String = LocalDateTime.now().toString()
)
