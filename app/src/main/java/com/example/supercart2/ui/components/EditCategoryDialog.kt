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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing
import com.example.supercart2.ui.theme.SuperCartShapes
import com.example.supercart2.models.Category
import com.example.supercart2.models.SubCategory
import com.example.supercart2.data.SubCategoryWithGroceries

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
    
    if (showCreateSubCategoryDialog) {
        CreateSubCategoryDialog(
            onDismiss = { showCreateSubCategoryDialog = false },
            onSubCategoryCreated = { subCategoryName ->
                val newSubCategory = SubCategory(
                    categoryId = category.uuid,
                    name = subCategoryName.trim(),
                    protected = false
                )
                onSubCategoryCreated(newSubCategory)
                showCreateSubCategoryDialog = false
            }
        )
    }
    
    if (editingSubCategory != null) {
        EditSubCategoryDialog(
            subCategory = editingSubCategory!!,
            onDismiss = { editingSubCategory = null },
            onSubCategoryUpdated = { updatedSubCategory ->
                onSubCategoryUpdated(updatedSubCategory)
                editingSubCategory = null
            }
        )
    }
    
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = {
                Text(
                    text = "Delete Category",
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
                            text = "⚠️ This category is protected and cannot be deleted.",
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = androidx.compose.ui.graphics.Color.Red,
                            modifier = Modifier.padding(bottom = SuperCartSpacing.sm)
                        )
                    }
                    Text(
                        text = "Are you sure you want to delete '${category.name}'? This will also delete all sub-categories and groceries linked to it.",
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
                            contentDescription = "Cancel"
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
                            contentDescription = if (category.protected) "Category Protected" else "Delete"
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
                    text = "Edit Category",
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
                        contentDescription = if (category.protected) "Category Protected" else "Delete Category",
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
                    label = { Text("Category Name") },
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
                        text = "Sub-Categories",
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        color = SuperCartColors.black
                    )
                    
                    // Add Sub-Category Button
                    androidx.compose.material3.IconButton(
                        onClick = { showCreateSubCategoryDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Sub-Category",
                            tint = SuperCartColors.primaryGreen
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(SuperCartSpacing.sm))
                
                // Sub-Categories List
                LazyColumn(
                    modifier = Modifier.height(200.dp)
                ) {
                    items(subCategories) { subCategoryWithGroceries ->
                        SubCategoryCard(
                            subCategory = subCategoryWithGroceries.subCategory,
                            groceriesCount = subCategoryWithGroceries.groceries.size,
                            onEditClick = { editingSubCategory = subCategoryWithGroceries.subCategory },
                            onDeleteClick = { onSubCategoryDeleted(subCategoryWithGroceries.subCategory) }
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
                        contentDescription = "Cancel"
                    )
                }
                
                // Accept Button (right) - primary styled
                Button(
                    onClick = {
                        if (categoryName.isNotBlank()) {
                            val updatedCategory = category.copy(
                                name = categoryName.trim()
                            )
                            onCategoryUpdated(updatedCategory)
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
                        contentDescription = "Save Changes"
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
                        text = subCategory.name,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                        color = SuperCartColors.black
                    )
                    
                    if (subCategory.protected) {
                        Spacer(modifier = Modifier.width(SuperCartSpacing.xs))
                        Text(
                            text = "(Protected)",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            color = SuperCartColors.gray
                        )
                    }
                }
                
                Text(
                    text = "Groceries: $groceriesCount",
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
                        contentDescription = "Edit Sub-Category",
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
                        contentDescription = if (subCategory.protected) "Sub-Category Protected" else "Delete Sub-Category",
                        tint = if (subCategory.protected) SuperCartColors.gray else androidx.compose.ui.graphics.Color.Red
                    )
                }
            }
        }
    }
}
