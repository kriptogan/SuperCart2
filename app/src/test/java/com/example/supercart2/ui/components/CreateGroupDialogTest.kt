package com.example.supercart2.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.supercart2.TestData
import com.example.supercart2.data.FirebaseManager
import com.example.supercart2.data.GroupManager
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CreateGroupDialogTest {

    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()

    @Before
    fun setUp() {
        TestData.reset()
        mockkStatic(FirebaseFirestore::class)
        every { FirebaseFirestore.getInstance() } returns mockk(relaxed = true)
        mockkObject(GroupManager, FirebaseManager)
        coEvery { FirebaseManager.uploadData() } coAnswers { events += "upload" }
        compose.setContent {
            CreateGroupDialog(onDismiss = { events += "dismiss" }, onGroupCreated = { events += "created:$it" })
        }
    }

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `idle state shows title, message and enabled buttons`() {
        compose.onNodeWithText("Create Family Group").assertIsDisplayed()
        compose.onNodeWithText("Your data has been successfully uploaded to the cloud.").assertIsDisplayed()
        compose.onNodeWithText("Create New Group").assertIsEnabled()
        compose.onNodeWithText("Cancel").assertIsEnabled()
    }

    @Test
    fun `cancel dismisses`() {
        compose.onNodeWithText("Cancel").performClick()
        assertEquals(listOf("dismiss"), events)
        coVerify(exactly = 0) { GroupManager.createGroup(any()) }
    }

    @Test
    fun `while creating shows progress text and disables both buttons`() {
        val gate = CompletableDeferred<String>()
        coEvery { GroupManager.createGroup(any()) } coAnswers { gate.await() }

        compose.onNodeWithText("Create New Group").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Please wait while your data is being uploaded...").assertIsDisplayed()
        compose.onNodeWithText("Your data has been successfully uploaded to the cloud.").assertDoesNotExist()
        compose.onNodeWithText("Create New Group").assertIsNotEnabled()
        compose.onNodeWithText("Cancel").assertIsNotEnabled()
    }

    @Test
    fun `success creates group, uploads, reports code, toasts and stays in progress state`() {
        coEvery { GroupManager.createGroup(any()) } coAnswers { events += "create"; "CODE1" }

        compose.onNodeWithText("Create New Group").performClick()
        compose.waitForIdle()

        assertEquals(listOf("create", "upload", "created:CODE1"), events)
        assertEquals("Group created successfully", ShadowToast.getTextOfLatestToast())
        compose.onNodeWithText("Please wait while your data is being uploaded...").assertIsDisplayed()
        compose.onNodeWithText("Create New Group").assertIsNotEnabled()
    }

    @Test
    fun `failure shows error, re-enables buttons and does not report a group`() {
        coEvery { GroupManager.createGroup(any()) } throws IllegalStateException("boom")

        compose.onNodeWithText("Create New Group").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Failed to create group: boom").assertIsDisplayed()
        compose.onNodeWithText("Your data has been successfully uploaded to the cloud.").assertIsDisplayed()
        compose.onNodeWithText("Create New Group").assertIsEnabled()
        compose.onNodeWithText("Cancel").assertIsEnabled()
        assertTrue(events.isEmpty())
    }

    @Test
    fun `upload failure also shows error and skips the callback`() {
        coEvery { GroupManager.createGroup(any()) } returns "CODE1"
        coEvery { FirebaseManager.uploadData() } throws RuntimeException("offline")

        compose.onNodeWithText("Create New Group").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Failed to create group: offline").assertIsDisplayed()
        assertTrue(events.isEmpty())
    }

    @Test
    fun `retry clears the previous error`() {
        coEvery { GroupManager.createGroup(any()) } throws IllegalStateException("boom")
        compose.onNodeWithText("Create New Group").performClick()
        compose.waitForIdle()
        val gate = CompletableDeferred<String>()
        coEvery { GroupManager.createGroup(any()) } coAnswers { gate.await() }

        compose.onNodeWithText("Create New Group").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Failed to create group: boom").assertDoesNotExist()
    }
}
