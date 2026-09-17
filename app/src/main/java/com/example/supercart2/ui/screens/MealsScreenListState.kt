package com.example.supercart2.ui.screens

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.models.Meal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** What a meal card on the Meals list shows. */
internal data class MealCardData(val uuid: String, val name: String, val ingredientCount: Int)

/** Visible meals sorted by name, each with the number of its ingredients whose grocery is visible. */
internal fun mealsScreenCards(): List<MealCardData> = DataManagerObject.getSortedMeals().map { meal ->
    MealCardData(meal.uuid, meal.name, DataManagerObject.mealIngredientRows(meal).size)
}

/** Persists the current data in [this] scope; call after every mutation of the Meals screens. */
internal fun CoroutineScope.mealsScreenSave() {
    launch { DataStoreManager.saveDataGlobally() }
}

/** UI state of the Meals list: the meal waiting for delete confirmation. Saves run in [scope]. */
@Stable
internal class MealsScreenListState(private val scope: CoroutineScope) {
    var mealToDelete by mutableStateOf<Meal?>(null)
        private set

    fun requestDelete(meal: Meal) {
        mealToDelete = meal
    }

    fun cancelDelete() {
        mealToDelete = null
    }

    /** Soft-deletes the meal waiting for confirmation (if any), saves and closes the confirmation. */
    fun confirmDelete() {
        val meal = mealToDelete ?: return
        DataManagerObject.deleteMeal(meal.uuid)
        scope.mealsScreenSave()
        mealToDelete = null
    }
}
