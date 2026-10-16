package com.example.supercart2.ui.components

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.supercart2.TestData
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.data.FirebaseManager
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "h1200dp")
class BurgerMenuTest {

    @get:Rule
    val compose = createComposeRule()

    private val clicks = mutableListOf<String>()

    private val items = listOf(
        "Import Groceries", "Manage Categories", "Manage Stores", "Settings", "Group Sharing", "Sync data",
        "Export / Import Data",
    )

    @Before
    fun setUp() {
        TestData.reset()
        mockkStatic(FirebaseFirestore::class)
        every { FirebaseFirestore.getInstance() } returns mockk(relaxed = true)
        mockkObject(FirebaseManager, DataStoreManager)
        coEvery { DataStoreManager.hasGroupCode(any()) } returns false
        coEvery { FirebaseManager.uploadData() } returns Unit
        coEvery { FirebaseManager.downloadData() } returns Unit
    }

    @After
    fun tearDown() = unmockkAll()

    private fun show() = compose.setContent {
        BurgerMenu(
            onCategoriesManagementClick = { clicks += "categories" },
            onManageStoresClick = { clicks += "stores" },
            onManageGroupClick = { clicks += "group" },
            onImportGroceriesClick = { clicks += "import" },
            onSettingsClick = { clicks += "settings" },
        )
    }

    private fun openMenu() = compose.onNodeWithContentDescription("Menu").performClick()

    private fun top(text: String) = compose.onNodeWithText(text).getUnclippedBoundsInRoot().top

    private fun assertMenuClosed() = compose.onAllNodesWithText("Manage Stores").assertCountEquals(0)

    @Test
    fun `menu is closed until the menu button is clicked`() {
        show()
        compose.onNodeWithContentDescription("Menu").assertIsDisplayed()
        items.forEach { compose.onAllNodesWithText(it).assertCountEquals(0) }
        openMenu()
        items.forEach { compose.onNodeWithText(it).assertIsDisplayed() }
    }

    @Test
    fun `menu items are listed in fixed order`() {
        show()
        openMenu()
        val tops = items.map { top(it) }
        assertEquals(tops.sorted(), tops)
    }

    @Test
    fun `each navigation item invokes its callback once and closes the menu`() {
        show()
        val expected = listOf(
            "Import Groceries" to "import", "Manage Categories" to "categories", "Manage Stores" to "stores",
            "Settings" to "settings", "Group Sharing" to "group",
        )
        expected.forEach { (label, _) ->
            openMenu()
            compose.onNodeWithText(label).performClick()
            assertMenuClosed()
        }
        assertEquals(expected.map { it.second }, clicks)
    }

    @Test
    fun `optional callbacks default to no-ops`() {
        compose.setContent { BurgerMenu(onCategoriesManagementClick = {}, onImportGroceriesClick = {}) }
        listOf("Manage Stores", "Settings", "Group Sharing").forEach {
            openMenu()
            compose.onNodeWithText(it).performClick()
            assertMenuClosed()
        }
    }

    @Test
    fun `without a group code sync is disabled with a hint and clicking does nothing`() {
        show()
        openMenu()
        compose.onNodeWithText("Requires family group").assertIsDisplayed()
        compose.onNodeWithText("Sync data").assertIsNotEnabled().performClick()
        compose.onNodeWithText("Manage Stores").assertIsDisplayed()
        coVerify(exactly = 0) { FirebaseManager.uploadData() }
        assertNull(ShadowToast.getLatestToast())
    }

    @Test
    fun `with a group code sync is enabled without hint`() {
        coEvery { DataStoreManager.hasGroupCode(any()) } returns true
        show()
        openMenu()
        compose.onNodeWithText("Sync data").assertIsEnabled()
        compose.onAllNodesWithText("Requires family group").assertCountEquals(0)
    }

    @Test
    fun `group code is checked again every time the menu opens`() {
        show()
        openMenu()
        compose.onNodeWithText("Sync data").assertIsNotEnabled()
        compose.onNodeWithText("Settings").performClick()
        coEvery { DataStoreManager.hasGroupCode(any()) } returns true
        openMenu()
        compose.onNodeWithText("Sync data").assertIsEnabled()
        coVerify(exactly = 2) { DataStoreManager.hasGroupCode(any()) }
    }

    @Test
    fun `sync uploads then downloads, showing progress, then a success dialog`() {
        val upload = CompletableDeferred<Unit>()
        val download = CompletableDeferred<Unit>()
        coEvery { DataStoreManager.hasGroupCode(any()) } returns true
        coEvery { FirebaseManager.uploadData() } coAnswers { upload.await() }
        coEvery { FirebaseManager.downloadData() } coAnswers { download.await() }
        show()
        openMenu()
        compose.onNodeWithText("Sync data").performClick()
        assertMenuClosed()
        compose.onNodeWithText("Uploading to Cloud").assertIsDisplayed()
        compose.onNodeWithText("Please wait while your data is being uploaded...").assertIsDisplayed()
        coVerify(exactly = 0) { FirebaseManager.downloadData() }

        upload.complete(Unit)
        compose.onNodeWithText("Downloading from Cloud").assertIsDisplayed()
        compose.onNodeWithText("Please wait while your data is being downloaded...").assertIsDisplayed()
        compose.onAllNodesWithText("Uploading to Cloud").assertCountEquals(0)

        download.complete(Unit)
        compose.onAllNodesWithText("Downloading from Cloud").assertCountEquals(0)
        compose.onNodeWithText("Sync Successful").assertIsDisplayed()
        compose.onNodeWithText("Your data has been synced with the cloud.").assertIsDisplayed()
        coVerifyOrder { FirebaseManager.uploadData(); FirebaseManager.downloadData() }

        compose.onNodeWithText("OK").performClick()
        compose.onAllNodesWithText("Sync Successful").assertCountEquals(0)
    }

    @Test
    fun `failed upload shows the error dialog and skips the download`() {
        coEvery { DataStoreManager.hasGroupCode(any()) } returns true
        coEvery { FirebaseManager.uploadData() } throws IllegalStateException("boom")
        show()
        openMenu()
        compose.onNodeWithText("Sync data").performClick()
        compose.onNodeWithText("Error").assertIsDisplayed()
        compose.onNodeWithText("Sync failed: boom").assertIsDisplayed()
        compose.onAllNodesWithText("Uploading to Cloud").assertCountEquals(0)
        coVerify(exactly = 0) { FirebaseManager.downloadData() }
        compose.onNodeWithText("OK").performClick()
        compose.onAllNodesWithText("Error").assertCountEquals(0)
    }

    @Test
    fun `failed download without message shows Unknown error`() {
        coEvery { DataStoreManager.hasGroupCode(any()) } returns true
        coEvery { FirebaseManager.downloadData() } throws RuntimeException()
        show()
        openMenu()
        compose.onNodeWithText("Sync data").performClick()
        compose.onNodeWithText("Sync failed: Unknown error").assertIsDisplayed()
        compose.onAllNodesWithText("Sync Successful").assertCountEquals(0)
    }
}
