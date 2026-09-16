package com.example.supercart2.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.example.supercart2.TestData
import com.example.supercart2.TestData.store
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.SettingsManager
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "h1200dp")
class HomeScreenDialogsTest {

    @get:Rule
    val compose = createComposeRule()

    private val navigated = mutableListOf<String>()

    @Before
    fun setUp() {
        TestData.installStandardTree()
        mockkObject(SettingsManager)
        every { SettingsManager.showAlerts } returns MutableStateFlow(true)
        compose.setContent { HomeScreen(onNavigateToStoreEdit = { navigated += it }) }
    }

    @After
    fun tearDown() = unmockkAll()

    private fun openMenuItem(label: String) {
        compose.onNodeWithContentDescription("Menu").performClick()
        compose.onNodeWithText(label).performClick()
    }

    private fun groceriesIn(subCategoryId: String) = DataManagerObject.categories
        .flatMap { it.subCategories }.single { it.subCategory.uuid == subCategoryId }.groceries

    @Test
    fun `manage categories opens the categories dialog, cancel closes it`() {
        openMenuItem("Manage Categories")
        compose.onNodeWithText("Categories Management").assertIsDisplayed()
        compose.onNodeWithContentDescription("Cancel").performClick()
        compose.onNodeWithText("Categories Management").assertDoesNotExist()
    }

    @Test
    fun `manage stores opens the stores dialog, editing a store closes it and navigates`() {
        DataManagerObject.stores.add(store("st1", viewOrder = 1))
        openMenuItem("Manage Stores")
        compose.onNodeWithText("Stores Management").assertIsDisplayed()
        compose.onNodeWithContentDescription("Edit Store").performClick()
        compose.onNodeWithText("Stores Management").assertDoesNotExist()
        assertEquals(listOf("st1"), navigated)
    }

    @Test
    fun `stores dialog cancel closes without navigating`() {
        openMenuItem("Manage Stores")
        compose.onNodeWithContentDescription("Cancel").performClick()
        compose.onNodeWithText("Stores Management").assertDoesNotExist()
        assertEquals(emptyList<String>(), navigated)
    }

    @Test
    fun `group sharing opens the family group dialog`() {
        openMenuItem("Group Sharing")
        compose.onNodeWithText("Family Group").assertIsDisplayed()
    }

    @Test
    fun `settings opens the settings dialog`() {
        openMenuItem("Settings")
        compose.onNodeWithText("Show Alerts").assertIsDisplayed()
    }

    @Test
    fun `import groceries opens the import dialog`() {
        openMenuItem("Import Groceries")
        compose.onNodeWithText("Import Groceries").assertIsDisplayed()
    }

    @Test
    fun `add button opens create dialog prefilled with the search query, cancel closes it`() {
        compose.onNodeWithText("Search groceries...").performTextInput("milk")
        compose.onNodeWithContentDescription("Add New Grocery").performClick()
        compose.onNodeWithText("Create New Grocery").assertIsDisplayed()
        compose.onNodeWithText("Grocery Name").assertTextContains("milk")
        compose.onNodeWithContentDescription("Cancel").performClick()
        compose.onNodeWithText("Create New Grocery").assertDoesNotExist()
    }

    @Test
    fun `creating a grocery adds it exactly once (the dialog never calls onGroceryCreated)`() {
        compose.onNodeWithContentDescription("Add New Grocery").performClick()
        compose.onNodeWithText("Grocery Name").performTextInput("Bread")
        compose.onNodeWithContentDescription("Create Grocery").performClick()
        compose.onNodeWithText("Create New Grocery").assertDoesNotExist()
        // first sorted category is c2 (viewOrder 1) with sub-category s3
        assertEquals(listOf("g3", "Bread"), groceriesIn("s3").map { it.name })
    }

    @Test
    fun `edit item opens the edit dialog, add afterwards opens a fresh create dialog`() {
        compose.onNodeWithContentDescription("Expand All").performClick()
        compose.onAllNodesWithContentDescription("More options")[0].performClick() // g3, c2 is first
        compose.onNodeWithText("Edit item").performClick()
        compose.onNodeWithText("Edit Grocery").assertIsDisplayed()
        compose.onNodeWithText("Grocery Name").assertTextContains("g3")
        compose.onNodeWithContentDescription("Cancel").performClick()
        compose.onNodeWithText("Edit Grocery").assertDoesNotExist()

        compose.onNodeWithContentDescription("Add New Grocery").performClick()
        compose.onNodeWithText("Create New Grocery").assertIsDisplayed()
    }

    @Test
    fun `saving an edit updates the grocery once in place and closes the dialog`() {
        compose.onNodeWithContentDescription("Expand All").performClick()
        compose.onAllNodesWithContentDescription("More options")[0].performClick()
        compose.onNodeWithText("Edit item").performClick()
        compose.onNodeWithText("Grocery Name").performTextClearance()
        compose.onNodeWithText("Grocery Name").performTextInput("g3x")
        compose.onNodeWithContentDescription("Save Changes").performClick()
        compose.onNodeWithText("Edit Grocery").assertDoesNotExist()
        assertEquals(listOf("g3x"), groceriesIn("s3").map { it.name })
        assertNotNull(TestData.findGrocery("g3"))
    }
}
