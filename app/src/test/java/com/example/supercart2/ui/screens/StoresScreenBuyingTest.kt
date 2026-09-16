package com.example.supercart2.ui.screens

import android.os.Looper
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.example.supercart2.TestData.findGrocery
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.ui.screens.StoresScreenTestFixtures.ALDI
import com.example.supercart2.ui.screens.StoresScreenTestFixtures.COSTCO
import com.example.supercart2.ui.screens.StoresScreenTestFixtures.bakery
import com.example.supercart2.ui.screens.StoresScreenTestFixtures.dairy
import com.example.supercart2.ui.screens.StoresScreenTestFixtures.install
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "h1200dp")
class StoresScreenBuyingTest {

    @get:Rule
    val compose = createComposeRule()

    @After
    fun flushMainLooper() = shadowOf(Looper.getMainLooper()).idle()

    private fun openCostco() {
        compose.setContent { StoresScreen() }
        compose.onNodeWithText("Select a Store").performClick()
        compose.onNodeWithText("Costco").performClick()
    }

    private fun positionOf(text: String): Float =
        compose.onAllNodesWithText(text).onFirst().fetchSemanticsNode().positionInRoot.y

    @Test
    fun `header shows store name, today's date, search field and edit icon`() {
        install(dairy("Milk", COSTCO))
        openCostco()
        compose.onNodeWithText("Costco").assertIsDisplayed()
        compose.onNodeWithText(LocalDate.now().toString()).assertIsDisplayed()
        compose.onNodeWithText("Search groceries...").assertIsDisplayed()
        compose.onNodeWithContentDescription("Edit Store").assertIsDisplayed()
        compose.onNodeWithContentDescription("Add New Grocery").assertIsDisplayed()
    }

    @Test
    fun `to-buy section lists this store's unbought shopping items in category order`() {
        install(
            bakery("Bread", COSTCO), dairy("Milk", COSTCO), dairy("Cheese", COSTCO),
            dairy("Butter", ALDI), dairy("Eggs", COSTCO, inList = false), dairy("Cream", COSTCO, bought = true),
        )
        openCostco()
        compose.onNodeWithText("Things to Buy").assertIsDisplayed()
        compose.onNodeWithText("(3)").assertIsDisplayed()
        assertTrue(positionOf("Dairy") < positionOf("Bakery"))

        compose.onAllNodesWithText("Dairy").onFirst().performClick()
        compose.onNodeWithText("Milk").assertIsDisplayed()
        compose.onNodeWithText("Cheese").assertIsDisplayed()
        compose.onNodeWithText("Butter").assertDoesNotExist()
        compose.onNodeWithText("Eggs").assertDoesNotExist()
    }

    @Test
    fun `store category order is applied`() {
        install(bakery("Bread", COSTCO), dairy("Milk", COSTCO))
        DataManagerObject.storeCategoryOrders[COSTCO] = listOf("Bakery", "Dairy")
        openCostco()
        assertTrue(positionOf("Bakery") < positionOf("Dairy"))
    }

    @Test
    fun `nothing to buy and nothing bought shows the empty card and no finish button`() {
        install(dairy("Milk", COSTCO))
        openCostco()
        DataManagerObject.updateGrocery("Milk") { it.copy(inShoppingList = false) }
        compose.waitForIdle()
        compose.onNodeWithText("No items to buy").assertIsDisplayed()
        compose.onNodeWithText("Things to Buy").assertDoesNotExist()
        compose.onNodeWithText("Already Bought").assertDoesNotExist()
        compose.onNodeWithText("Finish Shopping", substring = true).assertDoesNotExist()
    }

    @Test
    fun `only bought items hides the to-buy label and the empty card`() {
        install(dairy("Milk", COSTCO, bought = true))
        openCostco()
        compose.onNodeWithText("Things to Buy").assertDoesNotExist()
        compose.onNodeWithText("No items to buy").assertDoesNotExist()
        compose.onNodeWithText("Already Bought").assertIsDisplayed()
    }

    @Test
    fun `bought section shows cards with category name, count and finish button`() {
        install(dairy("Milk", COSTCO, bought = true), bakery("Bread", COSTCO, bought = true), dairy("Eggs", COSTCO))
        openCostco()
        compose.onNodeWithText("Already Bought").assertIsDisplayed()
        compose.onNodeWithText("(2)").assertIsDisplayed()
        compose.onNodeWithText("(1)").assertIsDisplayed()
        compose.onNodeWithText("Milk").assertIsDisplayed()
        compose.onNodeWithText("Bread").assertIsDisplayed()
        assertTrue(positionOf("Milk") < positionOf("Bread"))
        compose.onNodeWithText("Finish Shopping (2 bought)").assertIsDisplayed()
    }

    @Test
    fun `remove on a bought card clears isBought only`() {
        install(dairy("Milk", COSTCO, bought = true))
        openCostco()
        compose.onNodeWithContentDescription("Remove from list").performClick()

        val milk = findGrocery("Milk")!!
        assertFalse(milk.isBought)
        assertTrue(milk.inShoppingList)
        compose.onNodeWithText("Already Bought").assertDoesNotExist()
        compose.onNodeWithText("(1)").assertIsDisplayed()
    }

    @Test
    fun `marking an item bought moves it to the bought section`() {
        install(dairy("Milk", COSTCO))
        openCostco()
        compose.onNodeWithText("Dairy").performClick()
        compose.onNodeWithContentDescription("Mark as bought").performClick()

        assertTrue(findGrocery("Milk")!!.isBought)
        compose.onNodeWithText("Finish Shopping (1 bought)").assertIsDisplayed()
    }

    @Test
    fun `search filters both sections by name, ignoring case`() {
        install(dairy("Milk", COSTCO), dairy("Cheese", COSTCO), dairy("Buttermilk", COSTCO, bought = true),
            bakery("Bread", COSTCO, bought = true))
        openCostco()
        compose.onNodeWithText("Search groceries...").performTextInput("MILK")
        compose.onNodeWithText("Finish Shopping (1 bought)").assertIsDisplayed()
        compose.onNodeWithText("Bread").assertDoesNotExist()
        compose.onNodeWithText("Buttermilk").assertIsDisplayed()

        compose.onNodeWithText("MILK").performTextClearance()
        compose.onNodeWithText("Finish Shopping (2 bought)").assertIsDisplayed()
    }

    @Test
    fun `finish shopping shows the confirmation and cancel keeps everything`() {
        install(dairy("Milk", COSTCO, bought = true))
        openCostco()
        compose.onNodeWithText("Finish Shopping (1 bought)").performClick()
        compose.onNodeWithText("Finish Shopping").assertIsDisplayed()
        compose.onNodeWithText("Are you sure you want to finish shopping? This will:").assertIsDisplayed()
        compose.onNodeWithText("• Add today's date to the buy history of all bought items").assertIsDisplayed()
        compose.onNodeWithText("• Remove all bought items from the shopping list").assertIsDisplayed()

        compose.onNodeWithContentDescription("Cancel").performClick()
        compose.onNodeWithText("Finish Shopping").assertDoesNotExist()
        assertTrue(findGrocery("Milk")!!.isBought)
    }

    @Test
    fun `confirm finish shopping confirms only this store's bought items and returns to the store list`() {
        install(dairy("Milk", COSTCO, bought = true), bakery("Bread", ALDI, bought = true), dairy("Eggs", COSTCO))
        openCostco()
        compose.onNodeWithText("Finish Shopping (1 bought)").performClick()
        compose.onNodeWithContentDescription("Confirm").performClick()

        val milk = findGrocery("Milk")!!
        assertEquals(listOf(LocalDate.now()), milk.buyEvents)
        assertFalse(milk.inShoppingList)
        assertTrue(findGrocery("Bread")!!.isBought)
        compose.onNodeWithText("Select a Store").assertIsDisplayed()
        compose.onAllNodesWithText("Items: 1").assertCountEquals(2)
    }

    @Test
    fun `add grocery creates an item linked to the store and in the shopping list`() {
        install(dairy("Milk", COSTCO))
        openCostco()
        compose.onNodeWithContentDescription("Add New Grocery").performClick()
        compose.onNodeWithText("Create New Grocery").assertIsDisplayed()
        compose.onNodeWithText("Grocery Name").performTextInput("Yogurt")
        compose.onNodeWithContentDescription("Create Grocery").performClick()

        val yogurt = DataManagerObject.categories[0].subCategories[0].groceries.last()
        assertEquals("Yogurt", yogurt.name)
        assertEquals(listOf(COSTCO), yogurt.storeIds)
        assertTrue(yogurt.inShoppingList)
        compose.onNodeWithText("Create New Grocery").assertDoesNotExist()
        compose.onNodeWithText("(2)").assertIsDisplayed()
    }

    @Test
    fun `cancelling the add grocery dialog creates nothing`() {
        install(dairy("Milk", COSTCO))
        openCostco()
        compose.onNodeWithContentDescription("Add New Grocery").performClick()
        compose.onNodeWithContentDescription("Cancel").performClick()
        compose.onNodeWithText("Create New Grocery").assertDoesNotExist()
        assertEquals(1, DataManagerObject.categories[0].subCategories[0].groceries.size)
        assertNull(findGrocery("Yogurt"))
    }

    @Test
    fun `edit item from the grocery menu opens the edit dialog`() {
        install(dairy("Milk", COSTCO))
        openCostco()
        compose.onNodeWithText("Dairy").performClick()
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("Edit item").performClick()
        compose.onNodeWithText("Edit Grocery").assertIsDisplayed()

        compose.onNodeWithContentDescription("Cancel").performClick()
        compose.onNodeWithText("Edit Grocery").assertDoesNotExist()
    }
}
