package com.example.supercart2.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.supercart2.models.SubCategory
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import kotlinx.coroutines.launch

@Composable
fun EditSubCategoryDialog(
    subCategory: SubCategory,
    onDismiss: () -> Unit,
    onSubCategoryUpdated: (SubCategory) -> Unit,
    categoryId: String = subCategory.categoryId
) {
    var subCategoryName by remember { mutableStateOf(subCategory.name) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            containerColor = SuperCartColors.lightGreen,
            title = {
                Text(
                    text = stringResource(R.string.delete_subcategory),
                    style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (subCategory.protected) {
                        Text(
                            text = stringResource(R.string.subcategory_protected_cannot_delete),
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = Color.Red,
                            modifier = Modifier.padding(bottom = SuperCartSpacing.sm)
                        )
                    }
                    Text(
                        text = stringResource(R.string.delete_subcategory_confirmation, subCategory.name),
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
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
                        onClick = { showDeleteConfirmation = false },
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
                    
                    // Delete Button (right) - danger styled
                    Button(
                        onClick = {
                            if (!subCategory.protected) {
                                DataManagerObject.deleteSubCategory(categoryId, subCategory.uuid)
                                
                                // Save to DataStore
                                scope.launch {
                                    DataStoreManager.saveDataGlobally()
                                    android.util.Log.d("EditSubCategoryDialog", "Saved sub-category deletion to DataStore")
                                }
                                onDismiss()
                            }
                            showDeleteConfirmation = false
                        },
                        enabled = !subCategory.protected,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (subCategory.protected) SuperCartColors.gray else Color.Red,
                            contentColor = SuperCartColors.white
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = if (subCategory.protected) stringResource(R.string.subcategory_protected) else stringResource(R.string.delete)
                        )
                    }
                }
            }
        )
    }
    
    // Main edit dialog
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SuperCartColors.lightGreen,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.edit_subcategory),
                    style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                
                // Delete Icon
                IconButton(
                    onClick = { showDeleteConfirmation = true },
                    enabled = !subCategory.protected
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = if (subCategory.protected) stringResource(R.string.subcategory_protected) else stringResource(R.string.delete_subcategory),
                        tint = if (subCategory.protected) SuperCartColors.gray else Color.Red
                    )
                }
            }
        },
        text = {
            OutlinedTextField(
                value = subCategoryName,
                onValueChange = { subCategoryName = it },
                label = { Text(stringResource(R.string.subcategory_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SuperCartColors.primaryGreen,
                    unfocusedBorderColor = SuperCartColors.gray,
                    focusedLabelColor = SuperCartColors.black,
                    unfocusedLabelColor = SuperCartColors.black,
                    focusedContainerColor = SuperCartColors.white,
                    unfocusedContainerColor = SuperCartColors.white
                ),
                shape = SuperCartShapes.small
            )
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
                        if (subCategoryName.isNotBlank()) {
                            // Create updated sub-category preserving protected status
                            val updatedSubCategory = subCategory.copy(
                                name = subCategoryName.trim(),
                                lastUpdate = java.time.LocalDateTime.now(),
                                protected = subCategory.protected, // Preserve protected status
                                deleted = subCategory.deleted
                            )
                            
                            // Update using DataManagerObject helper
                            DataManagerObject.updateSubCategory(
                                categoryId,
                                subCategory.uuid
                            ) { updatedSubCategory }
                            
                            // Save to DataStore
                            scope.launch {
                                DataStoreManager.saveDataGlobally()
                                android.util.Log.d("EditSubCategoryDialog", "Saved sub-category update to DataStore")
                            }
                            
                            // Notify parent component
                            onSubCategoryUpdated(updatedSubCategory)
                            onDismiss()
                        }
                    },
                    enabled = subCategoryName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuperCartColors.primaryGreen,
                        contentColor = SuperCartColors.white
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.save_changes)
                    )
                }
            }
        }
    )
}