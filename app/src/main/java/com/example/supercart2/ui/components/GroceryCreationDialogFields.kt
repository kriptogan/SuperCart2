package com.example.supercart2.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.supercart2.R
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.ui.components.common.secondaryButtonColors
import com.example.supercart2.ui.components.common.superCartTextFieldColors
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartShapes
import com.example.supercart2.ui.theme.SuperCartSpacing
import com.example.supercart2.utils.localizedCategoryDisplayName
import com.example.supercart2.utils.localizedSubCategoryDisplayName
import java.time.LocalDate

/** Spacer followed by the grocery name input. */
@Composable
internal fun GroceryCreationDialogNameField(form: GroceryCreationDialogForm) {
    Spacer(modifier = Modifier.height(SuperCartSpacing.md))
    OutlinedTextField(
        value = form.name, onValueChange = { form.name = it }, label = { Text(stringResource(R.string.grocery_name)) },
        modifier = Modifier.fillMaxWidth(), singleLine = false, maxLines = 4,
        colors = superCartTextFieldColors(), shape = SuperCartShapes.small,
    )
}

/** Category and sub-category selectors, each preceded by a spacer. */
@Composable
internal fun GroceryCreationDialogLocationFields(form: GroceryCreationDialogForm, popups: GroceryCreationDialogPopups) {
    Spacer(modifier = Modifier.height(SuperCartSpacing.md))
    LabeledField(stringResource(R.string.category)) { CategoryButton(form) { popups.category = true } }
    Spacer(modifier = Modifier.height(SuperCartSpacing.md))
    LabeledField(stringResource(R.string.sub_category)) { SubCategoryButton(form) { popups.subCategory = true } }
}

/** Store and expiration date selectors, each preceded by a spacer. */
@Composable
internal fun GroceryCreationDialogStoreAndDateFields(
    form: GroceryCreationDialogForm, popups: GroceryCreationDialogPopups,
) {
    Spacer(modifier = Modifier.height(SuperCartSpacing.md))
    LabeledField(stringResource(R.string.stores)) { StoresButton(form.storeIds) { popups.stores = true } }
    Spacer(modifier = Modifier.height(SuperCartSpacing.md))
    LabeledField(stringResource(R.string.expiration_date)) {
        DateRow(form.date, onPick = { popups.datePicker = true }, onClear = { form.date = null })
    }
}

@Composable
private fun LabeledField(label: String, content: @Composable () -> Unit) {
    val labelModifier = Modifier.padding(bottom = SuperCartSpacing.xs)
    Column {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = SuperCartColors.black,
            modifier = labelModifier)
        content()
    }
}

@Composable
private fun CategoryButton(form: GroceryCreationDialogForm, onClick: () -> Unit) {
    val category = form.category
    val value = category?.let { localizedCategoryDisplayName(it.name) } ?: stringResource(R.string.select_category)
    SelectorButton(value, selectedColor(category != null), stringResource(R.string.select_category),
        Modifier.fillMaxWidth(), onClick = onClick)
}

@Composable
private fun SubCategoryButton(form: GroceryCreationDialogForm, onClick: () -> Unit) {
    val subCategory = form.subCategory
    val value = subCategory?.let { localizedSubCategoryDisplayName(it.name) }
        ?: stringResource(R.string.select_sub_category)
    SelectorButton(value, selectedColor(subCategory != null), stringResource(R.string.select_sub_category),
        Modifier.fillMaxWidth(), enabled = form.category != null, onClick = { if (form.category != null) onClick() })
}

@Composable
private fun StoresButton(storeIds: List<String>, onClick: () -> Unit) {
    val storeNames = remember(storeIds, DataManagerObject.version) { storeNamesOf(storeIds) }
    SelectorButton(storesLabel(storeNames), selectedColor(storeNames.isNotEmpty()),
        stringResource(R.string.select_stores), Modifier.fillMaxWidth(), onClick = onClick)
}

/** Names of the known stores among [storeIds]; unknown ids are skipped. */
private fun storeNamesOf(storeIds: List<String>): List<String> =
    storeIds.mapNotNull { storeId -> DataManagerObject.stores.find { it.uuid == storeId }?.name }

@Composable
private fun storesLabel(storeNames: List<String>): String = when {
    storeNames.isEmpty() -> stringResource(R.string.select_stores_optional)
    storeNames.size == 1 -> storeNames[0]
    else -> stringResource(R.string.stores_selected, storeNames.size)
}

@Composable
private fun DateRow(date: LocalDate?, onPick: () -> Unit, onClear: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SuperCartSpacing.sm)) {
        SelectorButton(groceryCreationDialogDateLabel(date), selectedColor(groceryCreationDialogIsValidDate(date)),
            "Select Date", Modifier.weight(1f), onClick = onPick)
        if (date != null) ClearDateButton(onClear)
    }
}

@Composable
private fun ClearDateButton(onClick: () -> Unit) {
    val colors =
        ButtonDefaults.buttonColors(containerColor = SuperCartColors.lightGray, contentColor = SuperCartColors.darkGray)
    Button(onClick = onClick, colors = colors, shape = SuperCartShapes.small) {
        Text(stringResource(R.string.clear))
    }
}

private fun selectedColor(hasValue: Boolean): Color = if (hasValue) SuperCartColors.black else SuperCartColors.gray

/** White button showing the current value and a drop-down arrow. */
@Composable
private fun SelectorButton(
    value: String, valueColor: Color, iconDescription: String, modifier: Modifier,
    enabled: Boolean = true, onClick: () -> Unit,
) {
    Button(onClick, modifier, enabled, colors = secondaryButtonColors(), shape = SuperCartShapes.small) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(text = value, color = valueColor)
            Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = iconDescription)
        }
    }
}
