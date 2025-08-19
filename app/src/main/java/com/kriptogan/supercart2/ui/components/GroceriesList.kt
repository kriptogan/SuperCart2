package com.kriptogan.supercart2.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kriptogan.supercart2.classes.Grocery
import com.kriptogan.supercart2.classes.Category
import com.kriptogan.supercart2.classes.FirebaseManager
import com.kriptogan.supercart2.classes.LocalStorageManager
import androidx.compose.material3.MaterialTheme
import kotlinx.coroutines.launch

@Composable
fun GroceriesList(
    firebaseManager: FirebaseManager,
    localStorageManager: LocalStorageManager,
    modifier: Modifier = Modifier,
    onGroceryClick: ((Grocery) -> Unit)? = null,
    showTitle: Boolean = true,
    title: String = "Groceries",
    onDataChanged: (() -> Unit)? = null,
    key: Any? = null,
    groceries: List<Grocery> = emptyList(),
    availableCategories: List<Category> = emptyList(),
    showEditButton: Boolean = false,
    onEditGrocery: ((Grocery) -> Unit)? = null
) {
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Fetch groceries from both sources only when component first loads
    LaunchedEffect(Unit) {
        try {
            isLoading = true
            
            // Try to get Firebase groceries and merge them
            try {
                // For now, just get local groceries since Firebase methods might not exist yet
                val localGroceries = localStorageManager.getGroceries()
                
                // Notify parent component that data has changed
                onDataChanged?.invoke()
                
            } catch (e: Exception) {
                // If Firebase fails, keep local groceries
                error = "Could not sync with cloud: ${e.message}"
            }
            
        } catch (e: Exception) {
            error = "Error loading groceries: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Column(modifier = modifier) {
        if (showTitle) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            error != null -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "⚠️ Warning",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error!!,
                            fontSize = 14.sp,
                            color = Color(0xFFD32F2F),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            groceries.isEmpty() -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "No Groceries",
                            modifier = Modifier.size(48.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No groceries yet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Create your first grocery item to get started",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(groceries) { grocery ->
                        GroceryCard(
                            grocery = grocery,
                            availableCategories = availableCategories,
                            onClick = { onGroceryClick?.invoke(grocery) },
                            showEditButton = showEditButton,
                            onEditClick = { onEditGrocery?.invoke(grocery) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GroceryCard(
    grocery: Grocery,
    availableCategories: List<Category>,
    onClick: (() -> Unit)? = null,
    showEditButton: Boolean = false,
    onEditClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = { onClick?.invoke() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header row with name and edit button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = "Grocery",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Text(
                    text = grocery.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                
                // Edit button - only show when enabled
                if (showEditButton && onEditClick != null) {
                    IconButton(
                        onClick = { onEditClick.invoke() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Grocery",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Grocery details
            Column(
                modifier = Modifier.padding(start = 40.dp)
            ) {
                // Category
                if (grocery.categoryId != null) {
                    val category = availableCategories.find { it.uuid == grocery.categoryId }
                    if (category != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Category",
                                modifier = Modifier.size(16.dp),
                                tint = Color.Gray
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = category.name,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
                
                // Expiration date
                if (grocery.expirationDate != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Expiration",
                            modifier = Modifier.size(16.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Expires: ${grocery.expirationDate}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
                
                // Status indicators
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (grocery.inShoppingList) {
                        Text(
                            text = "🛒 In Shopping List",
                            fontSize = 12.sp,
                            color = Color(0xFF1976D2)
                        )
                    }
                    
                    if (grocery.isBought) {
                        Text(
                            text = "✅ Bought",
                            fontSize = 12.sp,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }
        }
    }
}
