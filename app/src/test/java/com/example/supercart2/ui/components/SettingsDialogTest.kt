package com.example.supercart2.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.supercart2.TestData
import com.example.supercart2.data.BackupManager
import com.example.supercart2.data.SettingsManager
import com.example.supercart2.utils.AppLanguage
import com.example.supercart2.utils.LanguageManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "h1200dp")
class SettingsDialogTest {

    @get:Rule
    val compose = createComposeRule()

    private var dismissed = 0

    @Before
    fun setUp() {
        TestData.reset()
        mockkObject(SettingsManager, LanguageManager, BackupManager)
        coEvery { SettingsManager.getShowAlerts() } returns true
        coEvery { SettingsManager.getColorPalette() } returns "green"
        coEvery { SettingsManager.setShowAlerts(any()) } returns Unit
        coEvery { SettingsManager.setColorPalette(any()) } returns Unit
        coEvery { LanguageManager.getCurrentLanguage(any()) } returns AppLanguage.ENGLISH
        coEvery { LanguageManager.setLanguage(any(), any()) } returns Unit
        every { BackupManager.listBackupFiles() } returns listOf(File("supercart_backup_1.zip"))
    }

    @After
    fun tearDown() = unmockkAll()

    private fun show() = compose.setContent { SettingsDialog(onDismiss = { dismissed++ }) }

    private fun top(text: String) = compose.onNodeWithText(text).getUnclippedBoundsInRoot().top

    @Test
    fun `shows all sections in order with English texts`() {
        show()
        listOf(
            "Settings", "Show Alerts", "Language", "Color palette", "Backup & Restore", "Backup", "Restore",
        ).forEach { compose.onNodeWithText(it).assertIsDisplayed() }
        compose.onNodeWithText(
            "Display alert bell icon and highlight items with expiring dates or purchase reminders"
        ).assertIsDisplayed()
        compose.onNodeWithContentDescription("Create Backup").assertIsDisplayed()
        compose.onNodeWithContentDescription("Restore Backup").assertIsDisplayed()
        compose.onNodeWithContentDescription("Close").assertIsDisplayed()
        assertTrue(top("Show Alerts") < top("Language"))
        assertTrue(top("Language") < top("Color palette"))
        assertTrue(top("Color palette") < top("Backup & Restore"))
    }

    @Test
    fun `loads show-alerts, language and palette from the managers`() {
        coEvery { SettingsManager.getShowAlerts() } returns false
        coEvery { LanguageManager.getCurrentLanguage(any()) } returns AppLanguage.RUSSIAN
        coEvery { SettingsManager.getColorPalette() } returns "blue"
        show()
        compose.onNode(isToggleable()).assertIsOff()
        compose.onNodeWithText("Русский").assertIsDisplayed()
        compose.onNodeWithText("Blue").assertIsDisplayed()
    }

    @Test
    fun `unknown palette id is displayed as Green`() {
        coEvery { SettingsManager.getColorPalette() } returns "rainbow"
        show()
        compose.onNodeWithText("Green").assertIsDisplayed()
    }

    @Test
    fun `toggling the switch saves the new show-alerts value`() {
        show()
        compose.onNode(isToggleable()).assertIsOn().performClick()
        compose.onNode(isToggleable()).assertIsOff()
        coVerify(exactly = 1) { SettingsManager.setShowAlerts(false) }
        compose.onNode(isToggleable()).performClick()
        compose.onNode(isToggleable()).assertIsOn()
        coVerify(exactly = 1) { SettingsManager.setShowAlerts(true) }
    }

    @Test
    fun `palette menu lists the five palettes in order and saves the selected one`() {
        show()
        compose.onNodeWithText("Green").performClick()
        compose.onAllNodesWithText("Green").assertCountEquals(2)
        val tops = listOf("Orange", "Purple", "Blue", "Pink").map { top(it) }
        assertEquals(tops.sorted(), tops)
        compose.onNodeWithText("Orange").performClick()
        coVerify(exactly = 1) { SettingsManager.setColorPalette("orange") }
        compose.onAllNodesWithText("Orange").assertCountEquals(1)
        compose.onAllNodesWithText("Green").assertCountEquals(0)
    }

    @Test
    fun `language menu lists every language with display and native names`() {
        show()
        compose.onNodeWithText("English").performClick()
        listOf("Hebrew", "עברית", "Russian", "Русский", "Bulgarian", "Български").forEach {
            compose.onNodeWithText(it).assertIsDisplayed()
        }
        compose.onAllNodesWithText("English").assertCountEquals(2) // field + merged menu item
        assertTrue(top("Hebrew") < top("Russian"))
        assertTrue(top("Russian") < top("Bulgarian"))
    }

    @Test
    fun `selecting a language saves it through LanguageManager`() {
        show()
        compose.onNodeWithText("English").performClick()
        compose.onNodeWithText("Hebrew").performClick()
        compose.waitForIdle()
        coVerify(exactly = 1) { LanguageManager.setLanguage(any(), AppLanguage.HEBREW) }
    }

    class RecordingActivity : ComponentActivity() {
        var recreated = 0
        override fun recreate() { recreated++ }
    }

    @Test
    fun `selecting a language recreates the activity from the dialog's LocalContext`() {
        val host = Robolectric.buildActivity(RecordingActivity::class.java).setup().get()
        compose.setContent {
            CompositionLocalProvider(LocalContext provides host) { SettingsDialog(onDismiss = { dismissed++ }) }
        }
        compose.onNodeWithText("English").performClick()
        compose.onNodeWithText("Russian").performClick()
        compose.waitForIdle()
        coVerify(exactly = 1) { LanguageManager.setLanguage(host, AppLanguage.RUSSIAN) }
        assertEquals(1, host.recreated)
        compose.onNodeWithText("Русский").assertIsDisplayed()
        compose.onAllNodesWithText("Bulgarian").assertCountEquals(0)
    }

    @Test
    fun `backup button shows the created file name`() {
        coEvery { BackupManager.createBackup(any()) } returns File("dir/supercart_backup_42.zip")
        show()
        compose.onNodeWithText("Backup").performClick()
        compose.onNodeWithText("Backup created: supercart_backup_42.zip").assertIsDisplayed()
    }

    @Test
    fun `backup button shows failure message when no file was created`() {
        coEvery { BackupManager.createBackup(any()) } returns null
        show()
        compose.onNodeWithText("Backup").performClick()
        compose.onNodeWithText("Failed to create backup").assertIsDisplayed()
    }

    @Test
    fun `restore opens the backup picker and shows the restore result message`() {
        coEvery { BackupManager.restoreFromBackup(any(), any()) } returns true
        show()
        compose.onNodeWithText("Restore").performClick()
        compose.onNodeWithText("Select Backup File").assertIsDisplayed()
        compose.onNodeWithText("supercart_backup_1.zip").performClick()
        compose.onAllNodesWithText("Select Backup File").assertCountEquals(0)
        compose.onNodeWithText("Backup restored successfully!").assertIsDisplayed()
        assertEquals(0, dismissed)
    }

    @Test
    fun `backup exception shows the failure message`() {
        coEvery { BackupManager.createBackup(any()) } throws RuntimeException("disk full")
        show()
        compose.onNodeWithText("Backup").performClick()
        compose.onNodeWithText("Failed to create backup").assertIsDisplayed()
    }

    private fun backupState(): SettingsDialogState {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return SettingsDialogState(CoroutineScope(Dispatchers.Unconfined), context)
    }

    @Test
    fun `created backup is tracked as a success`() {
        coEvery { BackupManager.createBackup(any()) } returns File("dir/supercart_backup_42.zip")
        val state = backupState()
        state.createBackup()
        assertEquals(SettingsDialogStatus("Backup created: supercart_backup_42.zip", true), state.backupStatus)
    }

    @Test
    fun `failed or throwing backup is tracked as a failure`() {
        val state = backupState()
        coEvery { BackupManager.createBackup(any()) } returns null
        state.createBackup()
        assertEquals(SettingsDialogStatus("Failed to create backup", false), state.backupStatus)
        coEvery { BackupManager.createBackup(any()) } throws RuntimeException("disk full")
        state.createBackup()
        assertEquals(SettingsDialogStatus("Failed to create backup", false), state.backupStatus)
    }

    @Test
    fun `restore result is tracked with its success flag`() {
        val state = backupState()
        state.onRestoreComplete(true, "Backup restored successfully!")
        assertEquals(SettingsDialogStatus("Backup restored successfully!", true), state.restoreStatus)
        assertEquals(false, state.showRestoreDialog)
    }

    @Test
    fun `cancelling the backup picker closes it without a message`() {
        show()
        compose.onNodeWithText("Restore").performClick()
        compose.onNodeWithText("Cancel").performClick()
        compose.onAllNodesWithText("Select Backup File").assertCountEquals(0)
        compose.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun `backup and restore messages are both shown, backup first`() {
        coEvery { BackupManager.createBackup(any()) } returns null
        coEvery { BackupManager.restoreFromBackup(any(), any()) } returns true
        show()
        compose.onNodeWithText("Backup").performClick()
        compose.onNodeWithText("Restore").performClick()
        compose.onNodeWithText("supercart_backup_1.zip").performClick()
        assertTrue(top("Failed to create backup") < top("Backup restored successfully!"))
    }

    @Test
    fun `close button calls onDismiss`() {
        show()
        compose.onNodeWithContentDescription("Close").performClick()
        assertEquals(1, dismissed)
    }
}
