package com.example.supercart2.ui.screens

import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.supercart2.TestData
import com.example.supercart2.ui.screens.MealsScreenTestFixtures.food
import com.example.supercart2.ui.screens.MealsScreenTestFixtures.ingredient
import com.example.supercart2.ui.screens.MealsScreenTestFixtures.install
import com.example.supercart2.ui.screens.MealsScreenTestFixtures.meal
import com.example.supercart2.ui.screens.MealsScreenTestFixtures.storedMeal
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
class MealsScreenListTest {

    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()

    @Before
    fun setUp() = TestData.reset()

    @After
    fun flushMainLooper() = shadowOf(Looper.getMainLooper()).idle()

    private fun show() {
        compose.setContent {
            MealsListView(
                onCreate = { events += "create" },
                onOpen = { events += "open:$it" },
                onEdit = { events += "edit:$it" },
            )
        }
    }

    private fun installTwoMeals() = install(
        groceries = listOf(food("Tomato"), food("Cheese"), food("Gone", deleted = true), food("Apple")),
        meals = listOf(
            meal("pizza", "Pizza", ingredient("Tomato"), ingredient("Cheese"), ingredient("Gone"), ingredient("x")),
            meal("pie", "apple pie", ingredient("Apple")),
            meal("old", "Old meal", deleted = true),
        ),
    )

    @Test
    fun `empty state shows the title, the hint and a create button`() {
        show()
        compose.onNodeWithText("Meals").assertIsDisplayed()
        compose.onNodeWithText("No meals yet").assertIsDisplayed()
        compose.onNodeWithContentDescription("Create meal").performClick()
        assertEquals(listOf("create"), events)
    }

    @Test
    fun `cards are sorted by name and count only visible ingredients`() {
        installTwoMeals()
        show()
        compose.onNodeWithText("No meals yet").assertDoesNotExist()
        compose.onNodeWithText("Old meal").assertDoesNotExist()
        compose.onNodeWithText("2 ingredients").assertIsDisplayed()
        compose.onNodeWithText("1 ingredients").assertIsDisplayed()
        val pieTop = compose.onNodeWithText("apple pie").fetchSemanticsNode().boundsInRoot.top
        val pizzaTop = compose.onNodeWithText("Pizza").fetchSemanticsNode().boundsInRoot.top
        assertTrue(pieTop < pizzaTop)
    }

    @Test
    fun `tapping a card opens it and the edit icon edits it`() {
        installTwoMeals()
        show()
        compose.onNodeWithText("Pizza").performClick()
        compose.onAllNodesWithContentDescription("Edit meal")[0].performClick()
        assertEquals(listOf("open:pizza", "edit:pie"), events)
    }

    @Test
    fun `delete asks for confirmation and cancel keeps the meal`() {
        installTwoMeals()
        show()
        compose.onAllNodesWithContentDescription("Delete meal")[1].performClick()
        compose.onNodeWithText("Are you sure you want to delete 'Pizza'?").assertIsDisplayed()
        compose.onNodeWithContentDescription("Cancel").performClick()
        compose.onNodeWithText("Are you sure you want to delete 'Pizza'?").assertDoesNotExist()
        assertFalse(storedMeal("pizza").deleted)
        compose.onNodeWithText("Pizza").assertIsDisplayed()
        assertTrue(events.isEmpty())
    }

    @Test
    fun `confirming the deletion soft-deletes the meal and removes its card`() {
        installTwoMeals()
        show()
        compose.onAllNodesWithContentDescription("Delete meal")[1].performClick()
        compose.onNodeWithContentDescription("Delete").performClick()
        assertTrue(storedMeal("pizza").deleted)
        assertTrue(storedMeal("pizza").lastUpdate.isAfter(TestData.OLD))
        compose.onNodeWithText("Pizza").assertDoesNotExist()
        compose.onNodeWithText("apple pie").assertIsDisplayed()
        assertTrue(events.isEmpty())
    }
}
