package com.example.supercart2.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.supercart2.TestData
import com.example.supercart2.TestData.category
import com.example.supercart2.TestData.grocery
import com.example.supercart2.TestData.node
import com.example.supercart2.TestData.sub
import com.example.supercart2.TestData.subNode
import com.example.supercart2.data.DataManagerObject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Characterization tests: category → sub-category tree, expand/collapse behaviour. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w480dp-h2000dp")
class HierarchicalCategoryDisplayTreeTest {

    @get:Rule
    val compose = createComposeRule()

    private var expandAll by mutableStateOf(false)

    @Before
    fun setUp() {
        TestData.installStandardTree()
    }

    private fun show(useScroll: Boolean = false) {
        compose.setContent {
            HierarchicalCategoryDisplay(
                categories = DataManagerObject.categories, searchQuery = "", isAllExpanded = expandAll,
                onEditGrocery = {}, useScroll = useScroll,
            )
        }
    }

    @Test
    fun `collapsed tree shows category headers with counts only`() {
        show()
        compose.onNodeWithText("c1").assertIsDisplayed()
        compose.onNodeWithText("2 items").assertIsDisplayed()
        compose.onNodeWithText("c2").assertIsDisplayed()
        compose.onNodeWithText("1 items").assertIsDisplayed()
        compose.onNodeWithText("s1").assertDoesNotExist()
        compose.onAllNodesWithContentDescription("Expand").assertCountEquals(2)
        compose.onAllNodesWithContentDescription("Collapse").assertCountEquals(0)
    }

    @Test
    fun `expanding a category with several sub-categories shows them collapsed`() {
        show()
        compose.onNodeWithText("c1").performClick()
        compose.onNodeWithText("s1").assertIsDisplayed()
        compose.onNodeWithText("(2)").assertIsDisplayed()
        compose.onNodeWithText("s2").assertIsDisplayed()
        compose.onNodeWithText("(0)").assertIsDisplayed()
        compose.onNodeWithText("g1").assertDoesNotExist()
        compose.onAllNodesWithContentDescription("Collapse").assertCountEquals(1)
        compose.onAllNodesWithContentDescription("Expand").assertCountEquals(3)
    }

    @Test
    fun `expanding a category with a single sub-category auto-expands it`() {
        show()
        compose.onNodeWithText("c2").performClick()
        compose.onNodeWithText("s3").assertIsDisplayed()
        compose.onNodeWithText("g3").assertIsDisplayed()
    }

    @Test
    fun `sub-category toggles its groceries and shows placeholder when empty`() {
        show()
        compose.onNodeWithText("c1").performClick()
        compose.onNodeWithText("s1").performClick()
        compose.onNodeWithText("g1").assertIsDisplayed()
        compose.onNodeWithText("g2").assertIsDisplayed()
        compose.onNodeWithText("s2").performClick()
        compose.onNodeWithText("No groceries yet").assertIsDisplayed()
        compose.onNodeWithText("s1").performClick()
        compose.onNodeWithText("g1").assertDoesNotExist()
    }

    @Test
    fun `collapsing a category hides its sub-categories`() {
        show()
        compose.onNodeWithText("c1").performClick()
        compose.onNodeWithText("c1").performClick()
        compose.onNodeWithText("s1").assertDoesNotExist()
    }

    @Test
    fun `isAllExpanded expands and collapses the whole tree`() {
        expandAll = true
        show()
        listOf("s1", "s2", "s3", "g1", "g2", "g3", "No groceries yet").forEach {
            compose.onNodeWithText(it).assertIsDisplayed()
        }
        compose.onAllNodesWithContentDescription("Expand").assertCountEquals(0)
        expandAll = false
        compose.waitForIdle()
        compose.onNodeWithText("s1").assertDoesNotExist()
        compose.onNodeWithText("g3").assertDoesNotExist()
    }

    @Test
    fun `pinned - single sub-category re-expands when its category is collapsed and expanded again`() {
        show()
        compose.onNodeWithText("c2").performClick()
        compose.onNodeWithText("s3").performClick()
        compose.onNodeWithText("g3").assertDoesNotExist()
        compose.onNodeWithText("c2").performClick()
        compose.onNodeWithText("c2").performClick()
        compose.onNodeWithText("g3").assertIsDisplayed()
    }

    @Test
    fun `default category and sub-category names are shown localized`() {
        TestData.reset()
        DataManagerObject.categories.add(
            node(category("o", name = "Other"), subNode(sub("gen", "o", name = "General"), grocery("milk", "o", "gen")))
        )
        expandAll = true
        show()
        compose.onNodeWithText("Other").assertIsDisplayed()
        compose.onNodeWithText("General").assertIsDisplayed()
        compose.onNodeWithText("milk").assertIsDisplayed()
    }

    @Test
    fun `scrolling variant renders the same tree`() {
        show(useScroll = true)
        compose.onNodeWithText("c1").assertIsDisplayed()
        compose.onNodeWithText("c2").performClick()
        compose.onNodeWithText("g3").assertIsDisplayed()
    }
}
