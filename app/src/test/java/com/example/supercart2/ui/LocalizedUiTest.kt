package com.example.supercart2.ui

import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.supercart2.TestData
import com.example.supercart2.data.BackupManager
import com.example.supercart2.data.SettingsManager
import com.example.supercart2.ui.components.JoinGroupDialog
import com.example.supercart2.ui.components.SettingsDialog
import com.example.supercart2.ui.screens.StoresScreen
import com.example.supercart2.ui.screens.StoresScreenTestFixtures.COSTCO
import com.example.supercart2.ui.screens.StoresScreenTestFixtures.dairy
import com.example.supercart2.ui.screens.StoresScreenTestFixtures.install
import com.example.supercart2.utils.AppLanguage
import com.example.supercart2.utils.LanguageManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Screens and dialogs rendered under a Hebrew, Russian or Bulgarian locale show translated texts. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "h1200dp")
class LocalizedUiTest {

    @get:Rule
    val compose = createComposeRule()

    @After
    fun tearDown() {
        unmockkAll()
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun mockSettings() {
        TestData.reset()
        mockkObject(SettingsManager, LanguageManager, BackupManager)
        coEvery { SettingsManager.getShowAlerts() } returns true
        coEvery { SettingsManager.getColorPalette() } returns "green"
        coEvery { LanguageManager.getCurrentLanguage(any()) } returns AppLanguage.HEBREW
        every { BackupManager.listBackupFiles() } returns emptyList()
    }

    @Test
    @Config(qualifiers = "iw-h1200dp")
    fun `settings dialog is in Hebrew`() {
        mockSettings()
        compose.setContent { SettingsDialog(onDismiss = {}) }
        listOf("הגדרות", "הצג התראות", "גיבוי ושחזור", "גיבוי", "שחזור").forEach {
            compose.onNodeWithText(it).assertIsDisplayed()
        }
        compose.onNodeWithContentDescription("סגור").assertIsDisplayed()
        compose.onNodeWithText("שחזור").performClick()
        compose.onNodeWithText("בחר קובץ גיבוי").assertIsDisplayed()
        compose.onNodeWithText("לא נמצאו קובצי גיבוי בתיקיית ההורדות.").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "ru-h1200dp")
    fun `stores landing screen is in Russian`() {
        install(dairy("Milk", COSTCO))
        compose.setContent { StoresScreen(editModeStoreId = null, onEditModeStoreConsumed = {}) }
        compose.onNodeWithText("Магазины").assertIsDisplayed()
        compose.onNodeWithText("Выбрать магазин").assertIsDisplayed()
        compose.onNodeWithText("Просмотр продуктов").assertIsDisplayed()
        compose.onNodeWithText("Выбрать магазин").performClick()
        compose.onNodeWithText("Товаров: 1").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "bg-h1200dp")
    fun `join group dialog warning is in Bulgarian`() {
        TestData.reset()
        compose.setContent { JoinGroupDialog(onDismiss = {}, onGroupJoined = {}) }
        compose.onNodeWithText("⚠️ Внимание: Вашите локални данни ще бъдат заменени с данните на групата.")
            .assertIsDisplayed()
    }
}
