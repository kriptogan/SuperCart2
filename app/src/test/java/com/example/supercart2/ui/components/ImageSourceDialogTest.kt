package com.example.supercart2.ui.components

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.content.FileProvider
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ImageSourceDialogTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val events = mutableListOf<String>()
    private val hint = "Camera permission required to take photos"

    private fun show(cameraGranted: Boolean) {
        val app = shadowOf(compose.activity.application)
        if (cameraGranted) app.grantPermissions(Manifest.permission.CAMERA) else app.denyPermissions(Manifest.permission.CAMERA)
        compose.setContent {
            ImageSourceDialog(onDismiss = { events += "dismiss" }, onImageSelected = { events += "image:$it" })
        }
    }

    private fun lastStarted() = shadowOf(compose.activity).nextStartedActivityForResult

    private fun deliver(started: org.robolectric.shadows.ShadowActivity.IntentForResult, code: Int, data: Intent?) {
        shadowOf(compose.activity).receiveResult(started.intent, code, data)
        compose.waitForIdle()
    }

    @Test
    fun `shows both sources, cancel dismisses`() {
        show(cameraGranted = false)
        compose.onNodeWithText("Select Image Source").assertIsDisplayed()
        compose.onNodeWithText("Choose from Gallery").assertIsDisplayed()
        compose.onNodeWithText("Take Photo").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        assertEquals(listOf("dismiss"), events)
    }

    @Test
    fun `permission hint shown only while camera permission is missing`() {
        show(cameraGranted = false)
        compose.onNodeWithText(hint).assertIsDisplayed()
    }

    @Test
    fun `no permission hint when camera permission is granted`() {
        show(cameraGranted = true)
        compose.onNodeWithText(hint).assertDoesNotExist()
    }

    @Test
    fun `gallery launches image picker and reports the picked uri then dismisses`() {
        show(cameraGranted = false)
        compose.onNodeWithText("Choose from Gallery").performClick()
        val started = lastStarted()
        assertEquals(Intent.ACTION_GET_CONTENT, started.intent.action)
        assertEquals("image/*", started.intent.type)

        deliver(started, Activity.RESULT_OK, Intent().setData(Uri.parse("content://pics/1")))
        assertEquals(listOf("image:content://pics/1", "dismiss"), events)
    }

    @Test
    fun `gallery cancelled does nothing`() {
        show(cameraGranted = false)
        compose.onNodeWithText("Choose from Gallery").performClick()
        deliver(lastStarted(), Activity.RESULT_CANCELED, null)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `camera without permission requests it and does not launch the camera`() {
        show(cameraGranted = false)
        compose.onNodeWithText("Take Photo").performClick()
        compose.waitForIdle()
        val started = lastStarted()
        assertTrue(started == null || started.intent.action != MediaStore.ACTION_IMAGE_CAPTURE)
        assertTrue(shadowOf(compose.activity).lastRequestedPermission.requestedPermissions.contains(Manifest.permission.CAMERA))
        assertTrue(events.isEmpty())
    }

    /** FileProvider's path matching assumes '/' separators, so it cannot run on a Windows JVM; stub it. */
    private fun stubFileProvider(): MutableList<Pair<String, File>> {
        val calls = mutableListOf<Pair<String, File>>()
        mockkStatic(FileProvider::class)
        every { FileProvider.getUriForFile(any(), any(), any()) } answers {
            calls += secondArg<String>() to thirdArg<File>()
            Uri.parse("content://${secondArg<String>()}/camera_images/${thirdArg<File>().name}")
        }
        return calls
    }

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `camera with permission captures into a FileProvider uri for a new cache file and reports it on success`() {
        val calls = stubFileProvider()
        show(cameraGranted = true)
        compose.onNodeWithText("Take Photo").performClick()
        val started = lastStarted()
        assertEquals(MediaStore.ACTION_IMAGE_CAPTURE, started.intent.action)
        val (authority, file) = calls.single()
        assertEquals("com.example.supercart2.fileprovider", authority)
        assertEquals(File(compose.activity.cacheDir, "camera"), file.parentFile)
        assertTrue(file.parentFile!!.isDirectory)
        assertTrue(file.name.matches(Regex("temp_\\d+\\.jpg")))
        @Suppress("DEPRECATION")
        val output = started.intent.getParcelableExtra<Uri>(MediaStore.EXTRA_OUTPUT)!!
        assertEquals("content://com.example.supercart2.fileprovider/camera_images/${file.name}", output.toString())

        deliver(started, Activity.RESULT_OK, null)
        assertEquals(listOf("image:$output", "dismiss"), events)
    }

    @Test
    fun `camera failure does nothing`() {
        stubFileProvider()
        show(cameraGranted = true)
        compose.onNodeWithText("Take Photo").performClick()
        deliver(lastStarted(), Activity.RESULT_CANCELED, null)
        assertTrue(events.isEmpty())
    }
}
