package com.example.supercart2.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import com.example.supercart2.R
import kotlinx.coroutines.launch
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing
import com.example.supercart2.ui.theme.SuperCartShapes
import com.example.supercart2.models.Category
import com.example.supercart2.models.SubCategory
import com.example.supercart2.data.SubCategoryWithGroceries
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.utils.localizedSubCategoryDisplayName

@Composable
fun EditCategoryDialog(
    category: Category,
    subCategories: List<SubCategoryWithGroceries>,
    onDismiss: () -> Unit,
    onCategoryUpdated: (Category) -> Unit,
    onCategoryDeleted: (Category) -> Unit,
    onSubCategoryCreated: (SubCategory) -> Unit,
    onSubCategoryUpdated: (SubCategory) -> Unit,
    onSubCategoryDeleted: (SubCategory) -> Unit
) {
    var categoryName by remember { mutableStateOf(category.name) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showCreateSubCategoryDialog by remember { mutableStateOf(false) }
    var editingSubCategory by remember { mutableStateOf<SubCategory?>(null) }
    val scope = rememberCoroutineScope()
    
    // Get current sub-categories directly from DataManagerObject to ensure fresh data
    // Use derivedStateOf to automatically recompute when version changes
    val currentSubCategories by remember {
        derivedStateOf {
            // Accessing DataManagerObject.version ensures this recomputes when data changes
            // Accessing categories will also trigger recomposition when version changes
            val currentVersion = DataManagerObject.version
            DataManagerObject.categories.find { it.category.uuid == category.uuid }?.subCategories ?: subCategories
        }
    }
    
    if (showCreateSubCategoryDialog) {
        CreateSubCategoryDialog(
            onDismiss = { showCreateSubCategoryDialog = false },
            onSubCategoryCreated = { subCategoryName ->
                val newSubCategory = SubCategory(
                    categoryId = category.uuid,
                    name = subCategoryName.trim(),
                    protected = false,
                    lastUpdate = java.time.LocalDateTime.now(),
                    deleted = false
                )
                // Add the new sub-category using DataManagerObject helper
                DataManagerObject.addSubCategory(category.uuid, newSubCategory)
                
                // Save to DataStore
                scope.launch {
                    DataStoreManager.saveDataGlobally()
                    android.util.Log.d("EditCategoryDialog", "Saved new sub-category to DataStore")
                }
                
                showCreateSubCategoryDialog = false
            }
        )
    }
    
    if (editingSubCategory != null) {
        EditSubCategoryDialog(
            subCategory = editingSubCategory!!,
            onDismiss = { editingSubCategory = null },
            onSubCategoryUpdated = { updatedSubCategory ->
                // Data is already updated by EditSubCategoryDialog, just close the dialog
                // The currentSubCategories will automatically update via version observation
                editingSubCategory = null
            }
        )
    }
    
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = {
                Text(
                    text = stringResource(R.string.delete_category),
                    style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    if (category.protected) {
                        Text(
                            text = stringResource(R.string.category_protected_cannot_delete),
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = androidx.compose.ui.graphics.Color.Red,
                            modifier = Modifier.padding(bottom = SuperCartSpacing.sm)
                        )
                    }
                    Text(
                        text = stringResource(R.string.delete_category_confirmation, category.name),
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
                            onCategoryDeleted(category)
                            showDeleteConfirmation = false
                        },
                        enabled = !category.protected,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (category.protected) SuperCartColors.gray else androidx.compose.ui.graphics.Color.Red,
                            contentColor = SuperCartColors.white
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = if (category.protected) stringResource(R.string.category_protected) else stringResource(R.string.delete)
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
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.edit_category),
                    style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                
                // Delete Icon
                androidx.compose.material3.IconButton(
                    onClick = { showDeleteConfirmation = true },
                    enabled = !category.protected
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = if (category.protected) stringResource(R.string.category_protected) else stringResource(R.string.delete_category),
                        tint = if (category.protected) SuperCartColors.gray else androidx.compose.ui.graphics.Color.Red
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Category Name Input
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    label = { Text(stringResource(R.string.category_name)) },
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
                
                Spacer(modifier = Modifier.height(SuperCartSpacing.md))
                
                // Sub-Categories Section Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.sub_categories),
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        color = SuperCartColors.black
                    )
                    
                    // Add Sub-Category Button
                    androidx.compose.material3.IconButton(
                        onClick = { showCreateSubCategoryDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.add_sub_category),
                            tint = SuperCartColors.primaryGreen
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(SuperCartSpacing.sm))
                
                // Sub-Categories List
                LazyColumn(
                    modifier = Modifier.height(200.dp)
                ) {
                    items(
                        items = currentSubCategories,
                        key = { it.subCategory.uuid } // Use stable UUID as key
                    ) { subCategoryWithGroceries ->
                        SubCategoryCard(
                            subCategory = subCategoryWithGroceries.subCategory,
                            groceriesCount = subCategoryWithGroceries.groceries.size,
                            onEditClick = { editingSubCategory = subCategoryWithGroceries.subCategory },
                            onDeleteClick = {
                                if (!subCategoryWithGroceries.subCategory.protected) {
                                    // Delete the sub-category
                                    DataManagerObject.deleteSubCategory(category.uuid, subCategoryWithGroceries.subCategory.uuid)
                                    
                                    // Save to DataStore
                                    scope.launch {
                                        DataStoreManager.saveDataGlobally()
                                        android.util.Log.d("EditCategoryDialog", "Saved sub-category deletion to DataStore")
                                    }
                                }
                            }
                        )
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
                    onClick = {
                        if (categoryName.isNotBlank()) {
                            DataManagerObject.updateCategory(category.uuid) { 
                                it.copy(
                                    name = categoryName.trim(),
                                    lastUpdate = java.time.LocalDateTime.now(),
                                    deleted = it.deleted
                                )
                            }
                            onDismiss()
                        }
                    },
                    enabled = categoryName.isNotBlank(),
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

@Composable
private fun SubCategoryCard(
    subCategory: SubCategory,
    groceriesCount: Int,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
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
        shape = SuperCartShapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SuperCartSpacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            // Left side: Sub-category name and groceries count
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = localizedSubCategoryDisplayName(subCategory.name),
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                        color = SuperCartColors.black
                    )
                    
                    if (subCategory.protected) {
                        Spacer(modifier = Modifier.width(SuperCartSpacing.xs))
                        Text(
                            text = stringResource(R.string.protected_label),
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            color = SuperCartColors.gray
                        )
                    }
                }
                
                Text(
                    text = stringResource(R.string.groceries_count, groceriesCount),
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = SuperCartColors.darkGray
                )
            }
            
            // Right side: Edit and Delete icons
            Row(
                horizontalArrangement = Arrangement.spacedBy(SuperCartSpacing.xs),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                // Edit Icon
                androidx.compose.material3.IconButton(
                    onClick = onEditClick
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit_subcategory),
                        tint = SuperCartColors.primaryGreen
                    )
                }
                
                // Delete Icon
                androidx.compose.material3.IconButton(
                    onClick = onDeleteClick,
                    enabled = !subCategory.protected
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = if (subCategory.protected) stringResource(R.string.subcategory_protected) else stringResource(R.string.delete_subcategory),
                        tint = if (subCategory.protected) SuperCartColors.gray else androidx.compose.ui.graphics.Color.Red
                    )
                }
            }
        }
    }
}
