package com.example.supercart2.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
class HideStoresDialogTest {

    @get:Rule
    val compose = createComposeRule()

    private var dismissed = 0

    @Before
    fun setUp() {
        TestData.reset()
    }

    private fun show() {
        compose.setContent { HideStoresDialog(onDismiss = { dismissed++ }) }
    }

    private fun installStores() {
        DataManagerObject.stores.addAll(
            listOf(store("Beta", viewOrder = 2), store("Alpha", viewOrder = 1), store("Gone", deleted = true))
        )
        DataManagerObject.hiddenStoreIds.add("Beta")
    }

    private fun checkbox(index: Int) = compose.onAllNodes(isToggleable())[index]

    @Test
    fun `no stores shows empty message and Done dismisses`() {
        show()
        compose.onNodeWithText("Show/Hide Stores").assertIsDisplayed()
        compose.onNodeWithText("No stores available.").assertIsDisplayed()
        compose.onNodeWithText("Unchecked stores will be hidden from the store view.").assertDoesNotExist()
        compose.onNodeWithText("Done").performClick()
        assertEquals(1, dismissed)
    }

    @Test
    fun `lists non-deleted stores by viewOrder, checked when visible`() {
        installStores()
        show()
        compose.onNodeWithText("Unchecked stores will be hidden from the store view.").assertIsDisplayed()
        compose.onNodeWithText("Gone").assertDoesNotExist()
        val alphaTop = compose.onNodeWithText("Alpha").fetchSemanticsNode().boundsInRoot.top
        val betaTop = compose.onNodeWithText("Beta").fetchSemanticsNode().boundsInRoot.top
        assertTrue(alphaTop < betaTop)
        checkbox(0).assertIsOn()
        checkbox(1).assertIsOff()
    }

    @Test
    fun `clicking a row toggles its hidden flag`() {
        installStores()
        show()
        compose.onNodeWithText("Alpha").performClick()
        compose.waitForIdle()
        assertEquals(listOf("Beta", "Alpha"), DataManagerObject.hiddenStoreIds.toList())
        checkbox(0).assertIsOff()

        compose.onNodeWithText("Beta").performClick()
        compose.waitForIdle()
        assertEquals(listOf("Alpha"), DataManagerObject.hiddenStoreIds.toList())
        checkbox(1).assertIsOn()
    }

    @Test
    fun `checkbox hides a visible store and shows a hidden one`() {
        installStores()
        show()
        checkbox(0).performClick()
        compose.waitForIdle()
        assertEquals(listOf("Beta", "Alpha"), DataManagerObject.hiddenStoreIds.toList())
        checkbox(0).assertIsOff()

        checkbox(1).performClick()
        compose.waitForIdle()
        assertEquals(listOf("Alpha"), DataManagerObject.hiddenStoreIds.toList())
        checkbox(1).assertIsOn()
    }

    @Test
    fun `done dismisses without changing hidden stores`() {
        installStores()
        show()
        compose.onNodeWithText("Done").performClick()
        assertEquals(1, dismissed)
        assertEquals(listOf("Beta"), DataManagerObject.hiddenStoreIds.toList())
    }
}
