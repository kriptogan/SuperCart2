package com.example.supercart2.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.supercart2.R
import com.example.supercart2.models.Grocery
import com.example.supercart2.ui.components.HierarchicalCategoryDisplay
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing
import com.example.supercart2.utils.localizedCategoryDisplayName

/** Scrolling part of the store detail, below the fixed header. */
@Composable
internal fun StoresScreenDetailContent(model: StoresScreenDetailModel, state: StoresScreenDetailState) {
    val topPadding = storesScreenHeaderTopPadding(model.mode, model.boughtCount)
    val md = SuperCartSpacing.md
    Box(modifier = Modifier.fillMaxSize().padding(top = topPadding, bottom = md, start = md, end = md)) {
        LazyColumn(modifier = Modifier.fillMaxSize()) { storesScreenDetailItems(model, state) }
    }
}

private fun LazyListScope.storesScreenDetailItems(model: StoresScreenDetailModel, state: StoresScreenDetailState) {
    if (model.mode == StoreMode.Edit) storesScreenEditModeItems(model, state)
    item { StoresScreenToBuySection(model, state) }
    item { Spacer(modifier = Modifier.height(SuperCartSpacing.lg)) }
    item { StoresScreenBoughtSection(model, state) }
    item { Spacer(modifier = Modifier.height(80.dp)) } // room for the floating add button
}

/** Edit mode: store info card first, then the search row. */
private fun LazyListScope.storesScreenEditModeItems(model: StoresScreenDetailModel, state: StoresScreenDetailState) {
    item { StoresScreenStoreInfoItem(model.storeId, state) }
    item { StoresScreenSearchRow(state, buyingLayout = false) }
}

@Composable
private fun StoresScreenStoreInfoItem(storeId: String, state: StoresScreenDetailState) {
    val savedText = stringResource(R.string.done)
    StoreInfoCard(state, onConfirm = { state.confirmStoreInfo(storeId, savedText) })
    Spacer(modifier = Modifier.height(SuperCartSpacing.sm))
}

@Composable
private fun StoresScreenToBuySection(model: StoresScreenDetailModel, state: StoresScreenDetailState) {
    if (model.toBuy.isNotEmpty()) {
        if (model.mode == StoreMode.Buying) StoresScreenSectionLabel(R.string.things_to_buy, model.toBuyCount)
        StoresScreenToBuyTree(model, state)
    } else if (model.bought.isEmpty()) {
        StoresScreenNoItemsCard()
    }
}

@Composable
private fun StoresScreenToBuyTree(model: StoresScreenDetailModel, state: StoresScreenDetailState) {
    HierarchicalCategoryDisplay(
        categories = model.toBuy, searchQuery = state.searchQuery, isAllExpanded = state.isAllExpanded,
        onEditGrocery = { state.openGroceryEdit(it) }, useScroll = false,
        isShoppingList = model.mode == StoreMode.Buying,
    )
}

@Composable
private fun StoresScreenNoItemsCard() {
    val cardModifier = Modifier.fillMaxWidth().padding(vertical = SuperCartSpacing.sm)
    Card(modifier = cardModifier, colors = storesScreenFaintCardColors()) {
        val text = stringResource(R.string.no_items_to_buy)
        Text(text = text, modifier = Modifier.padding(SuperCartSpacing.md), color = SuperCartColors.gray)
    }
}

@Composable
private fun StoresScreenSectionLabel(titleRes: Int, count: Int) {
    val rowModifier = Modifier.fillMaxWidth().padding(vertical = SuperCartSpacing.sm)
    Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
        StoresScreenSectionTexts(titleRes, count)
    }
}

/** Title, gap and "(count)", emitted into the label row. */
@Composable
private fun StoresScreenSectionTexts(titleRes: Int, count: Int) {
    val green = SuperCartColors.primaryGreen
    Text(text = stringResource(titleRes), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = green)
    Spacer(modifier = Modifier.width(SuperCartSpacing.sm))
    Text(text = "($count)", fontSize = 16.sp, color = SuperCartColors.gray)
}

/** Buying mode only: the bought groceries as removable cards. */
@Composable
private fun StoresScreenBoughtSection(model: StoresScreenDetailModel, state: StoresScreenDetailState) {
    if (model.mode != StoreMode.Buying || model.bought.isEmpty()) return
    StoresScreenSectionLabel(R.string.already_bought, model.boughtCount)
    StoresScreenBoughtCards(model, state)
}

@Composable
private fun StoresScreenBoughtCards(model: StoresScreenDetailModel, state: StoresScreenDetailState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        storesScreenBoughtEntries(model.bought).forEach { (categoryName, grocery) ->
            DetailBoughtGroceryCard(grocery, categoryName, onRemove = { state.unmarkBought(grocery) })
        }
    }
}

@Composable
private fun DetailBoughtGroceryCard(grocery: Grocery, categoryName: String, onRemove: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = storesScreenFaintCardColors()) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            StoresScreenBoughtTexts(grocery.name, localizedCategoryDisplayName(categoryName), Modifier.weight(1f))
            StoresScreenRemoveBoughtButton(onRemove)
        }
    }
}

@Composable
private fun StoresScreenBoughtTexts(groceryName: String, categoryName: String, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(text = groceryName, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Text(text = categoryName, fontSize = 12.sp, color = SuperCartColors.gray)
    }
}

@Composable
private fun StoresScreenRemoveBoughtButton(onRemove: () -> Unit) {
    IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
        val description = stringResource(R.string.remove_from_list)
        Icon(Icons.Default.Close, description, tint = Color.Red, modifier = Modifier.size(20.dp))
    }
}
