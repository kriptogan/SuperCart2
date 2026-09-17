package com.example.supercart2.ui.screens

import com.example.supercart2.TestData
import com.example.supercart2.TestData.findGrocery
import com.example.supercart2.data.DataManagerObject
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
import org.junit.Before
import org.junit.Test

class MealsScreenPrepareStateTest {

    private val state = MealsScreenPrepareState(CoroutineScope(Dispatchers.Unconfined))

    @Before
    fun setUp() = install(
        groceries = listOf(food("Pasta"), food("Tomato"), food("Salt", inList = true), food("Basil")),
        meals = listOf(meal("m", "Pasta", ingredient("Pasta"), ingredient("Tomato", "3"), ingredient("Salt"))),
    )

    private fun rows() = DataManagerObject.mealIngredientRows(storedMeal("m"))

    @Test
    fun `every row starts selected and toggling flips it`() {
        assertTrue(state.isSelected("Pasta"))
        state.toggle("Pasta")
        assertFalse(state.isSelected("Pasta"))
        assertTrue(state.isSelected("Tomato"))
        state.toggle("Pasta")
        assertTrue(state.isSelected("Pasta"))
    }

    @Test
    fun `groceries already on the shopping list are always checked`() {
        state.toggle("Salt")
        state.toggle("Pasta")
        assertTrue(state.isChecked(findGrocery("Salt")!!))
        assertFalse(state.isChecked(findGrocery("Pasta")!!))
        assertTrue(state.isChecked(findGrocery("Tomato")!!))
    }

    @Test
    fun `selected ids skip deselected groceries and groceries already on the shopping list`() {
        assertEquals(listOf("Pasta", "Tomato"), state.selectedIds(rows()))
        state.toggle("Tomato")
        assertEquals(listOf("Pasta"), state.selectedIds(rows()))
    }

    @Test
    fun `adding puts the selected groceries on the shopping list and shows the count`() {
        state.toggle("Tomato")
        state.addToShoppingList(rows()) { count -> "added $count" }
        assertTrue(findGrocery("Pasta")!!.inShoppingList)
        assertTrue(findGrocery("Pasta")!!.lastUpdate.isAfter(TestData.OLD))
        assertFalse(findGrocery("Tomato")!!.inShoppingList)
        assertFalse(findGrocery("Basil")!!.inShoppingList)
        assertEquals("added 1", state.snackbarHostState.currentSnackbarData?.visuals?.message)
        assertEquals(emptyList<String>(), state.selectedIds(rows()))
    }

    @Test
    fun `adding with nothing selected changes nothing and shows no message`() {
        state.toggle("Pasta")
        state.toggle("Tomato")
        state.addToShoppingList(rows()) { count -> "added $count" }
        assertFalse(findGrocery("Pasta")!!.inShoppingList)
        assertNull(state.snackbarHostState.currentSnackbarData)
    }
}
