package com.example.supercart2.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.supercart2.R
import com.example.supercart2.data.CategoryWithSubCategories
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartShapes
import com.example.supercart2.ui.theme.SuperCartSpacing
import com.example.supercart2.utils.localizedCategoryDisplayName

/** Up/down reorder state and actions of one category card. */
internal class CategoriesManagementDialogMoves(
    val canMoveUp: Boolean,
    val canMoveDown: Boolean,
    val onMoveUp: () -> Unit,
    val onMoveDown: () -> Unit,
)

/** Category name, sub-category/grocery counts, reorder arrows and edit button. */
@Composable
internal fun CategoriesManagementDialogCard(
    item: CategoryWithSubCategories, moves: CategoriesManagementDialogMoves, onEditClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = SuperCartSpacing.sm),
        colors = CardDefaults.cardColors(containerColor = SuperCartColors.white),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), shape = SuperCartShapes.medium,
    ) { CategoriesManagementDialogCardContent(item, moves, onEditClick) }
}

@Composable
private fun CategoriesManagementDialogCardContent(
    item: CategoryWithSubCategories, moves: CategoriesManagementDialogMoves, onEditClick: () -> Unit,
) {
    val name = localizedCategoryDisplayName(item.category.name)
    Column(modifier = Modifier.padding(SuperCartSpacing.md)) {
        Text(text = name, style = MaterialTheme.typography.titleLarge, color = SuperCartColors.black)
        Spacer(modifier = Modifier.height(SuperCartSpacing.sm))
        CategoriesManagementDialogCardFooter(item, moves, onEditClick)
    }
}

@Composable
private fun CategoriesManagementDialogCardFooter(
    item: CategoryWithSubCategories, moves: CategoriesManagementDialogMoves, onEditClick: () -> Unit,
) {
    EditCategoryDialogSpacedRow {
        CategoriesManagementDialogCounts(item)
        CategoriesManagementDialogActions(moves, onEditClick)
    }
}

@Composable
private fun CategoriesManagementDialogCounts(item: CategoryWithSubCategories) {
    Column {
        CategoriesManagementDialogCountText(stringResource(R.string.sub_categories_count, item.subCategories.size))
        CategoriesManagementDialogCountText(stringResource(R.string.groceries_count, item.getGroceriesCount()))
    }
}

@Composable
private fun CategoriesManagementDialogCountText(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = SuperCartColors.darkGray)
}

/** Down, up and edit buttons packed closely together (slightly overlapping). */
@Composable
private fun CategoriesManagementDialogActions(moves: CategoriesManagementDialogMoves, onEditClick: () -> Unit) {
    val editDescription = stringResource(R.string.edit_category)
    Row(horizontalArrangement = Arrangement.spacedBy((-4).dp), verticalAlignment = Alignment.CenterVertically) {
        CategoriesManagementDialogArrows(moves)
        CategoriesManagementDialogIconButton(Icons.Default.Edit, editDescription, enabled = true, onClick = onEditClick)
    }
}

@Composable
private fun CategoriesManagementDialogArrows(moves: CategoriesManagementDialogMoves) {
    CategoriesManagementDialogIconButton(Icons.Default.KeyboardArrowDown, stringResource(R.string.move_down),
        moves.canMoveDown, moves.onMoveDown)
    CategoriesManagementDialogIconButton(Icons.Default.KeyboardArrowUp, stringResource(R.string.move_up),
        moves.canMoveUp, moves.onMoveUp)
}

/** 40dp icon button with a 20dp icon, green when enabled and grey otherwise. */
@Composable
private fun CategoriesManagementDialogIconButton(
    icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(40.dp)) {
        val tint = if (enabled) SuperCartColors.primaryGreen else SuperCartColors.gray
        Icon(imageVector = icon, contentDescription = description, tint = tint, modifier = Modifier.size(20.dp))
    }
}
