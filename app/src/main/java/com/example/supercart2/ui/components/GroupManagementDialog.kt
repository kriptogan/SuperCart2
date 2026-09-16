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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.supercart2.R
import com.example.supercart2.data.GroupManager
import com.example.supercart2.ui.theme.SuperCartColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun GroupManagementDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = remember { GroupManagementDialogState(context, scope) }
    LaunchedEffect(Unit) { state.groupCode = GroupManager.getCurrentGroupCode(context) }
    GroupManagementAlert(state, onDismiss)
    GroupManagementSubDialogs(state)
}

/** Current group code (null when not in a group) and which nested dialog is open. */
private class GroupManagementDialogState(private val context: Context, private val scope: CoroutineScope) {
    var groupCode by mutableStateOf<String?>(null)
    var showCreateDialog by mutableStateOf(false)
    var showJoinDialog by mutableStateOf(false)
    var showLeaveConfirmation by mutableStateOf(false)

    fun closeCreateDialog() {
        showCreateDialog = false
    }

    fun closeJoinDialog() {
        showJoinDialog = false
    }

    fun onGroupCreated(code: String) {
        groupCode = code
        showCreateDialog = false
    }

    fun onGroupJoined(code: String) {
        groupCode = code
        showJoinDialog = false
    }

    fun leaveGroup() {
        scope.launch {
            GroupManager.leaveGroup(context)
            groupCode = null
            showLeaveConfirmation = false
            Toast.makeText(context, context.getString(R.string.group_code), Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun GroupManagementAlert(state: GroupManagementDialogState, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.family_group), color = SuperCartColors.black) },
        text = { GroupManagementBody(state) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

@Composable
private fun GroupManagementBody(state: GroupManagementDialogState) {
    Column(modifier = Modifier.fillMaxWidth()) {
        val code = state.groupCode
        if (code != null) InGroupContent(code) { state.showLeaveConfirmation = true }
        else NoGroupContent(state)
    }
}

@Composable
private fun InGroupContent(code: String, onLeave: () -> Unit) {
    Text(stringResource(R.string.youre_in_group))
    Spacer(modifier = Modifier.height(8.dp))
    GroupCodeRow(code)
    Spacer(modifier = Modifier.height(16.dp))
    IconTextButton(Icons.Default.ExitToApp, R.string.leave_action, R.string.leave_group, Color.Red, onLeave)
}

@Composable
private fun GroupCodeRow(code: String) {
    val modifier = Modifier.fillMaxWidth().background(Color.LightGray.copy(alpha = 0.3f)).padding(8.dp)
    Row(modifier, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(code, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        CopyCodeButton(code)
    }
}

@Composable
private fun CopyCodeButton(code: String) {
    val context = LocalContext.current
    IconButton(onClick = { copyToClipboard(context, code) }) {
        Icon(Icons.Default.ContentCopy, stringResource(R.string.copy))
    }
}

@Composable
private fun NoGroupContent(state: GroupManagementDialogState) {
    Text(stringResource(R.string.share_groceries_message))
    Spacer(modifier = Modifier.height(16.dp))
    IconTextButton(Icons.Default.Add, R.string.create, R.string.create_group) { state.showCreateDialog = true }
    Spacer(modifier = Modifier.height(8.dp))
    IconTextButton(Icons.Default.GroupAdd, R.string.join_group, R.string.join_group) { state.showJoinDialog = true }
}

/** Full-width button with a leading icon (described by [descriptionRes]) and a label. */
@Composable
private fun IconTextButton(
    icon: ImageVector, descriptionRes: Int, textRes: Int, containerColor: Color = SuperCartColors.primaryGreen,
    onClick: () -> Unit,
) {
    val colors = ButtonDefaults.buttonColors(containerColor = containerColor)
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth(), colors = colors) {
        Icon(icon, stringResource(descriptionRes))
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(textRes))
    }
}

@Composable
private fun GroupManagementSubDialogs(state: GroupManagementDialogState) {
    if (state.showCreateDialog) CreateGroupDialog(state::closeCreateDialog, state::onGroupCreated)
    if (state.showJoinDialog) JoinGroupDialog(state::closeJoinDialog, state::onGroupJoined)
    if (state.showLeaveConfirmation) LeaveGroupConfirmation(state)
}

@Composable
private fun LeaveGroupConfirmation(state: GroupManagementDialogState) {
    val close = { state.showLeaveConfirmation = false }
    AlertDialog(
        onDismissRequest = close, text = { Text(stringResource(R.string.local_data_kept)) },
        title = { Text(stringResource(R.string.leave_group_question), color = SuperCartColors.black) },
        confirmButton = { LeaveButton(state::leaveGroup) }, dismissButton = { CancelTextButton(close) },
    )
}

@Composable
private fun CancelTextButton(onClick: () -> Unit) {
    TextButton(onClick = onClick) { Text(stringResource(R.string.cancel)) }
}

@Composable
private fun LeaveButton(onClick: () -> Unit) {
    Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
        Text(stringResource(R.string.leave_action))
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(context.getString(R.string.group_code), text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Group code copied!", Toast.LENGTH_SHORT).show()
}
