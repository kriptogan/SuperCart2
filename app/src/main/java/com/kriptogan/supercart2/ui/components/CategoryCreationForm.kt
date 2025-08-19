package com.kriptogan.supercart2.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kriptogan.supercart2.classes.Category

/**
 * Category creation form component
 * @param onSave Callback when form is submitted with category data
 * @param onCancel Callback when form is cancelled
 */
@Composable
fun CategoryCreationForm(
    onSave: (Category) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var isDefault by remember { mutableStateOf(false) }
    var viewOrder by remember { mutableStateOf("1") }
    var isFormValid by remember { mutableStateOf(false) }
    
    // Validate form
    LaunchedEffect(name, viewOrder) {
        isFormValid = name.trim().isNotEmpty() && viewOrder.toIntOrNull() != null
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Create New Category",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        
        // Category Name Input
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Category Name") },
            placeholder = { Text("e.g., Fruits, Vegetables, Dairy") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        // View Order Input
        OutlinedTextField(
            value = viewOrder,
            onValueChange = { viewOrder = it },
            label = { Text("Display Order") },
            placeholder = { Text("1, 2, 3...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        // Default Category Checkbox
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Checkbox(
                checked = isDefault,
                onCheckedChange = { isDefault = it }
            )
            Text("Default Category")
        }
        
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
                    val category = Category(
                        name = name.trim(),
                        default = isDefault,
                        viewOrder = viewOrder.toIntOrNull() ?: 1
                    )
                    onSave(category)
                },
                enabled = isFormValid,
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }
        }
    }
}
