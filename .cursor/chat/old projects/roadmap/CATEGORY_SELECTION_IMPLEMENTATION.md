# Category Selection Implementation Guide

This document outlines how to implement the full-screen category selection dialog system used in the original SuperCart project for SuperCart2.

## Overview

The category selection system provides users with an intuitive, full-screen interface to select categories when creating grocery items. It replaces dropdown menus with a more touch-friendly, visually appealing approach.

## Key Features

- **Full-Screen Dialog**: Covers entire screen for better visibility
- **Touch-Friendly Interface**: Large category rows with generous padding
- **Integrated Category Management**: Create new categories directly from selection
- **Visual Feedback**: Clear indication of selected categories
- **Consistent Design**: Follows SuperCart design system patterns

## Implementation Steps

### 1. Create Category Data Model

First, create a data class for categories:

```kotlin
data class Category(
    val id: Int,
    val name: String,
    val isDefault: Boolean = false,
    val viewOrder: Int = 0,
    val lastUpdate: String = ""
)
```

### 2. Create Category Selection Dialog Component

Create a new file: `app/src/main/java/com/example/supercart2/ui/components/CategorySelectionDialog.kt`

```kotlin
package com.example.supercart2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.supercart2.ui.theme.SuperCartSpacing
import com.example.supercart2.ui.theme.SuperCartShapes

@Composable
fun CategorySelectionDialog(
    categories: List<Category>,
    selectedCategoryId: Int?,
    onCategorySelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    onCreateNewCategory: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxSize(), // This makes it full-screen
        title = { 
            Text(
                text = "Select Category",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(SuperCartSpacing.sm)
            ) {
                items(categories.sortedBy { it.viewOrder }) { category ->
                    CategoryRow(
                        category = category,
                        isSelected = category.id == selectedCategoryId,
                        onClick = {
                            onCategorySelected(category.id)
                        }
                    )
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Close button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuperCartColors.primaryBlue,
                        contentColor = SuperCartColors.white
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
                
                Spacer(modifier = Modifier.width(SuperCartSpacing.sm))
                
                // Create new category button
                Button(
                    onClick = onCreateNewCategory,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuperCartColors.primaryGreen,
                        contentColor = SuperCartColors.white
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Category"
                    )
                }
            }
        }
    )
}

@Composable
private fun CategoryRow(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isSelected) Color(0xFFE8F5E8) else Color(0xFFF5F5F5),
                shape = SuperCartShapes.small
            )
            .padding(SuperCartSpacing.md)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Category name
        Text(
            text = category.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        // Selection indicator
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
```

### 3. Create Category Creation Dialog Component

Create: `app/src/main/java/com/example/supercart2/ui/components/CreateCategoryDialog.kt`

```kotlin
package com.example.supercart2.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.supercart2.ui.theme.SuperCartSpacing

@Composable
fun CreateCategoryDialog(
    categoryName: String,
    onCategoryNameChange: (String) -> Unit,
    onCreateCategory: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = "Create New Category",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            SuperCartOutlinedTextField(
                value = categoryName,
                onValueChange = onCategoryNameChange,
                label = "Category Name"
            )
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuperCartColors.primaryBlue,
                        contentColor = SuperCartColors.white
                    )
                ) {
                    Text("Cancel")
                }
                
                Button(
                    onClick = onCreateCategory,
                    enabled = categoryName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuperCartColors.primaryGreen,
                        contentColor = SuperCartColors.white
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Done,
                        contentDescription = "Create"
                    )
                }
            }
        }
    )
}
```

### 4. Update Home Screen with Category Selection

Modify your existing `HomeScreen.kt` to include category selection:

```kotlin
@Composable
fun HomeScreen() {
    var showCategorySelection by remember { mutableStateOf(false) }
    var showCreateCategory by remember { mutableStateOf(false) }
    var selectedCategoryId by remember { mutableStateOf<Int?>(null) }
    var newCategoryName by remember { mutableStateOf("") }
    var categories by remember { mutableStateOf(listOf<Category>()) }
    
    // Sample categories - replace with your actual data source
    LaunchedEffect(Unit) {
        categories = listOf(
            Category(1, "Fruits & Vegetables", true, 1),
            Category(2, "Dairy & Eggs", true, 2),
            Category(3, "Meat & Fish", true, 3),
            Category(4, "Pantry", true, 4)
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(SuperCartSpacing.md)
    ) {
        // Category selection button
        SuperCartPrimaryButton(
            text = if (selectedCategoryId != null) {
                val selectedCategory = categories.find { it.id == selectedCategoryId }
                selectedCategory?.name ?: "Select Category"
            } else {
                "Select Category"
            },
            onClick = { showCategorySelection = true }
        )
        
        // Other content...
    }
    
    // Category selection dialog
    if (showCategorySelection) {
        CategorySelectionDialog(
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            onCategorySelected = { categoryId ->
                selectedCategoryId = categoryId
                showCategorySelection = false
            },
            onDismiss = { showCategorySelection = false },
            onCreateNewCategory = {
                showCategorySelection = false
                showCreateCategory = true
            }
        )
    }
    
    // Create category dialog
    if (showCreateCategory) {
        CreateCategoryDialog(
            categoryName = newCategoryName,
            onCategoryNameChange = { newCategoryName = it },
            onCreateCategory = {
                // Handle category creation logic here
                val newCategory = Category(
                    id = (categories.maxOfOrNull { it.id } ?: 0) + 1,
                    name = newCategoryName,
                    viewOrder = (categories.maxOfOrNull { it.viewOrder } ?: 0) + 1
                )
                categories = categories + newCategory
                selectedCategoryId = newCategory.id
                newCategoryName = ""
                showCreateCategory = false
            },
            onDismiss = {
                newCategoryName = ""
                showCreateCategory = false
            }
        )
    }
}
```

## State Management

### Required State Variables

```kotlin
var showCategorySelection by remember { mutableStateOf(false) }
var showCreateCategory by remember { mutableStateOf(false) }
var selectedCategoryId by remember { mutableStateOf<Int?>(null) }
var newCategoryName by remember { mutableStateOf("") }
var categories by remember { mutableStateOf(listOf<Category>()) }
```

### State Flow

1. **Initial State**: `showCategorySelection = false`, `selectedCategoryId = null`
2. **Open Selection**: User clicks "Select Category" → `showCategorySelection = true`
3. **Select Category**: User taps category → `selectedCategoryId = categoryId`, close dialog
4. **Create Category**: User clicks "New Category" → `showCreateCategory = true`
5. **Save Category**: User creates category → add to list, select it, close dialogs

## Design System Integration

### Colors
- **Selected Category Background**: `Color(0xFFE8F5E8)` (light green)
- **Unselected Category Background**: `Color(0xFFF5F5F5)` (light gray)
- **Selection Indicator**: `Color(0xFF4CAF50)` (SuperCart green)

### Spacing
- **Category Row Padding**: `SuperCartSpacing.md` (16dp)
- **Row Spacing**: `SuperCartSpacing.sm` (8dp)
- **Button Spacing**: `SuperCartSpacing.sm` (8dp)

### Typography
- **Dialog Title**: `MaterialTheme.typography.headlineSmall`
- **Category Names**: `MaterialTheme.typography.titleMedium`
- **Button Text**: `MaterialTheme.typography.titleMedium`

## Key Implementation Details

### 1. Full-Screen Dialog
```kotlin
modifier = Modifier.fillMaxSize()
```
This makes the AlertDialog cover the entire screen.

### 2. Touch-Friendly Rows
```kotlin
modifier = Modifier
    .fillMaxWidth()
    .padding(SuperCartSpacing.md)
    .clickable { onClick() }
```
Each category is a full-width, generously padded row that's easy to tap.

### 3. Visual Selection Feedback
```kotlin
color = if (isSelected) Color(0xFFE8F5E8) else Color(0xFFF5F5F5)
```
Selected categories have a light green background to clearly indicate selection.

### 4. Integrated Workflow
The dialog includes both category selection and creation, providing a seamless user experience.

## Benefits of This Approach

1. **Better UX**: Full-screen dialog shows all categories clearly
2. **Touch-Friendly**: Large touch targets improve accessibility
3. **Professional Look**: Clean, modern interface design
4. **Consistent**: Follows established design patterns
5. **Efficient**: Integrated category creation workflow

## Future Enhancements

1. **Search Functionality**: Add search bar for large category lists
2. **Category Icons**: Include visual icons for each category
3. **Category Colors**: Allow custom colors for categories
4. **Drag & Drop**: Enable reordering of categories
5. **Category Groups**: Support hierarchical category organization

## Testing Considerations

1. **Touch Targets**: Ensure minimum 48dp touch targets
2. **State Persistence**: Verify selection persists across app restarts
3. **Performance**: Test with large category lists
4. **Accessibility**: Ensure proper content descriptions and navigation
5. **Edge Cases**: Handle empty category lists and invalid selections

This implementation provides a solid foundation for category selection that can be easily extended and customized as your app grows.
