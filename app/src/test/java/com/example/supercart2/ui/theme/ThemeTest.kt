package com.example.supercart2.ui.theme

import android.os.Looper
import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.example.supercart2.data.SettingsManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ThemeTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var context: Context
    private var primary: Color? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        SettingsManager.setGlobalContext(context)
        runBlocking { SettingsManager.setColorPalette("green") }
        SuperCartColors.updatePalette(AppPalettes.GREEN)
    }

    @After
    fun tearDown() {
        runBlocking { SettingsManager.setColorPalette("green") }
        SuperCartColors.updatePalette(AppPalettes.GREEN)
        shadowOf(Looper.getMainLooper()).idle() // deliver pending snapshot notifications
    }

    private fun render(darkTheme: Boolean = false, dynamicColor: Boolean = false) = compose.setContent {
        SuperCart2Theme(darkTheme = darkTheme, dynamicColor = dynamicColor) {
            primary = MaterialTheme.colorScheme.primary
            Text("content")
        }
    }

    @Test
    fun `renders content with the light scheme by default`() {
        render()
        compose.onNodeWithText("content").assertIsDisplayed()
        assertEquals(Purple40, primary)
    }

    @Test
    fun `dark theme uses the dark scheme`() {
        render(darkTheme = true)
        compose.waitForIdle()
        assertEquals(Purple80, primary)
    }

    @Test
    fun `dynamic color on API 31+ uses the dynamic scheme`() {
        render(dynamicColor = true)
        compose.waitForIdle()
        assertNotEquals(Purple40, primary)
        assertNotEquals(Purple80, primary)
    }

    @Test
    fun `applies the saved color palette and follows later changes`() {
        runBlocking { SettingsManager.setColorPalette("purple") }
        render()
        compose.waitUntil(5_000) { SuperCartColors.primaryGreen == AppPalettes.PURPLE.primary }

        runBlocking { SettingsManager.setColorPalette("blue") }
        compose.waitUntil(5_000) { SuperCartColors.primaryGreen == AppPalettes.BLUE.primary }
        assertEquals(AppPalettes.BLUE.lightPrimary, SuperCartColors.lightGreen)
    }
}
