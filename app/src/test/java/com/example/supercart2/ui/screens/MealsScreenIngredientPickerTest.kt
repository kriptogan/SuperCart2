package com.example.supercart2.ui.screens

import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.ui.screens.MealsScreenTestFixtures.food
import com.example.supercart2.ui.screens.MealsScreenTestFixtures.install
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
class MealsScreenIngredientPickerTest {

    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()

    @Before
    fun setUp() = install(
        groceries = listOf(food("tomato"), food("Basil"), food("Salt"), food("Gone", deleted = true), food("Pasta")),
    )

    @After
    fun flushMainLooper() = shadowOf(Looper.getMainLooper()).idle()

    private fun show(excluded: Set<String> = setOf("Salt")) {
        compose.setContent {
            MealsScreenIngredientPicker(
                excludedIds = excluded,
                onDismiss = { events += "dismiss" },
                onConfirm = { events += "confirm:$it" },
            )
        }
    }

    private fun checkbox(index: Int) = compose.onAllNodes(isToggleable())[index]
    private fun top(text: String) = compose.onNodeWithText(text).fetchSemanticsNode().boundsInRoot.top

    @Test
    fun `lists visible groceries by name, excluding the draft's groceries`() {
        show()
        compose.onNodeWithText("Select ingredients").assertIsDisplayed()
        compose.onNodeWithText("Salt").assertDoesNotExist()
        compose.onNodeWithText("Gone").assertDoesNotExist()
        assertTrue(top("Basil") < top("Pasta"))
        assertTrue(top("Pasta") < top("tomato"))
        checkbox(0).assertIsOff()
    }

    @Test
    fun `search filters by name ignoring case`() {
        show()
        compose.onNodeWithText("Search groceries...").performTextInput("TOM")
        compose.onNodeWithText("tomato").assertIsDisplayed()
        compose.onNodeWithText("Basil").assertDoesNotExist()
        compose.onNodeWithText("Pasta").assertDoesNotExist()
    }

    @Test
    fun `confirm reports the checked groceries and cancel dismisses`() {
        show()
        compose.onNodeWithText("tomato").performClick()
        checkbox(0).performClick()
        checkbox(0).assertIsOn()
        compose.onNodeWithContentDescription("Confirm").performClick()
        compose.onNodeWithContentDescription("Cancel").performClick()
        assertEquals(listOf("confirm:[Basil, tomato]", "dismiss"), events)
    }

    @Test
    fun `new grocery opens the grocery dialog with the search text and auto-selects the created grocery`() {
        show()
        compose.onNodeWithText("Search groceries...").performTextInput("Oreg")
        compose.onNodeWithText("tomato").assertDoesNotExist()
        compose.onNodeWithText("New grocery").performClick()

        compose.onNodeWithText("Select ingredients").assertDoesNotExist()
        compose.onNode(hasSetTextAction() and hasText("Oreg")).performTextClearance()
        compose.onNode(hasSetTextAction() and hasText("Grocery Name")).performTextInput("Oregano")
        compose.onNodeWithContentDescription("Create Grocery").performClick()

        compose.onNodeWithText("Select ingredients").assertIsDisplayed()
        val created = DataManagerObject.categories.flatMap { it.subCategories }.flatMap { it.groceries }
            .single { it.name == "Oregano" }
        compose.onNodeWithText("Oregano").assertIsDisplayed()
        checkbox(0).assertIsOn()
        compose.onNodeWithContentDescription("Confirm").performClick()
        assertEquals(listOf("confirm:[${created.uuid}]"), events)
    }

    @Test
    fun `cancelling the grocery dialog returns to the picker with its selection and search kept`() {
        show()
        compose.onNodeWithText("Basil").performClick()
        compose.onNodeWithText("Search groceries...").performTextInput("a")
        compose.onNodeWithText("New grocery").performClick()
        compose.onNodeWithContentDescription("Cancel").performClick()

        compose.onNodeWithText("Select ingredients").assertIsDisplayed()
        compose.onNode(hasSetTextAction()).assertTextContains("a")
        checkbox(0).assertIsOn()
        assertEquals(5, DataManagerObject.categories.single().subCategories.single().groceries.size)
        assertTrue(events.isEmpty())
    }
}
