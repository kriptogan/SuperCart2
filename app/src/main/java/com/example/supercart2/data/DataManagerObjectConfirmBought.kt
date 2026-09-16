package com.example.supercart2.data

import com.example.supercart2.models.Grocery
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Confirms every grocery matching [isConfirmed]: see [confirmBought].
 *
 * NOTE (known bug, pinned by tests): each category is rebuilt from `category`, the snapshot taken at the
 * start of the outer loop, so within one category only the last updated sub-category survives.
 */
internal fun MutableList<CategoryWithSubCategories>.confirmBought(today: LocalDate, isConfirmed: (Grocery) -> Boolean) {
    forEachIndexed { catIndex, category ->
        category.subCategories.forEachIndexed { subIndex, subCategory ->
            val updated = subCategory.confirmedGroceries(today, isConfirmed)
            if (updated != subCategory.groceries) this[catIndex] = category.withSubCategory(subIndex, updated)
        }
    }
}

private fun SubCategoryWithGroceries.confirmedGroceries(today: LocalDate, isConfirmed: (Grocery) -> Boolean) =
    groceries.map { if (isConfirmed(it)) confirmBought(it, today) else it }.toMutableList()

// Adds today's buy event (once per day), recomputes the average and takes the item off the list
private fun confirmBought(grocery: Grocery, today: LocalDate): Grocery {
    @Suppress("USELESS_ELVIS") val existingEvents = grocery.buyEvents ?: emptyList() // Gson can leave it null
    val updatedEvents = if (today in existingEvents) existingEvents else existingEvents + today
    return grocery.copy(
        isBought = false, inShoppingList = false, buyEvents = updatedEvents,
        averageBuyDays = calculateAverageBuyDays(updatedEvents), lastUpdate = LocalDateTime.now()
    )
}

// Average days between the newest (up to 4) buy events, rounded down; null with fewer than 2 events
private fun calculateAverageBuyDays(buyEvents: List<LocalDate>): Int? {
    if (buyEvents.size < 2) return null
    return buyEvents.sortedDescending().take(4)
        .zipWithNext { newer, older -> newer.toEpochDay() - older.toEpochDay() }
        .average().toInt()
}
