package com.example.supercart2.ui.components

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.supercart2.TestData
import com.example.supercart2.TestData.category
import com.example.supercart2.TestData.grocery
import com.example.supercart2.TestData.node
import com.example.supercart2.TestData.sub
import com.example.supercart2.TestData.subNode
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.models.Grocery
import com.example.supercart2.models.Store
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w480dp-h2000dp")
class StoreBasedDisplayTest {

    @get:Rule
    val compose = createComposeRule()

    private val navigated = mutableListOf<String>()
    private val edited = mutableListOf<Grocery>()

    @Before
    fun setUp() {
        TestData.reset()
        DataManagerObject.stores.addAll(
            listOf(
                Store(uuid = "a", name = "Alpha", viewOrder = 1),
                Store(uuid = "b", name = "Beta", viewOrder = 2),
                Store(uuid = "h", name = "Hidden", viewOrder = 3),
            )
        )
        DataManagerObject.hiddenStoreIds.add("h")
        DataManagerObject.categories.add(
            node(
                category("c1"),
                subNode(
                    sub("s1", "c1"),
                    grocery("g1", "c1", "s1", storeIds = listOf("a"), inShoppingList = true),
                    grocery("g2", "c1", "s1", storeIds = listOf("a", "b"), inShoppingList = true),
                    grocery("g3", "c1", "s1", inShoppingList = true),
                    grocery("g4", "c1", "s1", storeIds = listOf("h"), inShoppingList = true),
                ),
            )
        )
    }

    private fun show(useScroll: Boolean = false, expandAll: Boolean = true) {
        compose.setContent {
            StoreBasedDisplay(
                categories = DataManagerObject.categories, searchQuery = "", isAllExpanded = expandAll,
                onEditGrocery = { edited += it }, useScroll = useScroll, onNavigateToStore = { navigated += it },
            )
        }
    }

    @Test
    fun `shows a section per visible store then the not-linked section`() {
        show()
        compose.onNodeWithText("Alpha").assertIsDisplayed()
        compose.onAllNodesWithText("2 items").assertCountEquals(2)
        compose.onNodeWithText("Beta").assertIsDisplayed()
        compose.onNodeWithText("Not Linked to Any Store").assertIsDisplayed()
        compose.onAllNodesWithText("1 items").assertCountEquals(4)
        compose.onNodeWithText("Hidden").assertDoesNotExist()
        compose.onNodeWithText("g4").assertDoesNotExist()
        assertTrue(top("Alpha") < top("Beta"))
        assertTrue(top("Beta") < top("Not Linked to Any Store"))
    }

    @Test
    fun `store sections render groceries in shopping-list mode`() {
        show()
        compose.onAllNodesWithText("g2").assertCountEquals(2)
        compose.onAllNodesWithContentDescription("Mark as bought").assertCountEquals(4)
        compose.onAllNodesWithContentDescription("Add to shopping list").assertCountEquals(0)
    }

    @Test
    fun `store header collapses and expands its categories`() {
        show()
        compose.onAllNodesWithContentDescription("Collapse").assertCountEquals(9)
        compose.onNodeWithText("Beta").performClick()
        compose.onAllNodesWithText("g2").assertCountEquals(1)
        compose.onAllNodesWithText("2 items").assertCountEquals(2)
        compose.onAllNodesWithContentDescription("Expand").assertCountEquals(1)
        compose.onNodeWithText("Beta").performClick()
        compose.onAllNodesWithText("g2").assertCountEquals(2)
    }

    @Test
    fun `store chips navigate through the nested display`() {
        show()
        compose.onAllNodesWithText("Bet")[0].performClick()
        assertEquals(listOf("b"), navigated)
    }

    @Test
    fun `categories inside store sections follow isAllExpanded`() {
        show(expandAll = false)
        compose.onNodeWithText("Alpha").assertIsDisplayed()
        compose.onAllNodesWithText("c1").assertCountEquals(3)
        compose.onNodeWithText("g1").assertDoesNotExist()
    }

    @Test
    fun `scrolling variant renders the store sections`() {
        show(useScroll = true, expandAll = false)
        compose.onNodeWithText("Alpha").assertIsDisplayed()
        compose.onNodeWithText("Not Linked to Any Store").assertIsDisplayed()
    }

    private fun top(text: String) = compose.onNodeWithText(text).fetchSemanticsNode().boundsInRoot.top
}
