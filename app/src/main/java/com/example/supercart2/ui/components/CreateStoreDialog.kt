package com.example.supercart2.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing
import com.example.supercart2.ui.theme.SuperCartShapes
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.models.Store
import kotlinx.coroutines.launch

@Composable
fun CreateStoreDialog(
    onDismiss: () -> Unit,
    onStoreCreated: (Store) -> Unit = {}
) {
    var storeName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Create New Store",
                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
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
                            // Calculate the highest existing viewOrder + 1
                            val highestViewOrder = DataManagerObject.stores
                                .map { it.viewOrder }
                                .maxOrNull() ?: 0
                            
                            val newStore = Store(
                                name = storeName.trim(),
                                viewOrder = highestViewOrder + 1
                            )
                            
                            DataManagerObject.addStore(newStore)
                            
                            // Save to DataStore
                            scope.launch {
                                DataStoreManager.saveDataGlobally()
                            }
                            
                            onStoreCreated(newStore)
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
                        contentDescription = "Create Store"
                    )
                }
            }
        }
    )
}
