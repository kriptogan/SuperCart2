package com.example.supercart2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.supercart2.R
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.ui.components.common.CancelConfirmButtons
import com.example.supercart2.ui.components.common.secondaryButtonColors
import com.example.supercart2.ui.components.common.superCartTextFieldColors
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing

/**
 * Creates ([mealId] null) or edits a meal: name and ingredients with quantities. Saving reports the meal's uuid to
 * [onSaved]; an unknown or deleted [mealId] calls [onCancel].
 */
@Composable
internal fun MealsEditorView(mealId: String?, onCancel: () -> Unit, onSaved: (String) -> Unit) {
    val meal = remember(mealId) { mealsScreenVisibleMeal(mealId) }
    if (mealId != null && meal == null) return LaunchedEffect(mealId) { onCancel() }
    val scope = rememberCoroutineScope()
    val state = remember(mealId) { MealsScreenEditorState(mealId, scope).apply { load(meal) } }
    MealsScreenEditorContent(state, onCancel, onSave = { state.save()?.let(onSaved) })
    if (state.showPicker) MealsScreenEditorPicker(state)
}

@Composable
private fun MealsScreenEditorContent(state: MealsScreenEditorState, onCancel: () -> Unit, onSave: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(SuperCartColors.lightGreen)) {
        MealsScreenHeader(stringResource(if (state.isNew) R.string.new_meal else R.string.edit_meal), null) {}
        MealsScreenEditorForm(state, Modifier.weight(1f).padding(horizontal = SuperCartSpacing.md))
        MealsScreenEditorButtons(state, onCancel, onSave)
    }
}

/** Cancel (✕) and save (✓, enabled with a non-blank name). */
@Composable
private fun MealsScreenEditorButtons(state: MealsScreenEditorState, onCancel: () -> Unit, onSave: () -> Unit) {
    Row(modifier = Modifier.padding(SuperCartSpacing.md)) {
        CancelConfirmButtons(onCancel, stringResource(R.string.save), state.canSave, onSave)
    }
}

@Composable
private fun MealsScreenEditorForm(state: MealsScreenEditorState, modifier: Modifier) {
    val rows = remember(DataManagerObject.version, state.ingredients) { state.rows() }
    LazyColumn(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(SuperCartSpacing.sm)) {
        item { MealsScreenMealNameField(state) }
        items(rows, key = { it.first.uuid }) { row -> MealsScreenEditorRow(row, state) }
        item { MealsScreenAddIngredientsButton(onClick = { state.showPicker = true }) }
    }
}

@Composable
private fun MealsScreenMealNameField(state: MealsScreenEditorState) {
    OutlinedTextField(
        value = state.name, onValueChange = { state.name = it }, modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.meal_name)) }, singleLine = true, colors = superCartTextFieldColors(),
    )
}

@Composable
private fun MealsScreenEditorRow(row: MealsScreenIngredientRow, state: MealsScreenEditorState) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(row.first.name, Modifier.weight(1f), SuperCartColors.black, 16.sp, fontWeight = FontWeight.Medium)
        MealsScreenQuantityField(row.second.quantity) { state.setQuantity(row.first.uuid, it) }
        MealsScreenIconButton(Icons.Default.Close, stringResource(R.string.remove_ingredient)) { state.remove(row.first.uuid) }
    }
}

@Composable
private fun MealsScreenQuantityField(quantity: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = quantity, onValueChange = onChange, modifier = Modifier.width(140.dp),
        label = { Text(stringResource(R.string.quantity)) }, singleLine = true, colors = superCartTextFieldColors(),
    )
}

@Composable
private fun MealsScreenAddIngredientsButton(onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth(), colors = secondaryButtonColors()) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(modifier = Modifier.width(SuperCartSpacing.sm))
        Text(stringResource(R.string.add_ingredients))
    }
}

@Composable
private fun MealsScreenEditorPicker(state: MealsScreenEditorState) {
    MealsScreenIngredientPicker(
        excludedIds = state.ingredients.map { it.groceryId }.toSet(),
        onDismiss = { state.showPicker = false }, onConfirm = state::addGroceries,
    )
}
