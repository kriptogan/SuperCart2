package com.kriptogan.supercart2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import com.kriptogan.supercart2.classes.SubCategory
import com.kriptogan.supercart2.classes.Grocery
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroceryCreationForm(
    categories: List<Category>,
    subCategories: List<SubCategory>,
    onSave: (String, String, String?) -> Unit,
    onCancel: () -> Unit,
    groceryToEdit: Grocery? = null,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(groceryToEdit?.name ?: "") }
    var selectedCategoryId by remember { mutableStateOf("") }
    var selectedSubCategoryId by remember { mutableStateOf(groceryToEdit?.subCategoryId ?: "") }
    var expirationDate by remember { mutableStateOf<LocalDate?>(null) }
    var showCategorySelection by remember { mutableStateOf(false) }
    var showSubCategorySelection by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    LaunchedEffect(groceryToEdit) {
        if (groceryToEdit != null) {
            val subCategory = subCategories.find { it.uuid == groceryToEdit.subCategoryId }
            if (subCategory != null) {
                selectedCategoryId = subCategory.categoryId
                selectedSubCategoryId = subCategory.uuid
            }
            if (groceryToEdit.expirationDate != null) {
                try {
                    expirationDate = LocalDate.parse(groceryToEdit.expirationDate)
                } catch (e: Exception) { }
            }
        }
    }
    val filteredSubCategories = subCategories.filter { it.categoryId == selectedCategoryId }
    if (selectedCategoryId.isNotEmpty() && filteredSubCategories.isNotEmpty() && selectedSubCategoryId.isEmpty()) {
        selectedSubCategoryId = filteredSubCategories.first().uuid
    }
    
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (groceryToEdit != null) "Edit Grocery Item" else "Create New Grocery Item",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )
        
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Grocery Name") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Button(
            onClick = { showCategorySelection = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (selectedCategoryId.isNotEmpty()) {
                    val selectedCategory = categories.find { it.uuid == selectedCategoryId }
                    selectedCategory?.name ?: "Select Category"
                } else {
                    "Select Category"
                }
            )
        }
        
        Button(
            onClick = { showSubCategorySelection = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedCategoryId.isNotEmpty()
        ) {
            Text(
                if (selectedSubCategoryId.isNotEmpty()) {
                    val selectedSubCategory = filteredSubCategories.find { it.uuid == selectedSubCategoryId }
                    selectedSubCategory?.name ?: "Select Sub-Category"
                } else {
                    "Select Sub-Category"
                }
            )
        }
        
        Button(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (expirationDate != null) {
                    expirationDate.toString()
                } else {
                    "Set Expiration Date"
                }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
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
                        onSave(name, selectedSubCategoryId, expirationDate?.toString())
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = name.isNotBlank() && selectedSubCategoryId.isNotEmpty()
            ) {
                Text(if (groceryToEdit != null) "Update" else "Create")
            }
        }
    }
    if (showCategorySelection) {
        AlertDialog(
            onDismissRequest = { showCategorySelection = false },
            title = { Text("Select Category") },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = Color(0xFFF5F5F5),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(16.dp)
                                .clickable {
                                    selectedCategoryId = category.uuid
                                    selectedSubCategoryId = ""
                                    showCategorySelection = false
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selectedCategoryId == category.uuid) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.Green,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                            } else {
                                Spacer(modifier = Modifier.width(28.dp))
                            }
                            Text(
                                text = category.name,
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showCategorySelection = false }) {
                    Text("Close")
                }
            }
        )
    }
    if (showSubCategorySelection) {
        AlertDialog(
            onDismissRequest = { showSubCategorySelection = false },
            title = { Text("Select Sub-Category") },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredSubCategories) { subCategory ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = Color(0xFFF5F5F5),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(16.dp)
                                .clickable {
                                    selectedSubCategoryId = subCategory.uuid
                                    showSubCategorySelection = false
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selectedSubCategoryId == subCategory.uuid) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.Green,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                            } else {
                                Spacer(modifier = Modifier.width(28.dp))
                            }
                            Text(
                                text = subCategory.name,
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showSubCategorySelection = false }) {
                    Text("Close")
                }
            }
        )
    }
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    expirationDate = millis?.let {
                        LocalDate.ofEpochDay(it / (24 * 60 * 60 * 1000))
                    }
                    showDatePicker = false
                }) {
                    Text("Set Date")
                }
            },
            dismissButton = {
                Button(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

