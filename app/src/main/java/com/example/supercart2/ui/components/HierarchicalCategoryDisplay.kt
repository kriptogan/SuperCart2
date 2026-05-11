package com.example.supercart2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.supercart2.R
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.data.CategoryWithSubCategories
import com.example.supercart2.data.SubCategoryWithGroceries
import com.example.supercart2.models.Grocery
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing
import com.example.supercart2.utils.localizedCategoryDisplayName
import com.example.supercart2.utils.localizedSubCategoryDisplayName
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
    showAlerts: Boolean = true,
    onNavigateToStore: (storeId: String) -> Unit = {}
) {
    if (useScroll) {
        LazyColumn(
            modifier = modifier.fillMaxSize()
        ) {
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
                    showAlerts = showAlerts,
                    onNavigateToStore = onNavigateToStore
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    } else {
        Column(modifier = modifier) {
            Spacer(modifier = Modifier.height(SuperCartSpacing.lg))

            categories.forEach { categoryWithSubs ->
                CategorySection(
                    categoryWithSubs = categoryWithSubs,
                    searchQuery = searchQuery,
                    isAllExpanded = isAllExpanded,
                    onEditGrocery = onEditGrocery,
                    isShoppingList = isShoppingList,
                    showAlerts = showAlerts,
                    onNavigateToStore = onNavigateToStore
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
    showAlerts: Boolean = true,
    onNavigateToStore: (storeId: String) -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(isAllExpanded) }
    var subCategoriesExpanded by remember { mutableStateOf(isAllExpanded) }

    LaunchedEffect(isAllExpanded) {
        isExpanded = isAllExpanded
        subCategoriesExpanded = isAllExpanded
    }

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = SuperCartColors.darkGray,
                    modifier = Modifier.size(24.dp)
                )
            }

            if (isExpanded) {
                categoryWithSubs.subCategories.forEach { subCategoryWithGroceries ->
                    SubCategorySection(
                        subCategoryWithGroceries = subCategoryWithGroceries,
                        onEditGrocery = onEditGrocery,
                        isShoppingList = isShoppingList,
                        isAllExpanded = subCategoriesExpanded,
                        showAlerts = showAlerts,
                        onNavigateToStore = onNavigateToStore
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
    showAlerts: Boolean = true,
    onNavigateToStore: (storeId: String) -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(isAllExpanded) }

    LaunchedEffect(isAllExpanded) {
        isExpanded = isAllExpanded
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SuperCartColors.lightGray.copy(alpha = 0.2f))
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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

            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = SuperCartColors.darkGray,
                modifier = Modifier.size(24.dp)
            )
        }

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
                                showAlerts = showAlerts,
                                onNavigateToStore = onNavigateToStore
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            } else {
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

// ─── Shopping List Item Layout (unchanged) ────────────────────────────────────

@Composable
private fun GroceryItem(
    grocery: Grocery,
    onEdit: () -> Unit,
    isShoppingList: Boolean = false,
    showAlerts: Boolean = true,
    onNavigateToStore: (storeId: String) -> Unit = {}
) {
    val version = DataManagerObject.version
    val scope = rememberCoroutineScope()
    var showBuyHistory by remember { mutableStateOf(false) }

    val currentGrocery = remember(grocery.uuid, version) {
        android.util.Log.d("datastore test", "Recomputing grocery state for ${grocery.name}, version: $version")
        DataManagerObject.categories
            .asSequence()
            .flatMap { it.subCategories }
            .flatMap { it.groceries }
            .find { it.uuid == grocery.uuid } ?: grocery
    }

    val categoryName = remember(currentGrocery.categoryId) {
        DataManagerObject.categories
            .find { it.category.uuid == currentGrocery.categoryId }
            ?.category?.name ?: ""
    }

    val hasAlert = remember(currentGrocery, isShoppingList, showAlerts) {
        if (isShoppingList || !showAlerts) {
            false
        } else {
            val today = LocalDate.now()
            val tomorrow = today.plusDays(1)
            val isExpiringSoon = currentGrocery.expirationDate?.let { expDate ->
                expDate <= tomorrow
            } ?: false
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
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .background(
                when {
                    isShoppingList && currentGrocery.isBought -> SuperCartColors.lightGreen.copy(alpha = 0.1f)
                    else -> Color(0xFFF8F9FA)
                },
                shape = RoundedCornerShape(8.dp)
            )
            .then(
                if (hasAlert) {
                    Modifier.border(
                        width = 2.dp,
                        color = Color(0xFFE53935),
                        shape = RoundedCornerShape(8.dp)
                    )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isShoppingList && currentGrocery.isBought) {
            Column(modifier = Modifier.weight(1f)) {
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

            IconButton(
                onClick = {
                    DataManagerObject.updateGrocery(currentGrocery.uuid) {
                        it.copy(isBought = false)
                    }
                    scope.launch { DataStoreManager.saveDataGlobally() }
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentGrocery.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                if (currentGrocery.storeIds.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        currentGrocery.storeIds.forEach { storeId ->
                            val storeName = remember(storeId, version) {
                                DataManagerObject.stores.find { it.uuid == storeId }?.name ?: "Unknown"
                            }
                            val storeAlias = remember(storeName) { storeAlias(storeName) }
                            androidx.compose.material3.AssistChip(
                                onClick = { onNavigateToStore(storeId) },
                                label = { Text(text = storeAlias, fontSize = 10.sp) },
                                modifier = Modifier.padding(end = 4.dp),
                                colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                                    containerColor = SuperCartColors.lightGray.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                }
            }

            if (isShoppingList) {
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
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.remove_from_shopping_list), color = SuperCartColors.black) },
                            onClick = {
                                DataManagerObject.updateGrocery(currentGrocery.uuid) {
                                    it.copy(inShoppingList = false)
                                }
                                scope.launch { DataStoreManager.saveDataGlobally() }
                                expanded = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.ShoppingCart, null, tint = SuperCartColors.primaryGreen)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.view_buy_history), color = SuperCartColors.black) },
                            onClick = { showBuyHistory = true; expanded = false },
                            leadingIcon = {
                                Icon(Icons.Outlined.DateRange, null, tint = SuperCartColors.primaryGreen)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit_item), color = SuperCartColors.black) },
                            onClick = { onEdit(); expanded = false },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, null, tint = SuperCartColors.primaryGreen)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        DataManagerObject.toggleBoughtStatus(currentGrocery.uuid)
                        scope.launch { DataStoreManager.saveDataGlobally() }
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
                Box {
                    var expanded by remember { mutableStateOf(false) }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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

                        IconButton(
                            onClick = {
                                DataManagerObject.toggleShoppingListStatus(currentGrocery.uuid)
                                scope.launch { DataStoreManager.saveDataGlobally() }
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = if (currentGrocery.inShoppingList)
                                    "Remove from shopping list" else "Add to shopping list",
                                tint = if (currentGrocery.inShoppingList) SuperCartColors.primaryGreen else Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit_item), color = SuperCartColors.black) },
                            onClick = { onEdit(); expanded = false },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, null, tint = SuperCartColors.primaryGreen)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.view_buy_history), color = SuperCartColors.black) },
                            onClick = { showBuyHistory = true; expanded = false },
                            leadingIcon = {
                                Icon(Icons.Outlined.DateRange, null, tint = SuperCartColors.primaryGreen)
                            }
                        )
                    }
                }
            }
        }
    }

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

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun storeAlias(name: String): String {
    val words = name.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
    return if (words.size == 1) {
        words[0].take(3)
    } else {
        words.take(3).joinToString("") { it.first().uppercaseChar().toString() }
    }
}