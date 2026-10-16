package com.example.supercart2.ui.components

import androidx.annotation.StringRes
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.supercart2.R
import com.example.supercart2.ui.components.common.primaryButtonColors
import com.example.supercart2.ui.components.common.secondaryButtonColors
import com.example.supercart2.ui.theme.SuperCartColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Expiration dates outside 1900..2100 are treated as invalid. */
internal fun groceryCreationDialogIsValidDate(date: LocalDate?): Boolean = date != null && date.year in 1900..2100

/** Button label for the expiration date. */
@Composable
internal fun groceryCreationDialogDateLabel(date: LocalDate?): String = when {
    date == null -> stringResource(R.string.no_date_selected)
    groceryCreationDialogIsValidDate(date) -> formatDate(date) ?: stringResource(R.string.invalid_date)
    else -> stringResource(R.string.invalid_date)
}

/** The date as "MMM dd, yyyy", or null when it cannot be formatted. */
private fun formatDate(date: LocalDate): String? = try {
    date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
} catch (e: Exception) {
    null
}

/** Picker start: the selected date when valid, today otherwise. The Material3 picker works in UTC millis. */
private fun initialPickerMillis(date: LocalDate?): Long = try {
    startOfDayMillis(date?.takeIf { groceryCreationDialogIsValidDate(it) } ?: LocalDate.now())
} catch (e: Exception) {
    startOfDayMillis(LocalDate.now())
}

private fun startOfDayMillis(date: LocalDate): Long =
    date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

/** Picked UTC millis as a date; null when conversion fails or the year is out of range. */
private fun pickedDate(millis: Long): LocalDate? = try {
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
        .takeIf { groceryCreationDialogIsValidDate(it) }
} catch (e: Exception) {
    null
}

/** Reports the picked date (when there is a selection) and closes the picker. */
private fun confirmPicked(millis: Long?, onDateConfirmed: (LocalDate?) -> Unit, onDismiss: () -> Unit) {
    if (millis != null) onDateConfirmed(pickedDate(millis))
    onDismiss()
}

/** Material3 date picker opened from the expiration date field. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GroceryCreationDialogDatePicker(
    selectedDate: LocalDate?, onDateConfirmed: (LocalDate?) -> Unit, onDismiss: () -> Unit,
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initialPickerMillis(selectedDate))
    PickerDialog(state, onConfirm = { confirmPicked(state.selectedDateMillis, onDateConfirmed, onDismiss) }, onDismiss)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerDialog(state: DatePickerState, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    DatePickerDialog(
        onDismissRequest = onDismiss, colors = DatePickerDefaults.colors(containerColor = SuperCartColors.lightGreen),
        confirmButton = { PickerButton(R.string.ok, primaryButtonColors(), onConfirm) },
        dismissButton = { PickerButton(R.string.cancel, secondaryButtonColors(), onDismiss) },
    ) { DatePicker(state = state) }
}

@Composable
private fun PickerButton(@StringRes label: Int, colors: ButtonColors, onClick: () -> Unit) {
    Button(onClick = onClick, colors = colors) {
        Text(stringResource(label))
    }
}
