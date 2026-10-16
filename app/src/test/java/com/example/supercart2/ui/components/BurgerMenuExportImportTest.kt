package com.example.supercart2.ui.components

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.supercart2.TestData
import com.example.supercart2.data.BackupManager
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.data.FirebaseManager
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "h1200dp")
class BurgerMenuExportImportTest {

    @get:Rule
    val compose = createComposeRule()

    private val backupFile = File("supercart_backup_7.zip")

    @Before
    fun setUp() {
        TestData.reset()
        mockkStatic(FirebaseFirestore::class)
        every { FirebaseFirestore.getInstance() } returns mockk(relaxed = true)
        mockkObject(FirebaseManager, DataStoreManager, BackupManager)
        coEvery { DataStoreManager.hasGroupCode(any()) } returns false
        every { BackupManager.listBackupFiles() } returns listOf(backupFile)
        compose.setContent { BurgerMenu(onCategoriesManagementClick = {}, onImportGroceriesClick = {}) }
    }

    @After
    fun tearDown() = unmockkAll()

    private fun openPicker() {
        compose.onNodeWithContentDescription("Menu").performClick()
        compose.onNodeWithText("Export / Import Data").performClick()
    }

    private fun assertPickerClosed() = compose.onAllNodesWithText("Export to File").assertCountEquals(0)

    @Test
    fun `picker shows both options and closes the menu`() {
        openPicker()
        compose.onAllNodesWithText("Manage Stores").assertCountEquals(0)
        compose.onAllNodesWithText("Export / Import Data").assertCountEquals(1)
        listOf(
            "Export to File", "Save a backup to your Downloads folder",
            "Import from File", "Restore data from a backup file", "Cancel",
        ).forEach { compose.onNodeWithText(it).assertIsDisplayed() }
    }

    @Test
    fun `cancel closes the picker`() {
        openPicker()
        compose.onNodeWithText("Cancel").performClick()
        assertPickerClosed()
        coVerify(exactly = 0) { BackupManager.createBackup(any()) }
    }

    @Test
    fun `export shows progress then success dialog`() {
        val result = CompletableDeferred<File?>()
        coEvery { BackupManager.createBackup(any()) } coAnswers { result.await() }
        openPicker()
        compose.onNodeWithText("Export to File").performClick()
        assertPickerClosed()
        compose.onNodeWithText("Exporting Data").assertIsDisplayed()
        compose.onNodeWithText("Please wait while your data is being exported…").assertIsDisplayed()

        result.complete(backupFile)
        compose.onAllNodesWithText("Exporting Data").assertCountEquals(0)
        compose.onNodeWithText("Export Successful").assertIsDisplayed()
        compose.onNodeWithText("Your data has been saved to the Downloads folder.").assertIsDisplayed()
        compose.onNodeWithText("OK").performClick()
        compose.onAllNodesWithText("Export Successful").assertCountEquals(0)
        coVerify(exactly = 1) { BackupManager.createBackup(any()) }
    }

    @Test
    fun `export returning no file shows Unknown error`() {
        coEvery { BackupManager.createBackup(any()) } returns null
        openPicker()
        compose.onNodeWithText("Export to File").performClick()
        compose.onNodeWithText("Error").assertIsDisplayed()
        compose.onNodeWithText("Export failed: Unknown error").assertIsDisplayed()
        compose.onNodeWithText("OK").performClick()
        compose.onAllNodesWithText("Error").assertCountEquals(0)
    }

    @Test
    fun `export exception shows its message, or Unknown error when it has none`() {
        coEvery { BackupManager.createBackup(any()) } throws IllegalStateException("disk full")
        openPicker()
        compose.onNodeWithText("Export to File").performClick()
        compose.onAllNodesWithText("Exporting Data").assertCountEquals(0)
        compose.onNodeWithText("Export failed: disk full").assertIsDisplayed()
        compose.onNodeWithText("OK").performClick()

        coEvery { BackupManager.createBackup(any()) } throws RuntimeException()
        openPicker()
        compose.onNodeWithText("Export to File").performClick()
        compose.onNodeWithText("Export failed: Unknown error").assertIsDisplayed()
    }

    @Test
    fun `import opens the backup picker, cancel closes it`() {
        openPicker()
        compose.onNodeWithText("Import from File").performClick()
        assertPickerClosed()
        compose.onNodeWithText("Select Backup File").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.onAllNodesWithText("Select Backup File").assertCountEquals(0)
    }

    @Test
    fun `successful restore closes the backup picker`() {
        coEvery { BackupManager.restoreFromBackup(any(), any()) } returns true
        openPicker()
        compose.onNodeWithText("Import from File").performClick()
        compose.onNodeWithText("supercart_backup_7.zip").performClick()
        compose.onAllNodesWithText("Select Backup File").assertCountEquals(0)
        coVerify(exactly = 1) { BackupManager.restoreFromBackup(any(), backupFile) }
    }

    @Test
    fun `failed restore keeps the backup picker open with an error`() {
        coEvery { BackupManager.restoreFromBackup(any(), any()) } returns false
        openPicker()
        compose.onNodeWithText("Import from File").performClick()
        compose.onNodeWithText("supercart_backup_7.zip").performClick()
        compose.onNodeWithText("Failed to restore backup. Please check the file format.").assertIsDisplayed()
    }
}
