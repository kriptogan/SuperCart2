package com.example.supercart2.data

import com.example.supercart2.models.Grocery
import com.example.supercart2.models.Meal
import com.example.supercart2.models.MealIngredient
import java.time.LocalDateTime

// Meal helpers for DataManagerObject (see the "Meals" section there).

/** Non-deleted meals sorted by name, ignoring case. */
internal fun List<Meal>.sortedVisibleMeals(): List<Meal> =
    filterNot { it.deleted }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

/** Replaces the meal [uuid] with [update] applied and lastUpdate stamped; false when there is no such meal. */
internal fun MutableList<Meal>.replaceMeal(uuid: String, update: (Meal) -> Meal): Boolean {
    val index = indexOfFirst { it.uuid == uuid }
    if (index == -1) return false
    this[index] = update(this[index]).copy(lastUpdate = LocalDateTime.now())
    return true
}

/** Every grocery of this tree by uuid; used on getSortedCategories(), so only visible groceries. */
internal fun List<CategoryWithSubCategories>.visibleGroceriesByUuid(): Map<String, Grocery> =
    flatMap { it.subCategories }.flatMap { it.groceries }.associateBy { it.uuid }

/** Each ingredient of [meal] with its grocery from [groceries], in ingredient order; unresolved ones are skipped. */
internal fun ingredientRows(meal: Meal, groceries: Map<String, Grocery>): List<Pair<Grocery, MealIngredient>> =
    meal.ingredients.mapNotNull { ingredient -> groceries[ingredient.groceryId]?.let { it to ingredient } }

/** Distinct uuids among [groceryIds] of visible groceries not yet on the shopping list. */
internal fun Map<String, Grocery>.unlistedIds(groceryIds: Collection<String>): List<String> =
    groceryIds.distinct().filter { id -> this[id]?.inShoppingList == false }
