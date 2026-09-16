package com.example.supercart2.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.supercart2.TestData
import com.example.supercart2.TestData.store
import com.example.supercart2.data.DataManagerObject
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
class StoreSelectionDialogTest {

    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()

    @Before
    fun setUp() {
        TestData.reset()
    }

    private fun show(selected: List<String> = emptyList()) {
        compose.setContent {
            StoreSelectionDialog(
                selectedStoreIds = selected,
                onDismiss = { events += "dismiss" },
                onStoresSelected = { events += "selected:$it" },
            )
        }
    }

    private fun installStores() {
        DataManagerObject.stores.addAll(
            listOf(store("Beta", viewOrder = 2), store("Alpha", viewOrder = 1), store("Gone", deleted = true))
        )
    }

    private fun checkbox(index: Int) = compose.onAllNodes(isToggleable())[index]
    private fun close() = compose.onNodeWithContentDescription("Close").performClick()

    @Test
    fun `no stores shows create hint and both buttons`() {
        show()
        compose.onNodeWithText("Select Stores").assertIsDisplayed()
        compose.onNodeWithText("No stores available. Create your first store to get started.").assertIsDisplayed()
        compose.onNodeWithContentDescription("Close").assertIsDisplayed()
        compose.onNodeWithContentDescription("Create Store").assertIsDisplayed()
    }

    @Test
    fun `lists non-deleted stores by viewOrder with initial selection checked`() {
        installStores()
        show(listOf("Beta"))
        compose.onNodeWithText("Gone").assertDoesNotExist()
        val alphaTop = compose.onNodeWithText("Alpha").fetchSemanticsNode().boundsInRoot.top
        val betaTop = compose.onNodeWithText("Beta").fetchSemanticsNode().boundsInRoot.top
        assertTrue(alphaTop < betaTop)
        checkbox(0).assertIsOff()
        checkbox(1).assertIsOn()
    }

    @Test
    fun `close reports only existing non-deleted selected stores in display order then dismisses`() {
        installStores()
        show(listOf("unknown", "Beta", "Gone"))
        compose.onNodeWithText("Alpha").performClick()
        close()
        assertEquals(listOf("selected:[Alpha, Beta]", "dismiss"), events)
    }

    @Test
    fun `row and checkbox toggle selection`() {
        installStores()
        show(listOf("Beta"))
        compose.onNodeWithText("Beta").performClick()
        checkbox(1).assertIsOff()
        checkbox(0).performClick()
        checkbox(0).assertIsOn()
        checkbox(0).performClick()
        checkbox(0).assertIsOff()
        compose.onNodeWithText("Alpha").performClick()
        close()
        assertEquals(listOf("selected:[Alpha]", "dismiss"), events)
    }

    @Test
    fun `create store opens create dialog and hides the list, cancel returns to the list`() {
        installStores()
        show()
        compose.onNodeWithContentDescription("Create Store").performClick()
        compose.onNodeWithText("Store Name").assertIsDisplayed()
        compose.onNodeWithText("Alpha").assertDoesNotExist()
        compose.onNodeWithText("Select Stores").assertIsDisplayed()

        compose.onNodeWithContentDescription("Cancel").performClick()
        compose.onNodeWithText("Store Name").assertDoesNotExist()
        compose.onNodeWithText("Alpha").assertIsDisplayed()
        assertTrue(events.isEmpty())
    }

    @Test
    fun `created store is added, auto-selected and shown in the list`() {
        show(listOf("Beta"))
        compose.onNodeWithContentDescription("Create Store").performClick()
        compose.onNodeWithText("Store Name").performTextInput("Costco")
        compose.onNode(hasContentDescription("Create Store") and hasAnyAncestor(hasAnyDescendant(hasText("Store Name"))))
            .performClick()

        compose.onNodeWithText("Store Name").assertDoesNotExist()
        compose.onNodeWithText("Costco").assertIsDisplayed()
        checkbox(0).assertIsOn()
        val created = DataManagerObject.stores.single()
        close()
        assertEquals(listOf("selected:[${created.uuid}]", "dismiss"), events)
    }
}
