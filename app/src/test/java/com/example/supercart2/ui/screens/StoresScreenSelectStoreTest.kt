package com.example.supercart2.ui.screens

import android.os.Looper
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.supercart2.TestData
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.models.Store
import com.example.supercart2.ui.screens.StoresScreenTestFixtures.ALDI
import com.example.supercart2.ui.screens.StoresScreenTestFixtures.COSTCO
import com.example.supercart2.ui.screens.StoresScreenTestFixtures.aldi
import com.example.supercart2.ui.screens.StoresScreenTestFixtures.bakery
import com.example.supercart2.ui.screens.StoresScreenTestFixtures.costco
import com.example.supercart2.ui.screens.StoresScreenTestFixtures.dairy
import com.example.supercart2.ui.screens.StoresScreenTestFixtures.install
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "h1200dp")
class StoresScreenSelectStoreTest {

    @get:Rule
    val compose = createComposeRule()

    @After
    fun flushMainLooper() = shadowOf(Looper.getMainLooper()).idle()

    private fun showStoreList() {
        compose.setContent { StoresScreen() }
        compose.onNodeWithText("Select a Store").performClick()
    }

    private fun positionOf(text: String): Float =
        compose.onNodeWithText(text).fetchSemanticsNode().positionInRoot.y

    @Test
    fun `no active store shows the empty message`() {
        install(dairy("Milk", COSTCO, inList = false))
        showStoreList()
        compose.onNodeWithText("No stores yet").assertIsDisplayed()
        compose.onNodeWithText("Costco").assertDoesNotExist()
    }

    @Test
    fun `lists only stores with shopping-list items, sorted by viewOrder, with item and unique counts`() {
        val lidl = Store(uuid = "lidl", name = "Lidl", viewOrder = 3, lastUpdate = TestData.OLD)
        install(
            dairy("Milk", COSTCO), dairy("Cheese", COSTCO, ALDI), bakery("Bread", COSTCO, bought = true),
            bakery("Bagel", ALDI, inList = false), dairy("Yogurt", "lidl", inList = false),
            stores = listOf(costco, aldi, lidl),
        )
        showStoreList()

        compose.onNodeWithText("Lidl").assertDoesNotExist()
        compose.onNodeWithText("Items: 3").assertIsDisplayed()
        compose.onNodeWithText("Unique: 2").assertIsDisplayed()
        compose.onNodeWithText("Items: 1").assertIsDisplayed()
        compose.onNodeWithText("Unique: 0").assertIsDisplayed()
        assertTrue(positionOf("Aldi") < positionOf("Costco"))
    }

    @Test
    fun `deleted stores and deleted groceries are ignored`() {
        val deletedStore = Store(uuid = "old", name = "Old", deleted = true, lastUpdate = TestData.OLD)
        install(
            dairy("Milk", "old"), dairy("Cheese", ALDI).copy(deleted = true), dairy("Eggs", COSTCO),
            stores = listOf(costco, aldi, deletedStore),
        )
        showStoreList()
        compose.onNodeWithText("Old").assertDoesNotExist()
        compose.onNodeWithText("Aldi").assertDoesNotExist()
        compose.onNodeWithText("Costco").assertIsDisplayed()
    }

    @Test
    fun `hidden stores are still listed`() {
        install(dairy("Milk", COSTCO))
        DataManagerObject.hiddenStoreIds.add(COSTCO)
        showStoreList()
        compose.onNodeWithText("Costco").assertIsDisplayed()
    }

    @Test
    fun `unique counts groceries with a single store id even if that other store is unknown`() {
        install(dairy("Milk", COSTCO, "ghost"), dairy("Eggs", COSTCO))
        showStoreList()
        compose.onNodeWithText("Items: 2").assertIsDisplayed()
        compose.onNodeWithText("Unique: 1").assertIsDisplayed()
    }

    @Test
    fun `groceries of a deleted category are not counted`() {
        install(dairy("Milk", COSTCO), bakery("Bread", ALDI))
        val dairyNode = DataManagerObject.categories[0]
        DataManagerObject.categories[0] = dairyNode.copy(category = dairyNode.category.copy(deleted = true))
        showStoreList()
        compose.onNodeWithText("Costco").assertDoesNotExist()
        compose.onNodeWithText("Aldi").assertIsDisplayed()
    }

    @Test
    fun `groceries of a deleted sub-category are not counted`() {
        install(dairy("Milk", COSTCO), dairy("Eggs", COSTCO, ALDI))
        val dairyNode = DataManagerObject.categories[0]
        val subNode = dairyNode.subCategories[0]
        val deletedSub = subNode.copy(subCategory = subNode.subCategory.copy(deleted = true))
        DataManagerObject.categories[0] = dairyNode.copy(subCategories = mutableListOf(deletedSub))
        showStoreList()
        compose.onNodeWithText("No stores yet").assertIsDisplayed()
    }

    @Test
    fun `list updates when a grocery leaves the shopping list`() {
        install(dairy("Milk", COSTCO))
        showStoreList()
        compose.onNodeWithText("Costco").assertIsDisplayed()

        DataManagerObject.updateGrocery("Milk") { it.copy(inShoppingList = false) }
        compose.waitForIdle()
        compose.onNodeWithText("No stores yet").assertIsDisplayed()
    }

    @Test
    fun `clicking a store card opens that store in buying mode`() {
        install(dairy("Milk", COSTCO), dairy("Cheese", ALDI))
        showStoreList()
        compose.onNodeWithText("Aldi").performClick()
        compose.onNodeWithText("Aldi").assertIsDisplayed()
        compose.onNodeWithText("Things to Buy").assertIsDisplayed()
        compose.onNodeWithText("(1)").assertIsDisplayed()
        compose.onAllNodesWithText("Items: 1").assertCountEquals(0)
    }
}
