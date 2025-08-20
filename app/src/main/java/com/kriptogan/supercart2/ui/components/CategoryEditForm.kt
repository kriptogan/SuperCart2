package com.kriptogan.supercart2.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kriptogan.supercart2.classes.Category

@Composable
fun CategoryEditForm(
    category: Category,
    onSave: (String, Int) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var categoryName by remember { mutableStateOf(category.name) }
    var viewOrder by remember { mutableStateOf(category.viewOrder.toString()) }
    
    // Pre-fill the form with existing category data
    LaunchedEffect(category) {
        categoryName = category.name
        viewOrder = category.viewOrder.toString()
    }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header with back button and title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onCancel,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(24.dp),
                    tint = Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = "Edit Category",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Category name input field
        OutlinedTextField(
            value = categoryName,
            onValueChange = { categoryName = it },
            label = { Text("Category Name") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // View order input field
        OutlinedTextField(
            value = viewOrder,
            onValueChange = { viewOrder = it },
            label = { Text("View Order") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            singleLine = true,
            isError = viewOrder.isNotBlank() && viewOrder.toIntOrNull() == null
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            
            OutlinedButton(
                onClick = { 
                    if (categoryName.isNotBlank() && viewOrder.isNotBlank()) {
                        val orderValue = viewOrder.toIntOrNull()
                        if (orderValue != null) {
                            onSave(categoryName, orderValue)
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = categoryName.isNotBlank() && viewOrder.isNotBlank() && viewOrder.toIntOrNull() != null
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Save",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save")
            }
        }
    }
}
