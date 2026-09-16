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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.supercart2.R
import com.example.supercart2.data.CategoryWithSubCategories
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.models.Grocery
import com.example.supercart2.ui.components.HierarchicalCategoryDisplay
import com.example.supercart2.ui.components.StoreBasedDisplay
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing
import com.example.supercart2.utils.localizedCategoryDisplayName
import kotlinx.coroutines.CoroutineScope

/** Scrollable list below the fixed header: to-buy section, bought section and the finish button. */
@Composable
internal fun ShoppingListScreenContent(
    state: ShoppingListScreenState,
    onNavigateToStore: (String) -> Unit,
) {
    val lists = ShoppingListScreenRememberLists(state.searchQuery)
    val scope = rememberCoroutineScope()
    // Fixed offset below the header (taller in store view because of the hide-stores button)
    val top = if (state.isStoreView) 280.dp else 230.dp
    val modifier = Modifier.fillMaxSize()
        .padding(top = top, bottom = SuperCartSpacing.md, start = SuperCartSpacing.md, end = SuperCartSpacing.md)
    Box(modifier = modifier) { ShoppingListScreenList(state, lists, scope, onNavigateToStore) }
}

/** Recomputes the to-buy / bought trees whenever the data version or the search query changes. */
@Composable
private fun ShoppingListScreenRememberLists(searchQuery: String): ShoppingListScreenLists {
    val version = DataManagerObject.version
    return remember(version, searchQuery) {
        ShoppingListScreenLists.from(DataManagerObject.getSortedCategories(), searchQuery)
    }
}

@Composable
private fun ShoppingListScreenList(
    state: ShoppingListScreenState,
    lists: ShoppingListScreenLists,
    scope: CoroutineScope,
    onNavigateToStore: (String) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { ShoppingListScreenToBuySection(state, lists, onNavigateToStore) }
        item { Spacer(modifier = Modifier.height(SuperCartSpacing.lg)) }
        item { ShoppingListScreenBoughtSection(lists, scope) }
        if (lists.bought.isNotEmpty()) item { ShoppingListScreenFinishButton(state, lists.boughtCount) }
    }
}

@Composable
private fun ShoppingListScreenToBuySection(
    state: ShoppingListScreenState,
    lists: ShoppingListScreenLists,
    onNavigateToStore: (String) -> Unit,
) {
    if (lists.toBuy.isNotEmpty()) {
        ShoppingListScreenSectionHeader(stringResource(R.string.things_to_buy), lists.toBuyCount)
        ShoppingListScreenToBuyDisplay(state, lists.toBuy, onNavigateToStore)
    } else {
        ShoppingListScreenEmptyCard(stringResource(R.string.no_items_to_buy))
    }
}

@Composable
private fun ShoppingListScreenToBuyDisplay(
    state: ShoppingListScreenState,
    categories: List<CategoryWithSubCategories>,
    onNavigateToStore: (String) -> Unit,
) {
    when (state.viewMode) {
        ShoppingListViewMode.CATEGORY -> ShoppingListScreenCategoryDisplay(state, categories, onNavigateToStore)
        ShoppingListViewMode.STORE -> ShoppingListScreenStoreDisplay(state, categories, onNavigateToStore)
    }
}

@Composable
private fun ShoppingListScreenCategoryDisplay(
    state: ShoppingListScreenState,
    categories: List<CategoryWithSubCategories>,
    onNavigateToStore: (String) -> Unit,
) {
    HierarchicalCategoryDisplay(
        categories = categories, searchQuery = state.searchQuery, isAllExpanded = state.isAllExpanded,
        onEditGrocery = { state.editGrocery(it) }, useScroll = false, isShoppingList = true,
        onNavigateToStore = onNavigateToStore,
    )
}

@Composable
private fun ShoppingListScreenStoreDisplay(
    state: ShoppingListScreenState,
    categories: List<CategoryWithSubCategories>,
    onNavigateToStore: (String) -> Unit,
) {
    StoreBasedDisplay(
        categories = categories, searchQuery = state.searchQuery, isAllExpanded = state.isAllExpanded,
        onEditGrocery = { state.editGrocery(it) }, useScroll = false, onNavigateToStore = onNavigateToStore,
    )
}

@Composable
private fun ShoppingListScreenSectionHeader(title: String, count: Int) {
    Row(modifier = ShoppingListScreenSectionHeaderModifier, verticalAlignment = Alignment.CenterVertically) {
        Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SuperCartColors.primaryGreen)
        Spacer(modifier = Modifier.width(SuperCartSpacing.sm))
        Text(text = "($count)", fontSize = 16.sp, fontWeight = FontWeight.Normal, color = SuperCartColors.gray)
    }
}

private val ShoppingListScreenSectionHeaderModifier = Modifier.fillMaxWidth().padding(vertical = SuperCartSpacing.sm)

@Composable
private fun ShoppingListScreenEmptyCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = SuperCartSpacing.sm),
        colors = CardDefaults.cardColors(containerColor = SuperCartColors.lightGreen.copy(alpha = 0.1f)),
    ) { Text(text = text, modifier = Modifier.padding(SuperCartSpacing.md), color = SuperCartColors.gray) }
}

@Composable
private fun ShoppingListScreenBoughtSection(lists: ShoppingListScreenLists, scope: CoroutineScope) {
    if (lists.bought.isNotEmpty()) {
        ShoppingListScreenSectionHeader(stringResource(R.string.already_bought), lists.boughtCount)
        ShoppingListScreenBoughtList(lists.boughtEntries, scope)
    } else {
        ShoppingListScreenEmptyCard(stringResource(R.string.no_bought_items))
    }
}

/** Flat list of bought groceries (no category grouping). */
@Composable
private fun ShoppingListScreenBoughtList(entries: List<Pair<Grocery, String>>, scope: CoroutineScope) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        entries.forEach { (grocery, categoryName) ->
            val onRemove = { ShoppingListScreenActions.unmarkBought(grocery, scope) }
            ShoppingListScreenBoughtGroceryCard(grocery, localizedCategoryDisplayName(categoryName), onRemove)
        }
    }
}

@Composable
private fun ShoppingListScreenBoughtGroceryCard(grocery: Grocery, categoryName: String, onRemove: () -> Unit) {
    val background = SuperCartColors.lightGreen.copy(alpha = 0.1f)
    val modifier = Modifier.fillMaxWidth().background(background, shape = RoundedCornerShape(6.dp))
        .padding(horizontal = 12.dp, vertical = 8.dp)
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = background)) {
        ShoppingListScreenBoughtGroceryRow(grocery.name, categoryName, onRemove)
    }
}

@Composable
private fun ShoppingListScreenBoughtGroceryRow(name: String, categoryName: String, onRemove: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        ShoppingListScreenBoughtGroceryLabels(name, categoryName)
        IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Close, stringResource(R.string.remove_from_list), Modifier.size(20.dp), tint = Color.Red)
        }
    }
}

@Composable
private fun RowScope.ShoppingListScreenBoughtGroceryLabels(name: String, categoryName: String) {
    Column(modifier = Modifier.weight(1f)) {
        Text(text = name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Text(text = categoryName, fontSize = 12.sp, color = SuperCartColors.gray)
    }
}

@Composable
private fun ShoppingListScreenFinishButton(state: ShoppingListScreenState, boughtCount: Int) {
    Spacer(modifier = Modifier.height(SuperCartSpacing.lg))
    val text = stringResource(R.string.finish_shopping_button, boughtCount)
    Button(
        onClick = { state.showFinishConfirmation = true }, modifier = ShoppingListScreenFinishButtonModifier,
        colors = ButtonDefaults.buttonColors(containerColor = SuperCartColors.primaryGreen),
    ) { ShoppingListScreenIconLabel(Icons.Default.CheckCircle, text, MaterialTheme.typography.bodyLarge) }
}

private val ShoppingListScreenFinishButtonModifier = Modifier.fillMaxWidth()
    .padding(horizontal = SuperCartSpacing.md).padding(bottom = SuperCartSpacing.xxl)
