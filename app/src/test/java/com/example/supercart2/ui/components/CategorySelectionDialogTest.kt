package com.example.supercart2.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.supercart2.TestData
import com.example.supercart2.TestData.category
import com.example.supercart2.TestData.node
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.models.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CategorySelectionDialogTest {

    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()
    private val selected = mutableListOf<Category>()

    @Before
    fun setUp() {
        TestData.reset()
        DataManagerObject.categories.addAll(
            listOf(
                node(category("c-b", viewOrder = 2, name = "Bakery")),
                node(category("c-o", viewOrder = 1, name = "Other")),
                node(category("c-x", viewOrder = 3, name = "Removed", deleted = true)),
            )
        )
        compose.setContent {
            CategorySelectionDialog(
                onDismiss = { events += "dismiss" },
                onCategorySelected = { selected += it; events += "selected:${it.uuid}" },
                onNewCategoryCreated = { c, s ->
                    val inData = DataManagerObject.categories.any { it.category.uuid == c.uuid }
                    events += "created:${c.name}/${s.name}/inData=$inData"
                },
                selectedCategoryId = "c-b",
            )
        }
    }

    @Test
    fun `lists non-deleted categories by viewOrder with localized names`() {
        compose.onNodeWithText("Select Category").assertIsDisplayed()
        compose.onNodeWithText("Removed").assertDoesNotExist()
        val otherTop = compose.onNodeWithText("Other").fetchSemanticsNode().boundsInRoot.top
        val bakeryTop = compose.onNodeWithText("Bakery").fetchSemanticsNode().boundsInRoot.top
        assertTrue(otherTop < bakeryTop)
        compose.onNodeWithContentDescription("Close").assertIsDisplayed()
        compose.onNodeWithContentDescription("New Category").assertIsDisplayed()
    }

    @Test
    fun `clicking a category reports it without dismissing`() {
        compose.onNodeWithText("Bakery").performClick()
        assertEquals(listOf("selected:c-b"), events)
        assertEquals("Bakery", selected.single().name)
    }

    @Test
    fun `close dismisses`() {
        compose.onNodeWithContentDescription("Close").performClick()
        assertEquals(listOf("dismiss"), events)
    }

    @Test
    fun `new category opens create dialog, cancel closes it`() {
        compose.onNodeWithContentDescription("New Category").performClick()
        compose.onNodeWithText("Category Name").assertIsDisplayed()
        compose.onNodeWithContentDescription("Cancel").performClick()
        compose.onNodeWithText("Category Name").assertDoesNotExist()
        compose.onNodeWithText("Select Category").assertIsDisplayed()
        assertTrue(events.isEmpty())
    }

    @Test
    fun `created category is appended with General sub before the callback and shown in the list`() {
        val versionBefore = DataManagerObject.version
        compose.onNodeWithContentDescription("New Category").performClick()
        compose.onNodeWithText("Category Name").performTextInput("Dairy")
        compose.onNodeWithContentDescription("Create Category").performClick()

        assertEquals(listOf("created:Dairy/General/inData=true"), events)
        val added = DataManagerObject.categories.last()
        assertEquals("Dairy", added.category.name)
        assertEquals(4, added.category.viewOrder)
        assertEquals(listOf("General"), added.subCategories.map { it.subCategory.name })
        assertTrue(added.subCategories.single().groceries.isEmpty())
        assertEquals(versionBefore + 1, DataManagerObject.version)
        compose.onNodeWithText("Category Name").assertDoesNotExist()
        compose.onNodeWithText("Dairy").assertIsDisplayed()
    }
}
