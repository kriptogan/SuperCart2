package com.example.supercart2.data

import android.os.Looper
import android.content.Context
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import com.example.supercart2.TestData
import com.example.supercart2.utils.AppLanguage
import com.example.supercart2.utils.LanguageManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.UUID

/** GroupManager, SettingsManager and LanguageManager (all thin DataStore wrappers). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PreferencesManagersTest {

    private lateinit var context: Context

    // Run Compose snapshot notifications still queued on the main looper before Robolectric discards them;
    // a lost one leaves later Compose tests unable to go idle (AppNotIdleException).
    @After
    fun flushMainLooper() = shadowOf(Looper.getMainLooper()).idle()

    @Before
    fun setUp() = runTest {
        context = ApplicationProvider.getApplicationContext()
        TestData.reset()
        DataStoreManager.clearGroupCode(context)
    }

    private fun setSettingsContext(value: Context?) {
        SettingsManager::class.java.getDeclaredField("globalContext").apply { isAccessible = true }.set(SettingsManager, value)
    }

    // ---------- GroupManager ----------

    @Test
    fun `createGroup stores a fresh uuid as the group code`() = runTest {
        assertFalse(GroupManager.isInGroup(context))
        val code = GroupManager.createGroup(context)
        UUID.fromString(code) // valid format
        assertEquals(code, GroupManager.getCurrentGroupCode(context))
        assertTrue(GroupManager.isInGroup(context))
    }

    @Test
    fun `leaveGroup clears the code but keeps local data`() = runTest {
        TestData.installStandardTree()
        GroupManager.createGroup(context)
        GroupManager.leaveGroup(context)
        assertNull(GroupManager.getCurrentGroupCode(context))
        assertEquals(2, DataManagerObject.categories.size)
    }

    @Test
    fun `joinGroup rejects malformed codes before contacting Firebase`() = runTest {
        assertFalse(GroupManager.joinGroup(context, "not-a-uuid"))
        assertFalse(GroupManager.joinGroup(context, ""))
        assertNull(GroupManager.getCurrentGroupCode(context))
    }

    @Test
    fun `generateGroupCode returns distinct uuids`() {
        val a = GroupManager.generateGroupCode()
        assertEquals(a, UUID.fromString(a).toString())
        assertTrue(a != GroupManager.generateGroupCode())
    }

    // ---------- SettingsManager ----------

    @Test
    fun `settings defaults without a context`() = runTest {
        setSettingsContext(null)
        assertTrue(SettingsManager.getShowAlerts())
        assertEquals("green", SettingsManager.getColorPalette())
        assertEquals("green", SettingsManager.colorPalette.first())
    }

    @Test(expected = IllegalStateException::class)
    fun `showAlerts flow requires a context`() {
        setSettingsContext(null)
        SettingsManager.showAlerts
    }

    @Test
    fun `settings persist and palette ids are lower-cased`() = runTest {
        SettingsManager.setGlobalContext(context)
        SettingsManager.setShowAlerts(false)
        assertFalse(SettingsManager.getShowAlerts())
        assertFalse(SettingsManager.showAlerts.first())
        SettingsManager.setShowAlerts(true)
        assertTrue(SettingsManager.showAlerts.first())

        SettingsManager.setColorPalette("Purple")
        assertEquals("purple", SettingsManager.getColorPalette())
        assertEquals("purple", SettingsManager.colorPalette.first())
        SettingsManager.setColorPalette("green")
    }

    // ---------- LanguageManager ----------

    @Test
    fun `AppLanguage fromCode falls back to English`() {
        assertEquals(AppLanguage.HEBREW, AppLanguage.fromCode("iw"))
        assertEquals(AppLanguage.BULGARIAN, AppLanguage.fromCode("bg"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("he"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode(null))
    }

    @Test
    fun `only Hebrew is RTL`() {
        AppLanguage.values().forEach {
            val expected = if (it == AppLanguage.HEBREW) LayoutDirection.Rtl else LayoutDirection.Ltr
            assertEquals(expected, it.toLayoutDirection())
            assertEquals(expected, LanguageManager.getLayoutDirection(it))
        }
    }

    @Test
    fun `language preference persists and applies locale`() = runTest {
        LanguageManager.setLanguage(context, AppLanguage.RUSSIAN)
        assertEquals(AppLanguage.RUSSIAN, LanguageManager.getCurrentLanguage(context))

        val localized = LanguageManager.applyLanguage(context, AppLanguage.RUSSIAN)
        assertEquals("ru", localized.resources.configuration.locales[0].language)

        LanguageManager.setLanguage(context, AppLanguage.ENGLISH)
        assertEquals(AppLanguage.ENGLISH, LanguageManager.getCurrentLanguage(context))
    }

    @Test
    fun `Hebrew applyLanguage sets RTL layout direction and the default locale`() {
        val localized = LanguageManager.applyLanguage(context, AppLanguage.HEBREW)
        assertEquals(android.view.View.LAYOUT_DIRECTION_RTL, localized.resources.configuration.layoutDirection)
        assertEquals(java.util.Locale("iw"), java.util.Locale.getDefault())
        LanguageManager.applyLanguage(context, AppLanguage.ENGLISH)
        assertEquals(android.view.View.LAYOUT_DIRECTION_LTR, context.resources.configuration.layoutDirection)
    }
}
