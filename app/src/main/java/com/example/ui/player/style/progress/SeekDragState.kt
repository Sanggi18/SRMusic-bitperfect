package com.example.ui.player.style.progress

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.example.core.model.PlaybackProgress

class SeekDragState(
    private val onSeekCallback: () -> (Long) -> Unit
) {
    var isUserDragging by mutableStateOf(false)
    var dragFraction by mutableFloatStateOf(0f)

    fun currentProgressValue(progress: PlaybackProgress): Float {
        return if (isUserDragging) {
            dragFraction.coerceIn(0f, 1f)
        } else {
            progress.progressFraction.coerceIn(0f, 1f)
        }
    }

    fun displayedPositionMs(progress: PlaybackProgress): Long {
        return if (isUserDragging) {
            (dragFraction * progress.duration.toFloat()).toLong().coerceIn(0L, progress.duration.coerceAtLeast(0L))
        } else {
            progress.currentPosition.coerceIn(0L, progress.duration.coerceAtLeast(0L))
        }
    }

    fun onSeekTriggered(targetMs: Long) {
        onSeekCallback().invoke(targetMs)
    }

    fun onDraggingChange(dragging: Boolean, fraction: Float) {
        isUserDragging = dragging
        dragFraction = fraction.coerceIn(0f, 1f)
    }
}

@Composable
fun rememberSeekDragState(
    currentPosition: Long,
    onSeek: (Long) -> Unit
): SeekDragState {
    val currentOnSeek by rememberUpdatedState(onSeek)
    return remember { SeekDragState { currentOnSeek } }
}
