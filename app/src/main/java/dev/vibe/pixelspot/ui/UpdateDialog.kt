package dev.vibe.pixelspot.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.vibe.pixelspot.update.AppUpdate
import dev.vibe.pixelspot.update.UpdateState

@Composable
fun UpdateDialog(state: UpdateState, onUpdate: (AppUpdate) -> Unit, onLater: () -> Unit) {
    when (state) {
        UpdateState.None -> Unit
        is UpdateState.Available -> AlertDialog(
            onDismissRequest = onLater,
            title = { Text("Update available") },
            text = {
                Column(
                    Modifier
                        .heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("Version ${state.update.version} is ready to download.")
                    if (state.update.notes.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(state.update.notes, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = { Button(onClick = { onUpdate(state.update) }) { Text("Update") } },
            dismissButton = { TextButton(onClick = onLater) { Text("Later") } },
        )
        is UpdateState.Downloading -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Downloading update") },
            text = {
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {},
        )
        is UpdateState.Failed -> AlertDialog(
            onDismissRequest = onLater,
            title = { Text("Update failed") },
            text = { Text(state.message) },
            confirmButton = { Button(onClick = { onUpdate(state.update) }) { Text("Try again") } },
            dismissButton = { TextButton(onClick = onLater) { Text("Later") } },
        )
    }
}
