package com.example.supercart2.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.example.supercart2.TestData
import com.example.supercart2.TestData.category
import com.example.supercart2.TestData.node
import com.example.supercart2.TestData.sub
import com.example.supercart2.TestData.subNode
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.models.SubCategory
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
class EditSubCategoryDialogTest {

    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()
    private val updated = mutableListOf<SubCategory>()

    @Before
    fun setUp() {
        TestData.reset()
        mockkObject(DataStoreManager)
        coEvery { DataStoreManager.saveDataGlobally() } answers { events += "save" }
        DataManagerObject.categories.add(node(category("c1"), subNode(sub("s1", "c1", name = "Dairy"))))
    }

    @After
    fun tearDown() = unmockkAll()

    private fun show(subCategory: SubCategory = current(), categoryId: String? = null) {
        compose.setContent {
            val onDismiss = { events += "dismiss"; Unit }
            val onUpdated = { s: SubCategory -> events += "updated"; updated += s; Unit }
            if (categoryId == null) EditSubCategoryDialog(subCategory, onDismiss, onUpdated)
            else EditSubCategoryDialog(subCategory, onDismiss, onUpdated, categoryId)
        }
    }

    private fun current() = DataManagerObject.categories.single().subCategories.single().subCategory

    @Test
    fun `shows title, prefilled name, delete icon and buttons`() {
        show()
        compose.onNodeWithText("Edit Sub-Category").assertIsDisplayed()
        compose.onNodeWithText("Sub-Category Name").assertIsDisplayed()
        compose.onNodeWithText("Dairy").assertIsDisplayed()
        compose.onNodeWithContentDescription("Delete Sub-Category").assertIsEnabled()
        compose.onNodeWithContentDescription("Cancel").assertIsEnabled()
        compose.onNodeWithContentDescription("Save Changes").assertIsEnabled()
    }

    @Test
    fun `save is disabled for a blank name`() {
        show()
        compose.onNodeWithText("Dairy").performTextClearance()
        compose.onNodeWithContentDescription("Save Changes").assertIsNotEnabled()
        compose.onNodeWithText("Sub-Category Name").performTextInput("  ")
        compose.onNodeWithContentDescription("Save Changes").assertIsNotEnabled()
    }

    @Test
    fun `save trims, updates data, updated callback and dismiss`() {
        show()
        compose.onNodeWithText("Dairy").performTextClearance()
        compose.onNodeWithText("Sub-Category Name").performTextInput(" Milk ")
        compose.onNodeWithContentDescription("Save Changes").performClick()
        compose.waitForIdle()

        assertEquals("Milk", current().name)
        assertTrue(current().lastUpdate.isAfter(TestData.OLD))
        assertEquals("Milk", updated.single().name)
        assertEquals("s1", updated.single().uuid)
        assertEquals(listOf("save", "updated", "dismiss"), events)
    }

    @Test
    fun `save uses the explicit categoryId`() {
        show(categoryId = "other")
        compose.onNodeWithText("Dairy").performTextClearance()
        compose.onNodeWithText("Sub-Category Name").performTextInput("X")
        compose.onNodeWithContentDescription("Save Changes").performClick()
        compose.waitForIdle()
        assertEquals("Dairy", current().name)
        assertEquals("X", updated.single().name)
    }

    @Test
    fun `cancel dismisses without changes`() {
        show()
        compose.onNodeWithText("Dairy").performTextInput("X")
        compose.onNodeWithContentDescription("Cancel").performClick()
        compose.waitForIdle()
        assertEquals(listOf("dismiss"), events)
        assertEquals("Dairy", current().name)
    }

    @Test
    fun `delete asks for confirmation, then soft-deletes, saves and dismisses`() {
        show()
        compose.onNodeWithContentDescription("Delete Sub-Category").performClick()
        compose.onNodeWithText("Delete Sub-Category").assertIsDisplayed()
        compose.onNodeWithText(
            "Are you sure you want to delete 'Dairy'? This will also delete all groceries linked to it."
        ).assertIsDisplayed()
        compose.onNodeWithText("⚠️ This sub-category is protected and cannot be deleted.").assertDoesNotExist()
        compose.onNodeWithContentDescription("Delete").assertIsEnabled().performClick()
        compose.waitForIdle()

        assertTrue(current().deleted)
        assertEquals(listOf("save", "dismiss"), events)
        assertTrue(updated.isEmpty())
        compose.onNodeWithText("Delete Sub-Category").assertDoesNotExist()
    }

    @Test
    fun `cancelling the confirmation keeps the sub-category`() {
        show()
        compose.onNodeWithContentDescription("Delete Sub-Category").performClick()
        compose.onNodeWithContentDescription("Delete").assertIsDisplayed()
        compose.onAllNodesWithContentDescription("Cancel")[1].performClick()
        compose.waitForIdle()
        assertFalse(current().deleted)
        assertTrue(events.isEmpty())
        compose.onNodeWithContentDescription("Delete").assertDoesNotExist()
        compose.onNodeWithText("Edit Sub-Category").assertIsDisplayed()
    }

    @Test
    fun `protected sub-category cannot be deleted`() {
        show(current().copy(protected = true))
        compose.onNodeWithContentDescription("Sub-Category Protected").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Delete Sub-Category").assertDoesNotExist()
    }

    @Test
    fun `save keeps protected flag`() {
        show(current().copy(protected = true))
        compose.onNodeWithText("Dairy").performTextClearance()
        compose.onNodeWithText("Sub-Category Name").performTextInput("X")
        compose.onNodeWithContentDescription("Save Changes").performClick()
        compose.waitForIdle()
        assertTrue(current().protected)
        assertTrue(updated.single().protected)
    }
}
