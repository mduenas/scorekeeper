package com.markduenas.scorekeeper.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.markduenas.scorekeeper.data.models.Participant

@Composable
fun CustomScoreDialog(
    participant: Participant,
    onApplyDelta: (Double) -> Unit,
    onSetScore: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var isSetMode by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isSetMode) "Set Score for ${participant.name}" else "Adjust Score for ${participant.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Add/Subtract")
                    Switch(
                        checked = isSetMode,
                        onCheckedChange = { isSetMode = it; inputText = ""; isError = false }
                    )
                    Text("Set to")
                }
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it; isError = false },
                    label = { Text(if (isSetMode) "New score" else "Amount (+/-)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = isError,
                    supportingText = if (isError) { { Text("Enter a valid number") } } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Current score: ${formatScore(participant.score)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val value = inputText.toDoubleOrNull()
                if (value == null) {
                    isError = true
                } else {
                    if (isSetMode) onSetScore(value) else onApplyDelta(value)
                    onDismiss()
                }
            }) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
