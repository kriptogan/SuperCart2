package com.example.supercart2.ui.screens

import com.example.supercart2.TestData
import com.example.supercart2.ui.screens.MealsScreenTestFixtures.food
import com.example.supercart2.ui.screens.MealsScreenTestFixtures.ingredient
import com.example.supercart2.ui.screens.MealsScreenTestFixtures.install
import com.example.supercart2.ui.screens.MealsScreenTestFixtures.meal
import com.example.supercart2.ui.screens.MealsScreenTestFixtures.storedMeal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MealsScreenListStateTest {

    private val state = MealsScreenListState(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun `cards list visible meals by name with their visible ingredient count`() {
        install(
            groceries = listOf(food("Tomato"), food("Gone", deleted = true)),
            meals = listOf(
                meal("b", "Bolognese", ingredient("Tomato"), ingredient("Gone"), ingredient("missing")),
                meal("a", "Arrabbiata"),
                meal("d", "Deleted", deleted = true),
            ),
        )
        assertEquals(
            listOf(MealCardData("a", "Arrabbiata", 0), MealCardData("b", "Bolognese", 1)),
            mealsScreenCards(),
        )
    }

    @Test
    fun `requesting and cancelling a deletion`() {
        val pizza = meal("pizza", "Pizza")
        state.requestDelete(pizza)
        assertEquals(pizza, state.mealToDelete)
        state.cancelDelete()
        assertNull(state.mealToDelete)
    }

    @Test
    fun `confirming a deletion soft-deletes the meal and closes the confirmation`() {
        install(meals = listOf(meal("pizza", "Pizza"), meal("pie", "Pie")))
        state.requestDelete(storedMeal("pizza"))
        state.confirmDelete()
        assertTrue(storedMeal("pizza").deleted)
        assertTrue(storedMeal("pizza").lastUpdate.isAfter(TestData.OLD))
        assertFalse(storedMeal("pie").deleted)
        assertNull(state.mealToDelete)
    }

    @Test
    fun `confirming without a requested deletion does nothing`() {
        install(meals = listOf(meal("pizza", "Pizza")))
        state.confirmDelete()
        assertFalse(storedMeal("pizza").deleted)
    }
}
