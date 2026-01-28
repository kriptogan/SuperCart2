package com.example.supercart2.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextAlign
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing
import com.example.supercart2.ui.theme.SuperCartShapes
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.models.Store
import kotlinx.coroutines.launch

@Composable
fun EditStoreDialog(
    store: Store,
    onDismiss: () -> Unit,
    onStoreUpdated: (Store) -> Unit = {}
) {
    var storeName by remember { mutableStateOf(store.name) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = {
                Text(
                    text = "Delete Store",
                    style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${store.name}'? This will unlink all groceries from this store.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SuperCartSpacing.sm)
                ) {
                    // Cancel Button (left) - secondary styled
                    Button(
                        onClick = { showDeleteConfirmation = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SuperCartColors.white,
                            contentColor = SuperCartColors.primaryGreen
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel"
                        )
                    }
                    
                    // Delete Button (right) - danger styled
                    Button(
                        onClick = {
                            DataManagerObject.deleteStore(store.uuid)
                            
                            // Save to DataStore
                            scope.launch {
                                DataStoreManager.saveDataGlobally()
                                android.util.Log.d("EditStoreDialog", "Saved store deletion to DataStore")
                            }
                            onDismiss()
                            showDeleteConfirmation = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red,
                            contentColor = SuperCartColors.white
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete"
                        )
                    }
                }
            }
        )
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(0.1f))
                Text(
                    text = "Edit Store",
                    style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(0.8f)
                )
                
                // Delete Icon
                IconButton(
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier.weight(0.1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Store",
                        tint = Color.Red
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = storeName,
                    onValueChange = { storeName = it },
                    label = { Text("Store Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SuperCartColors.primaryGreen,
                        unfocusedBorderColor = SuperCartColors.gray,
                        focusedLabelColor = SuperCartColors.primaryGreen,
                        unfocusedLabelColor = SuperCartColors.gray
                    ),
                    shape = SuperCartShapes.small
                )
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
                        contentDescription = "Cancel"
                    )
                }
                
                // Accept Button (right) - primary styled
                Button(
                    onClick = {
                        if (storeName.isNotBlank()) {
                            val updatedStore = store.copy(
                                name = storeName.trim(),
                                lastUpdate = java.time.LocalDateTime.now()
                            )
                            
                            DataManagerObject.updateStore(store.uuid) { updatedStore }
                            
                            // Save to DataStore
                            scope.launch {
                                DataStoreManager.saveDataGlobally()
                            }
                            
                            onStoreUpdated(updatedStore)
                            onDismiss()
                        }
                    },
                    enabled = storeName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuperCartColors.primaryGreen,
                        contentColor = SuperCartColors.white
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Update Store"
                    )
                }
            }
        }
    )
}
