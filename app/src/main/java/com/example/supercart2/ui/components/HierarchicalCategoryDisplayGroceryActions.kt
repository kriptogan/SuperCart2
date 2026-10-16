package com.example.supercart2.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.supercart2.R
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.ui.theme.SuperCartColors

/** Shopping list: options menu (remove / history / edit), then the "bought" check button. */
@Composable
internal fun HierarchicalCategoryDisplayShoppingActions(row: HierarchicalCategoryDisplayRowContext) {
    Box { ShoppingMenu(row) }
    Spacer(modifier = Modifier.width(8.dp))
    BoughtToggleButton(row)
}

@Composable
private fun BoughtToggleButton(row: HierarchicalCategoryDisplayRowContext) {
    val bought = row.grocery.isBought
    val description = stringResource(if (bought) R.string.mark_not_bought else R.string.mark_bought)
    SmallIconButton(Icons.Default.CheckCircle, description,
        if (bought) SuperCartColors.primaryGreen else Color.Gray) {
        row.mutateAndSave { DataManagerObject.toggleBoughtStatus(row.grocery.uuid) }
    }
}

/** Catalogue: options button and shopping-cart toggle side by side, menu (edit / history) anchored to both. */
@Composable
internal fun HierarchicalCategoryDisplayCatalogueActions(row: HierarchicalCategoryDisplayRowContext) {
    Box { CatalogueMenu(row) }
}

@Composable
internal fun HierarchicalCategoryDisplayMarkNotBoughtButton(row: HierarchicalCategoryDisplayRowContext) {
    SmallIconButton(Icons.Default.Close, stringResource(R.string.mark_not_bought), Color.Red) {
        row.mutateAndSave { DataManagerObject.updateGrocery(row.grocery.uuid) { it.copy(isBought = false) } }
    }
}

@Composable
private fun ShoppingMenu(row: HierarchicalCategoryDisplayRowContext) {
    var expanded by remember { mutableStateOf(false) }
    MoreOptionsButton { expanded = true }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        ShoppingMenuItems(row) { expanded = false }
    }
}

@Composable
private fun ShoppingMenuItems(row: HierarchicalCategoryDisplayRowContext, close: () -> Unit) {
    MenuItem(R.string.remove_from_shopping_list, Icons.Default.ShoppingCart) {
        row.mutateAndSave { DataManagerObject.updateGrocery(row.grocery.uuid) { it.copy(inShoppingList = false) } }
        close()
    }
    MenuItem(R.string.view_buy_history, Icons.Outlined.DateRange) { row.onShowBuyHistory(); close() }
    MenuItem(R.string.edit_item, Icons.Default.Edit) { row.onEdit(); close() }
}

@Composable
private fun CatalogueMenu(row: HierarchicalCategoryDisplayRowContext) {
    var expanded by remember { mutableStateOf(false) }
    CatalogueButtons(row) { expanded = true }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        MenuItem(R.string.edit_item, Icons.Default.Edit) { row.onEdit(); expanded = false }
        MenuItem(R.string.view_buy_history, Icons.Outlined.DateRange) { row.onShowBuyHistory(); expanded = false }
    }
}

@Composable
private fun CatalogueButtons(row: HierarchicalCategoryDisplayRowContext, onMoreOptions: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        MoreOptionsButton(onMoreOptions)
        ShoppingListToggleButton(row)
    }
}

@Composable
private fun ShoppingListToggleButton(row: HierarchicalCategoryDisplayRowContext) {
    val inList = row.grocery.inShoppingList
    val description = if (inList) R.string.remove_from_shopping_list else R.string.add_to_shopping_list_description
    SmallIconButton(Icons.Default.ShoppingCart, stringResource(description),
        if (inList) SuperCartColors.primaryGreen else Color.Black) {
        row.mutateAndSave { DataManagerObject.toggleShoppingListStatus(row.grocery.uuid) }
    }
}

@Composable
private fun MoreOptionsButton(onClick: () -> Unit) {
    val description = stringResource(R.string.more_options)
    SmallIconButton(Icons.Default.MoreVert, description, SuperCartColors.primaryGreen, onClick)
}

/** 24dp icon button holding a 20dp icon. */
@Composable
private fun SmallIconButton(icon: ImageVector, description: String, tint: Color, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(24.dp)) {
        Icon(imageVector = icon, contentDescription = description, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun MenuItem(textRes: Int, icon: ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(textRes), color = SuperCartColors.black) }, onClick = onClick,
        leadingIcon = { Icon(icon, null, tint = SuperCartColors.primaryGreen) },
    )
}
