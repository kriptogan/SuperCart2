package com.example.supercart2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.CategoryWithSubCategories
import com.example.supercart2.data.SubCategoryWithGroceries
import com.example.supercart2.models.Grocery
import com.example.supercart2.ui.theme.SuperCartColors

@Composable
fun HierarchicalCategoryDisplay(
    categories: List<CategoryWithSubCategories>,
    searchQuery: String = "",
    isAllExpanded: Boolean = false
) {
    // State for expanded/collapsed categories and sub-categories
    val categoryExpansion = remember { mutableStateMapOf<String, Boolean>() }
    val subCategoryExpansion = remember { mutableStateMapOf<String, Boolean>() }
    
    // Initialize expansion state for all categories and sub-categories
    LaunchedEffect(categories, searchQuery, isAllExpanded) {
        categories.forEach { categoryWithSubs ->
            // If searching, always expand categories with matches
            if (searchQuery.isNotBlank()) {
                categoryExpansion[categoryWithSubs.category.uuid] = true
            } else {
                // Use the global collapse/expand all state
                categoryExpansion[categoryWithSubs.category.uuid] = isAllExpanded
            }
            
            categoryWithSubs.subCategories.forEach { subCategoryWithGroceries ->
                // If searching, always expand sub-categories with matches
                if (searchQuery.isNotBlank()) {
                    subCategoryExpansion[subCategoryWithGroceries.subCategory.uuid] = true
                } else {
                    // Use the global collapse/expand all state
                    subCategoryExpansion[subCategoryWithGroceries.subCategory.uuid] = isAllExpanded
                }
            }
        }
    }
    
    if (categories.isEmpty() && searchQuery.isNotBlank()) {
        // No search results
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = SuperCartColors.gray,
                    modifier = Modifier.size(56.dp) // Larger search icon for no results
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No groceries found",
                    style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                    color = SuperCartColors.gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Try a different search term",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = SuperCartColors.gray
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp) // Add bottom padding to avoid bottom navigation bar
        ) {
            items(categories) { categoryWithSubs ->
                CategoryCard(
                    categoryWithSubs = categoryWithSubs,
                    isExpanded = categoryExpansion[categoryWithSubs.category.uuid] ?: false,
                    onToggleExpansion = {
                        categoryExpansion[categoryWithSubs.category.uuid] = 
                            !(categoryExpansion[categoryWithSubs.category.uuid] ?: false)
                    },
                    subCategoryExpansion = subCategoryExpansion,
                    onSubCategoryToggleExpansion = { subCategoryId ->
                        subCategoryExpansion[subCategoryId] = 
                            !(subCategoryExpansion[subCategoryId] ?: true)
                    }
                )
            }
        }
    }
}

@Composable
private fun CategoryCard(
    categoryWithSubs: CategoryWithSubCategories,
    isExpanded: Boolean,
    onToggleExpansion: () -> Unit,
    subCategoryExpansion: Map<String, Boolean>,
    onSubCategoryToggleExpansion: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // Category Header (clickable for expand/collapse)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpansion() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = categoryWithSubs.category.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f)
                )
                
                // Groceries count badge
                Badge(
                    containerColor = SuperCartColors.primaryGreen,
                    contentColor = SuperCartColors.white
                ) {
                    Text(
                        text = categoryWithSubs.subCategories.sumOf { it.groceries.size }.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Expansion indicator
                Text(
                    text = if (isExpanded) "▲" else "▼",
                    fontSize = 18.sp,
                    color = SuperCartColors.primaryGreen
                )
            }
            
            Divider(color = SuperCartColors.lightGray)
            
            // Expanded Content
            if (isExpanded) {
                if (categoryWithSubs.subCategories.isNotEmpty()) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        categoryWithSubs.subCategories.forEach { subCategoryWithGroceries ->
                                                         SubCategoryCard(
                                 subCategoryWithGroceries = subCategoryWithGroceries,
                                 isExpanded = subCategoryExpansion[subCategoryWithGroceries.subCategory.uuid] ?: false,
                                 onToggleExpansion = {
                                     onSubCategoryToggleExpansion(subCategoryWithGroceries.subCategory.uuid)
                                 }
                             )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                } else {
                    // Empty state for no sub-categories
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = SuperCartColors.gray,
                            modifier = Modifier.size(20.dp) // Larger info icon
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "No sub-categories yet",
                            fontSize = 14.sp,
                            color = SuperCartColors.gray,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubCategoryCard(
    subCategoryWithGroceries: SubCategoryWithGroceries,
    isExpanded: Boolean,
    onToggleExpansion: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F9FA) // Very light gray background
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            // Sub-Category Header (clickable for expand/collapse)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpansion() }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subCategoryWithGroceries.subCategory.name,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                
                // Groceries count badge
                Badge(
                    containerColor = SuperCartColors.blue,
                    contentColor = SuperCartColors.white
                ) {
                    Text(
                        text = subCategoryWithGroceries.groceries.size.toString(),
                        fontSize = 10.sp
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Expansion indicator
                Text(
                    text = if (isExpanded) "▲" else "▼",
                    fontSize = 14.sp,
                    color = SuperCartColors.blue
                )
            }
            
            // Expanded Content
            if (isExpanded) {
                Divider(color = SuperCartColors.lightGray)
                if (subCategoryWithGroceries.groceries.isNotEmpty()) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        subCategoryWithGroceries.groceries.forEach { grocery ->
                            GroceryItem(grocery = grocery)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                } else {
                    // Empty state
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = SuperCartColors.gray,
                            modifier = Modifier.size(18.dp) // Larger info icon
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "No groceries yet",
                            fontSize = 12.sp,
                            color = SuperCartColors.gray,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GroceryItem(grocery: Grocery) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color.White,
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Grocery icon
        Icon(
            imageVector = Icons.Default.ShoppingCart,
            contentDescription = null,
            tint = SuperCartColors.primaryGreen,
            modifier = Modifier.size(20.dp) // Larger grocery icon
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Grocery name
        Text(
            text = grocery.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        
        // Date indicator with better styling (only show if date exists)
        if (grocery.date != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = SuperCartColors.lightGray
                ),
                shape = RoundedCornerShape(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                                    Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Expires",
                    tint = SuperCartColors.darkGray,
                    modifier = Modifier.size(12.dp) // Larger date icon
                )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = grocery.date.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd")),
                        fontSize = 11.sp,
                        color = SuperCartColors.darkGray
                    )
                }
            }
        }
    }
}
