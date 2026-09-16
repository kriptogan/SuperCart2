package com.example.supercart2.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.supercart2.TestData
import com.example.supercart2.TestData.category
import com.example.supercart2.TestData.grocery
import com.example.supercart2.TestData.node
import com.example.supercart2.TestData.sub
import com.example.supercart2.TestData.subNode
import com.example.supercart2.data.CategoryWithSubCategories
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
import java.time.LocalDate

/** Characterization tests: grocery row in the catalogue (isShoppingList = false). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w480dp-h2000dp")
class HierarchicalCategoryDisplayCatalogueRowTest {

    @get:Rule
    val compose = createComposeRule()

    private val edited = mutableListOf<Grocery>()
    private val navigated = mutableListOf<String>()

    @Before
    fun setUp() {
        TestData.reset()
        mockkObject(DataStoreManager)
        coEvery { DataStoreManager.saveDataGlobally() } just runs
    }

    @After
    fun tearDown() = unmockkAll()

    private fun install(g: Grocery) {
        DataManagerObject.categories.add(node(category("c1"), subNode(sub("s1", "c1"), g)))
    }

    private fun show(categories: List<CategoryWithSubCategories> = DataManagerObject.categories) {
        compose.setContent {
            HierarchicalCategoryDisplay(
                categories = categories, searchQuery = "", isAllExpanded = true, useScroll = false,
                onEditGrocery = { edited += it }, onNavigateToStore = { navigated += it },
            )
        }
    }

    @Test
    fun `store chips show aliases and navigate to the store`() {
        DataManagerObject.stores.addAll(
            listOf(Store(uuid = "st1", name = "Costco"), Store(uuid = "st2", name = " super  market deluxe plus "))
        )
        install(grocery("g1", "c1", "s1", storeIds = listOf("st1", "st2", "missing")))
        show()
        compose.onNodeWithText("Cos").assertIsDisplayed()
        compose.onNodeWithText("SMD").assertIsDisplayed()
        compose.onNodeWithText("Unk").assertIsDisplayed()
        compose.onNodeWithText("SMD").performClick()
        assertEquals(listOf("st2"), navigated)
    }

    @Test
    fun `two-letter single-word store keeps its whole name as alias`() {
        DataManagerObject.stores.add(Store(uuid = "st1", name = "Am"))
        install(grocery("g1", "c1", "s1", storeIds = listOf("st1")))
        show()
        compose.onNodeWithText("Am").assertIsDisplayed()
    }

    @Test
    fun `cart button toggles shopping list membership and saves`() {
        install(grocery("g1", "c1", "s1", isBought = true))
        show()
        compose.onNodeWithContentDescription("Add to shopping list").performClick()
        val g = TestData.findGrocery("g1")!!
        assertTrue(g.inShoppingList)
        assertFalse(g.isBought)
        compose.onNodeWithContentDescription("Remove from shopping list").performClick()
        assertFalse(TestData.findGrocery("g1")!!.inShoppingList)
        compose.onNodeWithContentDescription("Add to shopping list").assertIsDisplayed()
        coVerify(exactly = 2) { DataStoreManager.saveDataGlobally() }
    }

    @Test
    fun `more options menu lists edit before buy history and edit calls back`() {
        install(grocery("g1", "c1", "s1"))
        show()
        compose.onNodeWithContentDescription("More options").performClick()
        assertTrue(top("Edit item") < top("View Buy History"))
        compose.onNodeWithText("Remove from shopping list").assertDoesNotExist()
        compose.onNodeWithText("Edit item").performClick()
        assertEquals(listOf("g1"), edited.map { it.uuid })
        compose.onNodeWithText("Edit item").assertDoesNotExist()
    }

    @Test
    fun `pinned - edit passes the grocery from the categories argument, row shows the live one`() {
        install(grocery("g1", "c1", "s1").copy(name = "fresh", inShoppingList = true))
        val stale = grocery("g1", "c1", "s1").copy(name = "stale")
        show(listOf(node(category("c1"), subNode(sub("s1", "c1"), stale))))
        compose.onNodeWithText("fresh").assertIsDisplayed()
        compose.onNodeWithContentDescription("Remove from shopping list").assertIsDisplayed()
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("Edit item").performClick()
        assertEquals(listOf(stale), edited)
    }

    @Test
    fun `buy history dialog shows placeholder when empty and closes`() {
        install(grocery("g1", "c1", "s1"))
        show()
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("View Buy History").performClick()
        compose.onNodeWithText("Buy History - g1").assertIsDisplayed()
        compose.onNodeWithText("No purchase history yet").assertIsDisplayed()
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithText("Buy History - g1").assertDoesNotExist()
    }

    @Test
    fun `buy history lists dates newest first`() {
        val events = listOf(LocalDate.of(2024, 1, 5), LocalDate.of(2024, 3, 10), LocalDate.of(2023, 12, 25))
        install(grocery("g1", "c1", "s1").copy(buyEvents = events))
        show()
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("View Buy History").performClick()
        compose.onNodeWithText("No purchase history yet").assertDoesNotExist()
        assertTrue(top("Mar 10, 2024") < top("Jan 05, 2024"))
        assertTrue(top("Jan 05, 2024") < top("Dec 25, 2023"))
    }

    private fun top(text: String) = compose.onNodeWithText(text).fetchSemanticsNode().boundsInRoot.top
}
