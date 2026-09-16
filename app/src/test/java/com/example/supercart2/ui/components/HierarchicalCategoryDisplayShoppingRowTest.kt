package com.example.supercart2.ui.components

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.supercart2.TestData
import com.example.supercart2.TestData.category
import com.example.supercart2.TestData.grocery
import com.example.supercart2.TestData.node
import com.example.supercart2.TestData.sub
import com.example.supercart2.TestData.subNode
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.models.Grocery
import com.example.supercart2.models.Store
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.runs
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

/** Characterization tests: grocery row in shopping-list mode (isShoppingList = true). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w480dp-h2000dp")
class HierarchicalCategoryDisplayShoppingRowTest {

    @get:Rule
    val compose = createComposeRule()

    private val edited = mutableListOf<Grocery>()

    @Before
    fun setUp() {
        TestData.reset()
        mockkObject(DataStoreManager)
        coEvery { DataStoreManager.saveDataGlobally() } just runs
        DataManagerObject.stores.add(Store(uuid = "st1", name = "Costco"))
    }

    @After
    fun tearDown() = unmockkAll()

    private fun show(g: Grocery, categoryName: String = "Vegetables") {
        DataManagerObject.categories.add(node(category("c1", name = categoryName), subNode(sub("s1", "c1"), g)))
        compose.setContent {
            HierarchicalCategoryDisplay(
                categories = DataManagerObject.categories, searchQuery = "", isAllExpanded = true,
                onEditGrocery = { edited += it }, useScroll = false, isShoppingList = true,
            )
        }
    }

    @Test
    fun `not-bought row shows name, chips, menu and bought button`() {
        show(grocery("g1", "c1", "s1", storeIds = listOf("st1"), inShoppingList = true))
        compose.onNodeWithText("g1").assertIsDisplayed()
        compose.onNodeWithText("Cos").assertIsDisplayed()
        compose.onNodeWithContentDescription("More options").assertIsDisplayed()
        compose.onNodeWithContentDescription("Mark as bought").assertIsDisplayed()
        compose.onNodeWithContentDescription("Add to shopping list").assertDoesNotExist()
        compose.onAllNodesWithText("Vegetables").assertCountEquals(1)
    }

    @Test
    fun `menu order is remove, buy history, edit`() {
        show(grocery("g1", "c1", "s1", inShoppingList = true))
        compose.onNodeWithContentDescription("More options").performClick()
        assertTrue(top("Remove from shopping list") < top("View Buy History"))
        assertTrue(top("View Buy History") < top("Edit item"))
        compose.onNodeWithText("Edit item").performClick()
        assertEquals(listOf("g1"), edited.map { it.uuid })
    }

    @Test
    fun `remove from shopping list clears the flag, saves and closes the menu`() {
        show(grocery("g1", "c1", "s1", inShoppingList = true))
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("Remove from shopping list").performClick()
        assertFalse(TestData.findGrocery("g1")!!.inShoppingList)
        compose.onNodeWithText("Remove from shopping list").assertDoesNotExist()
        coVerify(exactly = 1) { DataStoreManager.saveDataGlobally() }
    }

    @Test
    fun `buy history opens from the shopping menu`() {
        show(grocery("g1", "c1", "s1", inShoppingList = true))
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("View Buy History").performClick()
        compose.onNodeWithText("Buy History - g1").assertIsDisplayed()
    }

    @Test
    fun `marking bought switches to the bought layout and back`() {
        show(grocery("g1", "c1", "s1", storeIds = listOf("st1"), inShoppingList = true))
        compose.onNodeWithContentDescription("Mark as bought").performClick()
        assertTrue(TestData.findGrocery("g1")!!.isBought)
        compose.onAllNodesWithText("Vegetables").assertCountEquals(2)
        compose.onNodeWithText("Cos").assertDoesNotExist()
        compose.onNodeWithContentDescription("More options").assertDoesNotExist()
        compose.onNodeWithContentDescription("Mark as not bought").performClick()
        assertFalse(TestData.findGrocery("g1")!!.isBought)
        compose.onNodeWithContentDescription("Mark as bought").assertIsDisplayed()
        coVerify(exactly = 2) { DataStoreManager.saveDataGlobally() }
    }

    @Test
    fun `bought row shows the localized category name`() {
        show(grocery("g1", "c1", "s1", inShoppingList = true, isBought = true), categoryName = "Other")
        compose.onNodeWithText("g1").assertIsDisplayed()
        compose.onAllNodesWithText("Other").assertCountEquals(2)
        compose.onNodeWithContentDescription("Mark as not bought").assertIsDisplayed()
        compose.onNodeWithContentDescription("Mark as bought").assertDoesNotExist()
    }

    private fun top(text: String) = compose.onNodeWithText(text).fetchSemanticsNode().boundsInRoot.top
}
