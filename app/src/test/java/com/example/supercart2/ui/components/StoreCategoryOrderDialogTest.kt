package com.example.supercart2.ui.components

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.supercart2.TestData
import com.example.supercart2.TestData.category
import com.example.supercart2.TestData.node
import com.example.supercart2.TestData.store
import com.example.supercart2.TestData.sub
import com.example.supercart2.TestData.subNode
import com.example.supercart2.data.DataManagerObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "h1200dp")
class StoreCategoryOrderDialogTest {

    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()
    private val shop = store("S1")
    private val names = setOf("Dairy", "Milk", "Cheese", "Bakery", "Bread", "Gone", "OldSub", "Snacks")

    @Before
    fun setUp() {
        TestData.reset()
        DataManagerObject.stores.add(shop)
    }

    /** Dairy(viewOrder 2): Milk, Cheese (+ deleted OldSub); Bakery(1): Bread; deleted category Gone(0). */
    private fun installTree() {
        DataManagerObject.categories.addAll(
            listOf(
                node(
                    category("cA", viewOrder = 2, name = "Dairy"),
                    subNode(sub("sA1", "cA", name = "Milk")), subNode(sub("sA2", "cA", name = "Cheese")),
                    subNode(sub("sOld", "cA", deleted = true, name = "OldSub")),
                ),
                node(category("cB", viewOrder = 1, name = "Bakery"), subNode(sub("sB1", "cB", name = "Bread"))),
                node(category("cGone", viewOrder = 0, deleted = true, name = "Gone")),
            )
        )
    }

    private fun show() = compose.setContent {
        StoreCategoryOrderDialog(
            store = shop, onDismiss = { events += "dismiss" }, onOrderUpdated = { events += "updated" },
        )
    }

    private fun rows(): List<String> = compose
        .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text), useUnmergedTree = true)
        .fetchSemanticsNodes()
        .map { it.config[SemanticsProperties.Text].joinToString { t -> t.text } }
        .filter { it in names }

    private fun up(i: Int) = compose.onAllNodesWithContentDescription("Move Up")[i]
    private fun down(i: Int) = compose.onAllNodesWithContentDescription("Move Down")[i]

    @Test
    fun `shows title and non-deleted categories by viewOrder with their sub-categories`() {
        installTree()
        show()
        compose.onNodeWithText("Reorder Categories for S1").assertIsDisplayed()
        assertEquals(listOf("Bakery", "Bread", "Dairy", "Milk", "Cheese"), rows())
        compose.onNodeWithContentDescription("Save Order").assertIsEnabled()
        compose.onNodeWithContentDescription("Cancel").assertIsEnabled()
    }

    @Test
    fun `arrow enabled states follow position within categories and within each sub-category group`() {
        installTree()
        show()
        // rows: Bakery, Bread, Dairy, Milk, Cheese
        up(0).assertIsNotEnabled(); down(0).assertIsEnabled()
        up(1).assertIsNotEnabled(); down(1).assertIsNotEnabled()
        up(2).assertIsEnabled(); down(2).assertIsNotEnabled()
        up(3).assertIsNotEnabled(); down(3).assertIsEnabled()
        up(4).assertIsEnabled(); down(4).assertIsNotEnabled()
    }

    @Test
    fun `custom store orders are applied, unknown ids ignored and the rest sorted by viewOrder`() {
        installTree()
        DataManagerObject.categories.add(node(category("cC", viewOrder = 5, name = "Snacks")))
        DataManagerObject.storeCategoryOrders["S1"] = listOf("ghost", "cA")
        DataManagerObject.storeSubCategoryOrders["S1"] = mapOf("cA" to listOf("sA2", "sA1"))
        show()
        assertEquals(listOf("Dairy", "Cheese", "Milk", "Bakery", "Bread", "Snacks"), rows())
    }

    @Test
    fun `moving rows only changes local state until save, which stores both orders then notifies`() {
        installTree()
        show()
        down(0).performClick() // Bakery below Dairy
        compose.waitForIdle()
        assertEquals(listOf("Dairy", "Milk", "Cheese", "Bakery", "Bread"), rows())
        up(2).performClick() // Cheese above Milk
        compose.waitForIdle()
        assertEquals(listOf("Dairy", "Cheese", "Milk", "Bakery", "Bread"), rows())
        assertNull(DataManagerObject.getStoreCategoryOrder("S1"))

        compose.onNodeWithContentDescription("Save Order").performClick()

        assertEquals(listOf("cA", "cB"), DataManagerObject.storeCategoryOrders["S1"])
        assertEquals(
            mapOf("cB" to listOf("sB1"), "cA" to listOf("sA2", "sA1")),
            DataManagerObject.storeSubCategoryOrders["S1"],
        )
        assertEquals(listOf("updated", "dismiss"), events)
    }

    @Test
    fun `move up of a category and move down of a sub-category`() {
        installTree()
        show()
        up(2).performClick() // Dairy above Bakery
        compose.waitForIdle()
        down(1).performClick() // Milk below Cheese
        compose.waitForIdle()
        assertEquals(listOf("Dairy", "Cheese", "Milk", "Bakery", "Bread"), rows())
    }

    @Test
    fun `cancel dismisses without saving`() {
        installTree()
        show()
        down(0).performClick()
        compose.onNodeWithContentDescription("Cancel").performClick()
        assertEquals(listOf("dismiss"), events)
        assertNull(DataManagerObject.getStoreCategoryOrder("S1"))
        assertNull(DataManagerObject.storeSubCategoryOrders["S1"])
    }

    @Test
    fun `without categories shows the no-categories text and save is disabled`() {
        show()
        compose.onNodeWithText("No categories yet").assertIsDisplayed()
        compose.onNodeWithText("No stores yet").assertDoesNotExist()
        compose.onNodeWithContentDescription("Save Order").assertIsNotEnabled()
    }

    @Test
    fun `sub-categories missing from a custom sub order are shown after the ordered ones and can move`() {
        installTree()
        DataManagerObject.storeSubCategoryOrders["S1"] = mapOf("cA" to listOf("sA2"))
        show()
        assertEquals(listOf("Bakery", "Bread", "Dairy", "Cheese", "Milk"), rows())
        up(4).assertIsEnabled().performClick()
        compose.waitForIdle()
        assertEquals(listOf("Bakery", "Bread", "Dairy", "Milk", "Cheese"), rows())
        compose.onNodeWithContentDescription("Save Order").performClick()
        assertEquals(listOf("sA1", "sA2"), DataManagerObject.storeSubCategoryOrders["S1"]!!["cA"])
    }

    @Test
    fun `stale and deleted ids in custom orders are dropped for arrows and on save`() {
        installTree()
        DataManagerObject.storeCategoryOrders["S1"] = listOf("ghostCat", "cGone", "cA", "cB")
        DataManagerObject.storeSubCategoryOrders["S1"] = mapOf(
            "cA" to listOf("sA1", "sA2", "ghost", "sOld"), "ghostCat" to listOf("x"), "cGone" to listOf("y"),
        )
        show()
        // rows: Dairy, Milk, Cheese, Bakery, Bread - Cheese is last in its group
        assertEquals(listOf("Dairy", "Milk", "Cheese", "Bakery", "Bread"), rows())
        down(2).assertIsNotEnabled()
        compose.onNodeWithContentDescription("Save Order").performClick()
        assertEquals(listOf("cA", "cB"), DataManagerObject.storeCategoryOrders["S1"])
        assertEquals(
            mapOf("cA" to listOf("sA1", "sA2"), "cB" to listOf("sB1")),
            DataManagerObject.storeSubCategoryOrders["S1"],
        )
    }

    @Test
    fun `a version bump without content change keeps unsaved local moves`() {
        installTree()
        show()
        down(0).performClick()
        compose.waitForIdle()
        compose.runOnIdle { DataManagerObject.updateData() }
        compose.waitForIdle()
        assertEquals(listOf("Dairy", "Milk", "Cheese", "Bakery", "Bread"), rows())
    }

    @Test
    fun `a category change resets unsaved local moves`() {
        installTree()
        show()
        down(0).performClick()
        compose.waitForIdle()
        compose.runOnIdle {
            DataManagerObject.addCategory(category("cC", viewOrder = 5, name = "Snacks"))
        }
        compose.waitForIdle()
        assertEquals(listOf("Bakery", "Bread", "Dairy", "Milk", "Cheese", "Snacks"), rows())
    }
}
