package com.example.supercart2.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.models.Grocery
import com.example.supercart2.models.Category
import com.example.supercart2.models.SubCategory
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun ImportGroceriesDialog(
    onDismiss: () -> Unit,
    onImportComplete: () -> Unit
) {
    var importText by remember { mutableStateOf("") }
    var showAddToShoppingListDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val newlyCreatedGroceries = remember { mutableStateListOf<Grocery>() }
    val existingGroceryNames = remember { mutableStateListOf<String>() }

    if (showAddToShoppingListDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddToShoppingListDialog = false
                onImportComplete()
            },
            title = { Text("Add to Shopping List?") },
            text = { Text("Should we add the items to the shopping list?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            // Add all newly created items to shopping list
                            newlyCreatedGroceries.forEach { grocery ->
                                DataManagerObject.toggleShoppingListStatus(grocery.uuid)
                            }
                            // Add existing items to shopping list
                            existingGroceryNames.forEach { name ->
                                DataManagerObject.categories.forEach { categoryWithSubs ->
                                    categoryWithSubs.subCategories.forEach { subCategoryWithGroceries ->
                                        subCategoryWithGroceries.groceries.find { it.name == name }?.let { grocery ->
                                            if (!grocery.inShoppingList) {
                                                DataManagerObject.toggleShoppingListStatus(grocery.uuid)
                                            }
                                        }
                                    }
                                }
                            }
                            DataStoreManager.saveDataGlobally()
                            showAddToShoppingListDialog = false
                            onImportComplete()
                        }
                    }
                ) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddToShoppingListDialog = false
                        onImportComplete()
                    }
                ) {
                    Text("No")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Groceries") },
        text = {
            Column {
                Text("Enter grocery names (one per line):")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = importText,
                    onValueChange = { importText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SuperCartColors.primaryGreen,
                        unfocusedBorderColor = SuperCartColors.gray
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        // Clear previous lists
                        newlyCreatedGroceries.clear()
                        existingGroceryNames.clear()
                        
                        // Get or create "Other" category and "General" sub-category
                        val otherCategory = DataManagerObject.categories.find { it.category.name == "Other" }?.category
                            ?: Category(name = "Other").also { category ->
                                DataManagerObject.addCategory(category)
                            }
                        
                        val generalSubCategory = DataManagerObject.categories
                            .find { it.category.uuid == otherCategory.uuid }
                            ?.subCategories
                            ?.find { it.subCategory.name == "General" }
                            ?.subCategory
                            ?: SubCategory(
                                categoryId = otherCategory.uuid,
                                name = "General"
                            ).also { subCategory ->
                                DataManagerObject.addSubCategory(otherCategory.uuid, subCategory)
                            }

                        // Process each line
                        importText.split("\\r?\\n".toRegex()).forEach { line ->
                            val trimmedName = line.trim()
                            if (trimmedName.isNotEmpty()) {
                                // Check if grocery already exists
                                var exists = false
                                DataManagerObject.categories.forEach { category ->
                                    category.subCategories.forEach { subCategory ->
                                        if (subCategory.groceries.any { it.name == trimmedName }) {
                                            exists = true
                                            existingGroceryNames.add(trimmedName)
                                            return@forEach
                                        }
                                    }
                                    if (exists) return@forEach
                                }

                                // Create new grocery if it doesn't exist
                                if (!exists) {
                                    val newGrocery = Grocery(
                                        uuid = UUID.randomUUID().toString(),
                                        name = trimmedName,
                                        categoryId = otherCategory.uuid,
                                        subCategoryId = generalSubCategory.uuid
                                    )
                                    DataManagerObject.addGrocery(newGrocery)
                                    newlyCreatedGroceries.add(newGrocery)
                                }
                            }
                        }

                        // Save changes
                        DataStoreManager.saveDataGlobally()

                        // Show add to shopping list dialog if any items were processed
                        if (newlyCreatedGroceries.isNotEmpty() || existingGroceryNames.isNotEmpty()) {
                            showAddToShoppingListDialog = true
                        } else {
                            onImportComplete()
                        }
                    }
                },
                enabled = importText.isNotBlank()
            ) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
