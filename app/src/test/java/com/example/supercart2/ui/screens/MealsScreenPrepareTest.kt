package com.example.supercart2.ui.screens

import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.supercart2.TestData
import com.example.supercart2.TestData.findGrocery
import com.example.supercart2.ui.screens.MealsScreenTestFixtures.food
import com.example.supercart2.ui.screens.MealsScreenTestFixtures.ingredient
import com.example.supercart2.ui.screens.MealsScreenTestFixtures.install
import com.example.supercart2.ui.screens.MealsScreenTestFixtures.meal
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "h1200dp")
class MealsScreenPrepareTest {

    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()

    @Before
    fun setUp() = TestData.reset()

    @After
    fun flushMainLooper() = shadowOf(Looper.getMainLooper()).idle()

    private fun show(mealId: String = "m") {
        compose.setContent { MealsPrepareView(mealId, onBack = { events += "back" }, onEdit = { events += "edit" }) }
        compose.waitForIdle()
    }

    private fun installPasta() = install(
        groceries = listOf(food("Spaghetti"), food("Tomato"), food("Salt", inList = true), food("Gone", deleted = true)),
        meals = listOf(
            meal("m", "Pasta night", ingredient("Spaghetti", "500 g"), ingredient("Tomato"), ingredient("Salt"),
                ingredient("Gone", "1")),
        ),
    )

    private fun checkbox(index: Int) = compose.onAllNodes(isToggleable())[index]

    @Test
    fun `header shows the meal name, back and edit`() {
        installPasta()
        show()
        compose.onNodeWithText("Pasta night").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithContentDescription("Edit meal").performClick()
        assertEquals(listOf("back", "edit"), events)
    }

    @Test
    fun `rows show visible ingredients with quantities, pre-selected unless already in the list`() {
        installPasta()
        show()
        compose.onNodeWithText("Spaghetti").assertIsDisplayed()
        compose.onNodeWithText("500 g").assertIsDisplayed()
        compose.onNodeWithText("Tomato").assertIsDisplayed()
        compose.onNodeWithText("Gone").assertDoesNotExist()
        compose.onNodeWithText("Already in list").assertIsDisplayed()
        compose.onAllNodes(isToggleable()).fetchSemanticsNodes().let { assertEquals(3, it.size) }
        checkbox(0).assertIsOn().assertIsEnabled()
        checkbox(1).assertIsOn().assertIsEnabled()
        checkbox(2).assertIsOn().assertIsNotEnabled()
        compose.onNodeWithText("Add 2 to shopping list").assertIsEnabled()
    }

    @Test
    fun `toggling rows and checkboxes updates the add button, which is disabled at zero`() {
        installPasta()
        show()
        compose.onNodeWithText("Tomato").performClick()
        checkbox(1).assertIsOff()
        compose.onNodeWithText("Add 1 to shopping list").assertIsEnabled()
        checkbox(0).performClick()
        checkbox(0).assertIsOff()
        compose.onNodeWithText("Add 0 to shopping list").assertIsNotEnabled()
        compose.onNodeWithText("Salt").performClick()
        checkbox(2).assertIsOn()
        checkbox(1).performClick()
        compose.onNodeWithText("Add 1 to shopping list").assertIsEnabled()
    }

    @Test
    fun `adding puts the selected groceries on the list, shows a snackbar and refreshes the rows`() {
        installPasta()
        show()
        compose.onNodeWithText("Tomato").performClick()
        compose.onNodeWithText("Add 1 to shopping list").performClick()

        assertTrue(findGrocery("Spaghetti")!!.inShoppingList)
        assertFalse(findGrocery("Tomato")!!.inShoppingList)
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("1 items added to the shopping list").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithText("Already in list").fetchSemanticsNodes().let { assertEquals(2, it.size) }
        checkbox(0).assertIsNotEnabled()
        compose.onNodeWithText("Add 0 to shopping list").assertIsNotEnabled()
    }

    @Test
    fun `a meal without visible ingredients offers to add ingredients`() {
        install(groceries = listOf(food("Gone", deleted = true)), meals = listOf(meal("m", "Empty", ingredient("Gone"))))
        show()
        compose.onNodeWithText("Empty").assertIsDisplayed()
        compose.onNodeWithText("No ingredients yet").assertIsDisplayed()
        compose.onNodeWithText("Add 0 to shopping list").assertDoesNotExist()
        compose.onNodeWithText("Add ingredients").performClick()
        assertEquals(listOf("edit"), events)
    }

    @Test
    fun `an unknown meal goes back`() {
        installPasta()
        show("unknown")
        assertEquals(listOf("back"), events)
    }

    @Test
    fun `a deleted meal goes back`() {
        install(meals = listOf(meal("m", "Old", deleted = true)))
        show()
        compose.onNodeWithText("Old").assertDoesNotExist()
        assertEquals(listOf("back"), events)
    }
}
