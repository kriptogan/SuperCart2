package com.example.supercart2.ui.screens

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.models.Meal
import com.example.supercart2.models.MealIngredient
import kotlinx.coroutines.CoroutineScope

/** Draft of the meal editor; [mealId] is null for a new meal. Saves run in [scope]. */
@Stable
internal class MealsScreenEditorState(private val mealId: String?, private val scope: CoroutineScope) {
    var name by mutableStateOf("")
    var ingredients by mutableStateOf(listOf<MealIngredient>())
        private set
    var showPicker by mutableStateOf(false)
    val isNew: Boolean get() = mealId == null
    val canSave: Boolean get() = name.isNotBlank()

    /** Fills the draft from [meal] (nothing for a new meal), keeping only ingredients with a visible grocery. */
    fun load(meal: Meal?) {
        name = meal?.name ?: ""
        ingredients = meal?.let { DataManagerObject.mealIngredientRows(it) }.orEmpty().map { it.second }
    }

    /** The draft's ingredients whose grocery is visible, with that grocery. */
    fun rows(): List<MealsScreenIngredientRow> =
        DataManagerObject.mealIngredientRows(Meal(uuid = "", name = "", ingredients = ingredients))

    fun setQuantity(groceryId: String, quantity: String) {
        ingredients = ingredients.map { if (it.groceryId == groceryId) it.copy(quantity = quantity) else it }
    }

    fun remove(groceryId: String) {
        ingredients = ingredients.filterNot { it.groceryId == groceryId }
    }

    /** Appends the groceries not in the draft yet, with an empty quantity, and closes the picker. */
    fun addGroceries(groceryIds: List<String>) {
        val known = ingredients.map { it.groceryId }.toSet()
        ingredients = ingredients + groceryIds.distinct().filterNot { it in known }.map { MealIngredient(it) }
        showPicker = false
    }

    /** Adds or updates the meal (trimmed, visible ingredients only), saves and returns its uuid; null if blank. */
    fun save(): String? {
        if (!canSave) return null
        val uuid = mealId?.also(::updateMeal) ?: addMeal()
        scope.mealsScreenSave()
        return uuid
    }

    private fun savedIngredients(): List<MealIngredient> = rows().map { (_, it) -> it.copy(quantity = it.quantity.trim()) }

    private fun addMeal(): String =
        Meal(name = name.trim(), ingredients = savedIngredients()).also(DataManagerObject::addMeal).uuid

    private fun updateMeal(uuid: String) {
        val trimmedName = name.trim()
        val saved = savedIngredients()
        DataManagerObject.updateMeal(uuid) { it.copy(name = trimmedName, ingredients = saved) }
    }
}
