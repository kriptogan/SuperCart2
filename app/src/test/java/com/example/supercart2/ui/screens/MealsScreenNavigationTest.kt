package com.example.supercart2.ui.screens

import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.example.supercart2.ui.screens.MealsScreenTestFixtures.food
import com.example.supercart2.ui.screens.MealsScreenTestFixtures.ingredient
import com.example.supercart2.ui.screens.MealsScreenTestFixtures.install
import com.example.supercart2.ui.screens.MealsScreenTestFixtures.meal
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "h1200dp")
class MealsScreenNavigationTest {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun setUp() {
        install(groceries = listOf(food("Spaghetti")), meals = listOf(meal("m", "Pasta", ingredient("Spaghetti"))))
        compose.setContent { MealsScreen() }
    }

    @After
    fun flushMainLooper() = shadowOf(Looper.getMainLooper()).idle()

    private fun nameField() = compose.onNode(hasSetTextAction() and hasText("Meal name"))

    @Test
    fun `cancel returns to the list for a new meal and to prepare for an existing one`() {
        assertEquals(MealsSubScreen.List, MealsSubScreen.Edit(null).cancelTarget())
        assertEquals(MealsSubScreen.Prepare("m"), MealsSubScreen.Edit("m").cancelTarget())
    }

    @Test
    fun `a card opens prepare whose back returns to the list`() {
        compose.onNodeWithText("Pasta").performClick()
        compose.onNodeWithText("Add 1 to shopping list").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("1 ingredients").assertIsDisplayed()
    }

    @Test
    fun `create opens a new meal whose cancel returns to the list`() {
        compose.onNodeWithContentDescription("Create meal").performClick()
        compose.onNodeWithText("New meal").assertIsDisplayed()
        compose.onNodeWithContentDescription("Cancel").performClick()
        compose.onNodeWithText("1 ingredients").assertIsDisplayed()
    }

    @Test
    fun `saving a new meal opens its prepare view, whose add ingredients opens the editor`() {
        compose.onNodeWithContentDescription("Create meal").performClick()
        nameField().performTextInput("Soup")
        compose.onNodeWithContentDescription("Save").performClick()
        compose.onNodeWithText("Soup").assertIsDisplayed()
        compose.onNodeWithText("No ingredients yet").assertIsDisplayed()

        compose.onNodeWithText("Add ingredients").performClick()
        compose.onNodeWithText("Edit meal").assertIsDisplayed()
        nameField().assertIsDisplayed()
    }

    @Test
    fun `the list edit icon opens the editor whose cancel goes to prepare`() {
        compose.onNodeWithContentDescription("Edit meal").performClick()
        nameField().assertIsDisplayed()
        compose.onNodeWithContentDescription("Cancel").performClick()
        compose.onNodeWithText("Add 1 to shopping list").assertIsDisplayed()
    }

    @Test
    fun `editing from prepare and saving returns to prepare with the new name`() {
        compose.onNodeWithText("Pasta").performClick()
        compose.onNodeWithContentDescription("Edit meal").performClick()
        nameField().performTextClearance()
        nameField().performTextInput("Pasta night")
        compose.onNodeWithContentDescription("Save").performClick()
        compose.onNodeWithText("Pasta night").assertIsDisplayed()
        compose.onNodeWithText("Add 1 to shopping list").assertIsDisplayed()
    }
}
