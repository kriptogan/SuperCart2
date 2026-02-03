package com.example.supercart2.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.supercart2.R
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing
import com.example.supercart2.ui.theme.SuperCartShapes
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.models.Store

@Composable
fun StoresManagementDialog(
    onDismiss: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var storeToEdit by remember { mutableStateOf<Store?>(null) }
    val scope = rememberCoroutineScope()
    
    // Observe version to trigger recomposition
    val version = DataManagerObject.version
    
    if (showCreateDialog) {
        CreateStoreDialog(
            onDismiss = { showCreateDialog = false },
            onStoreCreated = {
                // Store is already added in CreateStoreDialog
                showCreateDialog = false
            }
        )
    }
    
    if (storeToEdit != null) {
        EditStoreDialog(
            store = storeToEdit!!,
            onDismiss = { storeToEdit = null },
            onStoreUpdated = {
                // Store is already updated in EditStoreDialog
                storeToEdit = null
            }
        )
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxSize(),
        containerColor = SuperCartColors.lightGreen,
        title = { 
            Text(
                text = stringResource(R.string.stores_management),
                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Get stores sorted by viewOrder
                val sortedStores = remember(version) {
                    DataManagerObject.getSortedStores()
                }
                
                if (sortedStores.isEmpty()) {
                    // Empty state
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_stores_yet),
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                            color = SuperCartColors.gray
                        )
                        Spacer(modifier = Modifier.height(SuperCartSpacing.sm))
                        Text(
                            text = stringResource(R.string.no_stores_available_create),
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            color = SuperCartColors.gray,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // Stores List
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(sortedStores) { store ->
                            val index = sortedStores.indexOf(store)
                            StoreCard(
                                store = store,
                                onEditClick = { storeToEdit = store },
                                onMoveUp = {
                                    if (index > 0) {
                                        val prevStore = sortedStores[index - 1]
                                        DataManagerObject.swapStoreOrder(
                                            store.uuid,
                                            prevStore.uuid
                                        )
                                        scope.launch {
                                            DataStoreManager.saveDataGlobally()
                                        }
                                    }
                                },
                                onMoveDown = {
                                    if (index < sortedStores.size - 1) {
                                        val nextStore = sortedStores[index + 1]
                                        DataManagerObject.swapStoreOrder(
                                            store.uuid,
                                            nextStore.uuid
                                        )
                                        scope.launch {
                                            DataStoreManager.saveDataGlobally()
                                        }
                                    }
                                },
                                canMoveUp = index > 0,
                                canMoveDown = index < sortedStores.size - 1
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
                
                // Accept Button (right) - primary styled
                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuperCartColors.primaryGreen,
                        contentColor = SuperCartColors.white
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.create_new_store)
                    )
                }
            }
        }
    )
}

@Composable
private fun StoreCard(
    store: Store,
    onEditClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = SuperCartSpacing.sm),
        colors = CardDefaults.cardColors(
            containerColor = SuperCartColors.white
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        ),
        shape = SuperCartShapes.medium
    ) {
        Column(
            modifier = Modifier.padding(SuperCartSpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Store name
                Text(
                    text = store.name,
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                    color = SuperCartColors.black
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
                    
                    // Edit Icon
                    IconButton(
                        onClick = onEditClick
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit_store),
                            tint = SuperCartColors.primaryGreen
                        )
                    }
                }
            }
        }
    }
}
