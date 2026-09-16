package com.example.supercart2.ui.components

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.example.supercart2.TestData
import com.example.supercart2.TestData.category
import com.example.supercart2.TestData.grocery
import com.example.supercart2.TestData.node
import com.example.supercart2.TestData.sub
import com.example.supercart2.TestData.subNode
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import io.mockk.coEvery
import io.mockk.mockkObject
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "h1000dp")
class CategoriesManagementDialogTest {

    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()

    @Before
    fun setUp() {
        TestData.reset()
        mockkObject(DataStoreManager)
        coEvery { DataStoreManager.saveDataGlobally() } answers { events += "save" }
        DataManagerObject.categories.addAll(
            listOf(
                node(
                    category("fruit", viewOrder = 3, name = "Fruits"),
                    subNode(sub("s1", "fruit"), grocery("g1", "fruit", "s1"), grocery("g2", "fruit", "s1")),
                    subNode(sub("s2", "fruit", deleted = true)),
                ),
                node(
                    category("other", viewOrder = 1, name = "Other").copy(protected = true),
                    subNode(sub("s3", "other")),
                ),
                node(category("gone", viewOrder = 2, name = "Gone", deleted = true)),
                node(
                    category("veg", viewOrder = 2, name = "Veg"),
                    subNode(sub("s4", "veg"), grocery("g3", "veg", "s4"), grocery("g4", "veg", "s4", deleted = true)),
                ),
            )
        )
        compose.setContent { CategoriesManagementDialog(onDismiss = { events += "dismiss" }) }
    }

    @After
    fun tearDown() = unmockkAll()

    private fun categoryOf(uuid: String) = DataManagerObject.categories.first { it.category.uuid == uuid }.category

    private fun viewOrder(uuid: String) = categoryOf(uuid).viewOrder

    private fun assertListedInOrder(vararg names: String) {
        val tops = names.map { compose.onNodeWithText(it).assertIsDisplayed().fetchSemanticsNode().positionInRoot.y }
        assertEquals(tops.sorted(), tops)
    }

    @Test
    fun `lists non-deleted categories by viewOrder with counts and move states`() {
        compose.onNodeWithText("Categories Management").assertIsDisplayed()
        compose.onNodeWithText("Gone").assertDoesNotExist()
        assertListedInOrder("Other", "Veg", "Fruits")
        compose.onAllNodesWithText("Sub-Categories: 1").assertCountEquals(3)
        compose.onNodeWithText("Groceries: 0").assertIsDisplayed()
        compose.onNodeWithText("Groceries: 1").assertIsDisplayed()
        compose.onNodeWithText("Groceries: 2").assertIsDisplayed()
        compose.onAllNodesWithContentDescription("Move Up")[0].assertIsNotEnabled()
        compose.onAllNodesWithContentDescription("Move Up")[1].assertIsEnabled()
        compose.onAllNodesWithContentDescription("Move Up")[2].assertIsEnabled()
        compose.onAllNodesWithContentDescription("Move Down")[0].assertIsEnabled()
        compose.onAllNodesWithContentDescription("Move Down")[1].assertIsEnabled()
        compose.onAllNodesWithContentDescription("Move Down")[2].assertIsNotEnabled()
        compose.onAllNodesWithContentDescription("Edit Category").assertCountEquals(3)
        compose.onNodeWithContentDescription("Cancel").assertIsEnabled()
        compose.onNodeWithContentDescription("Create New Category").assertIsEnabled()
    }

    @Test
    fun `move up swaps viewOrder with the previous category and saves`() {
        compose.onAllNodesWithContentDescription("Move Up")[2].performClick()
        compose.waitForIdle()
        assertEquals(2, viewOrder("fruit"))
        assertEquals(3, viewOrder("veg"))
        assertEquals(listOf("save"), events)
        assertListedInOrder("Other", "Fruits", "Veg")
    }

    @Test
    fun `move down swaps viewOrder with the next category and saves`() {
        compose.onAllNodesWithContentDescription("Move Down")[0].performClick()
        compose.waitForIdle()
        assertEquals(2, viewOrder("other"))
        assertEquals(1, viewOrder("veg"))
        assertEquals(listOf("save"), events)
    }

    @Test
    fun `moving between equal viewOrders changes nothing but still saves (pinned)`() {
        DataManagerObject.updateCategory("fruit") { it.copy(viewOrder = 2) }
        compose.waitForIdle()
        compose.onAllNodesWithContentDescription("Move Down")[1].performClick()
        compose.waitForIdle()
        assertEquals(2, viewOrder("fruit"))
        assertEquals(2, viewOrder("veg"))
        assertEquals(listOf("save"), events)
    }

    @Test
    fun `cancel dismisses`() {
        compose.onNodeWithContentDescription("Cancel").performClick()
        compose.waitForIdle()
        assertEquals(listOf("dismiss"), events)
    }

    @Test
    fun `create adds a category with a protected General sub-category and saves`() {
        compose.onNodeWithContentDescription("Create New Category").performClick()
        compose.onNodeWithText("Category Name").performTextInput(" Bakery ")
        compose.onNodeWithContentDescription("Create Category").performClick()
        compose.waitForIdle()

        val added = DataManagerObject.categories.last()
        assertEquals("Bakery", added.category.name)
        assertEquals(4, added.category.viewOrder)
        assertEquals("General", added.subCategories.single().subCategory.name)
        assertTrue(added.subCategories.single().subCategory.protected)
        assertEquals(listOf("save"), events)
        compose.onNodeWithText("Category Name").assertDoesNotExist()
        compose.onNodeWithText("Bakery").assertExists()
    }

    @Test
    fun `edit opens the category dialog, cancel closes it`() {
        compose.onAllNodesWithContentDescription("Edit Category")[1].performClick()
        compose.onNodeWithText("Edit Category").assertIsDisplayed()
        compose.onAllNodesWithText("Veg").assertCountEquals(2)
        compose.onAllNodesWithContentDescription("Cancel")[1].performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Edit Category").assertDoesNotExist()
        assertTrue(events.isEmpty())
    }

    @Test
    fun `renaming in the edit dialog updates the list without saving (pinned)`() {
        compose.onAllNodesWithContentDescription("Edit Category")[1].performClick()
        compose.onNodeWithText("Category Name").performTextClearance()
        compose.onNodeWithText("Category Name").performTextInput("Greens")
        compose.onNodeWithContentDescription("Save Changes").performClick()
        compose.waitForIdle()
        assertEquals("Greens", categoryOf("veg").name)
        assertTrue(events.isEmpty())
        compose.onNodeWithText("Edit Category").assertDoesNotExist()
        compose.onNodeWithText("Greens").assertIsDisplayed()
    }

    @Test
    fun `deleting from the edit dialog soft-deletes, saves and closes it`() {
        DataManagerObject.storeCategoryOrders["store"] = listOf("veg", "fruit")
        compose.onAllNodesWithContentDescription("Edit Category")[1].performClick()
        compose.onNodeWithContentDescription("Delete Category").performClick()
        compose.onNodeWithContentDescription("Delete").performClick()
        compose.waitForIdle()

        assertTrue(categoryOf("veg").deleted)
        assertEquals(listOf("fruit"), DataManagerObject.storeCategoryOrders["store"])
        assertEquals(listOf("save"), events)
        compose.onNodeWithText("Edit Category").assertDoesNotExist()
        compose.onNodeWithText("Veg").assertDoesNotExist()
    }

    @Test
    fun `protected category cannot be deleted from the edit dialog`() {
        compose.onAllNodesWithContentDescription("Edit Category")[0].performClick()
        compose.onNodeWithContentDescription("Category Protected").assertIsNotEnabled()
        assertFalse(categoryOf("other").deleted)
    }
}
