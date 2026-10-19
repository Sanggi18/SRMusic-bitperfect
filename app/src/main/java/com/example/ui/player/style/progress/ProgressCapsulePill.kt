package com.example.ui.player.style.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.model.PlaybackProgress

@Composable
fun CapsulePillProgress(
    progress: PlaybackProgress,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color? = null,
    inactiveColor: Color? = null
) {
    val seekData = rememberProgressSeekState(progress, onSeek)
    val seekState = seekData.seekState
    val currentDuration = seekData.currentDuration
    val currentProgressValue = seekData.currentProgressValue
    val displayedPositionMs = seekData.displayedPositionMs

    val activeTrackColor = activeColor ?: MaterialTheme.colorScheme.primary
    val inactiveTrackColor = inactiveColor ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("now_playing_capsule_pill_slider")
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .progressSeekGesture(
                    durationMs = currentDuration,
                    onSeek = { seekState.onSeekTriggered(it) },
                    onDraggingChange = { dragging, fraction -> seekState.onDraggingChange(dragging, fraction) }
                )
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val centerY = canvasHeight / 2f
                val trackHeightPx = 4.dp.toPx()
                val pillWidthPx = 6.dp.toPx()
                val pillHeightPx = 18.dp.toPx()
                val pillCornerPx = 3.dp.toPx()

                val progressX = (canvasWidth * currentProgressValue).coerceIn(0f, canvasWidth)

                // Inactive Background Track
                drawLine(
                    color = inactiveTrackColor,
                    start = Offset(0f, centerY),
                    end = Offset(canvasWidth, centerY),
                    strokeWidth = trackHeightPx,
                    cap = StrokeCap.Round
                )

                // Active Foreground Track
                if (progressX > 0f) {
                    drawLine(
                        color = activeTrackColor,
                        start = Offset(0f, centerY),
                        end = Offset(progressX, centerY),
                        strokeWidth = trackHeightPx,
                        cap = StrokeCap.Round
                    )
                }

                // Vertical Capsule Pill thumb
                val pillLeft = (progressX - pillWidthPx / 2f).coerceIn(0f, canvasWidth - pillWidthPx)
                val pillTop = centerY - pillHeightPx / 2f

                val pillPath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            rect = androidx.compose.ui.geometry.Rect(
                                offset = Offset(pillLeft, pillTop),
                                size = Size(pillWidthPx, pillHeightPx)
                            ),
                            cornerRadius = CornerRadius(pillCornerPx, pillCornerPx)
                        )
                    )
                }
                drawPath(path = pillPath, color = activeTrackColor)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        ProgressTimeRow(
            displayedPositionMs = displayedPositionMs,
            currentDuration = currentDuration,
            activeColor = activeColor
        )
    }
}
