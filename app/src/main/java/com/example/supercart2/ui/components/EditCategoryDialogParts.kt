package com.example.supercart2.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.example.supercart2.R
import com.example.supercart2.ui.components.common.DialogIconButton
import com.example.supercart2.ui.components.common.secondaryButtonColors
import com.example.supercart2.ui.components.common.superCartTextFieldColors
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartShapes
import com.example.supercart2.ui.theme.SuperCartSpacing

/** Building blocks shared by EditCategoryDialog and EditSubCategoryDialog. */

/** Texts of a delete confirmation; [titleColor] may be [Color.Unspecified] for the default content color. */
internal class EditCategoryDialogDeleteTexts(
    val title: String,
    val titleColor: Color,
    val protectedWarning: String,
    val message: String,
    val protectedDescription: String,
)

/** Full-width row with its children pushed to both ends and vertically centered. */
@Composable
internal fun EditCategoryDialogSpacedRow(
    modifier: Modifier = Modifier.fillMaxWidth(),
    content: @Composable RowScope.() -> Unit,
) = Row(modifier, Arrangement.SpaceBetween, Alignment.CenterVertically, content)

/** Dialog headline (taking the remaining width) followed by a delete icon. */
@Composable
internal fun EditCategoryDialogTitleRow(title: String, deleteIcon: @Composable () -> Unit) {
    EditCategoryDialogSpacedRow {
        EditCategoryDialogHeadline(title, SuperCartColors.black, Modifier.weight(1f))
        deleteIcon()
    }
}

@Composable
private fun EditCategoryDialogHeadline(text: String, color: Color, modifier: Modifier) {
    Text(
        text = text, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center,
        color = color, modifier = modifier,
    )
}

/** Red trash icon button; grey, disabled and differently described when the item is protected. */
@Composable
internal fun EditCategoryDialogDeleteIcon(
    isProtected: Boolean, protectedDescription: String, description: String, onClick: () -> Unit,
) {
    IconButton(onClick = onClick, enabled = !isProtected) {
        val tint = if (isProtected) SuperCartColors.gray else Color.Red
        Icon(Icons.Default.Delete, if (isProtected) protectedDescription else description, tint = tint)
    }
}

/** Multi-line name input used by the edit dialogs. */
@Composable
internal fun EditCategoryDialogNameField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label) },
        modifier = Modifier.fillMaxWidth(), singleLine = false, maxLines = 4,
        colors = superCartTextFieldColors(), shape = SuperCartShapes.small,
    )
}

/** "Are you sure?" dialog with Cancel and a red Delete button (disabled for protected items). */
@Composable
internal fun EditCategoryDialogDeleteConfirmation(
    texts: EditCategoryDialogDeleteTexts, isProtected: Boolean, onCancel: () -> Unit, onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel, containerColor = SuperCartColors.lightGreen,
        title = { EditCategoryDialogHeadline(texts.title, texts.titleColor, Modifier.fillMaxWidth()) },
        text = { EditCategoryDialogDeleteMessage(texts, isProtected) },
        confirmButton = { EditCategoryDialogDeleteButtons(texts, isProtected, onCancel, onDelete) },
    )
}

@Composable
private fun EditCategoryDialogDeleteMessage(texts: EditCategoryDialogDeleteTexts, isProtected: Boolean) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (isProtected) EditCategoryDialogProtectedWarning(texts.protectedWarning)
        Text(text = texts.message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    }
}

@Composable
private fun EditCategoryDialogProtectedWarning(text: String) {
    Text(
        text = text, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
        color = Color.Red, modifier = Modifier.padding(bottom = SuperCartSpacing.sm),
    )
}

@Composable
private fun EditCategoryDialogDeleteButtons(
    texts: EditCategoryDialogDeleteTexts, isProtected: Boolean, onCancel: () -> Unit, onDelete: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SuperCartSpacing.sm)) {
        DialogIconButton(Icons.Default.Close, stringResource(R.string.cancel), secondaryButtonColors(),
            Modifier.weight(1f), onClick = onCancel)
        EditCategoryDialogDangerButton(texts, isProtected, Modifier.weight(1f), onDelete)
    }
}

/** Red Delete button; grey, disabled and described as protected for protected items. */
@Composable
private fun EditCategoryDialogDangerButton(
    texts: EditCategoryDialogDeleteTexts, isProtected: Boolean, modifier: Modifier, onDelete: () -> Unit,
) {
    val description = if (isProtected) texts.protectedDescription else stringResource(R.string.delete)
    val colors = ButtonDefaults.buttonColors(
        containerColor = if (isProtected) SuperCartColors.gray else Color.Red, contentColor = SuperCartColors.white,
    )
    DialogIconButton(Icons.Default.Delete, description, colors, modifier, !isProtected, onDelete)
}
