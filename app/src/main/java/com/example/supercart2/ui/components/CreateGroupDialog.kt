package com.example.supercart2.ui.components

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    val state = remember { CreateGroupState() }
    CreateGroupAlert(state, onDismiss, onCreate = { scope.launch { state.create(context, onGroupCreated) } })
}

/** Progress/error state of the dialog. On success it stays "creating" (the caller is expected to close it). */
private class CreateGroupState {
    var isCreating by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    suspend fun create(context: Context, onGroupCreated: (String) -> Unit) {
        update(creating = true, error = null)
        try {
            createUploadAndNotify(context, onGroupCreated)
        } catch (e: Exception) {
            update(creating = false, error = context.getString(R.string.failed_to_create, e.message))
        }
    }

    private fun update(creating: Boolean, error: String?) {
        isCreating = creating
        errorMessage = error
    }
}

private suspend fun createUploadAndNotify(context: Context, onGroupCreated: (String) -> Unit) {
    val groupCode = GroupManager.createGroup(context)
    FirebaseManager.uploadData()
    onGroupCreated(groupCode)
    Toast.makeText(context, context.getString(R.string.group_created), Toast.LENGTH_SHORT).show()
}

@Composable
private fun CreateGroupAlert(state: CreateGroupState, onDismiss: () -> Unit, onCreate: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, title = { CreateGroupTitle() },
        text = { CreateGroupBody(state.isCreating, state.errorMessage) },
        confirmButton = { CreateGroupConfirmButton(enabled = !state.isCreating, onCreate) },
        dismissButton = { CreateGroupCancelButton(enabled = !state.isCreating, onDismiss) },
    )
}

@Composable
private fun CreateGroupTitle() {
    Text(stringResource(R.string.create_family_group), color = SuperCartColors.black)
}

@Composable
private fun CreateGroupBody(isCreating: Boolean, errorMessage: String?) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (isCreating) CreateGroupProgress(R.string.please_wait_upload) else CreateGroupIdle(errorMessage)
    }
}

@Composable
private fun CreateGroupProgress(@StringRes message: Int) {
    CircularProgressIndicator(color = SuperCartColors.primaryGreen)
    Spacer(modifier = Modifier.height(8.dp))
    Text(stringResource(message))
}

@Composable
private fun CreateGroupIdle(errorMessage: String?) {
    Text(stringResource(R.string.upload_success_message))
    if (errorMessage != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = errorMessage, color = Color.Red, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun CreateGroupConfirmButton(enabled: Boolean, onClick: () -> Unit) {
    val colors = ButtonDefaults.buttonColors(containerColor = SuperCartColors.primaryGreen)
    Button(onClick = onClick, enabled = enabled, colors = colors) {
        Text(stringResource(R.string.create_group))
    }
}

@Composable
private fun CreateGroupCancelButton(enabled: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = enabled) {
        Text(stringResource(R.string.cancel))
    }
}
