package com.example.supercart2.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.supercart2.data.GroupManager
import com.example.supercart2.ui.theme.SuperCartColors
import kotlinx.coroutines.launch

@Composable
fun JoinGroupDialog(
    onDismiss: () -> Unit,
    onGroupJoined: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var groupCode by remember { mutableStateOf("") }
    var isJoining by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Join Family Group") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isJoining) {
                    CircularProgressIndicator(
                        color = SuperCartColors.primaryGreen
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Joining group and downloading data...")
                } else {
                    Text("Enter the group code shared by a family member.")
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = groupCode,
                        onValueChange = { 
                            groupCode = it
                            errorMessage = null
                        },
                        label = { Text("Group Code") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SuperCartColors.primaryGreen,
                            focusedLabelColor = SuperCartColors.primaryGreen
                        )
                    )
                    
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage!!,
                            color = Color.Red,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠️ Warning: Your local data will be replaced with the group's data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Red
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        isJoining = true
                        errorMessage = null
                        try {
                            val success = GroupManager.joinGroup(context, groupCode.trim())
                            if (success) {
                                onGroupJoined(groupCode.trim())
                                Toast.makeText(context, "Joined group!", Toast.LENGTH_SHORT).show()
                            } else {
                                errorMessage = "Group not found or invalid code"
                                isJoining = false
                            }
                        } catch (e: Exception) {
                            errorMessage = "Failed to join: ${e.message}"
                            isJoining = false
                        }
                    }
                },
                enabled = !isJoining && groupCode.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SuperCartColors.primaryGreen
                )
            ) {
                Text("Join & Download")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isJoining
            ) {
                Text("Cancel")
            }
        }
    )
}
