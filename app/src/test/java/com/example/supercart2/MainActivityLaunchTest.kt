package com.example.supercart2

import android.content.Context
import android.os.Looper
import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.utils.AppLanguage
import com.example.supercart2.utils.LanguageManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * MainActivity: applies the saved app language (locale + layout direction) and shows MainApp.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainActivityLaunchTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private lateinit var context: Context
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        TestData.reset()
        runBlocking { DataStoreManager.saveData(context) }
    }

    @After
    fun tearDown() {
        scenario?.onActivity { it.setContent { } } // dispose MainApp before closing
        compose.waitForIdle()
        scenario?.close()
        shadowOf(Looper.getMainLooper()).idle()
        runBlocking { LanguageManager.setLanguage(context, AppLanguage.ENGLISH) }
        LanguageManager.applyLanguage(context, AppLanguage.ENGLISH) // restores the JVM default locale
    }

    private fun launch(language: AppLanguage): MainActivity {
        runBlocking { LanguageManager.setLanguage(context, language) }
        lateinit var activity: MainActivity
        scenario = ActivityScenario.launch(MainActivity::class.java).onActivity { activity = it }
        // Let the start-up load finish so no state write is left pending for later tests
        compose.waitUntil(5_000) { DataManagerObject.categories.isNotEmpty() }
        compose.waitForIdle()
        return activity
    }

    private fun navItem(label: String) = compose.onNode(isSelectable() and hasText(label))

    @Test
    fun `Hebrew launch uses the Hebrew locale and right-to-left layout`() {
        val activity = launch(AppLanguage.HEBREW)
        // "iw" is reported as its modern code "he" by the JDK
        assertEquals("he", activity.resources.configuration.locales[0].language)
        val home = navItem(activity.getString(R.string.nav_home)).assertIsDisplayed().getBoundsInRoot()
        val stores = navItem(activity.getString(R.string.nav_stores)).assertIsDisplayed().getBoundsInRoot()
        assertTrue(home.left > stores.left)
    }
}
