package com.kriptogan.supercart2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kriptogan.supercart2.classes.Category
import com.kriptogan.supercart2.classes.SubCategory

@Composable
fun AppHeader(
    categories: List<Category>,
    subCategories: List<SubCategory>,
    onGroceryCreated: (String, String, String?) -> Unit,
    isAllExpanded: Boolean = true,
    onToggleAll: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showGroceryForm by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header with action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Burger menu button (does nothing for now)
            IconButton(
                onClick = { /* TODO: Implement burger menu */ },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF424242))
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Alert button (does nothing for now)
            IconButton(
                onClick = { /* TODO: Implement alerts */ },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF9800))
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Alerts",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Plus button - now functional for creating groceries
            IconButton(
                onClick = { showGroceryForm = true },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50))
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Grocery",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        // Search bar (does nothing for now)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(
                    color = Color(0xFFF5F5F5),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "חפש מצרך...",
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Toggle expand/collapse all button
        OutlinedButton(
            onClick = onToggleAll,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = if (isAllExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isAllExpanded) "Collapse All" else "Expand All",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isAllExpanded) "Collapse All" else "Expand All",
                fontSize = 14.sp
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
    
    // Grocery creation form dialog
    if (showGroceryForm) {
        ReusableFullScreenWindow(
            isVisible = showGroceryForm,
            onDismiss = { showGroceryForm = false },
            title = "Create New Grocery",
            content = {
                GroceryCreationForm(
                    categories = categories,
                    subCategories = subCategories,
                    onSave = { name, subCategoryId, expirationDate ->
                        onGroceryCreated(name, subCategoryId, expirationDate)
                        showGroceryForm = false
                    },
                    onCancel = { showGroceryForm = false }
                )
            }
        )
    }
}
