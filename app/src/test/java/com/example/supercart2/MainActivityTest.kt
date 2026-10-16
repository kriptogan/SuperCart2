package com.example.supercart2

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.ui.theme.SuperCart2Theme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** MainApp composition: bottom navigation, route switching and start-up data loading. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainActivityTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var context: Context
    private var shown by mutableStateOf(true)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        TestData.reset()
        runBlocking { DataStoreManager.saveData(context) } // empty storage → defaults on load
        compose.setContent { if (shown) SuperCart2Theme { MainApp() } }
    }

    // Dispose MainApp inside the test so nothing observing global state outlives it
    @After
    fun tearDown() {
        compose.waitUntil(5_000) { DataManagerObject.categories.isNotEmpty() }
        shown = false
        compose.waitForIdle()
    }

    private fun navItem(label: String) = compose.onNode(isSelectable() and hasText(label))

    @Test
    fun `shows the bottom navigation with Home selected`() {
        navItem("Home").assertIsDisplayed().assertIsSelected()
        navItem("Stores").assertIsDisplayed().assertIsNotSelected()
        assertTrue(navItem("Home").getBoundsInRoot().left < navItem("Stores").getBoundsInRoot().left) // LTR
    }

    @Test
    fun `clicking Stores and Home switches the selected route`() {
        navItem("Stores").performClick()
        navItem("Stores").assertIsSelected()
        navItem("Home").assertIsNotSelected()

        navItem("Home").performClick()
        navItem("Home").assertIsSelected()
    }

    @Test
    fun `clicking Meals selects it and shows the meals screen`() {
        navItem("Meals").performClick()
        navItem("Meals").assertIsSelected()
        navItem("Stores").assertIsNotSelected()
        compose.onNodeWithText("No meals yet").assertIsDisplayed()
    }

    @Test
    fun `start-up sets the global context and loads (default) data`() {
        compose.waitUntil(5_000) { DataManagerObject.categories.isNotEmpty() }
        compose.waitForIdle()
        assertSame(context, DataStoreManager.globalContext?.applicationContext)
        assertEquals(listOf("Other", "Vegetables", "Fruits"), DataManagerObject.categories.map { it.category.name })
    }
}
