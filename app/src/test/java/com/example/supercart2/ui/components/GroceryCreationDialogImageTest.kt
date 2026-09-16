package com.example.supercart2.ui.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.app.ActivityOptionsCompat
import androidx.test.core.app.ApplicationProvider
import com.example.supercart2.TestData
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.ImageManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** Image card of [GroceryCreationDialog]: shows, removes and replaces the grocery photo. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GroceryCreationDialogImageTest {

    @get:Rule
    val compose = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val picked = Uri.parse("content://test/picked.jpg")
    private var dismissed = 0

    /** Registry that answers every launched activity (the gallery picker) with [picked]. */
    private val registryOwner = object : ActivityResultRegistryOwner {
        override val activityResultRegistry = object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(
                requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?,
            ) {
                dispatchResult(requestCode, picked)
            }
        }
    }

    @Before
    fun setUp() {
        TestData.installStandardTree()
        mockkObject(ImageManager)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun imageFile(uuid: String): File = ImageManager.getImageFile(uuid, context)

    private fun showEditing(imageUUID: String?) {
        DataManagerObject.updateGrocery("g1") { it.copy(imageUUID = imageUUID) }
        val grocery = TestData.findGrocery("g1")!!
        compose.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides registryOwner) {
                GroceryCreationDialog(onDismiss = { dismissed++ }, onGroceryCreated = {}, groceryToEdit = grocery)
            }
        }
    }

    private fun save() = compose.onNodeWithContentDescription("Save Changes").performClick()
    private fun savedImage(): String? = TestData.findGrocery("g1")!!.imageUUID

    private fun pickFromGallery(placeholderText: String = "Add Image") {
        compose.onNodeWithText(placeholderText).performScrollTo().performClick()
        compose.onNodeWithText("Select Image Source").assertIsDisplayed()
        compose.onNodeWithText("Choose from Gallery").performClick()
    }

    @Test
    fun `existing local image is shown with a remove button`() {
        imageFile("img1").writeText("jpeg")
        showEditing("img1")
        compose.onNodeWithContentDescription("Grocery Image").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Remove Image").assertIsDisplayed()
        compose.onNodeWithText("Add Image").assertDoesNotExist()
    }

    @Test
    fun `removing the image deletes the local file and saves no image`() {
        imageFile("img1").writeText("jpeg")
        showEditing("img1")
        compose.onNodeWithContentDescription("Remove Image").performScrollTo().performClick()

        assertFalse(imageFile("img1").exists())
        compose.onNodeWithText("Add Image").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Tap to upload").assertIsDisplayed()
        save()
        assertEquals(null, savedImage())
    }

    @Test
    fun `missing local file shows the placeholder but keeps the image uuid`() {
        imageFile("gone").delete()
        showEditing("gone")
        compose.onNodeWithText("Add Image").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Remove Image").assertDoesNotExist()
        save()
        assertEquals("gone", savedImage())
    }

    @Test
    fun `picking an image compresses it under a fresh uuid, deletes the old file and saves the new uuid`() {
        // The old file is not created: on Windows Coil may still hold it open, making File.delete() racy.
        imageFile("old").delete()
        coEvery { ImageManager.compressAndSaveImage(picked, any(), any()) } answers {
            imageFile(secondArg<String>()).apply { writeText("new") }
        }
        showEditing("old")
        pickFromGallery()

        compose.onNodeWithText("Select Image Source").assertDoesNotExist()
        compose.onNodeWithContentDescription("Grocery Image").performScrollTo().assertIsDisplayed()
        coVerifyOrder {
            ImageManager.compressAndSaveImage(picked, any(), any())
            ImageManager.deleteLocalImage("old", any())
        }
        save()
        val saved = savedImage()!!
        assertNotEquals("old", saved)
        assertTrue(imageFile(saved).exists())
        assertEquals(1, dismissed)
    }

    @Test
    fun `failed compression keeps the old file and the old uuid`() {
        imageFile("img2").writeText("jpeg")
        coEvery { ImageManager.compressAndSaveImage(any(), any(), any()) } returns null
        showEditing("img2")
        compose.onNodeWithContentDescription("Grocery Image").performScrollTo().performClick()
        compose.onNodeWithText("Choose from Gallery").performClick()

        compose.waitForIdle()
        coVerify(exactly = 0) { ImageManager.deleteLocalImage(any(), any()) }
        assertTrue(imageFile("img2").exists())
        compose.onNodeWithContentDescription("Grocery Image").performScrollTo().assertIsDisplayed()
        save()
        assertEquals("img2", savedImage())
    }

    @Test
    fun `compression error keeps the old file and the old uuid`() {
        imageFile("img3").writeText("jpeg")
        coEvery { ImageManager.compressAndSaveImage(any(), any(), any()) } throws IllegalStateException("boom")
        showEditing("img3")
        compose.onNodeWithContentDescription("Grocery Image").performScrollTo().performClick()
        compose.onNodeWithText("Choose from Gallery").performClick()

        compose.waitForIdle()
        coVerify(exactly = 0) { ImageManager.deleteLocalImage(any(), any()) }
        assertTrue(imageFile("img3").exists())
        save()
        assertEquals("img3", savedImage())
    }

    @Test
    fun `compression errors are swallowed`() {
        coEvery { ImageManager.compressAndSaveImage(any(), any(), any()) } throws IllegalStateException("boom")
        showEditing(null)
        pickFromGallery()
        compose.onNodeWithText("Add Image").performScrollTo().assertIsDisplayed()
        save()
        assertEquals(null, savedImage())
    }

    @Test
    fun `a progress text is shown while the image is processed`() {
        val gate = CompletableDeferred<File?>()
        coEvery { ImageManager.compressAndSaveImage(any(), any(), any()) } coAnswers { gate.await() }
        showEditing(null)
        pickFromGallery()
        compose.onNodeWithText("Processing image...").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Add Image").assertDoesNotExist()

        gate.complete(null)
        compose.waitForIdle()
        compose.onNodeWithText("Processing image...").assertDoesNotExist()
        compose.onNodeWithText("Add Image").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `new grocery gets the picked image`() {
        coEvery { ImageManager.compressAndSaveImage(picked, any(), any()) } answers {
            imageFile(secondArg<String>()).apply { writeText("new") }
        }
        compose.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides registryOwner) {
                GroceryCreationDialog(onDismiss = {}, onGroceryCreated = {}, initialGroceryName = "Milk")
            }
        }
        pickFromGallery()
        compose.onNodeWithContentDescription("Create Grocery").performClick()
        val added = DataManagerObject.categories.flatMap { it.subCategories }.flatMap { it.groceries }
            .single { it.name == "Milk" }
        assertTrue(imageFile(added.imageUUID!!).exists())
    }
}
