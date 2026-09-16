package com.example.supercart2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.supercart2.models.Store
import com.example.supercart2.ui.theme.SuperCartColors

data class StoreWithCategories(
    val store: Store?,  // null for "Not Linked" section
    val categories: List<CategoryWithSubCategories>
)

@Composable
fun StoreBasedDisplay(
    categories: List<CategoryWithSubCategories>,
    searchQuery: String,
    isAllExpanded: Boolean,
    onEditGrocery: (Grocery) -> Unit,
    modifier: Modifier = Modifier,
    useScroll: Boolean = true,
    onNavigateToStore: (storeId: String) -> Unit = {}
) {
    val version = DataManagerObject.version
    val storesWithCategories = remember(categories, version) { DataManagerObject.groupGroceriesByStore(categories) }
    val options = StoreBasedDisplayOptions(searchQuery, isAllExpanded, onEditGrocery, onNavigateToStore)
    if (useScroll) StoreLazyColumn(storesWithCategories, options, modifier)
    else StoreColumn(storesWithCategories, options, modifier)
}

/** Arguments forwarded to the category tree inside every store section. */
internal data class StoreBasedDisplayOptions(
    val searchQuery: String,
    val isAllExpanded: Boolean,
    val onEditGrocery: (Grocery) -> Unit,
    val onNavigateToStore: (storeId: String) -> Unit,
)

@Composable
private fun StoreLazyColumn(stores: List<StoreWithCategories>, options: StoreBasedDisplayOptions, modifier: Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(items = stores, key = { it.store?.uuid ?: "not-linked" }) { StoreSectionWithGap(it, options) }
    }
}

@Composable
private fun StoreColumn(stores: List<StoreWithCategories>, options: StoreBasedDisplayOptions, modifier: Modifier) {
    Column(modifier = modifier) {
        stores.forEach { StoreSectionWithGap(it, options) }
    }
}

@Composable
private fun StoreSectionWithGap(storeWithCategories: StoreWithCategories, options: StoreBasedDisplayOptions) {
    StoreSection(storeWithCategories, options)
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun StoreSection(storeWithCategories: StoreWithCategories, options: StoreBasedDisplayOptions) {
    var isExpanded by remember { mutableStateOf(true) }
    val totalItems = remember(storeWithCategories) { countGroceries(storeWithCategories.categories) }
    StoreCard {
        StoreHeader(storeWithCategories.store, totalItems, isExpanded) { isExpanded = !isExpanded }
        if (isExpanded) StoreCategories(storeWithCategories.categories, options)
    }
}

private fun countGroceries(categories: List<CategoryWithSubCategories>): Int =
    categories.sumOf { categoryWithSubs -> categoryWithSubs.subCategories.sumOf { it.groceries.size } }

@Composable
private fun StoreCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SuperCartColors.white),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) { Column(content = content) }
}

/** [store] is null for the "not linked" section. */
@Composable
private fun StoreHeader(store: Store?, totalItems: Int, isExpanded: Boolean, onToggle: () -> Unit) {
    HierarchicalCategoryDisplayHeaderRow(storeHeaderModifier(store == null, onToggle)) {
        StoreTitle(store, totalItems)
        HierarchicalCategoryDisplayExpandIcon(isExpanded)
    }
}

private fun storeHeaderModifier(notLinked: Boolean, onToggle: () -> Unit): Modifier =
    Modifier.fillMaxWidth().clickable(onClick = onToggle).background(storeHeaderColor(notLinked)).padding(16.dp)

private fun storeHeaderColor(notLinked: Boolean): Color =
    if (notLinked) SuperCartColors.lightGray.copy(alpha = 0.3f) else SuperCartColors.primaryGreen.copy(alpha = 0.1f)

@Composable
private fun StoreTitle(store: Store?, totalItems: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val icon = if (store == null) Icons.Default.ShoppingCart else Icons.Default.Place
        Icon(icon, contentDescription = null, tint = SuperCartColors.primaryGreen, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(12.dp))
        StoreNameAndCount(store, totalItems)
    }
}

@Composable
private fun StoreNameAndCount(store: Store?, totalItems: Int) {
    Column {
        val name = store?.name ?: stringResource(R.string.not_linked_to_store)
        val nameColor = if (store == null) SuperCartColors.gray else SuperCartColors.black
        Text(text = name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = nameColor)
        Text(stringResource(R.string.store_items_count, totalItems), fontSize = 12.sp, color = SuperCartColors.gray)
    }
}

@Composable
private fun StoreCategories(categories: List<CategoryWithSubCategories>, options: StoreBasedDisplayOptions) {
    Divider(color = SuperCartColors.lightGray)
    HierarchicalCategoryDisplay(
        categories = categories, searchQuery = options.searchQuery, isAllExpanded = options.isAllExpanded,
        onEditGrocery = options.onEditGrocery, useScroll = false, isShoppingList = true,
        modifier = Modifier.padding(8.dp), onNavigateToStore = options.onNavigateToStore,
    )
}
