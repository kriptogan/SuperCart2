package com.example.supercart2.ui.components

import android.content.Context
import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.supercart2.TestData
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.ImageManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Tapping the stored photo in [GroceryCreationDialog] opens it full screen. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "h1200dp")
class GroceryCreationDialogFullScreenImageTest {

    @get:Rule
    val compose = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() = TestData.installStandardTree()

    @After
    fun flushMainLooper() = shadowOf(Looper.getMainLooper()).idle()

    private fun showEditing(imageUUID: String?) {
        DataManagerObject.updateGrocery("g1") { it.copy(imageUUID = imageUUID) }
        val grocery = TestData.findGrocery("g1")!!
        compose.setContent { GroceryCreationDialog(onDismiss = {}, groceryToEdit = grocery) }
    }

    private fun openFullScreen() {
        ImageManager.getImageFile("img1", context).writeText("jpeg")
        showEditing("img1")
        compose.onNodeWithContentDescription("Grocery Image").performScrollTo().performClick()
    }

    @Test
    fun `tapping the stored photo opens it full screen instead of the image source picker`() {
        openFullScreen()
        compose.onNodeWithContentDescription("Full screen image").assertIsDisplayed()
        compose.onNodeWithContentDescription("Close").assertIsDisplayed()
        compose.onNodeWithText("Change Image").assertIsDisplayed()
        compose.onNodeWithText("Select Image Source").assertDoesNotExist()
    }

    @Test
    fun `close returns to the grocery dialog`() {
        openFullScreen()
        compose.onNodeWithContentDescription("Close").performClick()
        compose.onNodeWithContentDescription("Full screen image").assertDoesNotExist()
        compose.onNodeWithContentDescription("Grocery Image").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Save Changes").assertIsDisplayed()
    }

    @Test
    fun `change image closes the viewer and opens the image source picker`() {
        openFullScreen()
        compose.onNodeWithText("Change Image").performClick()
        compose.onNodeWithContentDescription("Full screen image").assertDoesNotExist()
        compose.onNodeWithText("Select Image Source").assertIsDisplayed()
    }

    @Test
    fun `tapping the placeholder still opens the image source picker`() {
        showEditing(null)
        compose.onNodeWithText("Add Image").performScrollTo().performClick()
        compose.onNodeWithText("Select Image Source").assertIsDisplayed()
        compose.onNodeWithContentDescription("Full screen image").assertDoesNotExist()
    }

    @Test
    fun `a missing photo file opens the image source picker, not the viewer`() {
        ImageManager.getImageFile("gone", context).delete()
        showEditing("gone")
        compose.onNodeWithText("Add Image").performScrollTo().performClick()
        compose.onNodeWithText("Select Image Source").assertIsDisplayed()
        compose.onNodeWithContentDescription("Full screen image").assertDoesNotExist()
        assertEquals("gone", TestData.findGrocery("g1")!!.imageUUID)
    }
}
