package com.example.supercart2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.models.Category
import com.example.supercart2.models.SubCategory
import com.example.supercart2.ui.components.CreateCategoryDialog
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.data.CategoryWithSubCategories
import com.example.supercart2.data.SubCategoryWithGroceries

@Composable
fun CategorySelectionDialog(
    onDismiss: () -> Unit,
    onCategorySelected: (Category) -> Unit,
    onNewCategoryCreated: (Category, SubCategory) -> Unit,
    selectedCategoryId: String? = null
) {
    val sortedCategories = DataManagerObject.getSortedCategories()
    val scope = rememberCoroutineScope()
    var showCreateCategory by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxSize(),
        title = { 
            Text(
                text = "Select Category",
                modifier = Modifier.fillMaxWidth()
            ) 
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sortedCategories) { categoryWithSubs ->
                    val category = categoryWithSubs.category
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(0xFFF5F5F5),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(16.dp)
                            .clickable {
                                onCategorySelected(category)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedCategoryId == category.uuid) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.Green,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.width(28.dp))
                        }
                        Text(
                            text = category.name,
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
                Button(
                    onClick = { showCreateCategory = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Category",
                        tint = Color.White
                    )
                }
            }
        }
    )
    
    // Create Category Dialog
    if (showCreateCategory) {
        CreateCategoryDialog(
            onDismiss = { showCreateCategory = false },
            onCategoryCreated = { newCategory, newSubCategory ->
                // Add the new category with its "General" sub-category to DataManagerObject
                val generalSubCategoryWithGroceries = SubCategoryWithGroceries(
                    subCategory = newSubCategory,
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
                
                // Call the callback with both category and sub-category
                onNewCategoryCreated(newCategory, newSubCategory)
                
                // Close create dialog
                showCreateCategory = false
            }
        )
    }
}
