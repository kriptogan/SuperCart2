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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.supercart2.R
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.data.ImageManager
import com.example.supercart2.data.CategoryWithSubCategories
import com.example.supercart2.data.SubCategoryWithGroceries
import com.example.supercart2.models.Grocery
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing
import com.example.supercart2.utils.localizedCategoryDisplayName
import com.example.supercart2.utils.localizedSubCategoryDisplayName
import kotlinx.coroutines.launch
import java.time.LocalDate

private val GROCERY_CARD_HEIGHT = 150.dp

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
                if (!isShoppingList) {
                    // Home screen: 3-column grid of vertical cards
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        val chunkedGroceries = subCategoryWithGroceries.groceries.chunked(3)
                        chunkedGroceries.forEach { rowItems ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                rowItems.forEach { grocery ->
                                    key(grocery.uuid) {
                                        GroceryCardHome(
                                            grocery = grocery,
                                            onEdit = { onEditGrocery(grocery) },
                                            showAlerts = showAlerts,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                // Fill remaining empty slots so cards stay the same width
                                repeat(3 - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                } else {
                    // Shopping list: existing horizontal item layout
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        subCategoryWithGroceries.groceries.forEach { grocery ->
                            key(grocery.uuid) {
                                GroceryItem(
                                    grocery = grocery,
                                    onEdit = { onEditGrocery(grocery) },
                                    isShoppingList = true,
                                    showAlerts = showAlerts
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
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

// ─── Home Screen Card Layout ─────────────────────────────────────────────────

@Composable
private fun GroceryCardHome(
    grocery: Grocery,
    onEdit: () -> Unit,
    showAlerts: Boolean = true,
    modifier: Modifier = Modifier
) {
    val version = DataManagerObject.version
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showBuyHistory by remember { mutableStateOf(false) }
    var showImageViewer by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    val currentGrocery = remember(grocery.uuid, version) {
        DataManagerObject.categories
            .asSequence()
            .flatMap { it.subCategories }
            .flatMap { it.groceries }
            .find { it.uuid == grocery.uuid } ?: grocery
    }

    val hasAlert = remember(currentGrocery, showAlerts) {
        if (!showAlerts) false
        else {
            val today = LocalDate.now()
            val tomorrow = today.plusDays(1)
            val isExpiringSoon = currentGrocery.expirationDate?.let { it <= tomorrow } ?: false
            val needsToBuy = currentGrocery.averageBuyDays?.let { avgDays ->
                currentGrocery.buyEvents.maxOrNull()?.let { lastBuyDate ->
                    (today.toEpochDay() - lastBuyDate.toEpochDay()) >= avgDays
                }
            } ?: false
            isExpiringSoon || needsToBuy
        }
    }

    val storeNames = remember(currentGrocery.storeIds, version) {
        currentGrocery.storeIds.mapNotNull { storeId ->
            DataManagerObject.stores.find { it.uuid == storeId }?.name
        }
    }

    // Only show the image icon when the file actually exists on disk
    val hasImageFile = remember(currentGrocery.imageUUID, version) {
        currentGrocery.imageUUID?.let { uuid ->
            ImageManager.getLocalImageFile(uuid, context) != null
        } ?: false
    }

    val storeDisplayText: String? = when {
        storeNames.isEmpty() -> null
        storeNames.size == 1 -> storeNames[0]
        else -> "${storeNames[0]} +${storeNames.size - 1}"
    }

    Card(
        modifier = modifier
            .padding(4.dp)
            .height(GROCERY_CARD_HEIGHT)
            .then(
                if (hasAlert) Modifier.border(2.dp, Color(0xFFE53935), RoundedCornerShape(12.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header: 3-dots | image icon | cart ─────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 3-dots options menu
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = SuperCartColors.primaryGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit_item), color = SuperCartColors.black) },
                            onClick = { onEdit(); menuExpanded = false },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, null, tint = SuperCartColors.primaryGreen)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.view_buy_history), color = SuperCartColors.black) },
                            onClick = { showBuyHistory = true; menuExpanded = false },
                            leadingIcon = {
                                Icon(Icons.Outlined.DateRange, null, tint = SuperCartColors.primaryGreen)
                            }
                        )
                    }
                }

                // Image icon — only visible when item has an image file on disk
                if (hasImageFile) {
                    IconButton(
                        onClick = { showImageViewer = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = stringResource(R.string.grocery_image),
                            tint = SuperCartColors.primaryGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(32.dp))
                }

                // Cart icon — toggles shopping list membership
                IconButton(
                    onClick = {
                        DataManagerObject.toggleShoppingListStatus(currentGrocery.uuid)
                        scope.launch { DataStoreManager.saveDataGlobally() }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = if (currentGrocery.inShoppingList)
                            "Remove from shopping list" else "Add to shopping list",
                        tint = if (currentGrocery.inShoppingList) SuperCartColors.primaryGreen else Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // ── Body: scrollable item name ──────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = currentGrocery.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }

            // ── Footer: store tags ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (storeDisplayText != null) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color.White,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .border(
                                width = 0.5.dp,
                                color = SuperCartColors.gray.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = storeDisplayText,
                            fontSize = 10.sp,
                            color = SuperCartColors.black,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    // Image viewer dialog
    if (showImageViewer && hasImageFile && currentGrocery.imageUUID != null) {
        val imageFile = remember(currentGrocery.imageUUID) {
            ImageManager.getLocalImageFile(currentGrocery.imageUUID!!, context)
        }
        AlertDialog(
            onDismissRequest = { showImageViewer = false },
            title = {
                Text(
                    text = currentGrocery.name,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                if (imageFile != null && imageFile.exists()) {
                    AsyncImage(
                        model = imageFile,
                        contentDescription = stringResource(R.string.grocery_image),
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = stringResource(R.string.grocery_image),
                        color = SuperCartColors.gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showImageViewer = false },
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

    // Buy history dialog
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

// ─── Shopping List Item Layout (unchanged) ────────────────────────────────────

@Composable
private fun GroceryItem(
    grocery: Grocery,
    onEdit: () -> Unit,
    isShoppingList: Boolean = false,
    showAlerts: Boolean = true
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
            .height(102.dp)
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        currentGrocery.storeIds.take(3).forEach { storeId ->
                            val storeName = remember(storeId, version) {
                                DataManagerObject.stores.find { it.uuid == storeId }?.name ?: "Unknown"
                            }
                            androidx.compose.material3.AssistChip(
                                onClick = { },
                                label = { Text(text = storeName, fontSize = 10.sp) },
                                modifier = Modifier.padding(end = 4.dp),
                                colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                                    containerColor = SuperCartColors.lightGray.copy(alpha = 0.3f)
                                )
                            )
                        }
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
