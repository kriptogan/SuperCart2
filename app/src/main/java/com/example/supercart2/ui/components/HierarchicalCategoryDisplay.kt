package com.example.supercart2.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.supercart2.data.CategoryWithSubCategories
import com.example.supercart2.data.SubCategoryWithGroceries
import com.example.supercart2.models.Grocery
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import kotlinx.coroutines.launch

@Composable
fun HierarchicalCategoryDisplay(
    categories: List<CategoryWithSubCategories>,
    searchQuery: String,
    isAllExpanded: Boolean,
    onEditGrocery: (Grocery) -> Unit,
    modifier: Modifier = Modifier,
    useScroll: Boolean = true,
    isShoppingList: Boolean = false
) {
    if (useScroll) {
        LazyColumn(
            modifier = modifier.fillMaxSize()
        ) {
            items(categories) { categoryWithSubs ->
                CategorySection(
                    categoryWithSubs = categoryWithSubs,
                    searchQuery = searchQuery,
                    isAllExpanded = isAllExpanded,
                    onEditGrocery = onEditGrocery,
                    isShoppingList = isShoppingList
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    } else {
        Column(modifier = modifier) {
            categories.forEach { categoryWithSubs ->
                CategorySection(
                    categoryWithSubs = categoryWithSubs,
                    searchQuery = searchQuery,
                    isAllExpanded = isAllExpanded,
                    onEditGrocery = onEditGrocery,
                    isShoppingList = isShoppingList
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun CategorySection(
    categoryWithSubs: CategoryWithSubCategories,
    searchQuery: String,
    isAllExpanded: Boolean,
    onEditGrocery: (Grocery) -> Unit,
    isShoppingList: Boolean = false
) {
    var isExpanded by remember { mutableStateOf(isAllExpanded) }
    var subCategoriesExpanded by remember { mutableStateOf(isAllExpanded) }
    
    // Update expansion state when isAllExpanded changes
    LaunchedEffect(isAllExpanded) {
        isExpanded = isAllExpanded
        subCategoriesExpanded = isAllExpanded
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = SuperCartColors.white
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column {
            // Category header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category name and count
                Column {
                    Text(
                        text = categoryWithSubs.category.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${categoryWithSubs.getGroceriesCount()} items",
                        fontSize = 12.sp,
                        color = SuperCartColors.gray
                    )
                }
                
                // Expand/collapse icon
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = SuperCartColors.darkGray
                    )
                }
            }
            
            // Sub-categories (when expanded)
            if (isExpanded) {
                categoryWithSubs.subCategories.forEach { subCategoryWithGroceries ->
                    SubCategorySection(
                        subCategoryWithGroceries = subCategoryWithGroceries,
                        onEditGrocery = onEditGrocery,
                    isShoppingList = isShoppingList,
                        isAllExpanded = subCategoriesExpanded
                    )
                }
            }
        }
    }
}

@Composable
private fun SubCategorySection(
    subCategoryWithGroceries: SubCategoryWithGroceries,
    onEditGrocery: (Grocery) -> Unit,
    isAllExpanded: Boolean,
    isShoppingList: Boolean
) {
    var isExpanded by remember { mutableStateOf(isAllExpanded) }
    
    // Update expansion state when isAllExpanded changes
    LaunchedEffect(isAllExpanded) {
        isExpanded = isAllExpanded
    }
    
    Column {
        // Sub-category header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SuperCartColors.lightGray.copy(alpha = 0.2f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sub-category name and count
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subCategoryWithGroceries.subCategory.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "(${subCategoryWithGroceries.groceries.size})",
                    fontSize = 12.sp,
                    color = SuperCartColors.gray
                )
            }
            
            // Expand/collapse icon
            IconButton(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = SuperCartColors.darkGray
                )
            }
        }
        
        // Expanded Content
        if (isExpanded) {
            Divider(color = SuperCartColors.lightGray)
            if (subCategoryWithGroceries.groceries.isNotEmpty()) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    subCategoryWithGroceries.groceries.forEach { grocery ->
                        key(grocery.uuid) {
                            GroceryItem(
                                grocery = grocery,
                                onEdit = { onEditGrocery(grocery) },
                                isShoppingList = isShoppingList
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
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
                        modifier = Modifier.size(18.dp)
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

@Composable
private fun GroceryItem(
    grocery: Grocery,
    onEdit: () -> Unit,
    isShoppingList: Boolean = false
) {
    // Observe version to trigger recomposition
    val version = DataManagerObject.version
    val scope = rememberCoroutineScope()
    
    // Get current grocery state to ensure we have latest data
    val currentGrocery = remember(grocery.uuid, version) {
        android.util.Log.d("datastore test", "Recomputing grocery state for ${grocery.name}, version: $version")
        DataManagerObject.categories
            .asSequence()
            .flatMap { it.subCategories }
            .flatMap { it.groceries }
            .find { it.uuid == grocery.uuid } ?: grocery
    }

    // Find category name
    val categoryName = remember(currentGrocery.categoryId) {
        DataManagerObject.categories
            .find { it.category.uuid == currentGrocery.categoryId }
            ?.category?.name ?: ""
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (currentGrocery.isBought) SuperCartColors.lightGreen.copy(alpha = 0.1f) else Color.White,
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (currentGrocery.isBought) {
            // Bought item layout: name -> category -> remove
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = currentGrocery.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = categoryName,
                    fontSize = 12.sp,
                    color = SuperCartColors.gray
                )
            }
            
            // Remove icon
            IconButton(
                onClick = {
                    val newGrocery = currentGrocery.copy(
                        inShoppingList = false,
                        isBought = false
                    )

                    // Find and update the grocery in the data manager
                    DataManagerObject.categories.forEach { category ->
                        category.subCategories.forEach { subCategory ->
                            val index = subCategory.groceries.indexOfFirst { it.uuid == currentGrocery.uuid }
                            if (index != -1) {
                                subCategory.groceries[index] = newGrocery
                                android.util.Log.d("datastore test",
                                    "Removed ${currentGrocery.name} from shopping list")
                                DataManagerObject.updateData()
                                
                                // Save to DataStore
                                scope.launch {
                                    DataStoreManager.saveDataGlobally()
                                    android.util.Log.d("datastore test", "Saved removal to DataStore")
                                }
                                return@forEach
                            }
                        }
                    }
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove from list",
                    tint = Color.Red,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            // Regular item layout
            Text(
                text = currentGrocery.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            
            if (isShoppingList) {
                // Options menu for shopping list
                Box {
                    var expanded by remember { mutableStateOf(false) }

                    IconButton(
                        onClick = { expanded = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = SuperCartColors.primaryGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        // Remove from cart option
                        DropdownMenuItem(
                            text = { Text("Remove from shopping list") },
                            onClick = {
                                val newGrocery = currentGrocery.copy(
                                    inShoppingList = false
                                )

                                // Find and update the grocery in the data manager
                                DataManagerObject.categories.forEach { category ->
                                    category.subCategories.forEach { subCategory ->
                                        val index = subCategory.groceries.indexOfFirst { it.uuid == currentGrocery.uuid }
                                        if (index != -1) {
                                            subCategory.groceries[index] = newGrocery
                                            android.util.Log.d("datastore test",
                                                "Removed ${currentGrocery.name} from shopping list")
                                            DataManagerObject.updateData()
                                            expanded = false
                                            return@forEach
                                        }
                                    }
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    tint = SuperCartColors.primaryGreen
                                )
                            }
                        )

                        // Edit option
                        DropdownMenuItem(
                            text = { Text("Edit item") },
                            onClick = {
                                onEdit()
                                expanded = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = SuperCartColors.primaryGreen
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Check mark icon for bought status
                IconButton(
                    onClick = {
                        val newGrocery = currentGrocery.copy(
                            isBought = !currentGrocery.isBought
                        )

                        // Find and update the grocery in the data manager
                        DataManagerObject.categories.forEach { category ->
                            category.subCategories.forEach { subCategory ->
                                val index = subCategory.groceries.indexOfFirst { it.uuid == currentGrocery.uuid }
                                if (index != -1) {
                                    subCategory.groceries[index] = newGrocery
                                    android.util.Log.d("datastore test",
                                        "Toggled bought status for ${currentGrocery.name} to ${newGrocery.isBought}")
                                    DataManagerObject.updateData()
                                    
                                    // Save to DataStore
                                    scope.launch {
                                        DataStoreManager.saveDataGlobally()
                                        android.util.Log.d("datastore test", "Saved bought status to DataStore")
                                    }
                                    
                                    return@forEach
                                }
                            }
                        }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = if (currentGrocery.isBought) "Mark as not bought" else "Mark as bought",
                        tint = if (currentGrocery.isBought) SuperCartColors.primaryGreen else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                // Action icons for home screen (edit and cart)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Edit icon
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit grocery",
                            tint = SuperCartColors.primaryGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    // Cart icon with dynamic color based on shopping list status
                    IconButton(
                        onClick = {
                            // Toggle the shopping list status
                            val newGrocery = currentGrocery.copy(
                                inShoppingList = !currentGrocery.inShoppingList
                            )
                            
                            // Find and update the grocery in the data manager
                            DataManagerObject.categories.forEach { category ->
                                category.subCategories.forEach { subCategory ->
                                    val index = subCategory.groceries.indexOfFirst { it.uuid == currentGrocery.uuid }
                                    if (index != -1) {
                                        subCategory.groceries[index] = newGrocery
                                        android.util.Log.d("datastore test", 
                                            "Toggled shopping list status for ${currentGrocery.name} to ${newGrocery.inShoppingList}")
                                        DataManagerObject.updateData()
                                        return@IconButton
                                    }
                                }
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = if (currentGrocery.inShoppingList) "Remove from shopping list" else "Add to shopping list",
                            tint = if (currentGrocery.inShoppingList) SuperCartColors.primaryGreen else Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}