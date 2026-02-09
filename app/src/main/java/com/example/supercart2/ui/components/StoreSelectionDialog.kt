package com.example.supercart2.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.supercart2.R
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import kotlinx.coroutines.launch

@Composable
fun StoreSelectionDialog(
    selectedStoreIds: List<String>,
    onDismiss: () -> Unit,
    onStoresSelected: (List<String>) -> Unit
) {
    val version = DataManagerObject.version
    val allStores = remember(version) {
        DataManagerObject.getSortedStores()
    }
    
    var selectedIds by remember { 
        mutableStateOf(selectedStoreIds.toMutableSet()) 
    }
    var showCreateStore by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    AlertDialog(
        onDismissRequest = {
            // Auto-save selections when dismissing
            onStoresSelected(selectedIds.toList())
            onDismiss()
        },
        title = { Text(stringResource(R.string.select_stores), color = SuperCartColors.black) },
        text = {
            if (allStores.isEmpty() && !showCreateStore) {
                Text(
                    stringResource(R.string.no_stores_available_create),
                    color = SuperCartColors.gray,
                    fontStyle = FontStyle.Italic
                )
            } else if (!showCreateStore) {
                LazyColumn {
                    items(allStores) { store ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedIds = if (selectedIds.contains(store.uuid)) {
                                        selectedIds.toMutableSet().apply { remove(store.uuid) }
                                    } else {
                                        selectedIds.toMutableSet().apply { add(store.uuid) }
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedIds.contains(store.uuid),
                                onCheckedChange = { checked ->
                                    selectedIds = if (checked) {
                                        selectedIds.toMutableSet().apply { add(store.uuid) }
                                    } else {
                                        selectedIds.toMutableSet().apply { remove(store.uuid) }
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = SuperCartColors.primaryGreen
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = store.name)
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
                // Close Button - auto-saves selections
                Button(
                    onClick = {
                        onStoresSelected(selectedIds.toList())
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuperCartColors.white,
                        contentColor = SuperCartColors.primaryGreen
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close)
                    )
                }
                
                // Create Store Button
                Button(
                    onClick = { showCreateStore = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuperCartColors.primaryGreen,
                        contentColor = SuperCartColors.white
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.create_store)
                    )
                }
            }
        }
    )
    
    // Create Store Dialog
    if (showCreateStore) {
        CreateStoreDialog(
            onDismiss = { showCreateStore = false },
            onStoreCreated = { newStore ->
                // Auto-select the newly created store
                selectedIds = selectedIds.toMutableSet().apply { add(newStore.uuid) }
                
                // Close create dialog
                showCreateStore = false
            }
        )
    }
}
