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
import androidx.compose.ui.test.performTextInput
import com.example.supercart2.TestData
import com.example.supercart2.TestData.store
import com.example.supercart2.data.DataManagerObject
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
@Config(sdk = [34], qualifiers = "h1200dp")
class StoresManagementDialogTest {

    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()
    private val names = setOf("Aldi", "Lidl", "Costco", "Gone", "Target")

    @Before
    fun setUp() {
        TestData.reset()
    }

    private fun installStores() {
        DataManagerObject.stores.addAll(
            listOf(store("Lidl", viewOrder = 2), store("Aldi", viewOrder = 1),
                store("Gone", viewOrder = 0, deleted = true), store("Costco", viewOrder = 3))
        )
    }

    private fun show() = compose.setContent {
        StoresManagementDialog(onDismiss = { events += "dismiss" }, onEnterStoreEditMode = { events += "edit:$it" })
    }

    private fun rows(): List<String> = compose
        .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text), useUnmergedTree = true)
        .fetchSemanticsNodes()
        .map { it.config[SemanticsProperties.Text].joinToString { t -> t.text } }
        .filter { it in names }

    private fun up(i: Int) = compose.onAllNodesWithContentDescription("Move Up")[i]
    private fun down(i: Int) = compose.onAllNodesWithContentDescription("Move Down")[i]
    private fun viewOrderOf(uuid: String) = DataManagerObject.stores.first { it.uuid == uuid }.viewOrder

    @Test
    fun `shows title and non-deleted stores sorted by viewOrder with arrow states`() {
        installStores()
        show()
        compose.onNodeWithText("Stores Management").assertIsDisplayed()
        assertEquals(listOf("Aldi", "Lidl", "Costco"), rows())
        up(0).assertIsNotEnabled(); down(0).assertIsEnabled()
        up(1).assertIsEnabled(); down(1).assertIsEnabled()
        up(2).assertIsEnabled(); down(2).assertIsNotEnabled()
        assertEquals(3, compose.onAllNodesWithContentDescription("Delete Store").fetchSemanticsNodes().size)
        assertEquals(3, compose.onAllNodesWithContentDescription("Edit Store").fetchSemanticsNodes().size)
        compose.onNodeWithContentDescription("Cancel").assertIsEnabled()
        compose.onNodeWithContentDescription("Create New Store").assertIsEnabled()
    }

    @Test
    fun `empty state shows both texts`() {
        show()
        compose.onNodeWithText("No stores yet").assertIsDisplayed()
        compose.onNodeWithText("No stores available. Create your first store to get started.").assertIsDisplayed()
    }

    @Test
    fun `move down swaps viewOrder with the next store immediately`() {
        installStores()
        show()
        down(0).performClick()
        compose.waitForIdle()
        assertEquals(2, viewOrderOf("Aldi"))
        assertEquals(1, viewOrderOf("Lidl"))
        assertEquals(listOf("Lidl", "Aldi", "Costco"), rows())
    }

    @Test
    fun `move up swaps viewOrder with the previous store`() {
        installStores()
        show()
        up(2).performClick()
        compose.waitForIdle()
        assertEquals(2, viewOrderOf("Costco"))
        assertEquals(3, viewOrderOf("Lidl"))
        assertEquals(listOf("Aldi", "Costco", "Lidl"), rows())
    }

    @Test
    fun `edit dismisses first and then enters edit mode for that store`() {
        installStores()
        show()
        compose.onAllNodesWithContentDescription("Edit Store")[1].performClick()
        assertEquals(listOf("dismiss", "edit:Lidl"), events)
    }

    @Test
    fun `default edit mode callback does nothing`() {
        installStores()
        compose.setContent { StoresManagementDialog(onDismiss = { events += "dismiss" }) }
        compose.onAllNodesWithContentDescription("Edit Store")[0].performClick()
        assertEquals(listOf("dismiss"), events)
    }

    @Test
    fun `delete asks for confirmation and cancel keeps the store`() {
        installStores()
        show()
        compose.onAllNodesWithContentDescription("Delete Store")[0].performClick()
        compose.onNodeWithText("Delete Store").assertIsDisplayed()
        compose.onNodeWithText(
            "Are you sure you want to delete 'Aldi'? This will unlink all groceries from this store."
        ).assertIsDisplayed()
        compose.onAllNodesWithContentDescription("Cancel")[1].performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Delete Store").assertDoesNotExist()
        assertFalse(DataManagerObject.stores.first { it.uuid == "Aldi" }.deleted)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `confirming delete soft-deletes the store and closes the confirmation`() {
        installStores()
        DataManagerObject.hiddenStoreIds.add("Lidl")
        DataManagerObject.storeCategoryOrders["Lidl"] = listOf("c1")
        show()
        compose.onAllNodesWithContentDescription("Delete Store")[1].performClick()
        compose.onNodeWithContentDescription("Delete").assertIsEnabled().performClick()
        compose.waitForIdle()
        assertTrue(DataManagerObject.stores.first { it.uuid == "Lidl" }.deleted)
        assertFalse("Lidl" in DataManagerObject.hiddenStoreIds)
        assertFalse("Lidl" in DataManagerObject.storeCategoryOrders)
        compose.onNodeWithText("Delete Store").assertDoesNotExist()
        assertEquals(listOf("Aldi", "Costco"), rows())
        assertTrue(events.isEmpty())
    }

    @Test
    fun `cancel button dismisses`() {
        installStores()
        show()
        compose.onNodeWithContentDescription("Cancel").performClick()
        assertEquals(listOf("dismiss"), events)
    }

    @Test
    fun `create button opens the create dialog and a created store appears in the list`() {
        installStores()
        show()
        compose.onNodeWithContentDescription("Create New Store").performClick()
        compose.onNodeWithText("Create Store").assertIsDisplayed()
        compose.onNodeWithText("Store Name").performTextInput("Target")
        compose.onNodeWithContentDescription("Create Store").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Create Store").assertDoesNotExist()
        assertEquals(listOf("Aldi", "Lidl", "Costco", "Target"), rows())
        assertTrue(events.isEmpty())
    }

    @Test
    fun `cancelling the create dialog closes it only`() {
        show()
        compose.onNodeWithContentDescription("Create New Store").performClick()
        compose.onNodeWithText("Create Store").assertIsDisplayed()
        compose.onAllNodesWithContentDescription("Cancel")[1].performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Create Store").assertDoesNotExist()
        compose.onNodeWithText("Stores Management").assertIsDisplayed()
        assertTrue(events.isEmpty())
    }
}
