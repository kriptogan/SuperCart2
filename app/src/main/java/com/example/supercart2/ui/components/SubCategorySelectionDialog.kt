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
import com.example.supercart2.ui.components.CreateSubCategoryDialog
import com.example.supercart2.data.SubCategoryWithGroceries
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.example.supercart2.data.DataStoreManager

@Composable
fun SubCategorySelectionDialog(
    onDismiss: () -> Unit,
    onSubCategorySelected: (SubCategory) -> Unit,
    onNewSubCategoryCreated: (SubCategory) -> Unit,
    selectedCategory: Category,
    selectedSubCategoryId: String? = null
) {
    val categoryWithSubs = DataManagerObject.categories.find { it.category.uuid == selectedCategory.uuid }
    val subCategories = categoryWithSubs?.subCategories?.map { it.subCategory } ?: emptyList()
    
    val scope = rememberCoroutineScope()
    var showCreateSubCategory by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxSize(),
        title = { 
            Text(
                text = "Select Sub-Category",
                modifier = Modifier.fillMaxWidth()
            ) 
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(subCategories) { subCategory ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(0xFFF5F5F5),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(16.dp)
                            .clickable {
                                onSubCategorySelected(subCategory)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedSubCategoryId == subCategory.uuid) {
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
                            text = subCategory.name,
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
                    onClick = { showCreateSubCategory = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Sub-Category",
                        tint = Color.White
                    )
                }
            }
        }
    )
    
    // Create Sub-Category Dialog
    if (showCreateSubCategory) {
        CreateSubCategoryDialog(
            onDismiss = { showCreateSubCategory = false },
            onSubCategoryCreated = { subCategoryName ->
                // Create the new SubCategory object
                val newSubCategory = SubCategory(
                    categoryId = selectedCategory.uuid,
                    name = subCategoryName,
                    protected = false
                )
                
                // Add the new sub-category to DataManagerObject
                val categoryIndex = DataManagerObject.categories.indexOfFirst { it.category.uuid == selectedCategory.uuid }
                if (categoryIndex != -1) {
                    val categoryWithSubs = DataManagerObject.categories[categoryIndex]
                    val newSubCategoryWithGroceries = SubCategoryWithGroceries(
                        subCategory = newSubCategory,
                        groceries = mutableListOf()
                    )
                    
                    val updatedCategoryWithSubs = categoryWithSubs.copy(
                        subCategories = categoryWithSubs.subCategories.toMutableList().apply { 
                            add(newSubCategoryWithGroceries) 
                        }
                    )
                    DataManagerObject.categories[categoryIndex] = updatedCategoryWithSubs
                    
                    // Save the updated data to local storage immediately
                    scope.launch {
                        DataStoreManager.saveDataGlobally()
                    }
                    
                    // Call the callback with the new sub-category
                    onNewSubCategoryCreated(newSubCategory)
                    
                    // Close create dialog
                    showCreateSubCategory = false
                }
            }
        )
    }
}
