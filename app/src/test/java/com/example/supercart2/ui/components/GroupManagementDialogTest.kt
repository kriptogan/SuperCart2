package com.example.supercart2.ui.components

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.supercart2.TestData
import com.example.supercart2.data.FirebaseManager
import com.example.supercart2.data.GroupManager
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GroupManagementDialogTest {

    @get:Rule
    val compose = createComposeRule()

    private var dismissed = 0
    private val code = "123e4567-e89b-12d3-a456-426614174000"

    @Before
    fun setUp() {
        TestData.reset()
        mockkStatic(FirebaseFirestore::class)
        every { FirebaseFirestore.getInstance() } returns mockk(relaxed = true)
        mockkObject(FirebaseManager)
        mockkObject(GroupManager)
    }

    @After
    fun tearDown() = unmockkAll()

    private fun show(current: String?) {
        coEvery { GroupManager.getCurrentGroupCode(any()) } returns current
        compose.setContent { GroupManagementDialog(onDismiss = { dismissed++ }) }
    }

    @Test
    fun `not in a group shows create and join options`() {
        show(null)
        compose.onNodeWithText("Family Group").assertIsDisplayed()
        compose.onNodeWithText("Share groceries with family members!").assertIsDisplayed()
        compose.onNodeWithText("Create New Group").assertIsDisplayed()
        compose.onNodeWithContentDescription("Create").assertIsDisplayed()
        compose.onNodeWithText("Join Existing Group").assertIsDisplayed()
        compose.onNodeWithText("Leave Group").assertDoesNotExist()
    }

    @Test
    fun `in a group shows the code with copy and leave`() {
        show(code)
        compose.onNodeWithText("You're in a family group!").assertIsDisplayed()
        compose.onNodeWithText(code).assertIsDisplayed()
        compose.onNodeWithContentDescription("Copy").assertIsDisplayed()
        compose.onNodeWithText("Leave Group").assertIsDisplayed()
        compose.onNodeWithContentDescription("Leave").assertIsDisplayed()
        compose.onNodeWithText("Create New Group").assertDoesNotExist()
    }

    @Test
    fun `copy puts the code on the clipboard and toasts`() {
        show(code)
        compose.onNodeWithContentDescription("Copy").performClick()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val clip = (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip!!
        assertEquals(code, clip.getItemAt(0).text.toString())
        assertEquals("Group Code", clip.description.label.toString())
        assertEquals("Group code copied!", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun `leave asks for confirmation and cancel keeps the group`() {
        show(code)
        compose.onNodeWithText("Leave Group").performClick()
        compose.onNodeWithText("Leave Group?").assertIsDisplayed()
        compose.onNodeWithText("Your local data will be kept, but you'll stop syncing with the group.")
            .assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Leave Group?").assertDoesNotExist()
        compose.onNodeWithText(code).assertIsDisplayed()
        coVerify(exactly = 0) { GroupManager.leaveGroup(any()) }
    }

    @Test
    fun `confirming leave clears the group and toasts a left-group message`() {
        coEvery { GroupManager.leaveGroup(any()) } just runs
        show(code)
        compose.onNodeWithText("Leave Group").performClick()
        compose.onNodeWithText("Leave").performClick()
        compose.waitForIdle()
        coVerify(exactly = 1) { GroupManager.leaveGroup(any()) }
        compose.onNodeWithText("Leave Group?").assertDoesNotExist()
        compose.onNodeWithText("Share groceries with family members!").assertIsDisplayed()
        assertEquals("You left the family group", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun `creating a group through the create dialog shows the new code`() {
        coEvery { GroupManager.createGroup(any()) } returns code
        coEvery { FirebaseManager.uploadData() } just runs
        show(null)
        compose.onNodeWithText("Create New Group").performClick()
        compose.onNodeWithText("Create Family Group").assertIsDisplayed()
        compose.onAllNodesWithText("Create New Group")[1].performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Create Family Group").assertDoesNotExist()
        compose.onNodeWithText(code).assertIsDisplayed()
    }

    @Test
    fun `create dialog cancel returns to the options`() {
        show(null)
        compose.onNodeWithText("Create New Group").performClick()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Create Family Group").assertDoesNotExist()
        compose.onNodeWithText("Share groceries with family members!").assertIsDisplayed()
    }

    @Test
    fun `joining a group through the join dialog shows the joined code`() {
        coEvery { GroupManager.joinGroup(any(), code) } returns true
        show(null)
        compose.onNodeWithText("Join Existing Group").performClick()
        compose.onNodeWithText("Join Family Group").assertIsDisplayed()
        compose.onNodeWithText("Group Code").performTextInput(" $code ")
        compose.onAllNodesWithText("Join Existing Group")[1].performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Join Family Group").assertDoesNotExist()
        compose.onNodeWithText(code).assertIsDisplayed()
    }

    @Test
    fun `close dismisses`() {
        show(null)
        compose.onNodeWithText("Close").performClick()
        assertEquals(1, dismissed)
    }
}
