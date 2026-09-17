package com.example.supercart2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.supercart2.R
import java.io.File

/** The grocery photo on a black full-screen background: pinch to zoom, drag to pan, ✕ or back to close. */
@Composable
internal fun GroceryCreationDialogFullScreenImage(file: File, onClose: () -> Unit, onChangeImage: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            ZoomableImage(file)
            FullScreenImageTopBar(onClose, onChangeImage)
        }
    }
}

/** Zoom level (1x–5x) and pan offset of the full-screen photo. */
private class GroceryCreationDialogImageZoom {
    var scale by mutableFloatStateOf(1f)
    var offset by mutableStateOf(Offset.Zero)

    fun transform(zoomChange: Float, panChange: Offset) {
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offset = if (scale == 1f) Offset.Zero else offset + panChange
    }
}

@Composable
private fun ZoomableImage(file: File) {
    val zoom = remember { GroceryCreationDialogImageZoom() }
    val state = rememberTransformableState { zoomChange, panChange, _ -> zoom.transform(zoomChange, panChange) }
    val layer = Modifier.graphicsLayer(zoom.scale, zoom.scale, translationX = zoom.offset.x, translationY = zoom.offset.y)
    AsyncImage(model = file, contentDescription = stringResource(R.string.full_screen_image),
        modifier = Modifier.fillMaxSize().transformable(state).then(layer), contentScale = ContentScale.Fit)
}

@Composable
private fun BoxScope.FullScreenImageTopBar(onClose: () -> Unit, onChangeImage: () -> Unit) {
    Row(Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onClose) { Icon(Icons.Default.Close, stringResource(R.string.close), tint = Color.White) }
        Box(modifier = Modifier.weight(1f))
        TextButton(onClick = onChangeImage) { Text(stringResource(R.string.change_image), color = Color.White) }
    }
}
