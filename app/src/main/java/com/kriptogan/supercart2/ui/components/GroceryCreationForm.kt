package com.kriptogan.supercart2.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kriptogan.supercart2.classes.Grocery
import com.kriptogan.supercart2.classes.Category
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Grocery creation/editing form component
 * @param onSave Callback when form is submitted with grocery data
 * @param onCancel Callback when form is cancelled
 * @param initialGrocery Optional existing grocery for editing
 * @param availableCategories List of available categories to choose from
 */
@Composable
fun GroceryCreationForm(
    onSave: (Grocery) -> Unit,
    onCancel: () -> Unit,
    initialGrocery: Grocery? = null,
    availableCategories: List<Category> = emptyList()
) {
    var name by remember { mutableStateOf(initialGrocery?.name ?: "") }
    var selectedCategoryId by remember { mutableStateOf(initialGrocery?.categoryId ?: "") }
    var expirationDate by remember { mutableStateOf(initialGrocery?.expirationDate ?: "") }
    var isFormValid by remember { mutableStateOf(false) }
    
    // Validate form
    LaunchedEffect(name) {
        isFormValid = name.trim().isNotEmpty()
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (initialGrocery != null) "Edit Grocery Item" else "Create New Grocery Item",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        
        // Grocery Name Input
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Grocery Name") },
            placeholder = { Text("e.g., Milk, Bread, Apples") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        // Category Selection
        if (availableCategories.isNotEmpty()) {
            var expanded by remember { mutableStateOf(false) }
            
            // Debug: Show available categories count
            Text(
                text = "Available categories: ${availableCategories.size}",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(start = 16.dp)
            )
            
            OutlinedTextField(
                value = availableCategories.find { it.uuid == selectedCategoryId }?.name ?: "Select Category",
                onValueChange = {},
                readOnly = true,
                label = { Text("Category") },
                trailingIcon = {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand"
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
            )
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                availableCategories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.name) },
                        onClick = {
                            selectedCategoryId = category.uuid
                            expanded = false
                        }
                    )
                }
            }
        }
        
        // Expiration Date Input - Simple text input with validation
        OutlinedTextField(
            value = expirationDate,
            onValueChange = { 
                // Only allow valid date format characters
                if (it.isEmpty() || it.matches(Regex("^[0-9\\-]*$"))) {
                    expirationDate = it
                }
            },
            label = { Text("Expiration Date") },
            placeholder = { Text("YYYY-MM-DD (e.g., 2024-12-31)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Date",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )
        
        // Date format hint
        Text(
            text = "Enter date in YYYY-MM-DD format",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(start = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Action Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            
            Button(
                onClick = {
                    val grocery = if (initialGrocery != null) {
                        // Update existing grocery
                        initialGrocery.copy(
                            name = name.trim(),
                            categoryId = selectedCategoryId.ifEmpty { null },
                            expirationDate = expirationDate.ifEmpty { null }
                        )
                    } else {
                        // Create new grocery
                        Grocery(
                            name = name.trim(),
                            categoryId = selectedCategoryId.ifEmpty { null },
                            expirationDate = expirationDate.ifEmpty { null }
                        )
                    }
                    onSave(grocery)
                },
                enabled = isFormValid,
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }
        }
    }
}
