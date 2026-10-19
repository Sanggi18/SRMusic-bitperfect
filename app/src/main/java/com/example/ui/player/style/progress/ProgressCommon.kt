package com.example.ui.player.style.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.core.model.PlaybackProgress
import com.example.core.util.TimeUtils

@Composable
fun ProgressTimeRow(
    displayedPositionMs: Long,
    currentDuration: Long,
    activeColor: Color?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = TimeUtils.formatDuration(displayedPositionMs),
            style = MaterialTheme.typography.labelSmall.copy(
                color = activeColor?.copy(alpha = 0.85f) ?: MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Text(
            text = TimeUtils.formatDuration(currentDuration),
            style = MaterialTheme.typography.labelSmall.copy(
                color = activeColor?.copy(alpha = 0.85f) ?: MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

data class ProgressSeekData(
    val seekState: SeekDragState,
    val currentDuration: Long,
    val currentProgressValue: Float,
    val displayedPositionMs: Long
)

@Composable
fun rememberProgressSeekState(
    progress: PlaybackProgress,
    onSeek: (Long) -> Unit
): ProgressSeekData {
    val seekState = rememberSeekDragState(progress.currentPosition, onSeek)
    val currentDuration = progress.duration
    val currentProgressValue = seekState.currentProgressValue(progress)
    val displayedPositionMs = seekState.displayedPositionMs(progress)
    return remember(seekState, currentDuration, currentProgressValue, displayedPositionMs) {
        ProgressSeekData(seekState, currentDuration, currentProgressValue, displayedPositionMs)
    }
}

@Composable
fun rememberProgressSeekData(
    progress: PlaybackProgress,
    onSeek: (Long) -> Unit
): ProgressSeekData = rememberProgressSeekState(progress, onSeek)
