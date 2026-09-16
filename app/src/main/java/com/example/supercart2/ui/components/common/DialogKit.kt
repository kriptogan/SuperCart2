package com.example.supercart2.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.example.supercart2.R
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing

/** Shared building blocks for the app's dialogs. */

@Composable
fun superCartTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SuperCartColors.primaryGreen, unfocusedBorderColor = SuperCartColors.gray,
    focusedLabelColor = SuperCartColors.black, unfocusedLabelColor = SuperCartColors.black,
    focusedTextColor = SuperCartColors.black, unfocusedTextColor = SuperCartColors.black,
    focusedContainerColor = SuperCartColors.white, unfocusedContainerColor = SuperCartColors.white,
)

@Composable
fun primaryButtonColors(): ButtonColors =
    ButtonDefaults.buttonColors(containerColor = SuperCartColors.primaryGreen, contentColor = SuperCartColors.white)

@Composable
fun secondaryButtonColors(): ButtonColors =
    ButtonDefaults.buttonColors(containerColor = SuperCartColors.white, contentColor = SuperCartColors.primaryGreen)

/** Centered headline used as an AlertDialog title. */
@Composable
fun DialogTitle(text: String) {
    Text(
        text = text, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center,
        color = SuperCartColors.black, modifier = Modifier.fillMaxWidth(),
    )
}

/** Icon-only button filling its share of a row. */
@Composable
fun DialogIconButton(icon: ImageVector, description: String, colors: ButtonColors, modifier: Modifier,
                     enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, colors = colors, modifier = modifier) {
        Icon(imageVector = icon, contentDescription = description)
    }
}

/** Cancel (✕, left) and confirm (✓, right) icon buttons sharing the row width. */
@Composable
fun CancelConfirmButtons(onCancel: () -> Unit, confirmDescription: String, confirmEnabled: Boolean = true,
                         onConfirm: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SuperCartSpacing.sm)) {
        DialogIconButton(Icons.Default.Close, stringResource(R.string.cancel), secondaryButtonColors(),
            Modifier.weight(1f), onClick = onCancel)
        DialogIconButton(Icons.Default.Check, confirmDescription, primaryButtonColors(),
            Modifier.weight(1f), confirmEnabled, onConfirm)
    }
}
