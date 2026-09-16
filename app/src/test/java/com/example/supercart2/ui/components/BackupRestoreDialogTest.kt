package com.example.supercart2.ui.components

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.supercart2.TestData
import com.example.supercart2.data.BackupManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w480dp-h2000dp")
class BackupRestoreDialogTest {

    @get:Rule
    val compose = createComposeRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private val events = mutableListOf<String>()

    @Before
    fun setUp() {
        TestData.reset()
        mockkObject(BackupManager)
    }

    @After
    fun tearDown() = unmockkAll()

    private fun file(name: String, bytes: Int, modified: Long = 1_700_000_000_000L): File =
        tmp.newFile(name).apply { writeBytes(ByteArray(bytes)); setLastModified(modified) }

    private fun show(files: List<File>) {
        every { BackupManager.listBackupFiles() } returns files
        compose.setContent {
            BackupRestoreDialog(
                onDismiss = { events += "dismiss" },
                onRestoreComplete = { ok, msg -> events += "complete:$ok:$msg" },
            )
        }
    }

    @Test
    fun `shows title and empty message when there are no backups`() {
        show(emptyList())
        compose.onNodeWithText("Select Backup File").assertIsDisplayed()
        compose.onNodeWithText("No backup files found in Downloads folder.").assertIsDisplayed()
    }

    @Test
    fun `lists backups with date and human readable size`() {
        val modified = 1_700_000_000_000L
        show(listOf(file("a.zip", 500), file("b.zip", 2048), file("c.zip", 3 * 1024 * 1024 + 512 * 1024)))
        compose.onNodeWithText("a.zip").assertIsDisplayed()
        compose.onNodeWithText("500 bytes").assertIsDisplayed()
        compose.onNodeWithText(String.format("%.2f KB", 2.0)).assertIsDisplayed()
        compose.onNodeWithText(String.format("%.2f MB", 3.5)).assertExists()
        val date = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(modified))
        compose.onAllNodesWithText(date).assertCountEquals(3)
        compose.onNodeWithText("No backup files found in Downloads folder.").assertDoesNotExist()
    }

    @Test
    fun `successful restore reports completion then dismisses`() {
        val f = file("a.zip", 10)
        coEvery { BackupManager.restoreFromBackup(any(), f) } returns true
        show(listOf(f))
        compose.onNodeWithText("a.zip").performClick()
        compose.waitForIdle()
        assertEquals(listOf("complete:true:Backup restored successfully!", "dismiss"), events)
    }

    @Test
    fun `failed restore shows an error above the list`() {
        val f = file("a.zip", 10)
        coEvery { BackupManager.restoreFromBackup(any(), f) } returns false
        show(listOf(f))
        compose.onNodeWithText("a.zip").performClick()
        compose.onNodeWithText("Failed to restore backup. Please check the file format.").assertIsDisplayed()
        compose.onNodeWithText("a.zip").assertIsDisplayed()
        assertTrue(events.isEmpty())
    }

    @Test
    fun `shows a progress indicator instead of the list while restoring`() {
        val f = file("a.zip", 10)
        val result = CompletableDeferred<Boolean>()
        coEvery { BackupManager.restoreFromBackup(any(), f) } coAnswers { result.await() }
        show(listOf(f))
        compose.onNodeWithText("a.zip").performClick()
        compose.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertExists()
        compose.onNodeWithText("a.zip").assertDoesNotExist()
        result.complete(false)
        compose.waitForIdle()
        compose.onNodeWithText("a.zip").assertIsDisplayed()
    }

    @Test
    fun `cancel dismisses`() {
        show(emptyList())
        compose.onNodeWithText("Cancel").performClick()
        assertEquals(listOf("dismiss"), events)
    }
}
