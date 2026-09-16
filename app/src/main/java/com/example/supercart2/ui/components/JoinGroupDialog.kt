package com.example.supercart2.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.supercart2.R
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
    val state = remember { JoinGroupState() }
    JoinGroupAlert(state, onDismiss, onJoin = { scope.launch { state.join(context, onGroupJoined) } })
}

private const val GROUP_NOT_FOUND = "Group not found or invalid code"

/** Code/progress/error state of the dialog. On success it stays "joining" (the caller is expected to close it). */
private class JoinGroupState {
    var groupCode by mutableStateOf("")
    var isJoining by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    fun onCodeChange(code: String) {
        groupCode = code
        errorMessage = null
    }

    suspend fun join(context: Context, onGroupJoined: (String) -> Unit) {
        update(joining = true, error = null)
        try {
            if (!joinAndNotify(context, onGroupJoined)) update(joining = false, error = GROUP_NOT_FOUND)
        } catch (e: Exception) {
            update(joining = false, error = "Failed to join: ${e.message}")
        }
    }

    private suspend fun joinAndNotify(context: Context, onGroupJoined: (String) -> Unit): Boolean {
        if (!GroupManager.joinGroup(context, groupCode.trim())) return false
        onGroupJoined(groupCode.trim())
        Toast.makeText(context, context.getString(R.string.group_joined), Toast.LENGTH_SHORT).show()
        return true
    }

    private fun update(joining: Boolean, error: String?) {
        isJoining = joining
        errorMessage = error
    }
}

@Composable
private fun JoinGroupAlert(state: JoinGroupState, onDismiss: () -> Unit, onJoin: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, title = { JoinGroupTitle() }, text = { JoinGroupBody(state) },
        confirmButton = { JoinGroupConfirmButton(enabled = !state.isJoining && state.groupCode.isNotBlank(), onJoin) },
        dismissButton = { JoinGroupCancelButton(enabled = !state.isJoining, onDismiss) },
    )
}

@Composable
private fun JoinGroupTitle() {
    Text(stringResource(R.string.join_family_group), color = SuperCartColors.black)
}

@Composable
private fun JoinGroupBody(state: JoinGroupState) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (state.isJoining) JoinGroupProgress() else JoinGroupForm(state)
    }
}

@Composable
private fun JoinGroupProgress() {
    CircularProgressIndicator(color = SuperCartColors.primaryGreen)
    Spacer(modifier = Modifier.height(8.dp))
    Text(stringResource(R.string.please_wait_download))
}

@Composable
private fun JoinGroupForm(state: JoinGroupState) {
    Text(stringResource(R.string.enter_group_code))
    Spacer(modifier = Modifier.height(8.dp))
    GroupCodeField(state.groupCode, state::onCodeChange)
    state.errorMessage?.let { JoinGroupSmallRedText(it) }
    JoinGroupSmallRedText("⚠️ Warning: Your local data will be replaced with the group's data.")
}

/** 8dp gap followed by a small red message (errors and the data-replacement warning). */
@Composable
private fun JoinGroupSmallRedText(text: String) {
    Spacer(modifier = Modifier.height(8.dp))
    Text(text = text, color = Color.Red, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun GroupCodeField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(stringResource(R.string.group_code)) },
        modifier = Modifier.fillMaxWidth(), singleLine = true, colors = groupCodeFieldColors(),
    )
}

@Composable
private fun groupCodeFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SuperCartColors.primaryGreen, focusedLabelColor = SuperCartColors.primaryGreen,
    focusedTextColor = SuperCartColors.black, unfocusedTextColor = SuperCartColors.black,
)

@Composable
private fun JoinGroupConfirmButton(enabled: Boolean, onClick: () -> Unit) {
    val colors = ButtonDefaults.buttonColors(containerColor = SuperCartColors.primaryGreen)
    Button(onClick = onClick, enabled = enabled, colors = colors) {
        Text(stringResource(R.string.join_group))
    }
}

@Composable
private fun JoinGroupCancelButton(enabled: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = enabled) {
        Text(stringResource(R.string.cancel))
    }
}
