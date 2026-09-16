package com.example.supercart2.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.example.supercart2.R
import com.example.supercart2.ui.components.common.CancelConfirmButtons
import com.example.supercart2.ui.components.common.superCartTextFieldColors
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartShapes

/** Asks for a sub-category name. Confirm passes the trimmed name and leaves dismissing to the caller. */
@Composable
fun CreateSubCategoryDialog(
    onDismiss: () -> Unit,
    onSubCategoryCreated: (String) -> Unit
) {
    var subCategoryName by remember { mutableStateOf("") }
    val create: () -> Unit = { if (subCategoryName.isNotBlank()) onSubCategoryCreated(subCategoryName.trim()) }
    CreateSubCategoryAlert(subCategoryName, { subCategoryName = it }, onDismiss, create)
}

@Composable
private fun CreateSubCategoryAlert(
    name: String, onNameChange: (String) -> Unit, onDismiss: () -> Unit, onCreate: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = SuperCartColors.lightGreen,
        title = { SubCategoryDialogTitle() }, text = { SubCategoryNameField(name, onNameChange) },
        confirmButton = { SubCategoryDialogButtons(onDismiss, name.isNotBlank(), onCreate) },
    )
}

/** Like DialogTitle, but in the theme's default title color. */
@Composable
private fun SubCategoryDialogTitle() {
    Text(
        text = stringResource(R.string.create_new_subcategory), style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SubCategoryNameField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(stringResource(R.string.subcategory_name)) },
        modifier = Modifier.fillMaxWidth(), singleLine = false, maxLines = 4,
        colors = superCartTextFieldColors(), shape = SuperCartShapes.small,
    )
}

@Composable
private fun SubCategoryDialogButtons(onDismiss: () -> Unit, canCreate: Boolean, onCreate: () -> Unit) {
    CancelConfirmButtons(onDismiss, stringResource(R.string.create_subcategory), canCreate, onCreate)
}
