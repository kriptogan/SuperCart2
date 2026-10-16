package com.example.supercart2.ui.screens

import com.example.supercart2.models.Grocery
import com.example.supercart2.ui.screens.MealsScreenTestFixtures.food
import com.example.supercart2.ui.screens.MealsScreenTestFixtures.install
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MealsScreenIngredientPickerStateTest {

    private val state = MealsScreenIngredientPickerState(excludedIds = setOf("Salt"))

    @Before
    fun setUp() = install(
        groceries = listOf(food("tomato"), food("Basil"), food("Salt"), food("Gone", deleted = true), food("Pasta")),
    )

    private fun names(groceries: List<Grocery>) = groceries.map { it.name }

    @Test
    fun `candidates are visible groceries not excluded, sorted by name ignoring case`() {
        assertEquals(listOf("Basil", "Pasta", "tomato"), names(state.candidates()))
    }

    @Test
    fun `the query filters candidates by name ignoring case`() {
        state.query = "TO"
        assertEquals(listOf("tomato"), names(state.visibleCandidates(state.candidates())))
        state.query = ""
        assertEquals(listOf("Basil", "Pasta", "tomato"), names(state.visibleCandidates(state.candidates())))
    }

    @Test
    fun `toggling selects and deselects, confirmed ids follow the candidate order`() {
        state.toggle("tomato")
        state.toggle("Basil")
        state.toggle("Pasta")
        state.toggle("Pasta")
        assertTrue(state.isSelected("tomato"))
        assertFalse(state.isSelected("Pasta"))
        assertEquals(listOf("Basil", "tomato"), state.confirmedIds(state.candidates()))
    }

    @Test
    fun `opening and closing the grocery creation`() {
        state.openGroceryCreation()
        assertTrue(state.showGroceryCreation)
        state.closeGroceryCreation()
        assertFalse(state.showGroceryCreation)
    }

    @Test
    fun `a created grocery is selected and stays visible whatever the query`() {
        val created = food("Oregano")
        install(groceries = listOf(food("Basil"), created))
        state.query = "zzz"
        state.onGroceryCreated(created)
        assertTrue(state.isSelected("Oregano"))
        assertEquals(listOf("Oregano"), names(state.visibleCandidates(state.candidates())))
        assertEquals(listOf("Oregano"), state.confirmedIds(state.candidates()))
    }
}
