package com.example.supercart2.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing
import com.example.supercart2.ui.theme.SuperCartShapes
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.CategoryWithSubCategories
import com.example.supercart2.data.SubCategoryWithGroceries
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.models.Category
import com.example.supercart2.ui.components.CreateCategoryDialog
import com.example.supercart2.ui.components.EditCategoryDialog

@Composable
fun CategoriesManagementDialog(
    onDismiss: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    val scope = rememberCoroutineScope()
    
    if (showCreateDialog) {
        CreateCategoryDialog(
            onDismiss = { showCreateDialog = false },
            onCategoryCreated = { newCategory, generalSubCategory ->
                // Add the new category with its "General" sub-category to DataManagerObject
                val generalSubCategoryWithGroceries = SubCategoryWithGroceries(
                    subCategory = generalSubCategory,
                    groceries = mutableListOf()
                )
                
                val newCategoryWithSubs = CategoryWithSubCategories(
                    category = newCategory,
                    subCategories = mutableListOf(generalSubCategoryWithGroceries)
                )
                DataManagerObject.categories.add(newCategoryWithSubs)
                
                // Save the updated data to local storage immediately
                scope.launch {
                    DataStoreManager.saveDataGlobally()
                }
            }
        )
    }
    
    if (editingCategory != null) {
        EditCategoryDialog(
            category = editingCategory!!,
            onDismiss = { editingCategory = null },
            onCategoryUpdated = { updatedCategory ->
                // Find and update the category in DataManagerObject
                val index = DataManagerObject.categories.indexOfFirst { 
                    it.category.uuid == updatedCategory.uuid 
                }
                if (index != -1) {
                    val updatedCategoryWithSubs = DataManagerObject.categories[index].copy(
                        category = updatedCategory
                    )
                    DataManagerObject.categories[index] = updatedCategoryWithSubs
                    
                    // Save the updated data to local storage immediately
                    scope.launch {
                        DataStoreManager.saveDataGlobally()
                    }
                }
                editingCategory = null
            }
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxSize(), // This makes it full-screen
        title = { 
            Text(
                text = "Categories Management",
                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Categories List
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Get categories sorted by viewOrder for consistent display order
                    val sortedCategories = DataManagerObject.getSortedCategories()
                    items(sortedCategories) { categoryWithSubs ->
                        val index = sortedCategories.indexOf(categoryWithSubs)
                        CategoryCard(
                            category = categoryWithSubs.category,
                            subCategoriesCount = categoryWithSubs.subCategories.size,
                            groceriesCount = categoryWithSubs.getGroceriesCount(),
                            onEditClick = { editingCategory = categoryWithSubs.category },
                            onMoveUp = {
                                if (index > 0) {
                                    // Get the previous category from the sorted list
                                    val prevCategory = sortedCategories[index - 1]
                                    val currentViewOrder = categoryWithSubs.category.viewOrder
                                    val prevViewOrder = prevCategory.category.viewOrder
                                    
                                    // Find the actual indices in the original DataManagerObject.categories
                                    val currentIndex = DataManagerObject.categories.indexOfFirst { it.category.uuid == categoryWithSubs.category.uuid }
                                    val prevIndex = DataManagerObject.categories.indexOfFirst { it.category.uuid == prevCategory.category.uuid }
                                    
                                    if (currentIndex != -1 && prevIndex != -1) {
                                        // Update view orders
                                        val updatedCurrent = categoryWithSubs.copy(
                                            category = categoryWithSubs.category.copy(viewOrder = prevViewOrder)
                                        )
                                        val updatedPrev = prevCategory.copy(
                                            category = prevCategory.category.copy(viewOrder = currentViewOrder)
                                        )
                                        
                                        DataManagerObject.categories[currentIndex] = updatedCurrent
                                        DataManagerObject.categories[prevIndex] = updatedPrev
                                        
                                        // Save changes
                                        scope.launch {
                                            DataStoreManager.saveDataGlobally()
                                        }
                                    }
                                }
                            },
                            onMoveDown = {
                                if (index < sortedCategories.size - 1) {
                                    // Get the next category from the sorted list
                                    val nextCategory = sortedCategories[index + 1]
                                    val currentViewOrder = categoryWithSubs.category.viewOrder
                                    val nextViewOrder = nextCategory.category.viewOrder
                                    
                                    // Find the actual indices in the original DataManagerObject.categories
                                    val currentIndex = DataManagerObject.categories.indexOfFirst { it.category.uuid == categoryWithSubs.category.uuid }
                                    val nextIndex = DataManagerObject.categories.indexOfFirst { it.category.uuid == nextCategory.category.uuid }
                                    
                                    if (currentIndex != -1 && nextIndex != -1) {
                                        // Update view orders
                                        val updatedCurrent = categoryWithSubs.copy(
                                            category = categoryWithSubs.category.copy(viewOrder = nextViewOrder)
                                        )
                                        val updatedNext = nextCategory.copy(
                                            category = nextCategory.category.copy(viewOrder = currentViewOrder)
                                        )
                                        
                                        DataManagerObject.categories[currentIndex] = updatedCurrent
                                        DataManagerObject.categories[nextIndex] = updatedNext
                                        
                                        // Save changes
                                        scope.launch {
                                            DataStoreManager.saveDataGlobally()
                                        }
                                    }
                                }
                            },
                            canMoveUp = index > 0,
                            canMoveDown = index < sortedCategories.size - 1
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
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuperCartColors.primaryGreen,
                        contentColor = SuperCartColors.white
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create New Category"
                    )
                }
            }
        }
    )
}

@Composable
private fun CategoryCard(
    category: Category,
    subCategoriesCount: Int,
    groceriesCount: Int,
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
            Text(
                text = category.name,
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                color = SuperCartColors.black
            )
            
            Spacer(modifier = Modifier.height(SuperCartSpacing.sm))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                // Left side: Sub-categories and Groceries count
                Column {
                    Text(
                        text = "Sub-Categories: $subCategoriesCount",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = SuperCartColors.darkGray
                    )
                    
                    Text(
                        text = "Groceries: $groceriesCount",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = SuperCartColors.darkGray
                    )
                }
                
                // Right side: View order arrows and edit icon
                Row(
                    horizontalArrangement = Arrangement.spacedBy(SuperCartSpacing.xs),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    // Move Up Arrow
                    androidx.compose.material3.IconButton(
                        onClick = onMoveUp,
                        enabled = canMoveUp
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.KeyboardArrowUp,
                            contentDescription = "Move Up",
                            tint = if (canMoveUp) SuperCartColors.primaryGreen else SuperCartColors.gray
                        )
                    }
                    
                    // Move Down Arrow
                    androidx.compose.material3.IconButton(
                        onClick = onMoveDown,
                        enabled = canMoveDown
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.KeyboardArrowDown,
                            contentDescription = "Move Down",
                            tint = if (canMoveDown) SuperCartColors.primaryGreen else SuperCartColors.gray
                        )
                    }
                    
                    // Edit Icon
                    androidx.compose.material3.IconButton(
                        onClick = onEditClick
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Edit,
                            contentDescription = "Edit Category",
                            tint = SuperCartColors.primaryGreen
                        )
                    }
                }
            }
        }
    }
}
