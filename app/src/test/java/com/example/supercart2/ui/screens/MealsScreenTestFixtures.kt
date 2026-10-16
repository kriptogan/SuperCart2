package com.example.supercart2.ui.screens

import com.example.supercart2.TestData
import com.example.supercart2.TestData.category
import com.example.supercart2.TestData.node
import com.example.supercart2.TestData.sub
import com.example.supercart2.TestData.subNode
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.models.Grocery
import com.example.supercart2.models.Meal
import com.example.supercart2.models.MealIngredient

/** Shared data for the Meals screen tests: one category "Food" with sub-category "FoodSub". */
object MealsScreenTestFixtures {

    /** A grocery whose uuid and name are both [name]. */
    fun food(name: String, inList: Boolean = false, deleted: Boolean = false): Grocery =
        TestData.grocery(name, "Food", "FoodSub", inShoppingList = inList, deleted = deleted)

    fun ingredient(groceryId: String, quantity: String = "") = MealIngredient(groceryId, quantity)

    fun meal(uuid: String, name: String = uuid, vararg ingredients: MealIngredient, deleted: Boolean = false) =
        Meal(uuid = uuid, name = name, ingredients = ingredients.toList(), lastUpdate = TestData.OLD, deleted = deleted)

    fun install(groceries: List<Grocery> = emptyList(), meals: List<Meal> = emptyList()) {
        TestData.reset()
        DataManagerObject.categories.add(
            node(category("Food", viewOrder = 1), subNode(sub("FoodSub", "Food"), *groceries.toTypedArray()))
        )
        DataManagerObject.meals.addAll(meals)
    }

    fun storedMeal(uuid: String): Meal = DataManagerObject.meals.first { it.uuid == uuid }
}
