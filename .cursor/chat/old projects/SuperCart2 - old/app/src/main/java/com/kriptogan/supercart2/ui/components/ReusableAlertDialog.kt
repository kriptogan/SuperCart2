package com.kriptogan.supercart2.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * Reusable AlertDialog component for the SuperCart app
 * @param isVisible Whether the dialog is visible
 * @param onDismiss Callback when dialog is dismissed
 * @param title Dialog title
 * @param content Dialog content
 * @param confirmText Text for confirm button (optional)
 * @param dismissText Text for dismiss button (optional)
 * @param onConfirm Callback when confirm button is clicked (optional)
 * @param onDismissButton Callback when dismiss button is clicked (optional)
 */
@Composable
fun ReusableAlertDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    title: String,
    content: String,
    confirmText: String? = null,
    dismissText: String? = null,
    onConfirm: (() -> Unit)? = null,
    onDismissButton: (() -> Unit)? = null
) {
    if (isVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = content,
                    fontSize = 16.sp
                )
            },
            confirmButton = {
                if (confirmText != null && onConfirm != null) {
                    TextButton(
                        onClick = onConfirm
                    ) {
                        Text(confirmText)
                    }
                }
            },
            dismissButton = {
                if (dismissText != null) {
                    TextButton(
                        onClick = {
                            onDismissButton?.invoke() ?: onDismiss()
                        }
                    ) {
                        Text(dismissText)
                    }
                }
            }
        )
    }
}
