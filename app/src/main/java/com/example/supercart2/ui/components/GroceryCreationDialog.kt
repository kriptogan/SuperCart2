package com.example.supercart2.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing
import com.example.supercart2.ui.theme.SuperCartShapes
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.models.Category
import com.example.supercart2.models.SubCategory
import com.example.supercart2.models.Grocery
import com.example.supercart2.ui.components.CategorySelectionDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroceryCreationDialog(
    onDismiss: () -> Unit,
    onGroceryCreated: (Grocery) -> Unit
) {
    var groceryName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedSubCategory by remember { mutableStateOf<SubCategory?>(null) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    
    // Auto-select first category and sub-category when dialog opens
    LaunchedEffect(Unit) {
        val sortedCategories = DataManagerObject.getSortedCategories()
        if (sortedCategories.isNotEmpty()) {
            selectedCategory = sortedCategories.first().category
            val firstSubCategory = sortedCategories.first().subCategories.firstOrNull()?.subCategory
            if (firstSubCategory != null) {
                selectedSubCategory = firstSubCategory
            }
        }
    }
    
    // Dialog states
    var showCategorySelection by remember { mutableStateOf(false) }
    var showSubCategoryDropdown by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    // Get sorted categories for consistent display
    val sortedCategories = DataManagerObject.getSortedCategories()
    
    // Get sub-categories for selected category
    val availableSubCategories = selectedCategory?.let { category ->
        DataManagerObject.categories.find { it.category.uuid == category.uuid }?.subCategories?.map { it.subCategory } ?: emptyList()
    } ?: emptyList()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Create New Grocery",
                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Grocery Name Input
                OutlinedTextField(
                    value = groceryName,
                    onValueChange = { groceryName = it },
                    label = { Text("Grocery Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SuperCartColors.primaryGreen,
                        unfocusedBorderColor = SuperCartColors.gray,
                        focusedLabelColor = SuperCartColors.primaryGreen,
                        unfocusedLabelColor = SuperCartColors.gray
                    ),
                    shape = SuperCartShapes.small
                )
                
                Spacer(modifier = Modifier.height(SuperCartSpacing.md))
                
                // Category Selector
                Column {
                    Text(
                        text = "Category",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = SuperCartColors.black,
                        modifier = Modifier.padding(bottom = SuperCartSpacing.xs)
                    )
                    
                    Button(
                        onClick = { showCategorySelection = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SuperCartColors.white,
                            contentColor = SuperCartColors.primaryGreen
                        ),
                        shape = SuperCartShapes.small
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedCategory?.name ?: "Select Category",
                                color = if (selectedCategory != null) SuperCartColors.black else SuperCartColors.gray
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Select Category"
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(SuperCartSpacing.md))
                
                // Sub-Category Selector
                Column {
                    Text(
                        text = "Sub-Category",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = SuperCartColors.black,
                        modifier = Modifier.padding(bottom = SuperCartSpacing.xs)
                    )
                    
                    Button(
                        onClick = { 
                            if (selectedCategory != null) {
                                showSubCategoryDropdown = true 
                            }
                        },
                        enabled = selectedCategory != null,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SuperCartColors.white,
                            contentColor = SuperCartColors.primaryGreen
                        ),
                        shape = SuperCartShapes.small
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedSubCategory?.name ?: "Select Sub-Category",
                                color = if (selectedSubCategory != null) SuperCartColors.black else SuperCartColors.gray
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Select Sub-Category"
                            )
                        }
                    }
                    
                    // Sub-Category Dropdown
                    DropdownMenu(
                        expanded = showSubCategoryDropdown,
                        onDismissRequest = { showSubCategoryDropdown = false }
                    ) {
                        availableSubCategories.forEach { subCategory ->
                            DropdownMenuItem(
                                text = { Text(subCategory.name) },
                                onClick = {
                                    selectedSubCategory = subCategory
                                    showSubCategoryDropdown = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(SuperCartSpacing.md))
                
                // Date Selector
                Column {
                    Text(
                        text = "Date",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = SuperCartColors.black,
                        modifier = Modifier.padding(bottom = SuperCartSpacing.xs)
                    )
                    
                    Button(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SuperCartColors.white,
                            contentColor = SuperCartColors.primaryGreen
                        ),
                        shape = SuperCartShapes.small
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                                color = SuperCartColors.black
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Select Date"
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SuperCartSpacing.sm)
            ) {
                // Cancel Button (left) - secondary styled
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuperCartColors.white,
                        contentColor = SuperCartColors.primaryGreen
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel"
                    )
                }
                
                // Create Button (right) - primary styled
                Button(
                    onClick = {
                        if (groceryName.isNotBlank() && selectedCategory != null && selectedSubCategory != null) {
                            val newGrocery = Grocery(
                                name = groceryName.trim(),
                                categoryId = selectedCategory!!.uuid,
                                subCategoryId = selectedSubCategory!!.uuid,
                                date = selectedDate
                            )
                            onGroceryCreated(newGrocery)
                            onDismiss()
                        }
                    },
                    enabled = groceryName.isNotBlank() && selectedCategory != null && selectedSubCategory != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuperCartColors.primaryGreen,
                        contentColor = SuperCartColors.white
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Create Grocery"
                    )
                }
            }
        }
    )
    
    // Material3 Date Picker
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            selectedDate = java.time.Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()
                        }
                        showDatePicker = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuperCartColors.primaryGreen,
                        contentColor = SuperCartColors.white
                    )
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDatePicker = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuperCartColors.white,
                        contentColor = SuperCartColors.primaryGreen
                    )
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    // Category Selection Dialog
    if (showCategorySelection) {
        CategorySelectionDialog(
            onDismiss = { showCategorySelection = false },
            onCategorySelected = { category ->
                selectedCategory = category
                // Auto-select first sub-category in the new category
                val firstSubCategory = DataManagerObject.categories
                    .find { it.category.uuid == category.uuid }
                    ?.subCategories
                    ?.firstOrNull()?.subCategory
                selectedSubCategory = firstSubCategory
                showCategorySelection = false
            },
            onNewCategoryRequested = {
                // For now, just close the dialog
                // TODO: Implement category creation flow
                showCategorySelection = false
            },
            selectedCategoryId = selectedCategory?.uuid
        )
    }
}
