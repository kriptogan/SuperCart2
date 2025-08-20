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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

@Composable
fun CollapsibleCategorySection(
    category: Category,
    subCategories: List<SubCategory>,
    groceries: List<Grocery>,
    onEditGrocery: (Grocery) -> Unit,
    isExpandedGlobal: Boolean = true,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(isExpandedGlobal) }
    
    LaunchedEffect(isExpandedGlobal) {
        isExpanded = isExpandedGlobal
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            // Category header - clickable to expand/collapse
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier.size(24.dp),
                    tint = Color.Gray
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                val categoryGroceriesCount = groceries.count { grocery -> 
                    subCategories.any { it.uuid == grocery.subCategoryId }
                }
                
                Text(
                    text = "${category.name} ($categoryGroceriesCount items)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Sub-categories content - only visible when expanded
            if (isExpanded) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    subCategories.forEach { subCategory ->
                        val subCategoryGroceries = groceries.filter { it.subCategoryId == subCategory.uuid }
                        SubCategoryItem(
                            subCategory = subCategory,
                            groceries = subCategoryGroceries,
                            onEditGrocery = onEditGrocery,
                            isExpandedGlobal = isExpandedGlobal
                        )
                        if (subCategory != subCategories.last()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubCategoryItem(
    subCategory: SubCategory,
    groceries: List<Grocery>,
    onEditGrocery: (Grocery) -> Unit,
    isExpandedGlobal: Boolean = true,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(isExpandedGlobal) }
    
    LaunchedEffect(isExpandedGlobal) {
        isExpanded = isExpandedGlobal
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Column {
            // Sub-category header - clickable to expand/collapse
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier.size(20.dp),
                    tint = Color.Gray
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = subCategory.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                
                Text(
                    text = "(${groceries.size} items)",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            
            // Groceries list - only visible when expanded
            if (isExpanded) {
                Column(
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                ) {
                    if (groceries.isEmpty()) {
                        Text(
                            text = "No groceries in this sub-category",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(8.dp)
                        )
                    } else {
                        groceries.forEach { grocery ->
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
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = grocery.name,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    // Edit icon - opens grocery form with pre-filled data
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit grocery",
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable { 
                                                onEditGrocery(grocery)
                                            },
                                        tint = Color.Blue
                                    )
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    // Cart icon - does nothing for now
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = "Add to cart",
                                        modifier = Modifier.size(18.dp),
                                        tint = Color.Green
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
