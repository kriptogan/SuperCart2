package com.example.supercart2.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.example.supercart2.R
import com.example.supercart2.models.Grocery
import com.example.supercart2.ui.components.common.DialogIconButton
import com.example.supercart2.ui.components.common.secondaryButtonColors
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing
import kotlinx.coroutines.CoroutineScope

/** "Are you sure?" dialog; confirming soft-deletes [grocery], persists, and dismisses the grocery dialog too. */
@Composable
internal fun GroceryCreationDialogDeleteConfirmation(
    grocery: Grocery, popups: GroceryCreationDialogPopups, scope: CoroutineScope, onDismiss: () -> Unit,
) {
    val close = { popups.deleteConfirmation = false }
    AlertDialog(
        onDismissRequest = close, containerColor = SuperCartColors.lightGreen,
        title = { DeleteTitle() }, text = { DeleteMessage(grocery.name) },
        confirmButton = { DeleteButtons(close, onDelete = { deleteAndClose(grocery, scope, onDismiss, close) }) },
    )
}

private fun deleteAndClose(grocery: Grocery, scope: CoroutineScope, onDismiss: () -> Unit, close: () -> Unit) {
    groceryCreationDialogDelete(grocery, scope)
    onDismiss()
    close()
}

@Composable
private fun DeleteTitle() {
    Text(
        text = stringResource(R.string.delete_grocery), style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun DeleteMessage(groceryName: String) {
    Text(
        text = stringResource(R.string.delete_grocery_confirmation, groceryName),
        style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
    )
}

@Composable
private fun DeleteButtons(onCancel: () -> Unit, onDelete: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SuperCartSpacing.sm)) {
        val half = Modifier.weight(1f)
        LabeledIconButton(Icons.Default.Close, R.string.cancel, secondaryButtonColors(), half, onCancel)
        LabeledIconButton(Icons.Default.Delete, R.string.delete, dangerColors(), half, onDelete)
    }
}

@Composable
private fun LabeledIconButton(
    icon: ImageVector, @StringRes description: Int, colors: ButtonColors, modifier: Modifier, onClick: () -> Unit,
) {
    DialogIconButton(icon, stringResource(description), colors, modifier, onClick = onClick)
}

@Composable
private fun dangerColors(): ButtonColors =
    ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = SuperCartColors.white)
