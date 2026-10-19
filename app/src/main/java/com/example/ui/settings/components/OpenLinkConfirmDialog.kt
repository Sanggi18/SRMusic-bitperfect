package com.example.ui.settings.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun OpenLinkConfirmDialog(
    url: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Buka di browser?") },
        text = {
            Text(text = "Izinkan SRMusic membuka tautan ini di browser?\n\n$url")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Buka")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Batal")
            }
        }
    )
}
