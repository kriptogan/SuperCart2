package com.example.supercart2.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.supercart2.data.SettingsManager
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing
import kotlinx.coroutines.launch

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit
) {
    var showAlerts by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    
    // Load current settings
    LaunchedEffect(Unit) {
        showAlerts = SettingsManager.getShowAlerts()
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Show Alerts Setting
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = SuperCartSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Show Alerts",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Display alert bell icon and highlight items with expiring dates or purchase reminders",
                            style = MaterialTheme.typography.bodySmall,
                            color = SuperCartColors.gray,
                            modifier = Modifier.padding(top = SuperCartSpacing.xs)
                        )
                    }
                    Switch(
                        checked = showAlerts,
                        onCheckedChange = { 
                            showAlerts = it
                            scope.launch {
                                SettingsManager.setShowAlerts(it)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SuperCartColors.primaryGreen,
                            checkedTrackColor = SuperCartColors.primaryGreen.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SuperCartSpacing.sm)
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuperCartColors.primaryGreen,
                        contentColor = SuperCartColors.white
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Close"
                    )
                }
            }
        }
    )
}
