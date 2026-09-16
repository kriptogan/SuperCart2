package com.example.supercart2.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.example.supercart2.TestData
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.models.Grocery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Edit mode of [GroceryCreationDialog]: pre-filled form, updates, moves and delete confirmation. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GroceryCreationDialogEditTest {

    @get:Rule
    val compose = createComposeRule()

    private var dismissed = 0
    private val buyEvents = listOf(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 8))

    @Before
    fun setUp() {
        TestData.installStandardTree()
        DataManagerObject.updateGrocery("g1") {
            it.copy(
                expirationDate = LocalDate.of(2030, 5, 1), storeIds = listOf("st1"), inShoppingList = true,
                isBought = true, buyEvents = buyEvents, averageBuyDays = 7,
            )
        }
    }

    private fun edited(): Grocery = TestData.findGrocery("g1")!!

    private fun show(grocery: Grocery = edited()) {
        compose.setContent {
            GroceryCreationDialog(
                onDismiss = { dismissed++ }, groceryToEdit = grocery,
            )
        }
    }

    private fun text(value: String) = compose.onNodeWithText(value).performScrollTo()
    private fun desc(value: String) = compose.onNodeWithContentDescription(value).performScrollTo()

    @Test
    fun `edit mode pre-fills the form from the grocery`() {
        show()
        compose.onNodeWithText("Edit Grocery").assertIsDisplayed()
        compose.onNodeWithContentDescription("Delete Grocery").assertIsDisplayed()
        text("g1").assertIsDisplayed()
        text("c1").assertIsDisplayed()
        text("s1").assertIsDisplayed()
        text("May 01, 2030").assertIsDisplayed()
        text("Clear").assertIsDisplayed()
        compose.onNodeWithContentDescription("Save Changes").assertIsEnabled()
        compose.onNodeWithContentDescription("Create Grocery").assertDoesNotExist()
    }

    @Test
    fun `saving without changes keeps every other field, stamps lastUpdate and dismisses`() {
        val before = edited()
        show()
        compose.onNodeWithContentDescription("Save Changes").performClick()

        val after = edited()
        assertEquals(before.copy(lastUpdate = after.lastUpdate), after)
        assertFalse(after.lastUpdate.isBefore(before.lastUpdate))
        assertEquals("c1" to "s1", TestData.locationOf("g1"))
        assertEquals(1, dismissed)
    }

    @Test
    fun `renaming trims the name`() {
        show()
        text("g1").performTextClearance()
        text("Grocery Name").performTextInput(" Bread ")
        compose.onNodeWithContentDescription("Save Changes").performClick()
        assertEquals("Bread", edited().name)
    }

    @Test
    fun `blank name disables save`() {
        show()
        text("g1").performTextClearance()
        compose.onNodeWithContentDescription("Save Changes").assertIsNotEnabled()
    }

    @Test
    fun `changing the sub-category moves the grocery and keeps its data`() {
        show()
        desc("Select Sub-Category").performClick()
        compose.onNodeWithText("s2").performClick()
        compose.onNodeWithContentDescription("Save Changes").performClick()

        assertEquals("c1" to "s2", TestData.locationOf("g1"))
        val after = edited()
        assertEquals("s2", after.subCategoryId)
        assertTrue(after.inShoppingList)
        assertEquals(buyEvents, after.buyEvents)
        assertEquals(listOf("g2"), DataManagerObject.categories[0].subCategories[0].groceries.map { it.uuid })
    }

    @Test
    fun `changing the category moves the grocery to that category's first sub-category`() {
        show()
        desc("Select Category").performClick()
        compose.onNodeWithText("c2").performClick()
        compose.onNodeWithContentDescription("Save Changes").performClick()

        assertEquals("c2" to "s3", TestData.locationOf("g1"))
        assertEquals("c2", edited().categoryId)
    }

    @Test
    fun `clearing the date saves no expiration date`() {
        show()
        text("Clear").performClick()
        text("No date selected").assertIsDisplayed()
        compose.onNodeWithText("Clear").assertDoesNotExist()
        compose.onNodeWithContentDescription("Save Changes").performClick()
        assertEquals(null, edited().expirationDate)
    }

    @Test
    fun `grocery pointing to an unknown category shows no selection and cannot be saved`() {
        show(edited().copy(categoryId = "missing"))
        text("Select Category").assertIsDisplayed()
        text("Select Sub-Category").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Save Changes").assertIsNotEnabled()
    }

    @Test
    fun `delete asks for confirmation and cancel keeps the grocery`() {
        show()
        compose.onNodeWithContentDescription("Delete Grocery").performClick()
        compose.onNodeWithText("Delete Grocery").assertIsDisplayed()
        compose.onNodeWithText("Are you sure you want to delete 'g1'?").assertIsDisplayed()

        compose.onAllNodesWithContentDescription("Cancel")[1].performClick()
        compose.onNodeWithText("Delete Grocery").assertDoesNotExist()
        assertFalse(edited().deleted)
        assertEquals(0, dismissed)
    }

    @Test
    fun `confirming delete soft-deletes the grocery and dismisses`() {
        show()
        compose.onNodeWithContentDescription("Delete Grocery").performClick()
        compose.onNodeWithContentDescription("Delete").performClick()

        assertTrue(edited().deleted)
        assertEquals(1, dismissed)
        compose.onNodeWithText("Are you sure you want to delete 'g1'?").assertDoesNotExist()
    }
}
