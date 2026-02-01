package com.example.supercart2.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.supercart2.R
import com.example.supercart2.data.GroupManager
import com.example.supercart2.ui.theme.SuperCartColors
import kotlinx.coroutines.launch

@Composable
fun GroupManagementDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var groupCode by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var showLeaveConfirmation by remember { mutableStateOf(false) }
    
    // Load group code on open
    LaunchedEffect(Unit) {
        groupCode = GroupManager.getCurrentGroupCode(context)
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.family_group)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (groupCode != null) {
                    // Already in a group - show code and leave option
                    Text(stringResource(R.string.youre_in_group))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Group code with copy button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.LightGray.copy(alpha = 0.3f))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = groupCode!!,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            copyToClipboard(context, groupCode!!)
                        }) {
                            Icon(Icons.Default.ContentCopy, stringResource(R.string.copy))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Leave group button
                    Button(
                        onClick = { showLeaveConfirmation = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ExitToApp, stringResource(R.string.leave_action))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.leave_group))
                    }
                } else {
                    // Not in a group - show create/join options
                    Text(stringResource(R.string.share_groceries_message))
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SuperCartColors.primaryGreen
                        )
                    ) {
                        Icon(Icons.Default.Add, stringResource(R.string.create))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.create_group))
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = { showJoinDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SuperCartColors.primaryGreen
                        )
                    ) {
                        Icon(Icons.Default.GroupAdd, stringResource(R.string.join_group))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.join_group))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
    
    // Show create dialog
    if (showCreateDialog) {
        CreateGroupDialog(
            onDismiss = { showCreateDialog = false },
            onGroupCreated = { code ->
                groupCode = code
                showCreateDialog = false
            }
        )
    }
    
    // Show join dialog
    if (showJoinDialog) {
        JoinGroupDialog(
            onDismiss = { showJoinDialog = false },
            onGroupJoined = { code ->
                groupCode = code
                showJoinDialog = false
            }
        )
    }
    
    // Leave confirmation
    if (showLeaveConfirmation) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirmation = false },
            title = { Text(stringResource(R.string.leave_group_question)) },
            text = { Text(stringResource(R.string.local_data_kept)) },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            GroupManager.leaveGroup(context)
                            groupCode = null
                            showLeaveConfirmation = false
                            Toast.makeText(context, context.getString(R.string.group_code), Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    )
                ) {
                    Text(stringResource(R.string.leave_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(context.getString(R.string.group_code), text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Group code copied!", Toast.LENGTH_SHORT).show()
}
