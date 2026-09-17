package com.example.supercart2.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.supercart2.R
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.models.Grocery
import com.example.supercart2.ui.components.GroceryCreationDialog
import com.example.supercart2.ui.components.common.CancelConfirmButtons
import com.example.supercart2.ui.components.common.DialogTitle
import com.example.supercart2.ui.components.common.secondaryButtonColors
import com.example.supercart2.ui.components.common.superCartTextFieldColors
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing

/**
 * Dialog choosing groceries to add to a meal draft, with search and grocery creation. While the grocery dialog is
 * open the picker is hidden; its state is kept, and a created grocery comes back selected.
 */
@Composable
internal fun MealsScreenIngredientPicker(
    excludedIds: Set<String>, onDismiss: () -> Unit, onConfirm: (List<String>) -> Unit,
) {
    val state = remember { MealsScreenIngredientPickerState(excludedIds) }
    if (state.showGroceryCreation) return MealsScreenPickerGroceryCreation(state)
    val candidates = remember(DataManagerObject.version) { state.candidates() }
    MealsScreenPickerAlert(state, candidates, onDismiss, onConfirm = { onConfirm(state.confirmedIds(candidates)) })
}

@Composable
private fun MealsScreenPickerGroceryCreation(state: MealsScreenIngredientPickerState) {
    GroceryCreationDialog(
        onDismiss = state::closeGroceryCreation, initialGroceryName = state.query.trim(),
        onGroceryCreated = state::onGroceryCreated,
    )
}

@Composable
private fun MealsScreenPickerAlert(
    state: MealsScreenIngredientPickerState, candidates: List<Grocery>, onDismiss: () -> Unit, onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = SuperCartColors.lightGreen,
        title = { DialogTitle(stringResource(R.string.select_ingredients)) },
        text = { MealsScreenPickerContent(state, state.visibleCandidates(candidates)) },
        confirmButton = { MealsScreenPickerButtons(state, onDismiss, onConfirm) },
    )
}

@Composable
private fun MealsScreenPickerContent(state: MealsScreenIngredientPickerState, groceries: List<Grocery>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        MealsScreenPickerSearchField(state)
        MealsScreenPickerList(state, groceries)
    }
}

@Composable
private fun MealsScreenPickerList(state: MealsScreenIngredientPickerState, groceries: List<Grocery>) {
    if (groceries.isEmpty()) return MealsScreenEmptyText(stringResource(R.string.no_groceries_yet))
    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
        items(groceries, key = { it.uuid }) { grocery -> MealsScreenPickerRow(grocery, state) }
    }
}

@Composable
private fun MealsScreenPickerSearchField(state: MealsScreenIngredientPickerState) {
    OutlinedTextField(
        value = state.query, onValueChange = { state.query = it }, modifier = Modifier.fillMaxWidth(),
        singleLine = true, placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { Icon(Icons.Default.Search, null, tint = SuperCartColors.gray) },
        colors = superCartTextFieldColors(),
    )
}

@Composable
private fun MealsScreenPickerRow(grocery: Grocery, state: MealsScreenIngredientPickerState) {
    val onToggle = { state.toggle(grocery.uuid) }
    val modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 2.dp)
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        MealsScreenCheckbox(state.isSelected(grocery.uuid), enabled = true, onToggle)
        Text(text = grocery.name, color = SuperCartColors.black)
    }
}

/** "New grocery" above cancel (✕) and confirm (✓). */
@Composable
private fun MealsScreenPickerButtons(
    state: MealsScreenIngredientPickerState, onDismiss: () -> Unit, onConfirm: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SuperCartSpacing.sm)) {
        MealsScreenNewGroceryButton(onClick = state::openGroceryCreation)
        CancelConfirmButtons(onDismiss, stringResource(R.string.confirm), onConfirm = onConfirm)
    }
}

@Composable
private fun MealsScreenNewGroceryButton(onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth(), colors = secondaryButtonColors()) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(modifier = Modifier.width(SuperCartSpacing.sm))
        Text(stringResource(R.string.new_grocery))
    }
}
