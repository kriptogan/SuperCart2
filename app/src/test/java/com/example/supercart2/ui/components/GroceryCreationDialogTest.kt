package com.example.supercart2.ui.components

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.example.supercart2.TestData
import com.example.supercart2.TestData.category
import com.example.supercart2.TestData.node
import com.example.supercart2.TestData.store
import com.example.supercart2.TestData.sub
import com.example.supercart2.TestData.subNode
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.models.Grocery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Create mode of [GroceryCreationDialog]: defaults, pickers and saving a new grocery. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GroceryCreationDialogTest {

    @get:Rule
    val compose = createComposeRule()

    private var dismissed = 0

    @Before
    fun setUp() {
        TestData.installStandardTree()
    }

    private fun show(name: String = "", addToShoppingList: Boolean = false, storeId: String? = null) {
        compose.setContent {
            GroceryCreationDialog(
                onDismiss = { dismissed++ },
                initialGroceryName = name, addToShoppingList = addToShoppingList, initialStoreId = storeId,
            )
        }
    }

    private fun text(value: String) = compose.onNodeWithText(value).performScrollTo()
    private fun desc(value: String) = compose.onNodeWithContentDescription(value).performScrollTo()

    private fun newGroceries() = DataManagerObject.categories.flatMap { it.subCategories }
        .flatMap { it.groceries }.filterNot { it.uuid in setOf("g1", "g2", "g3") }

    @Test
    fun `create mode shows title, fields and preselects first sorted category and its first sub-category`() {
        show()
        compose.onNodeWithText("Create New Grocery").assertIsDisplayed()
        text("Add Image").assertIsDisplayed()
        text("Tap to upload").assertIsDisplayed()
        text("Grocery Name").assertIsDisplayed()
        text("Category").assertIsDisplayed()
        text("c2").assertIsDisplayed()
        text("Sub-Category").assertIsDisplayed()
        text("s3").assertIsDisplayed()
        text("Stores").assertIsDisplayed()
        text("Select Stores (Optional)").assertIsDisplayed()
        text("Expiration Date").assertIsDisplayed()
        text("No date selected").assertIsDisplayed()
        compose.onNodeWithText("Clear").assertDoesNotExist()
        compose.onNodeWithContentDescription("Delete Grocery").assertDoesNotExist()
        compose.onNodeWithContentDescription("Create Grocery").assertIsNotEnabled()
    }

    @Test
    fun `save is enabled only with a non-blank name`() {
        show()
        text("Grocery Name").performTextInput("   ")
        compose.onNodeWithContentDescription("Create Grocery").assertIsNotEnabled()
        text("Grocery Name").performTextClearance()
        text("Grocery Name").performTextInput("Milk")
        compose.onNodeWithContentDescription("Create Grocery").assertIsEnabled()
    }

    @Test
    fun `without categories nothing is selected and sub-category button and save are disabled`() {
        TestData.reset()
        show(name = "Milk")
        text("Select Category").assertIsDisplayed()
        text("Select Sub-Category").assertIsDisplayed().assertIsNotEnabled()
        compose.onNodeWithContentDescription("Create Grocery").assertIsNotEnabled()
    }

    @Test
    fun `create adds a trimmed grocery to the selected location and dismisses`() {
        show(name = "  Milk ")
        compose.onNodeWithContentDescription("Create Grocery").assertIsEnabled().performClick()

        val added = newGroceries().single()
        assertEquals("Milk", added.name)
        assertEquals("c2" to "s3", TestData.locationOf(added.uuid))
        assertFalse(added.inShoppingList)
        assertFalse(added.isBought)
        assertNull(added.expirationDate)
        assertNull(added.imageUUID)
        assertTrue(added.storeIds.isEmpty())
        assertEquals(1, dismissed)
    }

    @Test
    fun `new grocery is appended after the existing groceries of the sub-category`() {
        show(name = "Milk")
        compose.onNodeWithContentDescription("Create Grocery").performClick()
        val ids = DataManagerObject.categories.first { it.category.uuid == "c2" }
            .subCategories.single().groceries.map { it.uuid }
        assertEquals(listOf("g3", newGroceries().single().uuid), ids)
    }

    @Test
    fun `addToShoppingList and initialStoreId are applied to the new grocery`() {
        DataManagerObject.stores.add(store("st1"))
        show(name = "Milk", addToShoppingList = true, storeId = "st1")
        text("st1").assertIsDisplayed()
        compose.onNodeWithContentDescription("Create Grocery").performClick()

        val added = newGroceries().single()
        assertTrue(added.inShoppingList)
        assertEquals(listOf("st1"), added.storeIds)
    }

    @Test
    fun `cancel dismisses without saving`() {
        show(name = "Milk")
        compose.onNodeWithContentDescription("Cancel").performClick()
        assertEquals(1, dismissed)
        assertTrue(newGroceries().isEmpty())
    }

    @Test
    fun `choosing a category selects its first sub-category`() {
        show(name = "Milk")
        desc("Select Category").performClick()
        compose.onNodeWithText("c1").performClick()
        text("c1").assertIsDisplayed()
        text("s1").assertIsDisplayed()
        compose.onNodeWithContentDescription("Create Grocery").performClick()
        assertEquals("c1" to "s1", TestData.locationOf(newGroceries().single().uuid))
    }

    @Test
    fun `choosing a category selects its first non-deleted sub-category`() {
        DataManagerObject.categories.add(
            node(category("c3", viewOrder = 3), subNode(sub("dead", "c3", deleted = true)), subNode(sub("s4", "c3")))
        )
        show(name = "Milk")
        desc("Select Category").performClick()
        compose.onNodeWithText("c3").performClick()
        compose.onNodeWithText("dead").assertDoesNotExist()
        text("s4").assertIsDisplayed()
        compose.onNodeWithContentDescription("Create Grocery").performClick()
        assertEquals("c3" to "s4", TestData.locationOf(newGroceries().single().uuid))
    }

    @Test
    fun `choosing a category whose sub-categories are all deleted selects no sub-category`() {
        DataManagerObject.categories.add(node(category("c3", viewOrder = 3), subNode(sub("dead", "c3", deleted = true))))
        show(name = "Milk")
        desc("Select Category").performClick()
        compose.onNodeWithText("c3").performClick()
        text("Select Sub-Category").assertIsDisplayed()
        compose.onNodeWithContentDescription("Create Grocery").assertIsNotEnabled()
    }

    @Test
    fun `category picker does not offer deleted categories`() {
        DataManagerObject.categories.add(node(category("gone", viewOrder = 3, deleted = true), subNode(sub("s9", "gone"))))
        show(name = "Milk")
        desc("Select Category").performClick()
        compose.onNodeWithText("c1").assertIsDisplayed()
        compose.onNodeWithText("gone").assertDoesNotExist()
    }

    @Test
    fun `sub-category picker does not offer deleted sub-categories`() {
        DataManagerObject.categories.add(
            node(category("c3", viewOrder = 3), subNode(sub("s4", "c3")), subNode(sub("dead", "c3", deleted = true)))
        )
        show(name = "Milk")
        desc("Select Category").performClick()
        compose.onNodeWithText("c3").performClick()
        desc("Select Sub-Category").performClick()
        compose.onAllNodesWithText("s4").assertCountEquals(2) // selector button + picker row
        compose.onNodeWithText("dead").assertDoesNotExist()
    }

    @Test
    fun `a sub-category created from the picker is added, selected and used for the new grocery`() {
        show(name = "Milk")
        desc("Select Sub-Category").performClick()
        compose.onNodeWithContentDescription("New Sub-Category").performClick()
        compose.onNodeWithText("Sub-Category Name").performTextInput("Dairy")
        compose.onNodeWithContentDescription("Create Sub-Category").performClick()
        compose.onNodeWithText("Sub-Category Name").assertDoesNotExist()
        text("Dairy").assertIsDisplayed()
        compose.onNodeWithContentDescription("Create Grocery").performClick()
        val dairy = DataManagerObject.categories.first { it.category.uuid == "c2" }.subCategories.last().subCategory
        assertEquals("Dairy", dairy.name)
        assertEquals("c2" to dairy.uuid, TestData.locationOf(newGroceries().single().uuid))
    }

    @Test
    fun `choosing a sub-category updates the selection`() {
        show(name = "Milk")
        desc("Select Category").performClick()
        compose.onNodeWithText("c1").performClick()
        desc("Select Sub-Category").performClick()
        compose.onNodeWithText("s2").performClick()
        text("s2").assertIsDisplayed()
        compose.onNodeWithContentDescription("Create Grocery").performClick()
        assertEquals("c1" to "s2", TestData.locationOf(newGroceries().single().uuid))
    }

    @Test
    fun `store label shows one name or a count and selected stores are saved`() {
        DataManagerObject.stores.addAll(listOf(store("A", viewOrder = 1), store("B", viewOrder = 2)))
        show(name = "Milk")
        desc("Select Stores").performClick()
        compose.onNodeWithText("A").performClick()
        compose.onNodeWithContentDescription("Close").performClick()
        text("A").assertIsDisplayed()

        desc("Select Stores").performClick()
        compose.onNodeWithText("B").performClick()
        compose.onNodeWithContentDescription("Close").performClick()
        text("2 stores selected").assertIsDisplayed()

        compose.onNodeWithContentDescription("Create Grocery").performClick()
        assertEquals(listOf("A", "B"), newGroceries().single().storeIds)
    }

    @Test
    fun `unknown store ids are not counted in the label but are still saved`() {
        show(name = "Milk", storeId = "ghost")
        text("Select Stores (Optional)").assertIsDisplayed()
        compose.onNodeWithContentDescription("Create Grocery").performClick()
        assertEquals(listOf("ghost"), newGroceries().single().storeIds)
    }
}
