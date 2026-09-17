package com.example.supercart2.ui.components

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.data.ImageManager
import com.example.supercart2.models.Category
import com.example.supercart2.models.Grocery
import com.example.supercart2.models.SubCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

private const val TAG = "GroceryCreationDialog"

/** Editable values of [GroceryCreationDialog]. */
@Stable
internal class GroceryCreationDialogForm {
    var name by mutableStateOf("")
    var category by mutableStateOf<Category?>(null)
    var subCategory by mutableStateOf<SubCategory?>(null)
    var date by mutableStateOf<LocalDate?>(null)
    var storeIds by mutableStateOf<List<String>>(emptyList())
    var imageUUID by mutableStateOf<String?>(null)
    var isProcessingImage by mutableStateOf(false)

    val canSave: Boolean get() = name.isNotBlank() && category != null && subCategory != null
}

/** Which secondary dialog of [GroceryCreationDialog] is open. */
@Stable
internal class GroceryCreationDialogPopups {
    var category by mutableStateOf(false)
    var subCategory by mutableStateOf(false)
    var stores by mutableStateOf(false)
    var datePicker by mutableStateOf(false)
    var deleteConfirmation by mutableStateOf(false)
    var imageSource by mutableStateOf(false)
    var fullScreenImage by mutableStateOf(false)

    /** A stored photo opens full screen; without one (or when its file is missing) the image source picker opens. */
    fun openImage(hasStoredImage: Boolean) {
        if (hasStoredImage) fullScreenImage = true else imageSource = true
    }

    /** Closes the full-screen viewer and opens the image source picker to replace the photo. */
    fun changeImage() {
        fullScreenImage = false
        imageSource = true
    }
}

/** Fills the form from the edited grocery, or with create-mode defaults. */
internal fun GroceryCreationDialogForm.load(groceryToEdit: Grocery?, initialName: String, initialStoreId: String?) {
    if (groceryToEdit != null) loadFrom(groceryToEdit) else loadDefaults(initialName, initialStoreId)
}

private fun GroceryCreationDialogForm.loadFrom(grocery: Grocery) {
    name = grocery.name
    selectLocation(grocery.categoryId, grocery.subCategoryId)
    date = grocery.expirationDate
    storeIds = grocery.storeIds
    imageUUID = grocery.imageUUID
}

private fun GroceryCreationDialogForm.selectLocation(categoryId: String, subCategoryId: String) {
    val node = DataManagerObject.categories.find { it.category.uuid == categoryId }
    category = node?.category
    subCategory = node?.subCategories?.find { it.subCategory.uuid == subCategoryId }?.subCategory
}

/** Create mode: initial name and store, first sorted category and its first sub-category. */
private fun GroceryCreationDialogForm.loadDefaults(initialName: String, initialStoreId: String?) {
    name = initialName
    if (initialStoreId != null) storeIds = listOf(initialStoreId)
    val first = DataManagerObject.getSortedCategories().firstOrNull() ?: return
    category = first.category
    first.subCategories.firstOrNull()?.subCategory?.let { subCategory = it }
}

/** Selects a category and its first non-deleted sub-category (none when all are deleted). */
internal fun GroceryCreationDialogForm.selectCategory(selected: Category) {
    category = selected
    subCategory = groceryCreationDialogActiveSubCategories(selected.uuid).firstOrNull()
}

/** Non-deleted sub-categories of the category [categoryId], in data order. */
internal fun groceryCreationDialogActiveSubCategories(categoryId: String): List<SubCategory> =
    DataManagerObject.categories.find { it.category.uuid == categoryId }
        ?.subCategories?.map { it.subCategory }?.filterNot { it.deleted } ?: emptyList()

internal fun GroceryCreationDialogForm.selectNewLocation(newCategory: Category, newSubCategory: SubCategory) {
    category = newCategory
    subCategory = newSubCategory
}

/**
 * Updates [groceryToEdit] or adds a new grocery, then persists. Requires [GroceryCreationDialogForm.canSave].
 * Returns the created grocery, or null when editing.
 */
internal fun GroceryCreationDialogForm.save(
    groceryToEdit: Grocery?, addToShoppingList: Boolean, scope: CoroutineScope,
): Grocery? {
    if (groceryToEdit != null) {
        updateExisting(groceryToEdit, scope)
        return null
    }
    return addNew(addToShoppingList, scope)
}

private fun GroceryCreationDialogForm.addNew(addToShoppingList: Boolean, scope: CoroutineScope): Grocery {
    val created = newGrocery(addToShoppingList)
    DataManagerObject.addGrocery(created)
    scope.launch { DataStoreManager.saveDataGlobally() }
    return created
}

private fun GroceryCreationDialogForm.updateExisting(original: Grocery, scope: CoroutineScope) {
    val updated = editedCopyOf(original)
    moveIfRelocated(original, scope)
    DataManagerObject.updateGrocery(original.uuid) { updated }
    scope.launch { DataStoreManager.saveDataGlobally() }
}

private fun GroceryCreationDialogForm.moveIfRelocated(original: Grocery, scope: CoroutineScope) {
    val categoryId = category!!.uuid
    val subCategoryId = subCategory!!.uuid
    if (original.categoryId == categoryId && original.subCategoryId == subCategoryId) return
    DataManagerObject.updateGroceryLocation(original.uuid, categoryId, subCategoryId)
    scope.launch { DataStoreManager.saveDataGlobally() }
}

private fun GroceryCreationDialogForm.editedCopyOf(original: Grocery) = original.copy(
    name = name.trim(), categoryId = category!!.uuid, subCategoryId = subCategory!!.uuid,
    expirationDate = date, storeIds = storeIds, imageUUID = imageUUID, lastUpdate = LocalDateTime.now(),
)

private fun GroceryCreationDialogForm.newGrocery(addToShoppingList: Boolean) = Grocery(
    name = name.trim(), categoryId = category!!.uuid, subCategoryId = subCategory!!.uuid,
    expirationDate = date, storeIds = storeIds, imageUUID = imageUUID,
    inShoppingList = addToShoppingList, isBought = false, lastUpdate = LocalDateTime.now(), deleted = false,
)

/** Soft-deletes the grocery and persists. */
internal fun groceryCreationDialogDelete(grocery: Grocery, scope: CoroutineScope) {
    DataManagerObject.deleteGrocery(grocery.uuid)
    scope.launch { DataStoreManager.saveDataGlobally() }
}

/** The local file of the current photo, or null when there is no photo or its file is missing. */
internal fun GroceryCreationDialogForm.storedImageFile(context: Context): java.io.File? =
    imageUUID?.let { ImageManager.getLocalImageFile(it, context) }

internal fun GroceryCreationDialogForm.removeImage(context: Context) {
    imageUUID?.let { ImageManager.deleteLocalImage(it, context) }
    imageUUID = null
}

/**
 * Compresses the picked image under a fresh UUID (so the image card recomposes); the previous file is deleted only
 * after the new one was saved. On failure the previous UUID and file are kept.
 */
internal suspend fun GroceryCreationDialogForm.replaceImage(uri: Uri, context: Context) {
    isProcessingImage = true
    try {
        storeCompressedImageOrLog(uri, context)
    } finally {
        isProcessingImage = false
    }
}

private suspend fun GroceryCreationDialogForm.storeCompressedImageOrLog(uri: Uri, context: Context) {
    try {
        storeCompressedImage(uri, context)
    } catch (e: Exception) {
        android.util.Log.e(TAG, "Error processing image", e)
    }
}

private suspend fun GroceryCreationDialogForm.storeCompressedImage(uri: Uri, context: Context) {
    val newUUID = UUID.randomUUID().toString()
    val savedFile = ImageManager.compressAndSaveImage(uri, newUUID, context)
    if (savedFile != null) switchToImage(newUUID, context) else android.util.Log.e(TAG, "Failed to save image")
}

/** Deletes the previous local image, then points the form at [newUUID]. */
private fun GroceryCreationDialogForm.switchToImage(newUUID: String, context: Context) {
    imageUUID?.let { ImageManager.deleteLocalImage(it, context) }
    imageUUID = newUUID
}
