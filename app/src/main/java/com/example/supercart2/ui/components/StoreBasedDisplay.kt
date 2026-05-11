package com.example.supercart2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.CategoryWithSubCategories
import com.example.supercart2.models.Grocery
import com.example.supercart2.models.Store
import com.example.supercart2.ui.theme.SuperCartColors

data class StoreWithCategories(
    val store: Store?,  // null for "Not Linked" section
    val categories: List<CategoryWithSubCategories>
)

@Composable
fun StoreBasedDisplay(
    categories: List<CategoryWithSubCategories>,
    searchQuery: String,
    isAllExpanded: Boolean,
    onEditGrocery: (Grocery) -> Unit,
    modifier: Modifier = Modifier,
    useScroll: Boolean = true,
    onNavigateToStore: (storeId: String) -> Unit = {}
) {
    val version = DataManagerObject.version
    
    // Group groceries by store
    val storesWithCategories = remember(categories, version) {
        DataManagerObject.groupGroceriesByStore(categories)
    }
    
    if (useScroll) {
        LazyColumn(modifier = modifier.fillMaxSize()) {
            items(
                items = storesWithCategories,
                key = { storeWithCats -> storeWithCats.store?.uuid ?: "not-linked" }
            ) { storeWithCats ->
                StoreSection(
                    storeWithCategories = storeWithCats,
                    searchQuery = searchQuery,
                    isAllExpanded = isAllExpanded,
                    onEditGrocery = onEditGrocery,
                    onNavigateToStore = onNavigateToStore
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    } else {
        Column(modifier = modifier) {
            storesWithCategories.forEach { storeWithCats ->
                StoreSection(
                    storeWithCategories = storeWithCats,
                    searchQuery = searchQuery,
                    isAllExpanded = isAllExpanded,
                    onEditGrocery = onEditGrocery,
                    onNavigateToStore = onNavigateToStore
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun StoreSection(
    storeWithCategories: StoreWithCategories,
    searchQuery: String,
    isAllExpanded: Boolean,
    onEditGrocery: (Grocery) -> Unit,
    onNavigateToStore: (storeId: String) -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(true) }
    
    val totalItems = remember(storeWithCategories) {
        storeWithCategories.categories.sumOf { categoryWithSubs ->
            categoryWithSubs.subCategories.sumOf { it.groceries.size }
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = SuperCartColors.white
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Store Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .background(
                        if (storeWithCategories.store == null) {
                            SuperCartColors.lightGray.copy(alpha = 0.3f)
                        } else {
                            SuperCartColors.primaryGreen.copy(alpha = 0.1f)
                        }
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (storeWithCategories.store == null) {
                            Icons.Default.ShoppingCart
                        } else {
                            Icons.Default.Place
                        },
                        contentDescription = null,
                        tint = SuperCartColors.primaryGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = storeWithCategories.store?.name ?: "Not Linked to Any Store",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (storeWithCategories.store == null) {
                                SuperCartColors.gray
                            } else {
                                SuperCartColors.black
                            }
                        )
                        Text(
                            text = "$totalItems items",
                            fontSize = 12.sp,
                            color = SuperCartColors.gray
                        )
                    }
                }
                
                Icon(
                    imageVector = if (isExpanded) {
                        Icons.Default.KeyboardArrowUp
                    } else {
                        Icons.Default.KeyboardArrowDown
                    },
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = SuperCartColors.darkGray,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Categories within store (when expanded)
            if (isExpanded) {
                Divider(color = SuperCartColors.lightGray)
                
                // Use HierarchicalCategoryDisplay for categories within store
                HierarchicalCategoryDisplay(
                    categories = storeWithCategories.categories,
                    searchQuery = searchQuery,
                    isAllExpanded = isAllExpanded,
                    onEditGrocery = onEditGrocery,
                    useScroll = false,
                    isShoppingList = true,
                    modifier = Modifier.padding(8.dp),
                    onNavigateToStore = onNavigateToStore
                )
            }
        }
    }
}
