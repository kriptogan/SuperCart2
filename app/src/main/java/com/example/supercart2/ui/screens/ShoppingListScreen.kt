package com.example.supercart2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.models.Grocery
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing
import com.example.supercart2.data.CategoryWithSubCategories
import com.example.supercart2.data.SubCategoryWithGroceries
import com.example.supercart2.ui.components.HierarchicalCategoryDisplay
import com.example.supercart2.ui.components.BurgerMenu
import com.example.supercart2.ui.components.CategoriesManagementDialog
import com.example.supercart2.ui.components.GroceryCreationDialog

@Composable
fun ShoppingListScreen() {
    var showCategoriesManagement by remember { mutableStateOf(false) }
    var showGroceryCreation by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isAllExpanded by remember { mutableStateOf(false) }
    
    // Edit mode state
    var groceryToEdit by remember { mutableStateOf<Grocery?>(null) }
    var isEditMode by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Observe version to trigger recomposition
    val version = DataManagerObject.version
    
    // Filter categories to only include those with shopping list items
    val toBuyCategories = remember(version, searchQuery) {
        DataManagerObject.getSortedCategories().mapNotNull { categoryWithSubs ->
            // Filter subcategories
            val filteredSubCategories = categoryWithSubs.subCategories.mapNotNull { subCategoryWithGroceries ->
                // Filter groceries in shopping list and not bought
                val filteredGroceries = subCategoryWithGroceries.groceries
                    .filter { grocery -> 
                        grocery.inShoppingList && 
                        !grocery.isBought &&
                        (searchQuery.isEmpty() || grocery.name.contains(searchQuery, ignoreCase = true))
                    }
                
                if (filteredGroceries.isNotEmpty()) {
                    // Keep subcategory with filtered groceries
                    SubCategoryWithGroceries(
                        subCategory = subCategoryWithGroceries.subCategory,
                        groceries = mutableListOf<Grocery>().apply { addAll(filteredGroceries) }
                    )
                } else null
            }
            
            if (filteredSubCategories.isNotEmpty()) {
                // Keep category with filtered subcategories
                CategoryWithSubCategories(
                    category = categoryWithSubs.category,
                    subCategories = mutableListOf<SubCategoryWithGroceries>().apply { addAll(filteredSubCategories) }
                )
            } else null
        }
    }
    
    // Filter categories to only include those with bought items
    val boughtCategories = remember(version, searchQuery) {
        DataManagerObject.getSortedCategories().mapNotNull { categoryWithSubs ->
            // Filter subcategories
            val filteredSubCategories = categoryWithSubs.subCategories.mapNotNull { subCategoryWithGroceries ->
                // Filter groceries in shopping list and bought
                val filteredGroceries = subCategoryWithGroceries.groceries
                    .filter { grocery -> 
                        grocery.inShoppingList && 
                        grocery.isBought &&
                        (searchQuery.isEmpty() || grocery.name.contains(searchQuery, ignoreCase = true))
                    }
                
                if (filteredGroceries.isNotEmpty()) {
                    // Keep subcategory with filtered groceries
                    SubCategoryWithGroceries(
                        subCategory = subCategoryWithGroceries.subCategory,
                        groceries = mutableListOf<Grocery>().apply { addAll(filteredGroceries) }
                    )
                } else null
            }
            
            if (filteredSubCategories.isNotEmpty()) {
                // Keep category with filtered subcategories
                CategoryWithSubCategories(
                    category = categoryWithSubs.category,
                    subCategories = mutableListOf<SubCategoryWithGroceries>().apply { addAll(filteredSubCategories) }
                )
            } else null
        }
    }
    
    // Function to handle editing a grocery
    fun onEditGrocery(grocery: Grocery) {
        groceryToEdit = grocery
        isEditMode = true
        showGroceryCreation = true
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Main content area with burger menu and add grocery button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(
                    start = SuperCartSpacing.md,
                    end = SuperCartSpacing.md,
                    bottom = SuperCartSpacing.md,
                    top = SuperCartSpacing.xl
                )
        ) {
            // Burger menu and add grocery button row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = SuperCartSpacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Burger menu with right padding
                Box(
                    modifier = Modifier.padding(start = 5.dp)
                ) {
                    BurgerMenu(
                        onCategoriesManagementClick = {
                            showCategoriesManagement = true
                        }
                    )
                }
                
                // Add Grocery Button (+ icon) with left padding
                Box(
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Card(
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(
                            containerColor = SuperCartColors.white
                        ),
                        border = CardDefaults.outlinedCardBorder(),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 2.dp
                        )
                    ) {
                        IconButton(
                            onClick = { showGroceryCreation = true },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add New Grocery",
                                tint = SuperCartColors.primaryGreen,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }
            }
            
            // Search bar and controls row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = SuperCartSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left spacer to align with burger icon
                Spacer(modifier = Modifier.width(15.dp))
                
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search groceries...") },
                    modifier = Modifier.fillMaxWidth(0.78f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SuperCartColors.primaryGreen,
                        unfocusedBorderColor = SuperCartColors.gray,
                        focusedLabelColor = SuperCartColors.primaryGreen
                    ),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = SuperCartColors.gray,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = SuperCartColors.darkGray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else null
                )
                
                Spacer(modifier = Modifier.width(SuperCartSpacing.sm))
                
                // Collapse/Expand all toggle
                Card(
                    modifier = Modifier
                        .height(56.dp)
                        .width(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = SuperCartColors.white
                    ),
                    border = CardDefaults.outlinedCardBorder(),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 2.dp
                    )
                ) {
                    IconButton(
                        onClick = { isAllExpanded = !isAllExpanded },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = if (isAllExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isAllExpanded) "Collapse All" else "Expand All",
                            tint = SuperCartColors.primaryGreen,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
            
            // To Buy Section
            if (toBuyCategories.isNotEmpty()) {
                Text(
                    text = "Things to Buy",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SuperCartColors.primaryGreen,
                    modifier = Modifier.padding(vertical = SuperCartSpacing.sm)
                )
                HierarchicalCategoryDisplay(
                    categories = toBuyCategories,
                    searchQuery = searchQuery,
                    isAllExpanded = isAllExpanded,
                    onEditGrocery = { onEditGrocery(it) }
                )
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = SuperCartSpacing.sm),
                    colors = CardDefaults.cardColors(
                        containerColor = SuperCartColors.lightGreen.copy(alpha = 0.1f)
                    )
                ) {
                    Text(
                        text = "No items to buy",
                        modifier = Modifier.padding(SuperCartSpacing.md),
                        color = SuperCartColors.gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(SuperCartSpacing.lg))
            
            // Bought Section
            if (boughtCategories.isNotEmpty()) {
                Text(
                    text = "Already Bought",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SuperCartColors.primaryGreen,
                    modifier = Modifier.padding(vertical = SuperCartSpacing.sm)
                )
                HierarchicalCategoryDisplay(
                    categories = boughtCategories,
                    searchQuery = searchQuery,
                    isAllExpanded = isAllExpanded,
                    onEditGrocery = { onEditGrocery(it) }
                )
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = SuperCartSpacing.sm),
                    colors = CardDefaults.cardColors(
                        containerColor = SuperCartColors.lightGreen.copy(alpha = 0.1f)
                    )
                ) {
                    Text(
                        text = "No bought items",
                        modifier = Modifier.padding(SuperCartSpacing.md),
                        color = SuperCartColors.gray
                    )
                }
            }
        }
    }
    
    // Categories Management Dialog
    if (showCategoriesManagement) {
        CategoriesManagementDialog(
            onDismiss = { showCategoriesManagement = false }
        )
    }
    
    // Grocery Creation/Edit Dialog
    if (showGroceryCreation) {
        GroceryCreationDialog(
            groceryToEdit = groceryToEdit,
            onDismiss = { 
                showGroceryCreation = false
                isEditMode = false
                groceryToEdit = null
            },
            onGroceryCreated = { /* Handle grocery creation/edit */ }
        )
    }
}