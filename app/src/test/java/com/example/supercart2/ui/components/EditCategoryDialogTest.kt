package com.example.supercart2.ui.components

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
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
import com.example.supercart2.data.SubCategoryWithGroceries
import com.example.supercart2.models.Category
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
@Config(sdk = [34])
class EditCategoryDialogTest {

    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()
    private val deletedCategories = mutableListOf<Category>()

    @Before
    fun setUp() {
        TestData.reset()
        mockkObject(DataStoreManager)
        coEvery { DataStoreManager.saveDataGlobally() } answers { events += "save" }
        DataManagerObject.categories.add(
            node(
                category("c1", name = "Dairy"),
                subNode(sub("s1", "c1", name = "General").copy(protected = true), grocery("g1", "c1", "s1")),
                subNode(sub("s2", "c1", name = "Cheese"), grocery("g2", "c1", "s2"), grocery("g3", "c1", "s2")),
            )
        )
    }

    @After
    fun tearDown() = unmockkAll()

    private fun node() = DataManagerObject.categories.single()

    private fun show(category: Category = node().category, subs: List<SubCategoryWithGroceries> = emptyList()) {
        compose.setContent {
            EditCategoryDialog(
                category = category,
                subCategories = subs,
                onDismiss = { events += "dismiss" },
                onCategoryUpdated = { events += "categoryUpdated" },
                onCategoryDeleted = { events += "categoryDeleted"; deletedCategories += it },
                onSubCategoryCreated = { events += "subCreated" },
                onSubCategoryUpdated = { events += "subUpdated" },
                onSubCategoryDeleted = { events += "subDeleted" },
            )
        }
    }

    @Test
    fun `shows title, name, sub-category cards and buttons`() {
        show()
        compose.onNodeWithText("Edit Category").assertIsDisplayed()
        compose.onNodeWithText("Category Name").assertIsDisplayed()
        compose.onNodeWithText("Dairy").assertIsDisplayed()
        compose.onNodeWithContentDescription("Delete Category").assertIsEnabled()
        compose.onNodeWithText("Sub-Categories").assertIsDisplayed()
        compose.onNodeWithContentDescription("Add Sub-Category").assertIsEnabled()
        compose.onNodeWithText("General").assertIsDisplayed()
        compose.onNodeWithText("(Protected)").assertIsDisplayed()
        compose.onNodeWithText("Groceries: 1").assertIsDisplayed()
        compose.onNodeWithText("Cheese").assertExists()
        compose.onNodeWithText("Groceries: 2").assertExists()
        compose.onAllNodesWithContentDescription("Edit Sub-Category").assertCountEquals(2)
        compose.onNodeWithContentDescription("Sub-Category Protected").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Delete Sub-Category").assertIsEnabled()
        compose.onNodeWithContentDescription("Cancel").assertIsEnabled()
        compose.onNodeWithContentDescription("Save Changes").assertIsEnabled()
    }

    @Test
    fun `falls back to the passed sub-categories when the category is not in data`() {
        val orphan = category("x", name = "Orphan")
        show(orphan, listOf(subNode(sub("sx", "x", name = "Loose"))))
        compose.onNodeWithText("Loose").assertIsDisplayed()
        compose.onNodeWithText("Groceries: 0").assertIsDisplayed()
        compose.onNodeWithText("Cheese").assertDoesNotExist()
    }

    @Test
    fun `save renames the category, saves and dismisses without calling onCategoryUpdated`() {
        show()
        compose.onNodeWithText("Dairy").performTextClearance()
        compose.onNodeWithContentDescription("Save Changes").assertIsNotEnabled()
        compose.onNodeWithText("Category Name").performTextInput("  Milk ")
        compose.onNodeWithContentDescription("Save Changes").performClick()
        compose.waitForIdle()

        assertEquals("Milk", node().category.name)
        assertTrue(node().category.lastUpdate.isAfter(TestData.OLD))
        assertEquals(listOf("dismiss", "save"), events.sorted())
    }

    @Test
    fun `deleted sub-categories are not listed and counts exclude deleted groceries`() {
        DataManagerObject.categories[0] = node(
            category("c1", name = "Dairy"),
            subNode(sub("s1", "c1", name = "General"), grocery("g1", "c1", "s1"), grocery("g4", "c1", "s1", deleted = true)),
            subNode(sub("s2", "c1", name = "Cheese", deleted = true), grocery("g2", "c1", "s2")),
        )
        show()
        compose.onNodeWithText("General").assertIsDisplayed()
        compose.onNodeWithText("Groceries: 1").assertIsDisplayed()
        compose.onNodeWithText("Cheese").assertDoesNotExist()
        compose.onAllNodesWithContentDescription("Edit Sub-Category").assertCountEquals(1)
    }

    @Test
    fun `fallback sub-categories are also shown without deleted items`() {
        val orphan = category("x", name = "Orphan")
        val subs = listOf(
            subNode(sub("sx", "x", name = "Loose"), grocery("gx", "x", "sx", deleted = true)),
            subNode(sub("sy", "x", name = "Gone", deleted = true)),
        )
        show(orphan, subs)
        compose.onNodeWithText("Loose").assertIsDisplayed()
        compose.onNodeWithText("Groceries: 0").assertIsDisplayed()
        compose.onNodeWithText("Gone").assertDoesNotExist()
    }

    @Test
    fun `cancel dismisses without renaming`() {
        show()
        compose.onNodeWithText("Dairy").performTextInput("X")
        compose.onNodeWithContentDescription("Cancel").performClick()
        compose.waitForIdle()
        assertEquals(listOf("dismiss"), events)
        assertEquals("Dairy", node().category.name)
    }

    @Test
    fun `delete confirmation calls onCategoryDeleted and closes itself`() {
        show()
        compose.onNodeWithContentDescription("Delete Category").performClick()
        compose.onNodeWithText("Delete Category").assertIsDisplayed()
        compose.onNodeWithText(
            "Are you sure you want to delete 'Dairy'? " +
                "This will also delete all sub-categories and groceries linked to it."
        ).assertIsDisplayed()
        compose.onNodeWithText("⚠️ This category is protected and cannot be deleted.").assertDoesNotExist()
        compose.onNodeWithContentDescription("Delete").assertIsEnabled().performClick()
        compose.waitForIdle()

        assertEquals(listOf("categoryDeleted"), events)
        assertEquals("c1", deletedCategories.single().uuid)
        assertFalse(node().category.deleted)
        compose.onNodeWithText("Delete Category").assertDoesNotExist()
        compose.onNodeWithText("Edit Category").assertIsDisplayed()
    }

    @Test
    fun `cancelling the delete confirmation closes it`() {
        show()
        compose.onNodeWithContentDescription("Delete Category").performClick()
        compose.onAllNodesWithContentDescription("Cancel")[1].performClick()
        compose.waitForIdle()
        assertTrue(events.isEmpty())
        compose.onNodeWithText("Delete Category").assertDoesNotExist()
    }

    @Test
    fun `protected category cannot be deleted`() {
        show(node().category.copy(protected = true))
        compose.onNodeWithContentDescription("Category Protected").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Delete Category").assertDoesNotExist()
    }

    @Test
    fun `add sub-category creates it, saves and closes the create dialog without callback`() {
        show()
        compose.onNodeWithContentDescription("Add Sub-Category").performClick()
        compose.onNodeWithText("Create New Sub-Category").assertIsDisplayed()
        compose.onNodeWithText("Sub-Category Name").performTextInput("  Yogurt ")
        compose.onNodeWithContentDescription("Create Sub-Category").performClick()
        compose.waitForIdle()

        val added = node().subCategories.last().subCategory
        assertEquals("Yogurt", added.name)
        assertEquals("c1", added.categoryId)
        assertFalse(added.protected)
        assertFalse(added.deleted)
        assertEquals(listOf("save"), events)
        compose.onNodeWithText("Create New Sub-Category").assertDoesNotExist()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Yogurt"))
        compose.onNodeWithText("Yogurt").assertIsDisplayed()
    }

    @Test
    fun `cancel on the create sub-category dialog closes it`() {
        show()
        compose.onNodeWithContentDescription("Add Sub-Category").performClick()
        compose.onAllNodesWithContentDescription("Cancel")[1].performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Create New Sub-Category").assertDoesNotExist()
        assertEquals(2, node().subCategories.size)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `deleting a sub-category soft-deletes, saves and removes its card`() {
        show()
        compose.onNodeWithContentDescription("Delete Sub-Category").performClick()
        compose.waitForIdle()
        assertTrue(node().subCategories[1].subCategory.deleted)
        assertEquals(listOf("save"), events)
        compose.onNodeWithText("Cheese").assertDoesNotExist()
    }

    @Test
    fun `edit sub-category opens its dialog and the list shows the new name after save`() {
        show()
        compose.onAllNodesWithContentDescription("Edit Sub-Category")[1].performClick()
        compose.onAllNodesWithText("Cheese").assertCountEquals(2)
        compose.onNodeWithText("Sub-Category Name").performTextClearance()
        compose.onNodeWithText("Sub-Category Name").performTextInput("Feta")
        compose.onAllNodesWithContentDescription("Save Changes")[1].performClick()
        compose.waitForIdle()

        assertEquals("Feta", node().subCategories[1].subCategory.name)
        assertEquals(listOf("save"), events)
        compose.onNodeWithText("Edit Sub-Category").assertDoesNotExist()
        compose.onNodeWithText("Feta").assertExists()
    }

    @Test
    fun `cancel on the edit sub-category dialog closes it`() {
        show()
        compose.onAllNodesWithContentDescription("Edit Sub-Category")[0].performClick()
        compose.onNodeWithText("Edit Sub-Category").assertIsDisplayed()
        compose.onAllNodesWithContentDescription("Cancel")[1].performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Edit Sub-Category").assertDoesNotExist()
        assertTrue(events.isEmpty())
    }
}
