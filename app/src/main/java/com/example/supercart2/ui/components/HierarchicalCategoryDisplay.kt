package com.example.supercart2.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.supercart2.R
import com.example.supercart2.data.CategoryWithSubCategories
import com.example.supercart2.data.SubCategoryWithGroceries
import com.example.supercart2.models.Grocery
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.utils.localizedCategoryDisplayName
import com.example.supercart2.utils.localizedSubCategoryDisplayName
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
import java.time.LocalDate

@Composable
fun HierarchicalCategoryDisplay(
    categories: List<CategoryWithSubCategories>,
    searchQuery: String,
    isAllExpanded: Boolean,
    onEditGrocery: (Grocery) -> Unit,
    modifier: Modifier = Modifier,
    useScroll: Boolean = true,
    isShoppingList: Boolean = false,
    showAlerts: Boolean = true
) {
    if (useScroll) {
        LazyColumn(
            modifier = modifier.fillMaxSize()
        ) {
            // Add spacing at the top before first category
            item {
                Spacer(modifier = Modifier.height(SuperCartSpacing.lg))
            }
            
            items(categories) { categoryWithSubs ->
                CategorySection(
                    categoryWithSubs = categoryWithSubs,
                    searchQuery = searchQuery,
                    isAllExpanded = isAllExpanded,
                    onEditGrocery = onEditGrocery,
                    isShoppingList = isShoppingList,
                    showAlerts = showAlerts
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    } else {
        Column(modifier = modifier) {
            // Add spacing at the top before first category
            Spacer(modifier = Modifier.height(SuperCartSpacing.lg))
            
            categories.forEach { categoryWithSubs ->
                CategorySection(
                    categoryWithSubs = categoryWithSubs,
                    searchQuery = searchQuery,
                    isAllExpanded = isAllExpanded,
                    onEditGrocery = onEditGrocery,
                    isShoppingList = isShoppingList,
                    showAlerts = showAlerts
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
    isShoppingList: Boolean = false,
    showAlerts: Boolean = true
) {
    var isExpanded by remember { mutableStateOf(isAllExpanded) }
    var subCategoriesExpanded by remember { mutableStateOf(isAllExpanded) }
    
    // Update expansion state when isAllExpanded changes
    LaunchedEffect(isAllExpanded) {
        isExpanded = isAllExpanded
        subCategoriesExpanded = isAllExpanded
    }
    
    // Auto-expand single sub-category when category is expanded
    LaunchedEffect(isExpanded, categoryWithSubs.subCategories.size) {
        if (isExpanded && categoryWithSubs.subCategories.size == 1) {
            subCategoriesExpanded = true
        }
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
                    .clickable { isExpanded = !isExpanded }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category name and count
                Column {
                    Text(
                        text = localizedCategoryDisplayName(categoryWithSubs.category.name),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${categoryWithSubs.getGroceriesCount()} ${stringResource(R.string.items)}",
                        fontSize = 12.sp,
                        color = SuperCartColors.gray
                    )
                }
                
                // Expand/collapse icon
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = SuperCartColors.darkGray,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Sub-categories (when expanded)
            if (isExpanded) {
                categoryWithSubs.subCategories.forEach { subCategoryWithGroceries ->
                    SubCategorySection(
                        subCategoryWithGroceries = subCategoryWithGroceries,
                        onEditGrocery = onEditGrocery,
                        isShoppingList = isShoppingList,
                        isAllExpanded = subCategoriesExpanded,
                        showAlerts = showAlerts
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
    isShoppingList: Boolean,
    showAlerts: Boolean = true
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
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sub-category name and count
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = localizedSubCategoryDisplayName(subCategoryWithGroceries.subCategory.name),
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
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = SuperCartColors.darkGray,
                modifier = Modifier.size(24.dp)
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
                        key(grocery.uuid) {
                            GroceryItem(
                                grocery = grocery,
                                onEdit = { onEditGrocery(grocery) },
                                isShoppingList = isShoppingList,
                                showAlerts = showAlerts
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
                        text = stringResource(R.string.no_groceries_yet),
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
    isShoppingList: Boolean = false,
    showAlerts: Boolean = true
) {
    // Observe version to trigger recomposition
    val version = DataManagerObject.version
    val scope = rememberCoroutineScope()
    var showBuyHistory by remember { mutableStateOf(false) }
    
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
    
    // Check if this grocery item has an alert (only for home screen, not shopping list, and if alerts are enabled)
    val hasAlert = remember(currentGrocery, isShoppingList, showAlerts) {
        if (isShoppingList || !showAlerts) {
            false // No alerts in shopping list or if alerts are disabled
        } else {
            val today = java.time.LocalDate.now()
            val tomorrow = today.plusDays(1)
            
            // Check expiration date condition
            val isExpiringSoon = currentGrocery.expirationDate?.let { expDate ->
                expDate <= tomorrow // Due tomorrow or already passed
            } ?: false
            
            // Check buy pattern condition
            val needsToBuy = currentGrocery.averageBuyDays?.let { avgDays ->
                currentGrocery.buyEvents.maxOrNull()?.let { lastBuyDate ->
                    val daysSinceLastBuy = today.toEpochDay() - lastBuyDate.toEpochDay()
                    daysSinceLastBuy >= avgDays
                }
            } ?: false
            
            isExpiringSoon || needsToBuy
        }
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(102.dp) // Fixed height to ensure all items take the same vertical space
            .padding(horizontal = 16.dp, vertical = 12.dp) // Outer padding like old design
            .background(
                when {
                    isShoppingList && currentGrocery.isBought -> SuperCartColors.lightGreen.copy(alpha = 0.1f)
                    else -> Color(0xFFF8F9FA) // Very light gray background (matching old design)
                },
                shape = RoundedCornerShape(8.dp) // 8dp rounded corners like old design
            )
            .then(
                if (hasAlert) {
                    Modifier.border(
                        width = 2.dp,
                        color = Color(0xFFE53935), // Red border for alert items
                        shape = RoundedCornerShape(8.dp)
                    )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 12.dp, vertical = 8.dp), // Inner padding like old design
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isShoppingList && currentGrocery.isBought) {
            // Bought item layout: name -> category -> remove (only in shopping list)
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = currentGrocery.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = localizedCategoryDisplayName(categoryName),
                    fontSize = 12.sp,
                    color = SuperCartColors.gray
                )
            }
            
            // Remove icon
            IconButton(
                onClick = {
                    DataManagerObject.updateGrocery(currentGrocery.uuid) { 
                        it.copy(isBought = false)
                    }
                    android.util.Log.d("datastore test",
                        "Marked ${currentGrocery.name} as not bought")
                    
                    // Save to DataStore
                    scope.launch {
                        DataStoreManager.saveDataGlobally()
                        android.util.Log.d("datastore test", "Saved bought status to DataStore")
                    }
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Mark as not bought",
                    tint = Color.Red,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            // Regular item layout
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = currentGrocery.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                
                // Display store badges if grocery has stores linked
                if (currentGrocery.storeIds.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        // Show up to 3 stores
                        currentGrocery.storeIds.take(3).forEach { storeId ->
                            val storeName = remember(storeId, version) {
                                DataManagerObject.stores.find { it.uuid == storeId }?.name ?: "Unknown"
                            }
                            
                            androidx.compose.material3.AssistChip(
                                onClick = { /* Read-only */ },
                                label = { 
                                    Text(
                                        text = storeName, 
                                        fontSize = 10.sp
                                    ) 
                                },
                                modifier = Modifier.padding(end = 4.dp),
                                colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                                    containerColor = SuperCartColors.lightGray.copy(alpha = 0.3f)
                                )
                            )
                        }
                        
                        // Show "+X more" if more than 3 stores
                        if (currentGrocery.storeIds.size > 3) {
                            Text(
                                text = "+${currentGrocery.storeIds.size - 3} ${stringResource(R.string.more)}",
                                fontSize = 10.sp,
                                color = SuperCartColors.gray,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                    }
                }
            }
            
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
                            text = { Text(stringResource(R.string.remove_from_shopping_list), color = SuperCartColors.black) },
                            onClick = {
                                DataManagerObject.updateGrocery(currentGrocery.uuid) { 
                                    it.copy(inShoppingList = false)
                                }
                                android.util.Log.d("datastore test",
                                    "Removed ${currentGrocery.name} from shopping list")
                                
                                // Save to DataStore
                                scope.launch {
                                    DataStoreManager.saveDataGlobally()
                                    android.util.Log.d("datastore test", "Saved shopping list status to DataStore")
                                }
                                expanded = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    tint = SuperCartColors.primaryGreen
                                )
                            }
                        )

                        // View Buy History option
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.view_buy_history), color = SuperCartColors.black) },
                            onClick = {
                                showBuyHistory = true
                                expanded = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.DateRange,
                                    contentDescription = null,
                                    tint = SuperCartColors.primaryGreen
                                )
                            }
                        )

                        // Edit option
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit_item), color = SuperCartColors.black) },
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
                        DataManagerObject.toggleBoughtStatus(currentGrocery.uuid)
                        
                        // Save to DataStore
                        scope.launch {
                            DataStoreManager.saveDataGlobally()
                            android.util.Log.d("datastore test", "Saved bought status to DataStore")
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
                // Options menu for home screen
                Box {
                    var expanded by remember { mutableStateOf(false) }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Options menu
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
                        
                        // Cart icon with dynamic color based on shopping list status
                        IconButton(
                            onClick = {
                                // Toggle the shopping list status
                                DataManagerObject.toggleShoppingListStatus(currentGrocery.uuid)
                                
                                // Save to DataStore
                                scope.launch {
                                    DataStoreManager.saveDataGlobally()
                                    android.util.Log.d("datastore test", "Saved shopping list status to DataStore")
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

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        // Edit option
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit_item), color = SuperCartColors.black) },
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

                        // View Buy History option
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.view_buy_history), color = SuperCartColors.black) },
                            onClick = {
                                showBuyHistory = true
                                expanded = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.DateRange,
                                    contentDescription = null,
                                    tint = SuperCartColors.primaryGreen
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    // Buy History Dialog
    if (showBuyHistory) {
        AlertDialog(
            onDismissRequest = { showBuyHistory = false },
            title = {
                Text(
                    text = "Buy History - ${currentGrocery.name}",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                if (currentGrocery.buyEvents.isEmpty()) {
                    // Empty state
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = SuperCartColors.gray,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No purchase history yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SuperCartColors.gray,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // List of buy events
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        currentGrocery.buyEvents.sortedDescending().forEach { date ->
                            Text(
                                text = date.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showBuyHistory = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuperCartColors.primaryGreen,
                        contentColor = SuperCartColors.white
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}