package com.example.supercart2.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.models.Meal
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Persists the current data in [this] scope; call after every mutation of the Meals screens. */
internal fun CoroutineScope.mealsScreenSave() {
    launch { DataStoreManager.saveDataGlobally() }
}

/** The non-deleted meal [mealId], or null. */
internal fun mealsScreenVisibleMeal(mealId: String?): Meal? =
    DataManagerObject.getSortedMeals().find { it.uuid == mealId }

/** Header of a Meals sub-screen: optional back button, title and trailing [actions]. */
@Composable
internal fun MealsScreenHeader(title: String, onBack: (() -> Unit)?, actions: @Composable RowScope.() -> Unit) {
    StoresScreenHeaderColumn {
        StoresScreenTitleRow(bottomPadding = SuperCartSpacing.md) { MealsScreenHeaderRow(title, onBack, actions) }
    }
}

@Composable
private fun RowScope.MealsScreenHeaderRow(title: String, onBack: (() -> Unit)?, actions: @Composable RowScope.() -> Unit) {
    onBack?.let { StoresScreenBackButton(it) }
    StoresScreenTitle(title, Modifier.weight(1f).padding(start = 8.dp))
    actions()
}

/** Plain icon button in the primary color. */
@Composable
internal fun MealsScreenIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(imageVector = icon, contentDescription = description, tint = SuperCartColors.primaryGreen)
    }
}

/** Gray centered hint. */
@Composable
internal fun MealsScreenEmptyText(text: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(SuperCartSpacing.xl), contentAlignment = Alignment.Center) {
        Text(text = text, color = SuperCartColors.gray, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Checkbox in the primary color. */
@Composable
internal fun MealsScreenCheckbox(checked: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    val colors = CheckboxDefaults.colors(checkedColor = SuperCartColors.primaryGreen)
    Checkbox(checked = checked, onCheckedChange = { onToggle() }, enabled = enabled, colors = colors)
}
