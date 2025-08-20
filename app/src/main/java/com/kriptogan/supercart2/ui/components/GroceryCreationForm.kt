package com.kriptogan.supercart2.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kriptogan.supercart2.classes.Category
import com.kriptogan.supercart2.classes.SubCategory

@Composable
fun GroceryCreationForm(
    categories: List<Category>,
    subCategories: List<SubCategory>,
    onSave: (String, String, String?) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf(categories.firstOrNull()?.uuid ?: "") }
    var selectedSubCategoryId by remember { mutableStateOf("") }
    var expirationDate by remember { mutableStateOf("") }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showSubCategoryDropdown by remember { mutableStateOf(false) }
    
    // Filter sub-categories based on selected category
    val filteredSubCategories = subCategories.filter { it.categoryId == selectedCategoryId }
    
    // Update selected sub-category when category changes
    if (selectedCategoryId.isNotEmpty() && filteredSubCategories.isNotEmpty() && selectedSubCategoryId.isEmpty()) {
        selectedSubCategoryId = filteredSubCategories.first().uuid
    }
    
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Create New Grocery Item",
            fontSize = 20.sp,
            modifier = Modifier.fillMaxWidth()
        )
        
        // 1. Name field
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Grocery Name") },
            modifier = Modifier.fillMaxWidth()
        )
        
        // 2. Category dropdown
        Column {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = categories.find { it.uuid == selectedCategoryId }?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Category") },
                    trailingIcon = { 
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Open dropdown"
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCategoryDropdown = true }
                )
            }
            
            DropdownMenu(
                expanded = showCategoryDropdown,
                onDismissRequest = { showCategoryDropdown = false }
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.name) },
                        onClick = {
                            selectedCategoryId = category.uuid
                            selectedSubCategoryId = "" // Reset sub-category selection
                            showCategoryDropdown = false
                        }
                    )
                }
            }
        }
        
        // 3. Sub-category dropdown
        Column {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = filteredSubCategories.find { it.uuid == selectedSubCategoryId }?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Sub-Category") },
                    trailingIcon = { 
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Open dropdown"
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSubCategoryDropdown = true }
                )
            }
            
            DropdownMenu(
                expanded = showSubCategoryDropdown,
                onDismissRequest = { showSubCategoryDropdown = false }
            ) {
                filteredSubCategories.forEach { subCategory ->
                    DropdownMenuItem(
                        text = { Text(subCategory.name) },
                        onClick = {
                            selectedSubCategoryId = subCategory.uuid
                            showSubCategoryDropdown = false
                        }
                    )
                }
            }
        }
        
        // 4. Expiration date field
        OutlinedTextField(
            value = expirationDate,
            onValueChange = { expirationDate = it },
            label = { Text("Expiration Date (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            
            Button(
                onClick = {
                    if (name.isNotBlank() && selectedSubCategoryId.isNotEmpty()) {
                        onSave(name, selectedSubCategoryId, expirationDate.ifBlank { null })
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = name.isNotBlank() && selectedSubCategoryId.isNotEmpty()
            ) {
                Text("Create")
            }
        }
    }
}
