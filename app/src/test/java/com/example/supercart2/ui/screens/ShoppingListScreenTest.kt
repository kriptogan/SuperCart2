package com.example.supercart2.ui.screens

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.supercart2.TestData
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import io.mockk.coVerify
import io.mockk.unmockkAll
import org.junit.After
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "h1200dp")
class ShoppingListScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val fixture = ShoppingListScreenTestFixture(compose)

    @Before
    fun setUp() {
        TestData.reset()
        fixture.mockPersistence()
    }

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `empty list shows both empty cards and no finish button`() {
        fixture.setScreen()
        compose.onNodeWithText("No items to buy").assertIsDisplayed()
        compose.onNodeWithText("No bought items").assertIsDisplayed()
        compose.onAllNodesWithText("Things to Buy").assertCountEquals(0)
        compose.onAllNodesWithText("Already Bought").assertCountEquals(0)
        compose.onAllNodesWithText("Finish Shopping", substring = true).assertCountEquals(0)
    }

    @Test
    fun `top bar shows today's date, add button, search, expand toggle and view toggle`() {
        fixture.setScreen()
        compose.onNodeWithText(LocalDate.now().toString()).assertIsDisplayed()
        compose.onNodeWithContentDescription("Add New Grocery").assertIsDisplayed()
        compose.onNodeWithContentDescription("Menu").assertIsDisplayed()
        compose.onNodeWithText("Search groceries...").assertIsDisplayed()
        compose.onNodeWithContentDescription("Search").assertIsDisplayed()
        compose.onAllNodesWithContentDescription("Clear search").assertCountEquals(0)
        compose.onNodeWithContentDescription("Expand All").assertIsDisplayed()
        compose.onNodeWithText("Switch to Store View").assertIsDisplayed()
        compose.onAllNodesWithText("Show/Hide Stores", substring = true).assertCountEquals(0)
    }

    @Test
    fun `back button is the first clickable only when onBack is given`() {
        fixture.setScreen(withBack = true)
        compose.onAllNodes(hasClickAction()).onFirst().performClick()
        assertEquals(1, fixture.backClicks)
    }

    @Test
    fun `sections show headers with counts and finish button`() {
        fixture.installShoppingData()
        fixture.setScreen()
        compose.onNodeWithText("Things to Buy").assertIsDisplayed()
        compose.onNodeWithText("(2)").assertIsDisplayed()
        compose.onNodeWithText("Already Bought").assertIsDisplayed()
        compose.onNodeWithText("(1)").assertIsDisplayed()
        compose.onNodeWithText("Finish Shopping (1 items)").assertIsDisplayed()
        compose.onAllNodesWithText("No items to buy").assertCountEquals(0)
        compose.onAllNodesWithText("No bought items").assertCountEquals(0)
    }

    @Test
    fun `to-buy categories start collapsed and expand all reveals only to-buy groceries`() {
        fixture.installShoppingData()
        fixture.setScreen()
        compose.onAllNodesWithText("milk").assertCountEquals(0)
        compose.onNodeWithContentDescription("Expand All").performClick()
        compose.onNodeWithText("milk").assertIsDisplayed()
        compose.onNodeWithText("bread").assertIsDisplayed()
        compose.onAllNodesWithText("butter").assertCountEquals(0)
        compose.onNodeWithContentDescription("Collapse All").performClick()
        compose.onNodeWithContentDescription("Expand All").assertIsDisplayed()
    }

    @Test
    fun `bought section lists bought groceries with their category name`() {
        fixture.installShoppingData()
        fixture.setScreen()
        compose.onNodeWithText("cheese").assertIsDisplayed()
        compose.onNodeWithContentDescription("Remove from list").assertIsDisplayed()
        compose.onAllNodesWithText("Dairy").assertCountEquals(2) // to-buy category header + bought card
    }

    @Test
    fun `search filters both sections case-insensitively and can be cleared`() {
        fixture.installShoppingData()
        fixture.setScreen()
        compose.onNodeWithText("Search groceries...").performTextInput("MIL")
        compose.onNodeWithText("(1)").assertIsDisplayed()
        compose.onNodeWithText("No bought items").assertIsDisplayed()
        compose.onAllNodesWithText("Finish Shopping", substring = true).assertCountEquals(0)
        compose.onNodeWithContentDescription("Expand All").performClick()
        compose.onAllNodesWithText("bread").assertCountEquals(0)

        compose.onNodeWithContentDescription("Clear search").performClick()
        compose.onNodeWithText("(2)").assertIsDisplayed()
        compose.onNodeWithText("Finish Shopping (1 items)").assertIsDisplayed()
        compose.onAllNodesWithContentDescription("Clear search").assertCountEquals(0)
    }

    @Test
    fun `search without matches shows both empty cards`() {
        fixture.installShoppingData()
        fixture.setScreen()
        compose.onNodeWithText("Search groceries...").performTextInput("zzz")
        compose.onNodeWithText("No items to buy").assertIsDisplayed()
        compose.onNodeWithText("No bought items").assertIsDisplayed()
    }

    @Test
    fun `removing a bought grocery moves it back to the to-buy section and saves`() {
        fixture.installShoppingData()
        fixture.setScreen()
        compose.onNodeWithContentDescription("Remove from list").performClick()

        val cheese = TestData.findGrocery("cheese")!!
        assertFalse(cheese.isBought)
        assertTrue(cheese.inShoppingList)
        compose.onNodeWithText("(3)").assertIsDisplayed()
        compose.onNodeWithText("No bought items").assertIsDisplayed()
        coVerify(exactly = 1) { DataStoreManager.saveDataGlobally() }
    }

    @Test
    fun `finish shopping asks for confirmation and cancel changes nothing`() {
        fixture.installShoppingData()
        fixture.setScreen()
        compose.onNodeWithText("Finish Shopping (1 items)").performClick()
        compose.onNodeWithText("Finish Shopping").assertIsDisplayed()
        compose.onNodeWithText("Are you sure you want to finish shopping? This will:").assertIsDisplayed()
        compose.onNodeWithText("• Add today's date to the buy history of all bought items").assertIsDisplayed()
        compose.onNodeWithText("• Remove all bought items from the shopping list").assertIsDisplayed()

        compose.onNodeWithContentDescription("Cancel").performClick()
        compose.onAllNodesWithText("Finish Shopping").assertCountEquals(0)
        assertTrue(TestData.findGrocery("cheese")!!.isBought)
        coVerify(exactly = 0) { DataStoreManager.saveDataGlobally() }
    }

    @Test
    fun `confirming finish shopping confirms bought items, saves and closes the dialog`() {
        fixture.installShoppingData()
        fixture.setScreen()
        compose.onNodeWithText("Finish Shopping (1 items)").performClick()
        compose.onNodeWithContentDescription("Confirm").performClick()

        val cheese = TestData.findGrocery("cheese")!!
        assertFalse(cheese.isBought)
        assertFalse(cheese.inShoppingList)
        assertEquals(listOf(LocalDate.now()), cheese.buyEvents)
        assertTrue(TestData.findGrocery("milk")!!.inShoppingList)
        compose.onAllNodesWithText("Finish Shopping").assertCountEquals(0)
        compose.onNodeWithText("No bought items").assertIsDisplayed()
        coVerify(exactly = 1) { DataStoreManager.saveDataGlobally() }
    }

    @Test
    fun `deleted categories are not shown`() {
        fixture.installShoppingData()
        val dairy = DataManagerObject.categories[0]
        DataManagerObject.categories[0] = dairy.copy(category = dairy.category.copy(deleted = true))
        fixture.setScreen()
        compose.onNodeWithText("(1)").assertIsDisplayed()
        compose.onNodeWithText("No bought items").assertIsDisplayed()
    }
}
