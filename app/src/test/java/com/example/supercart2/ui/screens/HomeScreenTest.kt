package com.example.supercart2.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.supercart2.TestData
import com.example.supercart2.TestData.category
import com.example.supercart2.TestData.grocery
import com.example.supercart2.TestData.node
import com.example.supercart2.TestData.sub
import com.example.supercart2.TestData.subNode
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.SettingsManager
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "h1200dp")
class HomeScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val showAlerts = MutableStateFlow(true)

    @Before
    fun setUp() {
        TestData.installStandardTree()
        mockkObject(SettingsManager)
        every { SettingsManager.showAlerts } returns showAlerts
    }

    @After
    fun tearDown() = unmockkAll()

    private fun launch() = compose.setContent { HomeScreen() }

    /** Makes g3 (in c2/s3) expire today, so it is the only alert grocery. */
    private fun makeG3Alert() {
        DataManagerObject.updateGrocery("g3") { it.copy(expirationDate = LocalDate.now()) }
    }

    private fun assertShown(vararg texts: String) = texts.forEach { compose.onNodeWithText(it).assertIsDisplayed() }
    private fun assertAbsent(vararg texts: String) = texts.forEach { compose.onNodeWithText(it).assertDoesNotExist() }

    @Test
    fun `top bar shows menu, add button, search field and expand toggle, categories sorted by viewOrder`() {
        launch()
        compose.onNodeWithContentDescription("Menu").assertIsDisplayed()
        compose.onNodeWithContentDescription("Add New Grocery").assertIsDisplayed()
        compose.onNodeWithText("Search groceries...").assertIsDisplayed()
        compose.onNodeWithContentDescription("Search").assertIsDisplayed()
        compose.onNodeWithContentDescription("Expand All").assertIsDisplayed()
        compose.onNodeWithContentDescription("Alerts").assertDoesNotExist()
        assertShown("c1", "c2")
        assertAbsent("s1", "g1")
        val c2Top = compose.onNodeWithText("c2").getUnclippedBoundsInRoot().top
        val c1Top = compose.onNodeWithText("c1").getUnclippedBoundsInRoot().top
        assertTrue(c2Top < c1Top)
    }

    @Test
    fun `deleted categories are not displayed`() {
        DataManagerObject.categories.add(node(category("gone", deleted = true), subNode(sub("sx", "gone"))))
        launch()
        assertAbsent("gone")
    }

    @Test
    fun `expand toggle expands and collapses everything`() {
        launch()
        compose.onNodeWithContentDescription("Expand All").performClick()
        compose.onNodeWithContentDescription("Collapse All").assertIsDisplayed()
        assertShown("s1", "s3", "g1", "g2", "g3")
        compose.onNodeWithContentDescription("Collapse All").performClick()
        compose.onNodeWithContentDescription("Expand All").assertIsDisplayed()
        assertAbsent("s1", "g1")
    }

    @Test
    fun `search filters case-insensitively by grocery name, expands, and clear restores collapsed tree`() {
        DataManagerObject.categories.add(
            node(category("c3", viewOrder = 3), subNode(sub("s4", "c3"), grocery("G1b", "c3", "s4")))
        )
        launch()
        compose.onNodeWithText("Search groceries...").performTextInput("1")
        assertShown("c1", "c3", "g1", "G1b")
        assertAbsent("c2", "g2", "s2")
        compose.onNodeWithContentDescription("Collapse All").assertIsDisplayed()

        compose.onNodeWithContentDescription("Clear search for '1'").performClick()
        compose.onNodeWithText("Search groceries...").assertIsDisplayed()
        compose.onNodeWithContentDescription("Expand All").assertIsDisplayed()
        assertShown("c1", "c2", "c3")
        assertAbsent("g1")
    }

    @Test
    fun `search matches only grocery names, not category names, and shows nothing without matches`() {
        launch()
        compose.onNodeWithText("Search groceries...").performTextInput("c1")
        assertAbsent("c2", "g1")
        compose.onNodeWithText("c1").assertExists() // only the search field text itself
        compose.onNodeWithContentDescription("Clear search for 'c1'").assertIsDisplayed()
    }

    @Test
    fun `search is uppercase-insensitive`() {
        launch()
        compose.onNodeWithText("Search groceries...").performTextInput("G2")
        assertShown("c1", "g2")
        assertAbsent("c2", "g1")
    }

    @Test
    fun `alert button appears only with alert groceries and toggles the alert filter`() {
        makeG3Alert()
        launch()
        compose.onNodeWithContentDescription("Alerts").performClick()
        assertShown("c2", "g3")
        assertAbsent("c1")
        compose.onNodeWithContentDescription("Collapse All").assertIsDisplayed()

        compose.onNodeWithContentDescription("Alerts").performClick()
        assertShown("c1", "c2")
        // Pinned: expansion forced by the filter stays on after the filter is turned off
        compose.onNodeWithContentDescription("Collapse All").assertIsDisplayed()
    }

    @Test
    fun `pinned - collapse all does not stick while the alert filter is active`() {
        makeG3Alert()
        launch()
        compose.onNodeWithContentDescription("Alerts").performClick()
        compose.onNodeWithContentDescription("Collapse All").performClick()
        compose.onNodeWithContentDescription("Collapse All").assertIsDisplayed()
        assertShown("g3")
    }

    @Test
    fun `alert filter combines with search`() {
        makeG3Alert()
        launch()
        compose.onNodeWithContentDescription("Alerts").performClick()
        compose.onNodeWithText("Search groceries...").performTextInput("1")
        assertAbsent("c1", "c2", "g1")
    }

    @Test
    fun `no alert button when alerts are disabled in settings`() {
        makeG3Alert()
        showAlerts.value = false
        launch()
        compose.onNodeWithContentDescription("Alerts").assertDoesNotExist()
        assertShown("c1", "c2")
    }

    @Test
    fun `disabling alerts while the filter is active turns the filter off`() {
        makeG3Alert()
        launch()
        compose.onNodeWithContentDescription("Alerts").performClick()
        assertAbsent("c1")
        showAlerts.value = false
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Alerts").assertDoesNotExist()
        assertShown("c1", "c2")
        showAlerts.value = true
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Alerts").assertIsDisplayed()
        assertShown("c1", "c2")
    }

    @Test
    fun `filter turns off when the last alert disappears and does not re-apply on a new alert`() {
        makeG3Alert()
        launch()
        compose.onNodeWithContentDescription("Alerts").performClick()
        DataManagerObject.updateGrocery("g3") { it.copy(expirationDate = null) }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Alerts").assertDoesNotExist()
        assertShown("c1", "c2")
        compose.onNodeWithContentDescription("Collapse All").performClick()
        compose.onNodeWithContentDescription("Expand All").assertIsDisplayed()

        DataManagerObject.updateGrocery("g1") { it.copy(expirationDate = LocalDate.now()) }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Alerts").assertIsDisplayed()
        assertShown("c1", "c2")
        assertAbsent("g1")
    }
}
