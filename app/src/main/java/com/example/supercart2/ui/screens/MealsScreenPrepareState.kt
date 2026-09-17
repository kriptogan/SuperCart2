package com.example.supercart2.ui.screens

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.models.Grocery
import com.example.supercart2.models.MealIngredient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** An ingredient row of the Prepare sub-screen: the current grocery and the meal's ingredient. */
internal typealias MealsScreenIngredientRow = Pair<Grocery, MealIngredient>

/**
 * UI state of the Prepare sub-screen. Every ingredient starts selected; only deselections are remembered.
 * Saves and messages run in [scope].
 */
@Stable
internal class MealsScreenPrepareState(private val scope: CoroutineScope) {
    private var deselectedIds by mutableStateOf(setOf<String>())
    val snackbarHostState = SnackbarHostState()

    fun isSelected(groceryId: String): Boolean = groceryId !in deselectedIds

    /** Checkbox state: groceries already on the shopping list always show as checked. */
    fun isChecked(grocery: Grocery): Boolean = grocery.inShoppingList || isSelected(grocery.uuid)

    fun toggle(groceryId: String) {
        deselectedIds = if (isSelected(groceryId)) deselectedIds + groceryId else deselectedIds - groceryId
    }

    /** Selected groceries of [rows] that are not on the shopping list yet, in row order. */
    fun selectedIds(rows: List<MealsScreenIngredientRow>): List<String> =
        rows.map { it.first }.filter { !it.inShoppingList && isSelected(it.uuid) }.map { it.uuid }

    /** Adds the selected groceries to the shopping list, saves and shows [message] for the count added. */
    fun addToShoppingList(rows: List<MealsScreenIngredientRow>, message: (Int) -> String) {
        val added = DataManagerObject.addGroceriesToShoppingList(selectedIds(rows))
        if (added == 0) return
        scope.mealsScreenSave()
        scope.launch { snackbarHostState.showSnackbar(message(added)) }
    }
}
