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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.supercart2.R
import com.example.supercart2.data.CategoryWithSubCategories
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.models.Grocery
import com.example.supercart2.models.Store
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing

internal data class StoreCardData(
    val storeId: String,
    val storeName: String,
    val itemCount: Int,
    val uniqueCount: Int
)

/**
 * Stores (in the given order) having at least one non-deleted shopping-list grocery, with the number of those
 * groceries and how many are linked to that store only. Groceries of deleted categories or sub-categories are ignored.
 */
internal fun storesScreenActiveStores(
    categories: List<CategoryWithSubCategories>, stores: List<Store>,
): List<StoreCardData> {
    val shoppingGroceries = categories.filter { !it.category.deleted }.flatMap { it.subCategories }
        .filter { !it.subCategory.deleted }.flatMap { it.groceries }.filter { it.inShoppingList && !it.deleted }
    return stores.mapNotNull { store -> storesScreenCardData(store, shoppingGroceries) }
}

private fun storesScreenCardData(store: Store, shoppingGroceries: List<Grocery>): StoreCardData? {
    val storeGroceries = shoppingGroceries.filter { store.uuid in it.storeIds }
    if (storeGroceries.isEmpty()) return null
    return StoreCardData(store.uuid, store.name, storeGroceries.size, storeGroceries.count { it.storeIds.size == 1 })
}

@Composable
internal fun StoresSelectStoreView(onStoreSelected: (String) -> Unit, onBack: () -> Unit) {
    val version = DataManagerObject.version
    val activeStores = remember(version) { storesScreenCurrentActiveStores() }
    Box(modifier = Modifier.fillMaxSize().background(SuperCartColors.lightGreen)) {
        StoresScreenSelectStoreHeader(onBack)
        StoresScreenStoreList(activeStores, onStoreSelected)
    }
}

private fun storesScreenCurrentActiveStores(): List<StoreCardData> =
    storesScreenActiveStores(DataManagerObject.categories, DataManagerObject.getSortedStores())

@Composable
private fun StoresScreenSelectStoreHeader(onBack: () -> Unit) {
    StoresScreenHeaderColumn {
        StoresScreenTitleRow(bottomPadding = SuperCartSpacing.md) {
            StoresScreenBackButton(onBack)
            StoresScreenTitle(stringResource(R.string.select_a_store), Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun StoresScreenStoreList(stores: List<StoreCardData>, onStoreSelected: (String) -> Unit) {
    val modifier = Modifier.fillMaxSize().padding(top = 100.dp, start = SuperCartSpacing.md, end = SuperCartSpacing.md)
    Box(modifier = modifier) {
        if (stores.isEmpty()) StoresScreenNoStores() else StoresScreenStoreCards(stores, onStoreSelected)
    }
}

@Composable
private fun StoresScreenNoStores() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val style = MaterialTheme.typography.bodyLarge
        Text(text = stringResource(R.string.no_stores_yet), color = SuperCartColors.gray, style = style)
    }
}

@Composable
private fun StoresScreenStoreCards(stores: List<StoreCardData>, onStoreSelected: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(SuperCartSpacing.sm)) {
        items(stores.size) { index ->
            StoreSelectionCard(storeData = stores[index], onClick = { onStoreSelected(stores[index].storeId) })
        }
    }
}

@Composable
private fun StoreSelectionCard(storeData: StoreCardData, onClick: () -> Unit) {
    Card(
        onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SuperCartColors.white),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) { StoresScreenStoreCardRow(storeData) }
}

@Composable
private fun StoresScreenStoreCardRow(storeData: StoreCardData) {
    val rowModifier = Modifier.fillMaxWidth().padding(horizontal = SuperCartSpacing.lg, vertical = SuperCartSpacing.md)
    Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
        StoresScreenStoreBadge()
        StoresScreenStoreSummary(storeData, Modifier.weight(1f))
        Icon(Icons.Default.ShoppingCart, null, tint = SuperCartColors.primaryGreen, modifier = Modifier.size(22.dp))
    }
}

/** Round store icon followed by its gap, emitted as separate children of the card row. */
@Composable
private fun StoresScreenStoreBadge() {
    val badgeModifier = Modifier.size(48.dp).background(SuperCartColors.primaryGreen.copy(alpha = 0.12f), CircleShape)
    Box(modifier = badgeModifier, contentAlignment = Alignment.Center) {
        Icon(Icons.Default.Store, null, tint = SuperCartColors.primaryGreen, modifier = Modifier.size(26.dp))
    }
    Spacer(modifier = Modifier.width(SuperCartSpacing.md))
}

@Composable
private fun StoresScreenStoreSummary(storeData: StoreCardData, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(storeData.storeName, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = SuperCartColors.black)
        Spacer(modifier = Modifier.height(4.dp))
        StoresScreenStoreStats(storeData)
    }
}

@Composable
private fun StoresScreenStoreStats(storeData: StoreCardData) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StoreStatChip(label = stringResource(R.string.items_count, storeData.itemCount))
        StoreStatChip(label = stringResource(R.string.unique_items_count, storeData.uniqueCount))
    }
}

@Composable
private fun StoreStatChip(label: String) {
    val chipModifier = Modifier.background(SuperCartColors.primaryGreen.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
        .padding(horizontal = 8.dp, vertical = 3.dp)
    Box(modifier = chipModifier) {
        Text(text = label, fontSize = 12.sp, color = SuperCartColors.primaryGreen, fontWeight = FontWeight.Medium)
    }
}
