package com.example.supercart2.ui.components

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.supercart2.R
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.models.Store
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing
import com.example.supercart2.ui.theme.SuperCartShapes
import com.example.supercart2.utils.localizedCategoryDisplayName
import kotlinx.coroutines.launch

@Composable
fun StoreCategoryOrderDialog(
    store: Store,
    onDismiss: () -> Unit,
    onOrderUpdated: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val version = DataManagerObject.version

    // All categories that have items in this store (for reordering purposes we show all non-deleted)
    val allCategories = remember(version) {
        DataManagerObject.getSortedCategories()
    }

    // Get current category order for this store
    val currentCategoryOrder = remember(version, allCategories) {
        val customOrder = DataManagerObject.getStoreCategoryOrder(store.uuid)
        if (customOrder != null) {
            val ordered = mutableListOf<com.example.supercart2.data.CategoryWithSubCategories>()
            val unordered = allCategories.toMutableList()
            customOrder.forEach { categoryId ->
                val category = unordered.find { it.category.uuid == categoryId }
                if (category != null) {
                    ordered.add(category)
                    unordered.remove(category)
                }
            }
            ordered.addAll(unordered.sortedBy { it.category.viewOrder })
            ordered
        } else {
            allCategories.sortedBy { it.category.viewOrder }
        }
    }

    // Local state: ordered list of category UUIDs
    var displayCategoryOrder by remember { mutableStateOf(currentCategoryOrder.map { it.category.uuid }) }

    // Local state: per-category sub-category order (Map<CategoryUUID, List<SubCategoryUUID>>)
    var displaySubCategoryOrders by remember {
        mutableStateOf(
            buildMap {
                currentCategoryOrder.forEach { catWithSubs ->
                    val customSubOrder = DataManagerObject.getStoreSubCategoryOrder(store.uuid, catWithSubs.category.uuid)
                    put(
                        catWithSubs.category.uuid,
                        customSubOrder ?: catWithSubs.subCategories.map { it.subCategory.uuid }
                    )
                }
            }
        )
    }

    LaunchedEffect(currentCategoryOrder) {
        displayCategoryOrder = currentCategoryOrder.map { it.category.uuid }
        val newSubOrders = buildMap {
            currentCategoryOrder.forEach { catWithSubs ->
                val customSubOrder = DataManagerObject.getStoreSubCategoryOrder(store.uuid, catWithSubs.category.uuid)
                put(
                    catWithSubs.category.uuid,
                    customSubOrder ?: catWithSubs.subCategories.map { it.subCategory.uuid }
                )
            }
        }
        displaySubCategoryOrders = newSubOrders
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxSize(),
        containerColor = SuperCartColors.lightGreen,
        title = {
            Text(
                text = stringResource(R.string.reorder_categories_for_store, store.name),
                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            if (allCategories.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_stores_yet),
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = SuperCartColors.gray
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(
                        items = displayCategoryOrder,
                        key = { _, id -> id }
                    ) { catIndex, categoryId ->
                        val catWithSubs = allCategories.find { it.category.uuid == categoryId }
                        if (catWithSubs != null) {
                            // Category row
                            CategoryOrderCard(
                                category = catWithSubs.category,
                                canMoveUp = catIndex > 0,
                                canMoveDown = catIndex < displayCategoryOrder.size - 1,
                                onMoveUp = {
                                    if (catIndex > 0) {
                                        val newOrder = displayCategoryOrder.toMutableList()
                                        val temp = newOrder[catIndex]
                                        newOrder[catIndex] = newOrder[catIndex - 1]
                                        newOrder[catIndex - 1] = temp
                                        displayCategoryOrder = newOrder
                                    }
                                },
                                onMoveDown = {
                                    if (catIndex < displayCategoryOrder.size - 1) {
                                        val newOrder = displayCategoryOrder.toMutableList()
                                        val temp = newOrder[catIndex]
                                        newOrder[catIndex] = newOrder[catIndex + 1]
                                        newOrder[catIndex + 1] = temp
                                        displayCategoryOrder = newOrder
                                    }
                                }
                            )

                            // Sub-category rows (if any)
                            val subOrder = displaySubCategoryOrders[categoryId] ?: catWithSubs.subCategories.map { it.subCategory.uuid }
                            subOrder.forEachIndexed { subIndex, subCategoryId ->
                                val subCatWithGroceries = catWithSubs.subCategories.find { it.subCategory.uuid == subCategoryId }
                                if (subCatWithGroceries != null) {
                                    SubCategoryOrderCard(
                                        name = subCatWithGroceries.subCategory.name,
                                        canMoveUp = subIndex > 0,
                                        canMoveDown = subIndex < subOrder.size - 1,
                                        onMoveUp = {
                                            if (subIndex > 0) {
                                                val newSubOrder = subOrder.toMutableList()
                                                val temp = newSubOrder[subIndex]
                                                newSubOrder[subIndex] = newSubOrder[subIndex - 1]
                                                newSubOrder[subIndex - 1] = temp
                                                displaySubCategoryOrders = displaySubCategoryOrders.toMutableMap().apply {
                                                    put(categoryId, newSubOrder)
                                                }
                                            }
                                        },
                                        onMoveDown = {
                                            if (subIndex < subOrder.size - 1) {
                                                val newSubOrder = subOrder.toMutableList()
                                                val temp = newSubOrder[subIndex]
                                                newSubOrder[subIndex] = newSubOrder[subIndex + 1]
                                                newSubOrder[subIndex + 1] = temp
                                                displaySubCategoryOrders = displaySubCategoryOrders.toMutableMap().apply {
                                                    put(categoryId, newSubOrder)
                                                }
                                            }
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(SuperCartSpacing.sm))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SuperCartSpacing.sm)
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuperCartColors.white,
                        contentColor = SuperCartColors.primaryGreen
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(R.string.cancel))
                }

                Button(
                    onClick = {
                        DataManagerObject.setStoreCategoryOrder(store.uuid, displayCategoryOrder)
                        DataManagerObject.setStoreSubCategoryOrders(store.uuid, displaySubCategoryOrders)
                        scope.launch {
                            DataStoreManager.saveDataGlobally()
                        }
                        onOrderUpdated()
                        onDismiss()
                    },
                    enabled = allCategories.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuperCartColors.primaryGreen,
                        contentColor = SuperCartColors.white
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = stringResource(R.string.save_order))
                }
            }
        }
    )
}

@Composable
private fun CategoryOrderCard(
    category: com.example.supercart2.models.Category,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        colors = CardDefaults.cardColors(containerColor = SuperCartColors.white),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = SuperCartShapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SuperCartSpacing.md, vertical = SuperCartSpacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = localizedCategoryDisplayName(category.name),
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SuperCartColors.primaryGreen,
                modifier = Modifier.weight(1f)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(SuperCartSpacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.move_up),
                        tint = if (canMoveUp) SuperCartColors.primaryGreen else SuperCartColors.gray
                    )
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.move_down),
                        tint = if (canMoveDown) SuperCartColors.primaryGreen else SuperCartColors.gray
                    )
                }
            }
        }
    }
}

@Composable
private fun SubCategoryOrderCard(
    name: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, bottom = 4.dp),
        colors = CardDefaults.cardColors(containerColor = SuperCartColors.lightGreen.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = SuperCartShapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SuperCartSpacing.md, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = SuperCartColors.black,
                modifier = Modifier.weight(1f)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(SuperCartSpacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.move_up),
                        tint = if (canMoveUp) SuperCartColors.primaryGreen else SuperCartColors.gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.move_down),
                        tint = if (canMoveDown) SuperCartColors.primaryGreen else SuperCartColors.gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
