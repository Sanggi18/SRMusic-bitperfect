package com.example.ui.library.song

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.model.Song
import com.example.core.util.TimeUtils
import com.example.core.util.formatBytes

@Composable
fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value.ifBlank { "Unknown" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongInfoBottomSheet(
    song: Song,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Song Info",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            InfoRow("Title", song.title)
            InfoRow("Artist", song.artist)
            InfoRow("Album", song.album)
            InfoRow("Duration", TimeUtils.formatDuration(song.duration))
            InfoRow("File Name", song.displayName?.ifBlank { null } ?: song.title)
            val dp = song.dataPath
            if (!dp.isNullOrBlank()) {
                InfoRow("Path", dp)
            } else {
                InfoRow("Content URI", song.contentUri.toString())
            }
            InfoRow("Size", song.size?.let { formatBytes(it) } ?: "—")
        }
    }
}
