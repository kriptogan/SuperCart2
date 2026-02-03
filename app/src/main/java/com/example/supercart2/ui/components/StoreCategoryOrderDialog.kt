package com.example.supercart2.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    
    // Get categories that have items in this store
    val categoriesWithItems = remember(version) {
        DataManagerObject.categories.mapNotNull { categoryWithSubs ->
            val hasItemsInStore = categoryWithSubs.subCategories.any { subCategoryWithGroceries ->
                subCategoryWithGroceries.groceries.any { grocery ->
                    grocery.storeIds.contains(store.uuid)
                }
            }
            if (hasItemsInStore) categoryWithSubs else null
        }
    }
    
    // Get current order (custom or default)
    val currentOrder = remember(version, categoriesWithItems) {
        val customOrder = DataManagerObject.getStoreCategoryOrder(store.uuid)
        if (customOrder != null) {
            // Use custom order, filter to only categories that have items
            val ordered = mutableListOf<com.example.supercart2.data.CategoryWithSubCategories>()
            val unordered = categoriesWithItems.toMutableList()
            
            customOrder.forEach { categoryId ->
                val category = unordered.find { it.category.uuid == categoryId }
                if (category != null) {
                    ordered.add(category)
                    unordered.remove(category)
                }
            }
            
            // Add any remaining categories sorted by viewOrder
            ordered.addAll(unordered.sortedBy { it.category.viewOrder })
            ordered
        } else {
            // Use default order (viewOrder)
            categoriesWithItems.sortedBy { it.category.viewOrder }
        }
    }
    
    // Local state for reordering (starts with current order)
    var displayOrder by remember { mutableStateOf(currentOrder.map { it.category.uuid }) }
    
    // Update displayOrder when currentOrder changes externally
    androidx.compose.runtime.LaunchedEffect(currentOrder) {
        displayOrder = currentOrder.map { it.category.uuid }
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
            if (categoriesWithItems.isEmpty()) {
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = displayOrder,
                        key = { it }
                    ) { categoryId ->
                        val category = categoriesWithItems.find { it.category.uuid == categoryId }
                        if (category != null) {
                            val index = displayOrder.indexOf(categoryId)
                            CategoryOrderCard(
                                category = category.category,
                                canMoveUp = index > 0,
                                canMoveDown = index < displayOrder.size - 1,
                                onMoveUp = {
                                    if (index > 0) {
                                        val newOrder = displayOrder.toMutableList()
                                        val temp = newOrder[index]
                                        newOrder[index] = newOrder[index - 1]
                                        newOrder[index - 1] = temp
                                        displayOrder = newOrder
                                    }
                                },
                                onMoveDown = {
                                    if (index < displayOrder.size - 1) {
                                        val newOrder = displayOrder.toMutableList()
                                        val temp = newOrder[index]
                                        newOrder[index] = newOrder[index + 1]
                                        newOrder[index + 1] = temp
                                        displayOrder = newOrder
                                    }
                                }
                            )
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
                // Cancel Button (left) - secondary styled
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuperCartColors.white,
                        contentColor = SuperCartColors.primaryGreen
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cancel)
                    )
                }
                
                // Save Button (right) - primary styled
                Button(
                    onClick = {
                        // Save the order
                        DataManagerObject.setStoreCategoryOrder(store.uuid, displayOrder)
                        
                        // Save to DataStore
                        scope.launch {
                            DataStoreManager.saveDataGlobally()
                            android.util.Log.d("StoreCategoryOrderDialog", "Saved category order for store ${store.name}")
                        }
                        
                        onOrderUpdated()
                        onDismiss()
                    },
                    enabled = categoriesWithItems.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuperCartColors.primaryGreen,
                        contentColor = SuperCartColors.white
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.save_order)
                    )
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
            .padding(bottom = SuperCartSpacing.sm),
        colors = CardDefaults.cardColors(
            containerColor = SuperCartColors.white
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        shape = SuperCartShapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SuperCartSpacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category name
            Text(
                text = localizedCategoryDisplayName(category.name),
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                color = SuperCartColors.black,
                modifier = Modifier.weight(1f)
            )
            
            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(SuperCartSpacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Move Up Arrow
                IconButton(
                    onClick = onMoveUp,
                    enabled = canMoveUp
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.move_up),
                        tint = if (canMoveUp) SuperCartColors.primaryGreen else SuperCartColors.gray
                    )
                }
                
                // Move Down Arrow
                IconButton(
                    onClick = onMoveDown,
                    enabled = canMoveDown
                ) {
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
