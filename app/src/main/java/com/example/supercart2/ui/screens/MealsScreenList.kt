package com.example.supercart2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.supercart2.R
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.models.Meal
import com.example.supercart2.ui.components.common.CancelConfirmButtons
import com.example.supercart2.ui.components.common.DialogTitle
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing

/** Meals tab entry: all meals as cards, with create, edit and delete actions. */
@Composable
internal fun MealsListView(onCreate: () -> Unit, onOpen: (String) -> Unit, onEdit: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val state = remember { MealsScreenListState(scope) }
    MealsScreenListContent(onCreate, MealsScreenCardActions(onOpen, onEdit, onDelete = state::requestDelete))
    state.mealToDelete?.let { MealsScreenDeleteConfirmation(it, state) }
}

@Composable
private fun MealsScreenListContent(onCreate: () -> Unit, actions: MealsScreenCardActions) {
    val cards = remember(DataManagerObject.version) { mealsScreenCards() }
    Column(modifier = Modifier.fillMaxSize().background(SuperCartColors.lightGreen)) {
        MealsScreenListHeader(onCreate)
        MealsScreenListBody(cards, actions)
    }
}

/** Callbacks of a meal card, by meal uuid. */
private class MealsScreenCardActions(
    val onOpen: (String) -> Unit, val onEdit: (String) -> Unit, val onDelete: (Meal) -> Unit,
) {
    fun delete(uuid: String) {
        DataManagerObject.getSortedMeals().find { it.uuid == uuid }?.let(onDelete)
    }
}

@Composable
private fun MealsScreenListHeader(onCreate: () -> Unit) {
    StoresScreenHeaderColumn {
        StoresScreenTitleRow(bottomPadding = SuperCartSpacing.md) {
            StoresScreenTitle(stringResource(R.string.nav_meals), Modifier.weight(1f).padding(start = 8.dp))
            MealsScreenIconButton(Icons.Default.Add, stringResource(R.string.create_meal), onCreate)
        }
    }
}

/** Plain icon button in the primary color. */
@Composable
internal fun MealsScreenIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(imageVector = icon, contentDescription = description, tint = SuperCartColors.primaryGreen)
    }
}

@Composable
private fun MealsScreenListBody(cards: List<MealCardData>, actions: MealsScreenCardActions) {
    if (cards.isEmpty()) return MealsScreenEmptyText(stringResource(R.string.no_meals_yet))
    val modifier = Modifier.fillMaxSize().padding(horizontal = SuperCartSpacing.md)
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(SuperCartSpacing.sm)) {
        items(cards, key = { it.uuid }) { card -> MealsScreenMealCard(card, actions) }
    }
}

/** Gray centered hint filling the remaining space. */
@Composable
internal fun MealsScreenEmptyText(text: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(SuperCartSpacing.xl), contentAlignment = Alignment.Center) {
        Text(text = text, color = SuperCartColors.gray, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun MealsScreenMealCard(card: MealCardData, actions: MealsScreenCardActions) {
    Card(
        onClick = { actions.onOpen(card.uuid) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SuperCartColors.white),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) { MealsScreenMealCardRow(card, actions) }
}

@Composable
private fun MealsScreenMealCardRow(card: MealCardData, actions: MealsScreenCardActions) {
    val rowModifier = Modifier.fillMaxWidth().padding(start = SuperCartSpacing.lg, top = 12.dp, bottom = 12.dp)
    Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
        MealsScreenMealSummary(card, Modifier.weight(1f))
        MealsScreenIconButton(Icons.Default.Edit, stringResource(R.string.edit_meal)) { actions.onEdit(card.uuid) }
        MealsScreenIconButton(Icons.Default.Delete, stringResource(R.string.delete_meal)) { actions.delete(card.uuid) }
    }
}

@Composable
private fun MealsScreenMealSummary(card: MealCardData, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(card.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = SuperCartColors.black)
        Spacer(modifier = Modifier.height(4.dp))
        val count = stringResource(R.string.meal_ingredients_count, card.ingredientCount)
        Text(text = count, fontSize = 13.sp, color = SuperCartColors.gray)
    }
}

@Composable
private fun MealsScreenDeleteConfirmation(meal: Meal, state: MealsScreenListState) {
    AlertDialog(
        onDismissRequest = state::cancelDelete, containerColor = SuperCartColors.lightGreen,
        title = { DialogTitle(stringResource(R.string.delete_meal)) },
        text = { MealsScreenDeleteMessage(meal.name) },
        confirmButton = { MealsScreenDeleteButtons(state) },
    )
}

@Composable
private fun MealsScreenDeleteButtons(state: MealsScreenListState) {
    CancelConfirmButtons(state::cancelDelete, stringResource(R.string.delete), onConfirm = state::confirmDelete)
}

@Composable
private fun MealsScreenDeleteMessage(mealName: String) {
    val text = stringResource(R.string.delete_grocery_confirmation, mealName)
    Text(text = text, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
}
