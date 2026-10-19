package com.example.ui.player.style.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.model.PlaybackProgress

@Composable
fun RoundedBarProgress(
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

    val trackActiveColor = activeColor ?: MaterialTheme.colorScheme.primary
    val trackInactiveColor = inactiveColor ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("now_playing_rounded_bar_slider")
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .progressSeekGesture(
                    durationMs = currentDuration,
                    onSeek = { seekState.onSeekTriggered(it) },
                    onDraggingChange = { dragging, fraction -> seekState.onDraggingChange(dragging, fraction) }
                ),
            contentAlignment = Alignment.Center
        ) {
            val barWidth = maxWidth
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(trackInactiveColor)
            ) {
                if (currentProgressValue > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(barWidth * currentProgressValue)
                            .clip(RoundedCornerShape(4.dp))
                            .background(trackActiveColor)
                    )
                }
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
