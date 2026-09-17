package com.example.supercart2.ui.screens

import com.example.supercart2.TestData
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.SubCategoryWithGroceries
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

class MealsScreenEditorStateTest {

    private val scope = CoroutineScope(Dispatchers.Unconfined)

    @Before
    fun setUp() = install(
        groceries = listOf(food("Pasta"), food("Tomato"), food("Gone", deleted = true), food("Basil")),
        meals = listOf(meal("m", "Pasta", ingredient("Pasta", "500 g"), ingredient("Gone", "1"), ingredient("Tomato"))),
    )

    private fun existing() = MealsScreenEditorState("m", scope).apply { load(storedMeal("m")) }

    @Test
    fun `a new meal starts empty and needs a non-blank name`() {
        val state = MealsScreenEditorState(null, scope).apply { load(null) }
        assertTrue(state.isNew)
        assertEquals("", state.name)
        assertTrue(state.ingredients.isEmpty())
        assertFalse(state.canSave)
        state.name = "   "
        assertFalse(state.canSave)
        state.name = "Soup"
        assertTrue(state.canSave)
    }

    @Test
    fun `an existing meal loads its name and visible ingredients`() {
        val state = existing()
        assertFalse(state.isNew)
        assertEquals("Pasta", state.name)
        assertEquals(listOf(ingredient("Pasta", "500 g"), ingredient("Tomato")), state.ingredients)
        assertEquals(listOf("Pasta", "Tomato"), state.rows().map { it.first.name })
    }

    @Test
    fun `quantities can be edited and ingredients removed`() {
        val state = existing()
        state.setQuantity("Tomato", "3")
        state.remove("Pasta")
        assertEquals(listOf(ingredient("Tomato", "3")), state.ingredients)
    }

    @Test
    fun `adding groceries appends new ones with an empty quantity and closes the picker`() {
        val state = existing()
        state.showPicker = true
        state.addGroceries(listOf("Basil", "Pasta"))
        assertEquals(listOf(ingredient("Pasta", "500 g"), ingredient("Tomato"), ingredient("Basil")), state.ingredients)
        assertFalse(state.showPicker)
    }

    @Test
    fun `saving a new meal adds it with a trimmed name and returns its uuid`() {
        val state = MealsScreenEditorState(null, scope).apply { load(null) }
        state.name = "  Soup "
        state.addGroceries(listOf("Basil"))
        state.setQuantity("Basil", " 2 leaves ")
        val uuid = state.save()
        val saved = storedMeal(uuid!!)
        assertEquals("Soup", saved.name)
        assertEquals(listOf(ingredient("Basil", "2 leaves")), saved.ingredients)
        assertEquals(2, DataManagerObject.meals.size)
    }

    @Test
    fun `saving an existing meal updates it and drops ingredients whose grocery disappeared`() {
        val state = existing()
        state.name = " Pasta al pomodoro "
        state.setQuantity("Tomato", "4")
        val sub: SubCategoryWithGroceries = DataManagerObject.categories.single().subCategories.single()
        sub.groceries.replaceAll { if (it.uuid == "Pasta") it.copy(deleted = true) else it }
        assertEquals("m", state.save())
        val saved = storedMeal("m")
        assertEquals("Pasta al pomodoro", saved.name)
        assertEquals(listOf(ingredient("Tomato", "4")), saved.ingredients)
        assertTrue(saved.lastUpdate.isAfter(TestData.OLD))
        assertEquals(1, DataManagerObject.meals.size)
    }

    @Test
    fun `saving with a blank name does nothing`() {
        val state = existing()
        state.name = " "
        assertNull(state.save())
        assertEquals("Pasta", storedMeal("m").name)
        assertEquals(TestData.OLD, storedMeal("m").lastUpdate)
    }
}
