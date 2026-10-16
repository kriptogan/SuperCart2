package com.example.supercart2.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.supercart2.R
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.models.Category
import com.example.supercart2.models.SubCategory
import com.example.supercart2.ui.components.common.CancelConfirmButtons
import com.example.supercart2.ui.components.common.DialogTitle
import com.example.supercart2.ui.components.common.superCartTextFieldColors
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartShapes

@Composable
fun CreateCategoryDialog(
    onDismiss: () -> Unit,
    onCategoryCreated: (Category, SubCategory) -> Unit
) {
    var categoryName by remember { mutableStateOf("") }
    val create: () -> Unit = {
        buildCategory(categoryName)?.let { (category, general) -> onCategoryCreated(category, general); onDismiss() }
    }
    CreateCategoryAlert(categoryName, { categoryName = it }, onDismiss, create)
}

@Composable
private fun CreateCategoryAlert(
    name: String, onNameChange: (String) -> Unit, onDismiss: () -> Unit, onCreate: () -> Unit,
) {
    val createLabel = stringResource(R.string.create_category)
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = SuperCartColors.lightGreen,
        title = { DialogTitle(createLabel) }, text = { CategoryNameField(name, onNameChange) },
        confirmButton = { CancelConfirmButtons(onDismiss, createLabel, name.isNotBlank(), onCreate) },
    )
}

@Composable
private fun CategoryNameField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(stringResource(R.string.category_name)) },
        modifier = Modifier.fillMaxWidth(), singleLine = false, maxLines = 4,
        colors = superCartTextFieldColors(), shape = SuperCartShapes.small,
    )
}

/**
 * Builds a category placed after the highest existing viewOrder (deleted categories included) together with its
 * protected "General" sub-category; null when the name is blank. Nothing is added to [DataManagerObject].
 */
private fun buildCategory(name: String): Pair<Category, SubCategory>? {
    if (name.isBlank()) return null
    val viewOrder = (DataManagerObject.categories.maxOfOrNull { it.category.viewOrder } ?: 0) + 1
    val category = Category(name = name.trim(), default = false, viewOrder = viewOrder, protected = false)
    return category to SubCategory(categoryId = category.uuid, name = "General", protected = true)
}
