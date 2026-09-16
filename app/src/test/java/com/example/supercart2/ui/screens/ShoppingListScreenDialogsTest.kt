package com.example.supercart2.ui.screens

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.example.supercart2.TestData
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.data.SettingsManager
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "h1200dp")
class ShoppingListScreenDialogsTest {

    @get:Rule
    val compose = createComposeRule()

    private val fixture = ShoppingListScreenTestFixture(compose)

    @Before
    fun setUp() {
        TestData.reset()
        fixture.installShoppingData()
    }

    @After
    fun tearDown() = unmockkAll()

    private fun openMenuItem(label: String) {
        compose.onNodeWithContentDescription("Menu").performClick()
        compose.onNodeWithText(label).performClick()
    }

    /** The list area (fixed top padding) overlaps the lower part of the header buttons, so tap near the top. */
    private fun clickTopEdge(text: String) {
        compose.onNodeWithText(text).performTouchInput { click(Offset(centerX, top + 4f)) }
    }

    @Test
    fun `pinned quirk - the list overlaps the view toggle so tapping its center does nothing`() {
        fixture.mockPersistence()
        fixture.setScreen()
        val toggle = compose.onNodeWithText("Switch to Store View").fetchSemanticsNode().boundsInRoot
        val list = compose.onNode(hasScrollAction()).fetchSemanticsNode().boundsInRoot
        assertTrue(list.top < toggle.center.y)
        compose.onNodeWithText("Switch to Store View").performClick()
        compose.onNodeWithText("Switch to Store View").assertIsDisplayed()
        coVerify(exactly = 0) { DataStoreManager.saveStoreViewMode(any(), any()) }
    }

    @Test
    fun `saved store view mode is loaded and shows the hide stores button`() {
        fixture.mockPersistence(storeView = true)
        fixture.setScreen()
        compose.onNodeWithText("Switch to Category View").assertIsDisplayed()
        compose.onNodeWithText("Show/Hide Stores").assertIsDisplayed()
        compose.onNodeWithContentDescription("Expand All").performClick()
        compose.onNodeWithText("shop").assertIsDisplayed()
        compose.onNodeWithText("milk").assertIsDisplayed()
    }

    @Test
    fun `hide stores button shows the hidden count`() {
        DataManagerObject.hiddenStoreIds.add("shop")
        fixture.mockPersistence(storeView = true)
        fixture.setScreen()
        compose.onNodeWithText("Show/Hide Stores (1 hidden)").assertIsDisplayed()
    }

    @Test
    fun `view toggle switches mode and persists each choice`() {
        fixture.mockPersistence()
        fixture.setScreen()
        clickTopEdge("Switch to Store View")
        compose.onNodeWithText("Switch to Category View").assertIsDisplayed()
        compose.onNodeWithText("Show/Hide Stores").assertIsDisplayed()
        clickTopEdge("Switch to Category View")
        compose.onNodeWithText("Switch to Store View").assertIsDisplayed()
        compose.onAllNodesWithText("Show/Hide Stores").assertCountEquals(0)
        coVerifyOrder {
            DataStoreManager.saveStoreViewMode(any(), true)
            DataStoreManager.saveStoreViewMode(any(), false)
        }
    }

    @Test
    fun `hide stores button opens the hide stores dialog`() {
        fixture.mockPersistence(storeView = true)
        fixture.setScreen()
        clickTopEdge("Show/Hide Stores")
        compose.onNodeWithText("Done").assertIsDisplayed().performClick()
        compose.onAllNodesWithText("Done").assertCountEquals(0)
    }

    @Test
    fun `menu opens categories management dialog`() {
        fixture.mockPersistence()
        fixture.setScreen()
        openMenuItem("Manage Categories")
        compose.onNodeWithText("Categories Management").assertIsDisplayed()
    }

    @Test
    fun `menu opens stores management and editing a store navigates and closes it`() {
        fixture.mockPersistence()
        fixture.setScreen()
        openMenuItem("Manage Stores")
        compose.onNodeWithText("Stores Management").assertIsDisplayed()
        compose.onNodeWithContentDescription("Edit Store").performClick()
        assertEquals(listOf("shop"), fixture.navigatedStores)
        compose.onAllNodesWithText("Stores Management").assertCountEquals(0)
    }

    @Test
    fun `menu opens import groceries dialog and cancel closes it`() {
        fixture.mockPersistence()
        fixture.setScreen()
        openMenuItem("Import Groceries")
        compose.onNodeWithText("Import").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.onAllNodesWithText("Import").assertCountEquals(0)
    }

    @Test
    fun `menu opens settings dialog`() {
        SettingsManager.setGlobalContext(ApplicationProvider.getApplicationContext())
        fixture.mockPersistence()
        fixture.setScreen()
        openMenuItem("Settings")
        compose.onNodeWithText("Color palette").assertIsDisplayed()
    }

    @Test
    fun `menu opens group sharing dialog`() {
        fixture.mockPersistence()
        fixture.setScreen()
        openMenuItem("Group Sharing")
        compose.onNodeWithText("Family Group").assertIsDisplayed()
    }

    @Test
    fun `add button opens creation dialog prefilled with the search query`() {
        fixture.mockPersistence()
        fixture.setScreen()
        compose.onNodeWithText("Search groceries...").performTextInput("tomato")
        compose.onNodeWithContentDescription("Add New Grocery").performClick()
        compose.onNodeWithText("Create New Grocery").assertIsDisplayed()
        compose.onAllNodesWithText("tomato").assertCountEquals(2)
    }

    @Test
    fun `creating a grocery adds it once to the shopping list`() {
        fixture.mockPersistence()
        fixture.setScreen()
        compose.onNodeWithContentDescription("Add New Grocery").performClick()
        compose.onNodeWithText("Grocery Name").performTextInput("eggs")
        compose.onNodeWithContentDescription("Create Grocery").performClick()

        val eggs = DataManagerObject.categories.flatMap { it.subCategories }.flatMap { it.groceries }
            .filter { it.name == "eggs" }
        assertEquals(1, eggs.size)
        assertTrue(eggs.single().inShoppingList)
        assertEquals("c1" to "s1", eggs.single().categoryId to eggs.single().subCategoryId)
        compose.onAllNodesWithText("Create New Grocery").assertCountEquals(0)
        compose.onNodeWithText("(3)").assertIsDisplayed()
        // Pinned: GroceryCreationDialog saves by itself and never calls onGroceryCreated.
        coVerify(exactly = 1) { DataStoreManager.saveDataGlobally() }
    }

    @Test
    fun `edit item opens the edit dialog without the search query, and add afterwards creates`() {
        fixture.mockPersistence()
        fixture.setScreen()
        compose.onNodeWithText("Search groceries...").performTextInput("mil")
        compose.onNodeWithContentDescription("Expand All").performClick()
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("Edit item").performClick()
        compose.onNodeWithText("Edit Grocery").assertIsDisplayed()
        compose.onAllNodesWithText("milk").assertCountEquals(2) // list row + name field

        compose.onNodeWithContentDescription("Cancel").performClick()
        compose.onAllNodesWithText("Edit Grocery").assertCountEquals(0)
        compose.onNodeWithContentDescription("Add New Grocery").performClick()
        compose.onNodeWithText("Create New Grocery").assertIsDisplayed()
        compose.onAllNodesWithText("mil").assertCountEquals(2) // search field + prefilled name
    }

    @Test
    fun `saving an edit updates the grocery`() {
        fixture.mockPersistence()
        fixture.setScreen()
        compose.onNodeWithContentDescription("Expand All").performClick()
        compose.onAllNodesWithContentDescription("More options")[0].performClick()
        compose.onNodeWithText("Edit item").performClick()
        compose.onNodeWithText("Grocery Name").performTextInput("2")
        compose.onNodeWithContentDescription("Save Changes").performClick()
        compose.onAllNodesWithText("Edit Grocery").assertCountEquals(0)
        assertTrue(TestData.findGrocery("milk")!!.name.contains("2"))
    }
}
