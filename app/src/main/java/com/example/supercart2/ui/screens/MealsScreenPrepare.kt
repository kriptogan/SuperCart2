package com.example.supercart2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.supercart2.R
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.models.Meal
import com.example.supercart2.ui.components.common.primaryButtonColors
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing

/** A meal's ingredients as a checklist that can be added to the shopping list. Unknown meals go [onBack]. */
@Composable
internal fun MealsPrepareView(mealId: String, onBack: () -> Unit, onEdit: () -> Unit) {
    val meal = remember(DataManagerObject.version, mealId) { mealsScreenVisibleMeal(mealId) }
    if (meal == null) return LaunchedEffect(mealId) { onBack() }
    val scope = rememberCoroutineScope()
    val state = remember(mealId) { MealsScreenPrepareState(scope) }
    MealsScreenPrepareContent(meal, state, onBack, onEdit)
}

@Composable
private fun MealsScreenPrepareContent(
    meal: Meal, state: MealsScreenPrepareState, onBack: () -> Unit, onEdit: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().background(SuperCartColors.lightGreen)) {
        MealsScreenPrepareColumn(meal, state, onBack, onEdit)
        SnackbarHost(state.snackbarHostState, Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp))
    }
}

@Composable
private fun MealsScreenPrepareColumn(
    meal: Meal, state: MealsScreenPrepareState, onBack: () -> Unit, onEdit: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        MealsScreenHeader(meal.name, onBack) {
            MealsScreenIconButton(Icons.Default.Edit, stringResource(R.string.edit_meal), onEdit)
        }
        MealsScreenPrepareBody(meal, state, onEdit)
    }
}

@Composable
private fun ColumnScope.MealsScreenPrepareBody(meal: Meal, state: MealsScreenPrepareState, onEdit: () -> Unit) {
    val rows = remember(DataManagerObject.version, meal) { DataManagerObject.mealIngredientRows(meal) }
    if (rows.isEmpty()) return MealsScreenNoIngredients(onEdit)
    MealsScreenPrepareChecklist(rows, state, Modifier.weight(1f))
    MealsScreenAddToListButton(rows, state)
}

@Composable
private fun MealsScreenNoIngredients(onAddIngredients: () -> Unit) {
    MealsScreenEmptyText(stringResource(R.string.no_ingredients_yet))
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Button(onClick = onAddIngredients, colors = primaryButtonColors()) {
            Text(stringResource(R.string.add_ingredients))
        }
    }
}

@Composable
private fun MealsScreenPrepareChecklist(
    rows: List<MealsScreenIngredientRow>, state: MealsScreenPrepareState, modifier: Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxWidth().padding(horizontal = SuperCartSpacing.md)) {
        items(rows, key = { it.first.uuid }) { row -> MealsScreenPrepareRow(row, state) }
    }
}

/** Groceries already on the shopping list show a checked, disabled checkbox and cannot be toggled. */
@Composable
private fun MealsScreenPrepareRow(row: MealsScreenIngredientRow, state: MealsScreenPrepareState) {
    val onToggle = { state.toggle(row.first.uuid) }
    val rowModifier = Modifier.fillMaxWidth().clickable(enabled = !row.first.inShoppingList, onClick = onToggle)
    Row(modifier = rowModifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        MealsScreenCheckbox(state.isChecked(row.first), enabled = !row.first.inShoppingList, onToggle)
        MealsScreenPrepareRowTexts(row)
    }
}

@Composable
private fun MealsScreenPrepareRowTexts(row: MealsScreenIngredientRow) {
    Column {
        Text(row.first.name, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = SuperCartColors.black)
        if (row.second.quantity.isNotBlank()) MealsScreenSecondaryText(row.second.quantity)
        if (row.first.inShoppingList) MealsScreenSecondaryText(stringResource(R.string.already_in_list))
    }
}

@Composable
private fun MealsScreenSecondaryText(text: String) {
    Text(text = text, fontSize = 13.sp, color = SuperCartColors.gray)
}

@Composable
private fun MealsScreenAddToListButton(rows: List<MealsScreenIngredientRow>, state: MealsScreenPrepareState) {
    val count = state.selectedIds(rows).size
    val resources = LocalContext.current.resources
    val onClick = { state.addToShoppingList(rows) { resources.getString(R.string.items_added_to_shopping_list, it) } }
    Button(onClick, Modifier.fillMaxWidth().padding(SuperCartSpacing.md), count > 0, RoundedCornerShape(12.dp),
        primaryButtonColors()) { Text(stringResource(R.string.add_count_to_shopping_list, count)) }
}
