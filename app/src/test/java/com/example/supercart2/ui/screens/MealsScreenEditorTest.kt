package com.example.supercart2.ui.screens

import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.example.supercart2.TestData
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.ui.screens.MealsScreenTestFixtures.food
import com.example.supercart2.ui.screens.MealsScreenTestFixtures.ingredient
import com.example.supercart2.ui.screens.MealsScreenTestFixtures.install
import com.example.supercart2.ui.screens.MealsScreenTestFixtures.meal
import com.example.supercart2.ui.screens.MealsScreenTestFixtures.storedMeal
import org.junit.After
import org.junit.Assert.assertEquals
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
class MealsScreenEditorTest {

    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()

    @Before
    fun setUp() = install(
        groceries = listOf(food("Spaghetti"), food("Tomato"), food("Gone", deleted = true), food("Basil")),
        meals = listOf(
            meal("m", "Pasta", ingredient("Spaghetti", "500 g"), ingredient("Gone", "1"), ingredient("Tomato")),
        ),
    )

    @After
    fun flushMainLooper() = shadowOf(Looper.getMainLooper()).idle()

    private fun show(mealId: String?) {
        compose.setContent {
            MealsEditorView(mealId, onCancel = { events += "cancel" }, onSaved = { events += "saved:$it" })
        }
        compose.waitForIdle()
    }

    private fun nameField() = compose.onNode(hasSetTextAction() and hasText("Meal name"))
    private fun quantityField(value: String) = compose.onNode(hasSetTextAction() and hasText(value))
    private fun saveButton() = compose.onNodeWithContentDescription("Save")

    @Test
    fun `a new meal needs a non-blank name`() {
        show(null)
        compose.onNodeWithText("New meal").assertIsDisplayed()
        saveButton().assertIsNotEnabled()
        nameField().performTextInput("   ")
        saveButton().assertIsNotEnabled()
        nameField().performTextInput("Soup")
        saveButton().assertIsEnabled()
    }

    @Test
    fun `saving a new meal stores the trimmed name and reports its uuid`() {
        show(null)
        nameField().performTextInput("  Soup ")
        saveButton().performClick()
        val saved = DataManagerObject.meals.single { it.uuid != "m" }
        assertEquals("Soup", saved.name)
        assertTrue(saved.ingredients.isEmpty())
        assertEquals(listOf("saved:${saved.uuid}"), events)
    }

    @Test
    fun `an existing meal shows its name and visible ingredients with quantities`() {
        show("m")
        compose.onNodeWithText("Edit meal").assertIsDisplayed()
        nameField().assertTextContains("Pasta")
        compose.onNodeWithText("Spaghetti").assertIsDisplayed()
        compose.onNodeWithText("Tomato").assertIsDisplayed()
        compose.onNodeWithText("Gone").assertDoesNotExist()
        quantityField("500 g").assertIsDisplayed()
    }

    @Test
    fun `editing quantities, removing ingredients and saving updates the meal`() {
        show("m")
        nameField().performTextClearance()
        nameField().performTextInput("Pasta night ")
        quantityField("500 g").performTextClearance()
        quantityField("500 g").assertDoesNotExist()
        compose.onAllNodesWithContentDescription("Remove ingredient")[1].performClick()
        compose.onNodeWithText("Tomato").assertDoesNotExist()
        compose.onNode(hasSetTextAction() and hasText("Quantity")).performTextInput("1 kg")
        saveButton().performClick()

        val saved = storedMeal("m")
        assertEquals("Pasta night", saved.name)
        assertEquals(listOf(ingredient("Spaghetti", "1 kg")), saved.ingredients)
        assertTrue(saved.lastUpdate.isAfter(TestData.OLD))
        assertEquals(listOf("saved:m"), events)
    }

    @Test
    fun `add ingredients opens the picker without the draft's groceries and adds the confirmed ones`() {
        show("m")
        compose.onNodeWithText("Add ingredients").performClick()
        compose.onNodeWithText("Select ingredients").assertIsDisplayed()
        compose.onAllNodesWithText("Tomato").assertCountEquals(1) // the editor row only
        compose.onNodeWithText("Basil").performClick()
        compose.onNodeWithContentDescription("Confirm").performClick()

        compose.onNodeWithText("Select ingredients").assertDoesNotExist()
        compose.onNodeWithText("Basil").assertIsDisplayed()
        saveButton().performClick()
        assertEquals(
            listOf(ingredient("Spaghetti", "500 g"), ingredient("Tomato"), ingredient("Basil")),
            storedMeal("m").ingredients,
        )
    }

    @Test
    fun `cancelling the picker keeps the draft`() {
        show("m")
        compose.onNodeWithText("Add ingredients").performClick()
        compose.onNodeWithText("Basil").performClick()
        compose.onNode(hasContentDescription("Cancel") and hasAnyAncestor(isDialog())).performClick()
        compose.onNodeWithText("Select ingredients").assertDoesNotExist()
        compose.onNodeWithText("Basil").assertDoesNotExist()
        assertTrue(events.isEmpty())
    }

    @Test
    fun `cancel leaves the meal unchanged`() {
        show("m")
        nameField().performTextInput(" changed")
        compose.onNodeWithContentDescription("Cancel").performClick()
        assertEquals("Pasta", storedMeal("m").name)
        assertEquals(TestData.OLD, storedMeal("m").lastUpdate)
        assertEquals(listOf("cancel"), events)
    }

    @Test
    fun `an unknown or deleted meal cancels`() {
        DataManagerObject.meals.add(meal("old", "Old", deleted = true))
        show("old")
        assertEquals(listOf("cancel"), events)
    }
}
