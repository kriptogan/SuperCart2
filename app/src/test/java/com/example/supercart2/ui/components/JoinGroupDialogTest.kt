package com.example.supercart2.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.supercart2.TestData
import com.example.supercart2.data.GroupManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkObject
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
class JoinGroupDialogTest {

    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()
    private val warning = "⚠️ Warning: Your local data will be replaced with the group's data."

    @Before
    fun setUp() {
        TestData.reset()
        mockkObject(GroupManager)
        compose.setContent {
            JoinGroupDialog(onDismiss = { events += "dismiss" }, onGroupJoined = { events += "joined:$it" })
        }
    }

    @After
    fun tearDown() = unmockkAll()

    private fun join(code: String) {
        compose.onNodeWithText("Group Code").performTextInput(code)
        compose.onNodeWithText("Join Existing Group").performClick()
        compose.waitForIdle()
    }

    @Test
    fun `idle state shows prompt, field, warning, join disabled while code is blank`() {
        compose.onNodeWithText("Join Family Group").assertIsDisplayed()
        compose.onNodeWithText("Enter Group Code").assertIsDisplayed()
        compose.onNodeWithText("Group Code").assertIsDisplayed()
        compose.onNodeWithText(warning).assertIsDisplayed()
        compose.onNodeWithText("Cancel").assertIsEnabled()
        compose.onNodeWithText("Join Existing Group").assertIsNotEnabled()
        compose.onNodeWithText("Group Code").performTextInput("  ")
        compose.onNodeWithText("Join Existing Group").assertIsNotEnabled()
    }

    @Test
    fun `cancel dismisses`() {
        compose.onNodeWithText("Cancel").performClick()
        assertEquals(listOf("dismiss"), events)
    }

    @Test
    fun `while joining shows progress text only and disables buttons`() {
        val gate = CompletableDeferred<Boolean>()
        coEvery { GroupManager.joinGroup(any(), any()) } coAnswers { gate.await() }

        join("abc")

        compose.onNodeWithText("Please wait while your data is being downloaded...").assertIsDisplayed()
        compose.onNodeWithText("Group Code").assertDoesNotExist()
        compose.onNodeWithText(warning).assertDoesNotExist()
        compose.onNodeWithText("Join Existing Group").assertIsNotEnabled()
        compose.onNodeWithText("Cancel").assertIsNotEnabled()
    }

    @Test
    fun `success joins with trimmed code, reports it, toasts and stays in progress state`() {
        coEvery { GroupManager.joinGroup(any(), any()) } returns true

        join("  abc-1 ")

        coVerify { GroupManager.joinGroup(any(), "abc-1") }
        assertEquals(listOf("joined:abc-1"), events)
        assertEquals("Joined group successfully", ShadowToast.getTextOfLatestToast())
        compose.onNodeWithText("Please wait while your data is being downloaded...").assertIsDisplayed()
    }

    @Test
    fun `not found shows error, keeps code and re-enables join, typing clears the error`() {
        coEvery { GroupManager.joinGroup(any(), any()) } returns false

        join("abc")

        compose.onNodeWithText("Group not found or invalid code").assertIsDisplayed()
        compose.onNodeWithText("abc").assertIsDisplayed()
        compose.onNodeWithText("Join Existing Group").assertIsEnabled()
        assertTrue(events.isEmpty())

        compose.onNodeWithText("abc").performTextInput("d")
        compose.onNodeWithText("Group not found or invalid code").assertDoesNotExist()
    }

    @Test
    fun `exception shows failure message`() {
        coEvery { GroupManager.joinGroup(any(), any()) } throws IllegalStateException("boom")

        join("abc")

        compose.onNodeWithText("Failed to join: boom").assertIsDisplayed()
        compose.onNodeWithText("Join Existing Group").assertIsEnabled()
        assertTrue(events.isEmpty())
    }
}
