package com.example.ui.player.style.progress

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.progressSeekGesture(
    durationMs: Long,
    onSeek: (targetMs: Long) -> Unit,
    onDraggingChange: (isDragging: Boolean, fraction: Float) -> Unit
): Modifier = pointerInput(durationMs) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        down.consume()
        val widthPx = size.width.toFloat()
        if (widthPx <= 0f || durationMs <= 0L) return@awaitEachGesture
        var lastFraction = (down.position.x / widthPx).coerceIn(0f, 1f)
        onDraggingChange(true, lastFraction)
        try {
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) {
                    change.consume()
                    val finalFraction = (change.position.x / widthPx).coerceIn(0f, 1f)
                    onSeek((finalFraction * durationMs).toLong())
                    lastFraction = finalFraction
                    break
                } else {
                    change.consume()
                    lastFraction = (change.position.x / widthPx).coerceIn(0f, 1f)
                    onDraggingChange(true, lastFraction)
                }
            }
        } finally {
            onDraggingChange(false, lastFraction)
        }
    }
}
