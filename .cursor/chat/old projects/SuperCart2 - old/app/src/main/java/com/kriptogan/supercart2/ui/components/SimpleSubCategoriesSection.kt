package com.kriptogan.supercart2.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kriptogan.supercart2.classes.Category
import com.kriptogan.supercart2.classes.SubCategory
import com.kriptogan.supercart2.classes.Grocery
import com.kriptogan.supercart2.classes.LocalStorageManager

@Composable
fun SimpleSubCategoriesSection(
    category: Category,
    localStorageManager: LocalStorageManager,
    onAddSubCategory: () -> Unit,
    onEditSubCategory: (SubCategory) -> Unit,
    onDeleteSubCategory: (SubCategory) -> Unit
) {
    val subCategories = localStorageManager.getSubCategoriesByCategoryId(category.uuid)
    val groceries = localStorageManager.getGroceries()
    
    // State for tracking which sub-categories are expanded
    val expansionState = remember { mutableStateMapOf<String, Boolean>() }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF3E5F5)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Sub-Categories for: ${category.name}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (subCategories.isEmpty()) {
                Text(
                    text = "No sub-categories yet",
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            } else {
                subCategories.forEach { subCategory ->
                    val isExpanded = expansionState[subCategory.uuid] ?: false
                    val subCategoryGroceries = groceries.filter { it.subCategoryId == subCategory.uuid }
                    
                    // Sub-category header with collapse/expand
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                expansionState[subCategory.uuid] = !isExpanded 
                            }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                modifier = Modifier.size(20.dp),
                                tint = Color.Gray
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = subCategory.name,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = "(${subCategoryGroceries.size} items)",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    
                    // Groceries list (only visible when expanded)
                    if (isExpanded) {
                        Column(
                            modifier = Modifier.padding(start = 28.dp, top = 8.dp, bottom = 8.dp)
                        ) {
                            if (subCategoryGroceries.isEmpty()) {
                                Text(
                                    text = "No groceries in this sub-category",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(8.dp)
                                )
                            } else {
                                subCategoryGroceries.forEach { grocery ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFFF8F9FA)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = grocery.name,
                                                fontSize = 14.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (grocery.expirationDate != null) {
                                                Text(
                                                    text = grocery.expirationDate,
                                                    fontSize = 12.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            Button(
                onClick = onAddSubCategory
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Add Sub-Category")
            }
        }
    }
}
