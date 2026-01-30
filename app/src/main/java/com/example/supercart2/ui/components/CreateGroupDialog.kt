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
import com.example.supercart2.data.FirebaseManager
import com.example.supercart2.data.GroupManager
import com.example.supercart2.ui.theme.SuperCartColors
import kotlinx.coroutines.launch

@Composable
fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onGroupCreated: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isCreating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Family Group") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        color = SuperCartColors.primaryGreen
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Creating group and uploading data...")
                } else {
                    Text("This will create a new family group and upload your current data to the cloud.")
                    
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage!!,
                            color = Color.Red,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        isCreating = true
                        errorMessage = null
                        try {
                            val groupCode = GroupManager.createGroup(context)
                            FirebaseManager.uploadData()
                            onGroupCreated(groupCode)
                            Toast.makeText(context, "Group created!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            errorMessage = "Failed to create group: ${e.message}"
                            isCreating = false
                        }
                    }
                },
                enabled = !isCreating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SuperCartColors.primaryGreen
                )
            ) {
                Text("Create & Upload")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isCreating
            ) {
                Text("Cancel")
            }
        }
    )
}
