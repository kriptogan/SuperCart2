package com.example.supercart2.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.supercart2.R
import com.example.supercart2.models.Category
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartShapes
import com.example.supercart2.ui.theme.SuperCartSpacing
import com.example.supercart2.utils.localizedCategoryDisplayName

/** Whether a row can move up/down and what moving does. */
internal class StoreCategoryOrderDialogMoves(
    val canMoveUp: Boolean, val canMoveDown: Boolean, val onMoveUp: () -> Unit, val onMoveDown: () -> Unit,
)

@Composable
internal fun StoreCategoryOrderDialogCategoryCard(category: Category, moves: StoreCategoryOrderDialogMoves) {
    StoreCategoryOrderDialogCard(startPadding = 0.dp, containerColor = SuperCartColors.white, elevation = 2.dp) {
        StoreCategoryOrderDialogRow(verticalPadding = SuperCartSpacing.sm) {
            StoreCategoryOrderDialogCategoryName(category.name, Modifier.weight(1f))
            StoreCategoryOrderDialogArrows(moves, compact = false)
        }
    }
}

/** Smaller, indented row for a sub-category. */
@Composable
internal fun StoreCategoryOrderDialogSubCategoryCard(name: String, moves: StoreCategoryOrderDialogMoves) {
    StoreCategoryOrderDialogCard(20.dp, SuperCartColors.lightGreen.copy(alpha = 0.3f), elevation = 1.dp) {
        StoreCategoryOrderDialogRow(verticalPadding = 6.dp) {
            StoreCategoryOrderDialogSubCategoryName(name, Modifier.weight(1f))
            StoreCategoryOrderDialogArrows(moves, compact = true)
        }
    }
}

@Composable
private fun StoreCategoryOrderDialogCard(
    startPadding: Dp, containerColor: Color, elevation: Dp, content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(start = startPadding, bottom = 4.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation), shape = SuperCartShapes.medium,
        content = content,
    )
}

@Composable
private fun StoreCategoryOrderDialogRow(verticalPadding: Dp, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = SuperCartSpacing.md, vertical = verticalPadding),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun StoreCategoryOrderDialogCategoryName(name: String, modifier: Modifier) {
    Text(
        text = localizedCategoryDisplayName(name), style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold, color = SuperCartColors.primaryGreen, modifier = modifier,
    )
}

@Composable
private fun StoreCategoryOrderDialogSubCategoryName(name: String, modifier: Modifier) {
    Text(
        text = name, style = MaterialTheme.typography.bodyMedium, color = SuperCartColors.black, modifier = modifier,
    )
}

@Composable
private fun StoreCategoryOrderDialogArrows(
    moves: StoreCategoryOrderDialogMoves, compact: Boolean,
) {
    Row(Modifier, Arrangement.spacedBy(SuperCartSpacing.xs), Alignment.CenterVertically) {
        val up = Icons.Default.KeyboardArrowUp
        StoreCategoryOrderDialogArrow(up, R.string.move_up, moves.canMoveUp, compact, moves.onMoveUp)
        val down = Icons.Default.KeyboardArrowDown
        StoreCategoryOrderDialogArrow(down, R.string.move_down, moves.canMoveDown, compact, moves.onMoveDown)
    }
}

/** Arrow icon button, green when enabled and gray when disabled; [compact] shrinks it for sub-category rows. */
@Composable
private fun StoreCategoryOrderDialogArrow(
    icon: ImageVector, @StringRes description: Int, enabled: Boolean, compact: Boolean, onClick: () -> Unit,
) {
    val buttonModifier = if (compact) Modifier.size(36.dp) else Modifier
    val iconModifier = if (compact) Modifier.size(18.dp) else Modifier
    IconButton(onClick = onClick, enabled = enabled, modifier = buttonModifier) {
        val tint = if (enabled) SuperCartColors.primaryGreen else SuperCartColors.gray
        Icon(imageVector = icon, contentDescription = stringResource(description), tint = tint, modifier = iconModifier)
    }
}
