package com.example.supercart2.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.example.supercart2.R
import com.example.supercart2.models.Category
import com.example.supercart2.models.Grocery
import com.example.supercart2.models.SubCategory
import com.example.supercart2.ui.components.common.DialogIconButton
import com.example.supercart2.ui.components.common.primaryButtonColors
import com.example.supercart2.ui.components.common.secondaryButtonColors
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Add/edit grocery dialog. Saves (and deletes) through DataManagerObject + DataStore itself and then calls
 * [onDismiss]. When a new grocery was created, [onGroceryCreated] receives it just before [onDismiss].
 */
@Composable
fun GroceryCreationDialog(
    onDismiss: () -> Unit,
    groceryToEdit: Grocery? = null,
    initialGroceryName: String = "",
    addToShoppingList: Boolean = false,
    initialStoreId: String? = null,
    onGroceryCreated: (Grocery) -> Unit = {},
) {
    val form = remember { GroceryCreationDialogForm() }
    val popups = remember { GroceryCreationDialogPopups() }
    LaunchedEffect(groceryToEdit, initialGroceryName) { form.load(groceryToEdit, initialGroceryName, initialStoreId) }
    GroceryWindows(form, popups, groceryToEdit, addToShoppingList, onDismiss, onGroceryCreated)
}

/** Delete confirmation, main dialog and the secondary pickers, in that composition order. */
@Composable
private fun GroceryWindows(
    form: GroceryCreationDialogForm, popups: GroceryCreationDialogPopups, groceryToEdit: Grocery?,
    addToShoppingList: Boolean, onDismiss: () -> Unit, onGroceryCreated: (Grocery) -> Unit,
) {
    val scope = rememberCoroutineScope()
    DeleteConfirmationIfRequested(groceryToEdit, popups, scope, onDismiss)
    val onSave = { saveAndDismiss(form, groceryToEdit, addToShoppingList, scope, onDismiss, onGroceryCreated) }
    GroceryAlert(form, popups, isEdit = groceryToEdit != null, onDismiss = onDismiss, onSave = onSave)
    SecondaryDialogs(form, popups, scope)
}

@Composable
private fun DeleteConfirmationIfRequested(
    grocery: Grocery?, popups: GroceryCreationDialogPopups, scope: CoroutineScope, onDismiss: () -> Unit,
) {
    if (popups.deleteConfirmation && grocery != null) {
        GroceryCreationDialogDeleteConfirmation(grocery, popups, scope, onDismiss)
    }
}

private fun saveAndDismiss(
    form: GroceryCreationDialogForm, groceryToEdit: Grocery?, addToShoppingList: Boolean,
    scope: CoroutineScope, onDismiss: () -> Unit, onGroceryCreated: (Grocery) -> Unit,
) {
    if (!form.canSave) return
    form.save(groceryToEdit, addToShoppingList, scope)?.let(onGroceryCreated)
    onDismiss()
}

@Composable
private fun GroceryAlert(
    form: GroceryCreationDialogForm, popups: GroceryCreationDialogPopups, isEdit: Boolean,
    onDismiss: () -> Unit, onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = SuperCartColors.lightGreen,
        title = { TitleRow(isEdit, onDeleteClick = { popups.deleteConfirmation = true }) },
        text = { FormContent(form, popups) },
        confirmButton = { SaveButtons(isEdit, form.canSave, onDismiss, onSave) },
    )
}

@Composable
private fun TitleRow(isEdit: Boolean, onDeleteClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        TitleText(isEdit, Modifier.weight(1f))
        if (isEdit) DeleteIconButton(onDeleteClick)
    }
}

@Composable
private fun TitleText(isEdit: Boolean, modifier: Modifier) {
    Text(
        text = stringResource(if (isEdit) R.string.edit_grocery else R.string.create_new_grocery),
        style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center,
        color = SuperCartColors.black, modifier = modifier,
    )
}

@Composable
private fun DeleteIconButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_grocery), tint = Color.Red)
    }
}

@Composable
private fun FormContent(form: GroceryCreationDialogForm, popups: GroceryCreationDialogPopups) {
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        ImageCard(form, popups)
        GroceryCreationDialogNameField(form)
        GroceryCreationDialogLocationFields(form, popups)
        GroceryCreationDialogStoreAndDateFields(form, popups)
    }
}

/** Tapping a stored photo opens it full screen; otherwise the image source picker opens. */
@Composable
private fun ImageCard(form: GroceryCreationDialogForm, popups: GroceryCreationDialogPopups) {
    val context = LocalContext.current
    GroceryCreationDialogImageCard(form, onClick = { popups.openImage(form.storedImageFile(context) != null) })
}

/** Cancel (left) and save (right). */
@Composable
private fun SaveButtons(isEdit: Boolean, canSave: Boolean, onCancel: () -> Unit, onSave: () -> Unit) {
    val saveDescription = stringResource(if (isEdit) R.string.save_changes else R.string.create_grocery)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SuperCartSpacing.sm)) {
        val half = Modifier.weight(1f)
        CancelIconButton(half, onCancel)
        DialogIconButton(Icons.Default.Check, saveDescription, primaryButtonColors(), half, canSave, onSave)
    }
}

@Composable
private fun CancelIconButton(modifier: Modifier, onCancel: () -> Unit) {
    val description = stringResource(R.string.cancel)
    DialogIconButton(Icons.Default.Close, description, secondaryButtonColors(), modifier, onClick = onCancel)
}

@Composable
private fun SecondaryDialogs(
    form: GroceryCreationDialogForm, popups: GroceryCreationDialogPopups, scope: CoroutineScope,
) {
    if (popups.datePicker) GroceryCreationDialogDatePicker(form.date, { form.date = it }) { popups.datePicker = false }
    if (popups.category) CategoryPicker(form, popups)
    form.category?.let { if (popups.subCategory) SubCategoryPicker(form, it, popups, scope) }
    if (popups.stores) StoreSelectionDialog(form.storeIds, { popups.stores = false }, { form.storeIds = it })
    if (popups.imageSource) ImagePicker(form, popups, scope)
    if (popups.fullScreenImage) FullScreenImage(form, popups)
}

@Composable
private fun FullScreenImage(form: GroceryCreationDialogForm, popups: GroceryCreationDialogPopups) {
    val file = form.storedImageFile(LocalContext.current) ?: return
    GroceryCreationDialogFullScreenImage(file, { popups.fullScreenImage = false }, onChangeImage = popups::changeImage)
}

@Composable
private fun CategoryPicker(form: GroceryCreationDialogForm, popups: GroceryCreationDialogPopups) {
    CategorySelectionDialog(
        onDismiss = { popups.category = false },
        onCategorySelected = { form.selectCategory(it); popups.category = false },
        onNewCategoryCreated = { newCategory, newSub -> selectCreatedLocation(form, popups, newCategory, newSub) },
        selectedCategoryId = form.category?.uuid,
    )
}

private fun selectCreatedLocation(
    form: GroceryCreationDialogForm, popups: GroceryCreationDialogPopups, category: Category, subCategory: SubCategory,
) {
    form.selectNewLocation(category, subCategory)
    popups.category = false
}

@Composable
private fun SubCategoryPicker(
    form: GroceryCreationDialogForm, category: Category, popups: GroceryCreationDialogPopups, scope: CoroutineScope,
) {
    GroceryCreationDialogSubCategoryPicker(form, category, scope, onClose = { popups.subCategory = false })
}

@Composable
private fun ImagePicker(form: GroceryCreationDialogForm, popups: GroceryCreationDialogPopups, scope: CoroutineScope) {
    val context = LocalContext.current
    ImageSourceDialog(
        onDismiss = { popups.imageSource = false },
        onImageSelected = { uri -> scope.launch { form.replaceImage(uri, context) } },
    )
}
