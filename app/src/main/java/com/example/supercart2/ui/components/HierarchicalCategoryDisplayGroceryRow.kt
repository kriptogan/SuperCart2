package com.example.supercart2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.models.Grocery
import com.example.supercart2.models.hasAlert
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.utils.localizedCategoryDisplayName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Everything a grocery row and its action buttons need. [grocery] is the live copy from DataManagerObject. */
internal class HierarchicalCategoryDisplayRowContext(
    val grocery: Grocery,
    val categoryName: String,
    val options: HierarchicalCategoryDisplayOptions,
    private val scope: CoroutineScope,
    val onEdit: () -> Unit,
    val onShowBuyHistory: () -> Unit,
) {
    /** Applies [mutation] to the in-memory state, then saves in the background. */
    fun mutateAndSave(mutation: () -> Unit) {
        mutation()
        scope.launch { DataStoreManager.saveDataGlobally() }
    }
}

@Composable
internal fun HierarchicalCategoryDisplayGroceryItem(
    grocery: Grocery, onEdit: () -> Unit, options: HierarchicalCategoryDisplayOptions,
) {
    val scope = rememberCoroutineScope()
    var showBuyHistory by remember { mutableStateOf(false) }
    val row = rowContext(grocery, options, scope, onEdit) { showBuyHistory = true }
    GroceryRow(row)
    if (showBuyHistory) HierarchicalCategoryDisplayBuyHistoryDialog(row.grocery) { showBuyHistory = false }
}

@Composable
private fun rowContext(
    grocery: Grocery, options: HierarchicalCategoryDisplayOptions, scope: CoroutineScope,
    onEdit: () -> Unit, onShowBuyHistory: () -> Unit,
): HierarchicalCategoryDisplayRowContext {
    val current = rememberCurrentGrocery(grocery)
    val categoryName = remember(current.categoryId) { categoryNameOf(current.categoryId) }
    return HierarchicalCategoryDisplayRowContext(current, categoryName, options, scope, onEdit, onShowBuyHistory)
}

/** The grocery as currently stored in DataManagerObject (falls back to [grocery]); refreshed on every data change. */
@Composable
private fun rememberCurrentGrocery(grocery: Grocery): Grocery {
    val version = DataManagerObject.version
    return remember(grocery.uuid, version) {
        android.util.Log.d("datastore test", "Recomputing grocery state for ${grocery.name}, version: $version")
        DataManagerObject.categories.asSequence().flatMap { it.subCategories }.flatMap { it.groceries }
            .find { it.uuid == grocery.uuid } ?: grocery
    }
}

private fun categoryNameOf(categoryId: String): String =
    DataManagerObject.categories.find { it.category.uuid == categoryId }?.category?.name ?: ""

@Composable
private fun GroceryRow(row: HierarchicalCategoryDisplayRowContext) {
    val bought = row.options.isShoppingList && row.grocery.isBought
    val modifier = groceryRowModifier(bought, rememberHasAlert(row.grocery, row.options))
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (bought) BoughtGroceryContent(row) else PendingGroceryContent(row)
    }
}

/** Alerts are only highlighted in the catalogue, and only when enabled. */
@Composable
private fun rememberHasAlert(grocery: Grocery, options: HierarchicalCategoryDisplayOptions): Boolean =
    remember(grocery, options.isShoppingList, options.showAlerts) {
        !options.isShoppingList && options.showAlerts && grocery.hasAlert()
    }

private val groceryRowShape = RoundedCornerShape(8.dp)

/** Bought rows (shopping list only) get a light tint; rows with an alert get a red border. */
private fun groceryRowModifier(bought: Boolean, hasAlert: Boolean): Modifier = Modifier
    .fillMaxWidth()
    .padding(horizontal = 16.dp, vertical = 12.dp)
    .background(if (bought) SuperCartColors.lightGreen.copy(alpha = 0.1f) else Color(0xFFF8F9FA), groceryRowShape)
    .then(if (hasAlert) Modifier.border(2.dp, Color(0xFFE53935), groceryRowShape) else Modifier)
    .padding(horizontal = 12.dp, vertical = 8.dp)

@Composable
private fun RowScope.BoughtGroceryContent(row: HierarchicalCategoryDisplayRowContext) {
    Column(modifier = Modifier.weight(1f)) {
        GroceryName(row.grocery.name)
        Text(text = localizedCategoryDisplayName(row.categoryName), fontSize = 12.sp, color = SuperCartColors.gray)
    }
    HierarchicalCategoryDisplayMarkNotBoughtButton(row)
}

@Composable
private fun RowScope.PendingGroceryContent(row: HierarchicalCategoryDisplayRowContext) {
    Column(modifier = Modifier.weight(1f)) {
        GroceryName(row.grocery.name)
        if (row.grocery.storeIds.isNotEmpty()) StoreChips(row.grocery.storeIds, row.options.onNavigateToStore)
    }
    if (row.options.isShoppingList) HierarchicalCategoryDisplayShoppingActions(row)
    else HierarchicalCategoryDisplayCatalogueActions(row)
}

@Composable
private fun GroceryName(name: String) {
    Text(text = name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StoreChips(storeIds: List<String>, onNavigateToStore: (storeId: String) -> Unit) {
    Spacer(modifier = Modifier.height(4.dp))
    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        storeIds.forEach { storeId -> StoreChip(storeId) { onNavigateToStore(storeId) } }
    }
}

@Composable
private fun StoreChip(storeId: String, onClick: () -> Unit) {
    val storeName = rememberStoreName(storeId)
    val alias = remember(storeName) { storeAlias(storeName) }
    AssistChip(
        onClick = onClick, label = { Text(text = alias, fontSize = 10.sp) }, modifier = Modifier.padding(end = 4.dp),
        colors = AssistChipDefaults.assistChipColors(containerColor = SuperCartColors.lightGray.copy(alpha = 0.3f)),
    )
}

/** Name of the linked store ("Unknown" when missing), refreshed on every data change. */
@Composable
private fun rememberStoreName(storeId: String): String {
    val version = DataManagerObject.version
    return remember(storeId, version) { DataManagerObject.stores.find { it.uuid == storeId }?.name ?: "Unknown" }
}

/** First 3 letters of a one-word name, otherwise the upper-cased initials of its first 3 words. */
private fun storeAlias(name: String): String {
    val words = name.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
    return if (words.size == 1) words[0].take(3)
    else words.take(3).joinToString("") { it.first().uppercaseChar().toString() }
}
