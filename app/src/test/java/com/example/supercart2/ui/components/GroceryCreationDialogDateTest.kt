package com.example.supercart2.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.supercart2.TestData
import com.example.supercart2.data.DataManagerObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.TimeZone

/** Expiration date of [GroceryCreationDialog]: label, 1900..2100 validation and the date picker. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GroceryCreationDialogDateTest {

    @get:Rule
    val compose = createComposeRule()

    private val originalZone: TimeZone = TimeZone.getDefault()

    @Before
    fun setUp() {
        TestData.installStandardTree()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalZone)
    }

    private fun showEditing(date: LocalDate?) {
        DataManagerObject.updateGrocery("g1") { it.copy(expirationDate = date) }
        val grocery = TestData.findGrocery("g1")!!
        compose.setContent { GroceryCreationDialog(onDismiss = {}, onGroceryCreated = {}, groceryToEdit = grocery) }
    }

    private fun text(value: String) = compose.onNodeWithText(value).performScrollTo()
    private fun save() = compose.onNodeWithContentDescription("Save Changes").performClick()
    private fun savedDate(): LocalDate? = TestData.findGrocery("g1")!!.expirationDate
    private fun label(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))

    private fun openPickerAndConfirm() {
        compose.onNodeWithContentDescription("Select Date").performScrollTo().performClick()
        compose.onNodeWithText("OK").performClick()
    }

    @Test
    fun `year 2100 is valid`() {
        showEditing(LocalDate.of(2100, 12, 31))
        text("Dec 31, 2100").assertIsDisplayed()
    }

    @Test
    fun `year 1900 is valid`() {
        showEditing(LocalDate.of(1900, 1, 1))
        text("Jan 01, 1900").assertIsDisplayed()
    }

    @Test
    fun `out of range year shows Invalid date but can still be cleared and is saved unchanged`() {
        showEditing(LocalDate.of(1899, 12, 31))
        text("Invalid date").assertIsDisplayed()
        text("Clear").assertIsDisplayed()
        save()
        assertEquals(LocalDate.of(1899, 12, 31), savedDate())
    }

    @Test
    fun `year after 2100 shows Invalid date`() {
        showEditing(LocalDate.of(2101, 1, 1))
        text("Invalid date").assertIsDisplayed()
    }

    @Test
    fun `picker confirm keeps the currently selected valid date`() {
        showEditing(LocalDate.of(2030, 5, 1))
        openPickerAndConfirm()
        text("May 01, 2030").assertIsDisplayed()
        save()
        assertEquals(LocalDate.of(2030, 5, 1), savedDate())
    }

    @Test
    fun `picker opens on today when there is no date and confirm selects today`() {
        showEditing(null)
        text("No date selected").assertIsDisplayed()
        openPickerAndConfirm()
        text(label(LocalDate.now())).assertIsDisplayed()
        save()
        assertEquals(LocalDate.now(), savedDate())
    }

    @Test
    fun `picker opens on today for an invalid date and confirm replaces it`() {
        showEditing(LocalDate.of(1800, 1, 1))
        openPickerAndConfirm()
        text(label(LocalDate.now())).assertIsDisplayed()
    }

    @Test
    fun `picker cancel leaves the date unchanged`() {
        showEditing(null)
        compose.onNodeWithContentDescription("Select Date").performScrollTo().performClick()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("OK").assertDoesNotExist()
        text("No date selected").assertIsDisplayed()
    }

    private fun assertPickerRoundTripKeepsDate(zone: String) {
        TimeZone.setDefault(TimeZone.getTimeZone(zone))
        assertEquals(ZoneId.of(zone), ZoneId.systemDefault())
        showEditing(LocalDate.of(2030, 5, 1))
        openPickerAndConfirm()
        text("May 01, 2030").assertIsDisplayed()
        save()
        assertEquals(LocalDate.of(2030, 5, 1), savedDate())
    }

    @Test
    fun `picker round-trip keeps the date east of UTC (Asia-Jerusalem)`() {
        assertPickerRoundTripKeepsDate("Asia/Jerusalem")
    }

    @Test
    fun `picker round-trip keeps the date in UTC`() {
        assertPickerRoundTripKeepsDate("UTC")
    }

    @Test
    fun `picker round-trip keeps the date west of UTC (America-Los_Angeles)`() {
        assertPickerRoundTripKeepsDate("America/Los_Angeles")
    }

    @Test
    fun `picker opens on the local today far east of UTC (Pacific-Kiritimati)`() {
        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"))
        showEditing(null)
        openPickerAndConfirm()
        save()
        assertEquals(LocalDate.now(), savedDate())
    }

    @Test
    fun `created grocery gets the picked date`() {
        compose.setContent { GroceryCreationDialog(onDismiss = {}, onGroceryCreated = {}, initialGroceryName = "Milk") }
        openPickerAndConfirm()
        compose.onNodeWithContentDescription("Create Grocery").performClick()
        val added = DataManagerObject.categories.flatMap { it.subCategories }.flatMap { it.groceries }
            .single { it.name == "Milk" }
        assertEquals(LocalDate.now(), added.expirationDate)
    }
}
