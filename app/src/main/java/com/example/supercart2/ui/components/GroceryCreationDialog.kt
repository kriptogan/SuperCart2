package com.example.supercart2.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.data.ImageManager
import java.util.UUID
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
import androidx.compose.ui.res.stringResource
import com.example.supercart2.R
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing
import com.example.supercart2.ui.theme.SuperCartShapes
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.models.Category
import com.example.supercart2.models.SubCategory
import com.example.supercart2.models.Grocery
import com.example.supercart2.ui.components.CategorySelectionDialog
import com.example.supercart2.ui.components.SubCategorySelectionDialog
import com.example.supercart2.ui.components.StoreSelectionDialog
import com.example.supercart2.utils.localizedCategoryDisplayName
import com.example.supercart2.utils.localizedSubCategoryDisplayName
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroceryCreationDialog(
    onDismiss: () -> Unit,
    onGroceryCreated: (Grocery) -> Unit,
    groceryToEdit: Grocery? = null,
    initialGroceryName: String = "",
    addToShoppingList: Boolean = false
) {
    var groceryName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedSubCategory by remember { mutableStateOf<SubCategory?>(null) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedStoreIds by remember { mutableStateOf<List<String>>(emptyList()) }
    
    // Image state
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var currentImageUUID by remember { mutableStateOf<String?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var isProcessingImage by remember { mutableStateOf(false) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Initialize form with grocery data when editing, or auto-select first category/sub-category when creating
    LaunchedEffect(groceryToEdit, initialGroceryName) {
        if (groceryToEdit != null) {
            // Edit mode - populate form with existing grocery data
            groceryName = groceryToEdit.name
            selectedCategory = DataManagerObject.categories.find { categoryWithSubs ->
                categoryWithSubs.category.uuid == groceryToEdit.categoryId
            }?.category
            selectedSubCategory = DataManagerObject.categories.find { categoryWithSubs ->
                categoryWithSubs.category.uuid == groceryToEdit.categoryId
            }?.subCategories?.find { it.subCategory.uuid == groceryToEdit.subCategoryId }?.subCategory
            selectedDate = groceryToEdit.expirationDate
            selectedStoreIds = groceryToEdit.storeIds
            currentImageUUID = groceryToEdit.imageUUID
        } else {
            // Create mode - use initialGroceryName if provided, otherwise empty string
            groceryName = initialGroceryName
            // Auto-select first category and sub-category
            val sortedCategories = DataManagerObject.getSortedCategories()
            if (sortedCategories.isNotEmpty()) {
                selectedCategory = sortedCategories.first().category
                val firstSubCategory = sortedCategories.first().subCategories.firstOrNull()?.subCategory
                if (firstSubCategory != null) {
                    selectedSubCategory = firstSubCategory
                }
            }
        }
    }
    
    // Dialog states
    var showCategorySelection by remember { mutableStateOf(false) }
    var showSubCategorySelection by remember { mutableStateOf(false) }
    var showStoreSelection by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Delete confirmation dialog
    if (showDeleteConfirmation && groceryToEdit != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            containerColor = SuperCartColors.lightGreen,
            title = {
                Text(
                    text = stringResource(R.string.delete_grocery),
                    style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.delete_grocery_confirmation, groceryToEdit.name),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SuperCartSpacing.sm)
                ) {
                    // Cancel Button (left) - secondary styled
                    Button(
                        onClick = { showDeleteConfirmation = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SuperCartColors.white,
                            contentColor = SuperCartColors.primaryGreen
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cancel)
                        )
                    }
                    
                    // Delete Button (right) - danger styled
                    Button(
                        onClick = {
                            DataManagerObject.deleteGrocery(groceryToEdit.uuid)
                            
                            // Save to DataStore
                            scope.launch {
                                DataStoreManager.saveDataGlobally()
                                android.util.Log.d("GroceryCreationDialog", "Saved grocery deletion to DataStore")
                            }
                            onDismiss()
                            showDeleteConfirmation = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red,
                            contentColor = SuperCartColors.white
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete)
                        )
                    }
                }
            }
        )
    }
    
    // Get sorted categories for consistent display
    val sortedCategories = DataManagerObject.getSortedCategories()
    
    // Get sub-categories for selected category
    val availableSubCategories = selectedCategory?.let { category ->
        DataManagerObject.categories.find { it.category.uuid == category.uuid }?.subCategories?.map { it.subCategory } ?: emptyList()
    } ?: emptyList()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SuperCartColors.lightGreen,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (groceryToEdit != null) stringResource(R.string.edit_grocery) else stringResource(R.string.create_new_grocery),
                    style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                
                // Delete Icon (only show in edit mode)
                if (groceryToEdit != null) {
                    IconButton(
                        onClick = { showDeleteConfirmation = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete_grocery),
                            tint = Color.Red
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Image Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clickable { showImageSourceDialog = true },
                    colors = CardDefaults.cardColors(
                        containerColor = SuperCartColors.lightGray.copy(alpha = 0.3f)
                    ),
                    shape = SuperCartShapes.medium
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isProcessingImage) {
                            // Show loading indicator
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    color = SuperCartColors.primaryGreen
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.processing_image),
                                    color = SuperCartColors.gray,
                                    fontSize = 12.sp
                                )
                            }
                        } else if (currentImageUUID != null) {
                            // Show image using Coil
                            val imageFile = remember(currentImageUUID) {
                                ImageManager.getLocalImageFile(currentImageUUID!!, context)
                            }
                            
                            if (imageFile != null && imageFile.exists()) {
                                AsyncImage(
                                    model = imageFile,
                                    contentDescription = stringResource(R.string.grocery_image),
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                
                                // Remove button overlay (top-right)
                                IconButton(
                                    onClick = {
                                        // Remove image
                                        currentImageUUID?.let { uuid ->
                                            ImageManager.deleteLocalImage(uuid, context)
                                        }
                                        currentImageUUID = null
                                        imageUri = null
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.remove_image),
                                        tint = Color.White,
                                        modifier = Modifier
                                            .background(
                                                Color.Black.copy(alpha = 0.5f),
                                                shape = CircleShape
                                            )
                                            .padding(4.dp)
                                    )
                                }
                            } else {
                                // Image file not found - show placeholder
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        tint = SuperCartColors.gray,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = stringResource(R.string.add_image_prompt),
                                        color = SuperCartColors.gray,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = stringResource(R.string.tap_to_upload),
                                        color = SuperCartColors.gray,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        } else {
                            // No image - show placeholder
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = SuperCartColors.gray,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.add_image_prompt),
                                    color = SuperCartColors.gray,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = stringResource(R.string.tap_to_upload),
                                    color = SuperCartColors.gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(SuperCartSpacing.md))
                
                // Grocery Name Input
                OutlinedTextField(
                    value = groceryName,
                    onValueChange = { groceryName = it },
                    label = { Text(stringResource(R.string.grocery_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SuperCartColors.primaryGreen,
                        unfocusedBorderColor = SuperCartColors.gray,
                        focusedLabelColor = SuperCartColors.black,
                        unfocusedLabelColor = SuperCartColors.black,
                        focusedContainerColor = SuperCartColors.white,
                        unfocusedContainerColor = SuperCartColors.white
                    ),
                    shape = SuperCartShapes.small
                )
                
                Spacer(modifier = Modifier.height(SuperCartSpacing.md))
                
                // Category Selector
                Column {
                    Text(
                        text = stringResource(R.string.category),
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
                                text = selectedCategory?.let { localizedCategoryDisplayName(it.name) } ?: stringResource(R.string.select_category),
                                color = if (selectedCategory != null) SuperCartColors.black else SuperCartColors.gray
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = stringResource(R.string.select_category)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(SuperCartSpacing.md))
                
                // Sub-Category Selector
                Column {
                    Text(
                        text = stringResource(R.string.sub_category),
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = SuperCartColors.black,
                        modifier = Modifier.padding(bottom = SuperCartSpacing.xs)
                    )
                    
                    Button(
                        onClick = { 
                            if (selectedCategory != null) {
                                showSubCategorySelection = true 
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
                                text = selectedSubCategory?.let { localizedSubCategoryDisplayName(it.name) } ?: stringResource(R.string.select_sub_category),
                                color = if (selectedSubCategory != null) SuperCartColors.black else SuperCartColors.gray
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = stringResource(R.string.select_sub_category)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(SuperCartSpacing.md))
                
                // Store Selector
                Column {
                    Text(
                        text = stringResource(R.string.stores),
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = SuperCartColors.black,
                        modifier = Modifier.padding(bottom = SuperCartSpacing.xs)
                    )
                    
                    Button(
                        onClick = { showStoreSelection = true },
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
                            val storeNames = remember(selectedStoreIds, DataManagerObject.version) {
                                selectedStoreIds.mapNotNull { storeId ->
                                    DataManagerObject.stores.find { it.uuid == storeId }?.name
                                }
                            }
                            
                            Text(
                                text = when {
                                    storeNames.isEmpty() -> stringResource(R.string.select_stores_optional)
                                    storeNames.size == 1 -> storeNames[0]
                                    else -> stringResource(R.string.stores_selected, storeNames.size)
                                },
                                color = if (storeNames.isNotEmpty()) 
                                    SuperCartColors.black else SuperCartColors.gray
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = stringResource(R.string.select_stores)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(SuperCartSpacing.md))
                
                // Date Selector
                Column {
                                         Text(
                         text = stringResource(R.string.expiration_date),
                         style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                         color = SuperCartColors.black,
                         modifier = Modifier.padding(bottom = SuperCartSpacing.xs)
                     )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(SuperCartSpacing.sm)
                    ) {
                        // Date selection button
                        Button(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.weight(1f),
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
                                     text = try {
                                         selectedDate?.let { date ->
                                             if (date.year in 1900..2100) {
                                                 date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                                             } else {
                                                 "Invalid date"
                                             }
                                         } ?: "No date selected"
                                     } catch (e: Exception) {
                                         "Invalid date"
                                     },
                                     color = if (selectedDate != null && selectedDate!!.year in 1900..2100) SuperCartColors.black else SuperCartColors.gray
                                 )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Select Date"
                                )
                            }
                        }
                        
                        // Clear date button (only show when date is selected)
                        if (selectedDate != null) {
                            Button(
                                onClick = { selectedDate = null },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SuperCartColors.lightGray,
                                    contentColor = SuperCartColors.darkGray
                                ),
                                shape = SuperCartShapes.small
                            ) {
                                Text(stringResource(R.string.clear))
                            }
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
                
                // Save Button (right) - primary styled
                Button(
                                         onClick = {
                         if (groceryName.isNotBlank() && selectedCategory != null && selectedSubCategory != null) {
                             android.util.Log.d("GroceryCreationDialog", "Saving grocery. Selected date: $selectedDate")
                             
                             if (groceryToEdit != null) {
                                // Edit mode - update existing grocery
                                val updatedGrocery = groceryToEdit.copy(
                                   name = groceryName.trim(),
                                   categoryId = selectedCategory!!.uuid,
                                   subCategoryId = selectedSubCategory!!.uuid,
                                   expirationDate = selectedDate,
                                   storeIds = selectedStoreIds,
                                   // Preserve existing values for new properties
                                   buyEvents = groceryToEdit.buyEvents,
                                   imageUUID = currentImageUUID,
                                   // Update lastUpdate timestamp
                                   lastUpdate = java.time.LocalDateTime.now(),
                                   // Preserve isDeleted status
                                   deleted = groceryToEdit.deleted
                                )
                                 
                                 // If category or sub-category changed, use updateGroceryLocation
                                 if (groceryToEdit.categoryId != selectedCategory!!.uuid || 
                                     groceryToEdit.subCategoryId != selectedSubCategory!!.uuid) {
                                    DataManagerObject.updateGroceryLocation(
                                        groceryToEdit.uuid,
                                        selectedCategory!!.uuid,
                                        selectedSubCategory!!.uuid
                                    )
                                    
                                    // Save location change to DataStore
                                    scope.launch {
                                        DataStoreManager.saveDataGlobally()
                                        android.util.Log.d("GroceryCreationDialog", "Saved grocery location change to DataStore")
                                    }
                                }

                                // Update other properties
                                DataManagerObject.updateGrocery(groceryToEdit.uuid) { updatedGrocery }
                                
                                // Save property updates to DataStore
                                scope.launch {
                                    DataStoreManager.saveDataGlobally()
                                    android.util.Log.d("GroceryCreationDialog", "Saved grocery property updates to DataStore")
                                }
                                 android.util.Log.d("GroceryCreationDialog", "Updated grocery expiration date: ${updatedGrocery.expirationDate}")
                             } else {
                                // Create mode - create new grocery
                                val newGrocery = Grocery(
                                   name = groceryName.trim(),
                                   categoryId = selectedCategory!!.uuid,
                                   subCategoryId = selectedSubCategory!!.uuid,
                                   expirationDate = selectedDate,
                                   storeIds = selectedStoreIds,
                                   imageUUID = currentImageUUID,
                                   inShoppingList = addToShoppingList,
                                   isBought = false,
                                   lastUpdate = java.time.LocalDateTime.now(),
                                   deleted = false
                                )
                                 
                                 // Add the new grocery using DataManagerObject helper
                                 DataManagerObject.addGrocery(newGrocery)
                                
                                // Save to DataStore
                                scope.launch {
                                    DataStoreManager.saveDataGlobally()
                                    android.util.Log.d("GroceryCreationDialog", "Saved new grocery to DataStore")
                                }
                                 android.util.Log.d("GroceryCreationDialog", "New grocery expiration date: ${newGrocery.expirationDate}")
                             }
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
                        contentDescription = if (groceryToEdit != null) "Save Changes" else "Create Grocery"
                    )
                }
            }
        }
    )
    
    // Material3 Date Picker
    if (showDatePicker) {
        // Always use a safe, valid date for the picker to prevent crashes
        val safeInitialDate = try {
            // Validate the selected date and convert safely
            selectedDate?.let { date ->
                if (date.year in 1900..2100) { // Reasonable year range
                    date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                } else {
                    null
                }
            } ?: java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (e: Exception) {
            // Always fallback to current date if anything goes wrong
            java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
        
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = safeInitialDate
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            colors = androidx.compose.material3.DatePickerDefaults.colors(
                containerColor = SuperCartColors.lightGreen
            ),
            confirmButton = {
                Button(
                                         onClick = {
                         val millis = datePickerState.selectedDateMillis
                         android.util.Log.d("GroceryCreationDialog", "Date picker confirm clicked. Millis: $millis")
                         
                         if (millis != null) {
                             try {
                                 val newDate = java.time.Instant.ofEpochMilli(millis)
                                     .atZone(java.time.ZoneId.systemDefault())
                                     .toLocalDate()
                                 
                                 android.util.Log.d("GroceryCreationDialog", "Converted date: $newDate, Year: ${newDate.year}")
                                 
                                 // Validate the new date before setting it
                                 if (newDate.year in 1900..2100) {
                                     selectedDate = newDate
                                     android.util.Log.d("GroceryCreationDialog", "Date set successfully: $selectedDate")
                                 } else {
                                     android.util.Log.d("GroceryCreationDialog", "Date rejected - year out of range: ${newDate.year}")
                                     selectedDate = null
                                 }
                             } catch (e: Exception) {
                                 android.util.Log.e("GroceryCreationDialog", "Error converting date: ${e.message}", e)
                                 selectedDate = null
                             }
                         } else {
                             android.util.Log.d("GroceryCreationDialog", "No millis selected from date picker")
                         }
                         showDatePicker = false
                     },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuperCartColors.primaryGreen,
                        contentColor = SuperCartColors.white
                    )
                ) {
                    Text(stringResource(R.string.ok))
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
                    Text(stringResource(R.string.cancel))
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
            onNewCategoryCreated = { newCategory, newSubCategory ->
                // Set the newly created category and sub-category as selected
                selectedCategory = newCategory
                selectedSubCategory = newSubCategory
                // Close the category selection dialog and return to grocery creation
                showCategorySelection = false
            },
            selectedCategoryId = selectedCategory?.uuid
        )
    }
    
    // Sub-Category Selection Dialog
    if (showSubCategorySelection && selectedCategory != null) {
        SubCategorySelectionDialog(
            onDismiss = { showSubCategorySelection = false },
            onSubCategorySelected = { subCategory ->
                selectedSubCategory = subCategory
                showSubCategorySelection = false
            },
            onNewSubCategoryCreated = { newSubCategory ->
                selectedSubCategory = newSubCategory
                showSubCategorySelection = false
            },
            selectedCategory = selectedCategory!!,
            selectedSubCategoryId = selectedSubCategory?.uuid
        )
    }
    
    // Store Selection Dialog
    if (showStoreSelection) {
        StoreSelectionDialog(
            selectedStoreIds = selectedStoreIds,
            onDismiss = { showStoreSelection = false },
            onStoresSelected = { newSelectedIds ->
                selectedStoreIds = newSelectedIds
            }
        )
    }
    
    // Image Source Dialog
    if (showImageSourceDialog) {
        ImageSourceDialog(
            onDismiss = { showImageSourceDialog = false },
            onImageSelected = { uri ->
                // Process image in background
                scope.launch {
                    isProcessingImage = true
                    
                    try {
                        // Generate new UUID for image if creating new, or use existing
                        val imageUUID = currentImageUUID ?: UUID.randomUUID().toString()
                        
                        // Compress and save locally
                        val savedFile = ImageManager.compressAndSaveImage(uri, imageUUID, context)
                        
                        if (savedFile != null) {
                            currentImageUUID = imageUUID
                            imageUri = Uri.fromFile(savedFile)
                            android.util.Log.d("GroceryCreationDialog", "Image saved: $imageUUID")
                        } else {
                            android.util.Log.e("GroceryCreationDialog", "Failed to save image")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("GroceryCreationDialog", "Error processing image", e)
                    } finally {
                        isProcessingImage = false
                    }
                }
            }
        )
    }
}
