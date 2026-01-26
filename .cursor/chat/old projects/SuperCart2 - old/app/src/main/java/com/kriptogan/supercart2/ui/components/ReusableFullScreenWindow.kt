package com.kriptogan.supercart2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Reusable Full Screen Window component for the SuperCart app
 * @param isVisible Whether the window is visible
 * @param onDismiss Callback when window is dismissed
 * @param title Window title
 * @param content Window content
 * @param showCloseButton Whether to show close button (default: true)
 */
@Composable
fun ReusableFullScreenWindow(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    title: String,
    content: @Composable () -> Unit,
    showCloseButton: Boolean = true
) {
    if (isVisible) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header with title and close button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        
                        if (showCloseButton) {
                            IconButton(
                                onClick = onDismiss
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                    
                    // Content area
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        content()
                    }
                }
            }
        }
    }
}
