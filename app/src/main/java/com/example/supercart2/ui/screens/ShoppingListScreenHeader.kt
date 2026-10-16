package com.example.supercart2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.example.supercart2.R
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.ui.components.BurgerMenu
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Fixed header: back/menu/date/add row, search + expand toggle, view-mode and hide-stores buttons. */
@Composable
internal fun ShoppingListScreenHeader(state: ShoppingListScreenState, onBack: (() -> Unit)?) {
    Column(modifier = ShoppingListScreenHeaderModifier()) {
        ShoppingListScreenTopRow(state, onBack)
        ShoppingListScreenSearchRow(state)
        ShoppingListScreenViewModeButton(state)
        if (state.isStoreView) ShoppingListScreenHideStoresButton { state.showHideStoresDialog = true }
    }
}

/** Not a constant: the background follows the selected color palette. */
private fun ShoppingListScreenHeaderModifier() = Modifier.fillMaxWidth().background(SuperCartColors.lightGreen)
    .padding(start = SuperCartSpacing.md, end = SuperCartSpacing.md, top = SuperCartSpacing.xl)

@Composable
private fun ShoppingListScreenTopRow(state: ShoppingListScreenState, onBack: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = SuperCartSpacing.md),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
    ) { ShoppingListScreenTopRowItems(state, onBack) }
}

@Composable
private fun ShoppingListScreenTopRowItems(state: ShoppingListScreenState, onBack: (() -> Unit)?) {
    if (onBack != null) ShoppingListScreenBackButton(onBack)
    ShoppingListScreenMenu(state, hasBack = onBack != null)
    ShoppingListScreenDate()
    ShoppingListScreenAddButton { state.createGrocery() }
}

/** Today's date as yyyy-MM-dd. */
@Composable
private fun ShoppingListScreenDate() {
    Text(
        text = LocalDate.now().toString(), style = MaterialTheme.typography.titleMedium, color = Color.Black,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@Composable
private fun ShoppingListScreenBackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = SuperCartColors.black)
    }
}

@Composable
private fun ShoppingListScreenMenu(state: ShoppingListScreenState, hasBack: Boolean) {
    Box(modifier = Modifier.padding(start = if (hasBack) 0.dp else 5.dp)) { ShoppingListScreenBurgerMenu(state) }
}

@Composable
private fun ShoppingListScreenBurgerMenu(state: ShoppingListScreenState) {
    BurgerMenu(
        onCategoriesManagementClick = { state.showCategoriesManagement = true },
        onManageStoresClick = { state.showStoresManagement = true }, onSettingsClick = { state.showSettings = true },
        onManageGroupClick = { state.showGroupManagement = true },
        onImportGroceriesClick = { state.showImportGroceries = true },
    )
}

@Composable
private fun ShoppingListScreenAddButton(onClick: () -> Unit) {
    Box(modifier = Modifier.padding(end = 12.dp)) { ShoppingListScreenAddCard(onClick) }
}

@Composable
private fun ShoppingListScreenAddCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.size(56.dp), shape = CircleShape,
        colors = CardDefaults.cardColors(containerColor = SuperCartColors.primaryGreen),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) { ShoppingListScreenAddIconButton(onClick) }
}

@Composable
private fun ShoppingListScreenAddIconButton(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
        val description = stringResource(R.string.add_new_grocery)
        Icon(Icons.Default.Add, description, Modifier.size(34.dp), tint = SuperCartColors.white)
    }
}

@Composable
private fun ShoppingListScreenSearchRow(state: ShoppingListScreenState) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = SuperCartSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) { ShoppingListScreenSearchRowItems(state) }
}

@Composable
private fun ShoppingListScreenSearchRowItems(state: ShoppingListScreenState) {
    Spacer(modifier = Modifier.width(15.dp)) // aligns with the burger icon
    ShoppingListScreenSearchField(state.searchQuery) { state.searchQuery = it }
    Spacer(modifier = Modifier.width(SuperCartSpacing.sm))
    ShoppingListScreenExpandToggle(state.isAllExpanded) { state.isAllExpanded = !state.isAllExpanded }
}

@Composable
private fun ShoppingListScreenSearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query, onValueChange = onQueryChange, placeholder = { Text(stringResource(R.string.search_hint)) },
        modifier = Modifier.fillMaxWidth(0.78f), colors = ShoppingListScreenSearchColors(), singleLine = true,
        leadingIcon = { ShoppingListScreenSearchIcon() },
        trailingIcon = ShoppingListScreenClearSearchIcon(query, onQueryChange),
    )
}

/** Clear button shown only while there is a query. */
private fun ShoppingListScreenClearSearchIcon(
    query: String,
    onQueryChange: (String) -> Unit,
): (@Composable () -> Unit)? =
    if (query.isNotEmpty()) ({ ShoppingListScreenClearSearchButton { onQueryChange("") } }) else null

@Composable
private fun ShoppingListScreenSearchColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SuperCartColors.primaryGreen, unfocusedBorderColor = SuperCartColors.gray,
    focusedLabelColor = SuperCartColors.primaryGreen,
    focusedTextColor = SuperCartColors.black, unfocusedTextColor = SuperCartColors.black,
    focusedContainerColor = SuperCartColors.white, unfocusedContainerColor = SuperCartColors.white,
)

@Composable
private fun ShoppingListScreenSearchIcon() {
    Icon(Icons.Default.Search, stringResource(R.string.search), Modifier.size(24.dp), tint = SuperCartColors.gray)
}

@Composable
private fun ShoppingListScreenClearSearchButton(onClear: () -> Unit) {
    IconButton(onClick = onClear, modifier = Modifier.size(40.dp)) {
        val description = stringResource(R.string.clear_search)
        Icon(Icons.Default.Clear, description, Modifier.size(20.dp), tint = SuperCartColors.darkGray)
    }
}

@Composable
private fun ShoppingListScreenExpandToggle(isAllExpanded: Boolean, onToggle: () -> Unit) {
    Card(
        modifier = Modifier.height(56.dp).width(56.dp), shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SuperCartColors.white),
        border = CardDefaults.outlinedCardBorder(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) { ShoppingListScreenExpandIconButton(isAllExpanded, onToggle) }
}

@Composable
private fun ShoppingListScreenExpandIconButton(isAllExpanded: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle, modifier = Modifier.fillMaxSize()) { ShoppingListScreenExpandIcon(isAllExpanded) }
}

@Composable
private fun ShoppingListScreenExpandIcon(isAllExpanded: Boolean) {
    val icon = if (isAllExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
    val description = stringResource(if (isAllExpanded) R.string.collapse_all else R.string.expand_all)
    Icon(icon, description, Modifier.size(28.dp), tint = SuperCartColors.primaryGreen)
}

/** Toggles category/store view and persists the choice. */
@Composable
private fun ShoppingListScreenViewModeButton(state: ShoppingListScreenState) {
    val onClick = ShoppingListScreenViewModeToggle(state)
    val icon = if (state.isStoreView) Icons.AutoMirrored.Filled.List else Icons.Default.ShoppingCart
    val textId = if (state.isStoreView) R.string.switch_to_category_view else R.string.switch_to_store_view
    val text = stringResource(textId)
    ShoppingListScreenHeaderButton(SuperCartColors.white, icon, text, onClick)
}

@Composable
private fun ShoppingListScreenViewModeToggle(state: ShoppingListScreenState): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return {
        state.toggleViewMode()
        scope.launch { DataStoreManager.saveStoreViewMode(context, state.isStoreView) }
    }
}

@Composable
private fun ShoppingListScreenHideStoresButton(onClick: () -> Unit) {
    val hiddenCount = remember(DataManagerObject.version) { DataManagerObject.hiddenStoreIds.size }
    val text = if (hiddenCount > 0) {
        stringResource(R.string.show_hide_stores_hidden, hiddenCount)
    } else stringResource(R.string.show_hide_stores)
    val icon = if (hiddenCount > 0) Icons.Default.FavoriteBorder else Icons.Default.Favorite
    ShoppingListScreenHeaderButton(SuperCartColors.primaryGreen.copy(alpha = 0.1f), icon, text, onClick)
}

@Composable
private fun ShoppingListScreenHeaderButton(
    containerColor: Color,
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick, modifier = Modifier.fillMaxWidth().padding(horizontal = SuperCartSpacing.md),
        colors = ButtonDefaults.buttonColors(containerColor, contentColor = SuperCartColors.primaryGreen),
        shape = RoundedCornerShape(12.dp), elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
    ) { ShoppingListScreenIconLabel(icon, text) }
}

/** Icon (20dp), 8dp gap and label: the content of the screen's full-width buttons. */
@Composable
internal fun RowScope.ShoppingListScreenIconLabel(
    icon: ImageVector,
    text: String,
    style: TextStyle = LocalTextStyle.current,
) {
    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
    Spacer(modifier = Modifier.width(8.dp))
    Text(text = text, style = style)
}
