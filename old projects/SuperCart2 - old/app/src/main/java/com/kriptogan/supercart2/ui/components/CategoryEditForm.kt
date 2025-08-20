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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

@Composable
fun CategoryEditForm(
    category: Category,
    subCategories: List<SubCategory>,
    onSave: (String, Int) -> Unit,
    onCancel: () -> Unit,
    onSubCategoryUpdated: (SubCategory, String) -> Unit = { _, _ -> },
    onSubCategoryDeleted: (SubCategory) -> Unit = { },
    onSubCategoryCreated: (SubCategory) -> Unit = { },
    modifier: Modifier = Modifier
) {
    var categoryName by remember { mutableStateOf(category.name) }
    var viewOrder by remember { mutableStateOf(category.viewOrder.toString()) }
    var pendingSubCategoryUpdates by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var pendingSubCategoryDeletions by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingSubCategoryCreations by remember { mutableStateOf<List<SubCategory>>(emptyList()) }
    var showNewSubCategoryDialog by remember { mutableStateOf(false) }
    var newSubCategoryName by remember { mutableStateOf("") }
    
    LaunchedEffect(category) {
        categoryName = category.name
        viewOrder = category.viewOrder.toString()
    }
    
    val handleSave = {
        if (categoryName.isNotBlank() && viewOrder.isNotBlank()) {
            val orderValue = viewOrder.toIntOrNull()
            if (orderValue != null) {
                pendingSubCategoryUpdates.forEach { (subCategoryId, newName) ->
                    val subCategory = subCategories.find { it.uuid == subCategoryId }
                    subCategory?.let { onSubCategoryUpdated(it, newName) }
                }
                pendingSubCategoryDeletions.forEach { subCategoryId ->
                    val subCategory = subCategories.find { it.uuid == subCategoryId }
                    subCategory?.let { onSubCategoryDeleted(it) }
                }
                pendingSubCategoryCreations.forEach { newSubCategory ->
                    onSubCategoryCreated(newSubCategory)
                }
                pendingSubCategoryUpdates = emptyMap()
                pendingSubCategoryDeletions = emptySet()
                pendingSubCategoryCreations = emptyList()
                onSave(categoryName, orderValue)
            }
        }
    }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel, modifier = Modifier.size(48.dp)) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", modifier = Modifier.size(24.dp), tint = Color.Gray)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = "Edit Category", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = categoryName,
            onValueChange = { categoryName = it },
            label = { Text("Category Name") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = viewOrder,
            onValueChange = { viewOrder = it },
            label = { Text("View Order") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            singleLine = true,
            isError = viewOrder.isNotBlank() && viewOrder.toIntOrNull() == null
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Sub-Categories", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            OutlinedButton(onClick = { showNewSubCategoryDialog = true }, modifier = Modifier.size(32.dp)) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Sub-Category", modifier = Modifier.size(16.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        val sortedSubCategories = subCategories.filter { it.categoryId == category.uuid }.sortedBy { it.name }
        if (sortedSubCategories.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sortedSubCategories) { subCategory ->
                    SubCategoryEditRow(
                        subCategory = subCategory,
                        isPendingUpdate = pendingSubCategoryUpdates.containsKey(subCategory.uuid),
                        isPendingDeletion = pendingSubCategoryDeletions.contains(subCategory.uuid),
                        onNameUpdated = { newName -> pendingSubCategoryUpdates = pendingSubCategoryUpdates + (subCategory.uuid to newName) },
                        onDelete = { pendingSubCategoryDeletions = pendingSubCategoryDeletions + subCategory.uuid }
                    )
                }
            }
        } else {
            Text(
                text = "No sub-categories found",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
            OutlinedButton(
                onClick = handleSave,
                modifier = Modifier.weight(1f),
                enabled = categoryName.isNotBlank() && viewOrder.isNotBlank() && viewOrder.toIntOrNull() != null
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = "Save", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save")
            }
        }
    }
    
    if (showNewSubCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showNewSubCategoryDialog = false; newSubCategoryName = "" },
            title = { Text("New Sub-Category") },
            text = {
                OutlinedTextField(
                    value = newSubCategoryName,
                    onValueChange = { newSubCategoryName = it },
                    label = { Text("Sub-Category Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newSubCategoryName.isNotBlank()) {
                            pendingSubCategoryCreations = pendingSubCategoryCreations + SubCategory(
                                categoryId = category.uuid,
                                name = newSubCategoryName
                            )
                            showNewSubCategoryDialog = false
                            newSubCategoryName = ""
                        }
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showNewSubCategoryDialog = false; newSubCategoryName = "" }
                ) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SubCategoryEditRow(
    subCategory: SubCategory,
    isPendingUpdate: Boolean,
    isPendingDeletion: Boolean,
    onNameUpdated: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(subCategory.name) }
    LaunchedEffect(subCategory.name) { editName = subCategory.name }
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isPendingDeletion -> Color(0xFFFFEBEE)
                isPendingUpdate -> Color(0xFFE8F5E8)
                else -> Color(0xFFF5F5F5)
            }
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isEditing) {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { if (editName.isNotBlank()) { onNameUpdated(editName); isEditing = false } }, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "Save", modifier = Modifier.size(16.dp), tint = Color.Green)
                }
                IconButton(onClick = { editName = subCategory.name; isEditing = false }, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(16.dp), tint = Color.Red)
                }
            } else {
                Text(
                    text = subCategory.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    color = if (isPendingDeletion) Color.Gray else Color.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { isEditing = true }, modifier = Modifier.size(32.dp), enabled = !isPendingDeletion) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit sub-category name", modifier = Modifier.size(16.dp), tint = Color.Blue)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp), enabled = !isPendingDeletion) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete sub-category", modifier = Modifier.size(16.dp), tint = Color.Red)
                }
            }
        }
    }
}
