package com.example.supercart2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonSkippableComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.supercart2.R
import com.example.supercart2.data.ImageManager
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartShapes

/** Tappable photo card: progress while processing, the stored photo, or an "add image" placeholder. */
@Composable
internal fun GroceryCreationDialogImageCard(form: GroceryCreationDialogForm, onClick: () -> Unit) {
    val modifier = Modifier.fillMaxWidth().height(200.dp).clickable(onClick = onClick)
    val colors = CardDefaults.cardColors(containerColor = SuperCartColors.lightGray.copy(alpha = 0.3f))
    Card(modifier = modifier, colors = colors, shape = SuperCartShapes.medium) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { ImageCardContent(form) }
    }
}

@Composable
private fun BoxScope.ImageCardContent(form: GroceryCreationDialogForm) {
    val imageUUID = form.imageUUID
    when {
        form.isProcessingImage -> ProcessingIndicator()
        imageUUID != null -> StoredImage(imageUUID, form)
        else -> ImagePlaceholder()
    }
}

@Composable
private fun ProcessingIndicator() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = SuperCartColors.primaryGreen)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = stringResource(R.string.processing_image), color = SuperCartColors.gray, fontSize = 12.sp)
    }
}

/**
 * The local photo with a remove button, or the placeholder when its file is missing. Not skippable, so the file is
 * re-checked whenever the card recomposes (e.g. after a failed compression deleted it).
 */
@NonSkippableComposable
@Composable
private fun BoxScope.StoredImage(imageUUID: String, form: GroceryCreationDialogForm) {
    val context = LocalContext.current
    val imageFile = remember(imageUUID) { ImageManager.getLocalImageFile(imageUUID, context) }
    if (imageFile == null || !imageFile.exists()) return ImagePlaceholder()
    AsyncImage(model = imageFile, contentDescription = stringResource(R.string.grocery_image),
        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    RemoveImageButton(onClick = { form.removeImage(context) })
}

@Composable
private fun BoxScope.RemoveImageButton(onClick: () -> Unit) {
    val iconModifier = Modifier.background(Color.Black.copy(alpha = 0.5f), shape = CircleShape).padding(4.dp)
    IconButton(onClick = onClick, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
        Icon(Icons.Default.Close, stringResource(R.string.remove_image), iconModifier, tint = Color.White)
    }
}

@Composable
private fun ImagePlaceholder() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.AddPhotoAlternate, null, Modifier.size(48.dp), tint = SuperCartColors.gray)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = stringResource(R.string.add_image_prompt), color = SuperCartColors.gray, fontSize = 14.sp)
        Text(text = stringResource(R.string.tap_to_upload), color = SuperCartColors.gray, fontSize = 12.sp)
    }
}
