package com.example.ui.player.style.progress

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.model.PlaybackProgress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Material3Progress(
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
    val inactiveTrackColor = inactiveColor ?: activeTrackColor.copy(alpha = 0.24f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("material3_progress_bar")
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalMinimumInteractiveComponentSize provides androidx.compose.ui.unit.Dp.Unspecified
        ) {
            Slider(
                value = currentProgressValue,
                onValueChange = { fraction ->
                    seekState.onDraggingChange(true, fraction)
                },
                onValueChangeFinished = {
                    val targetMs = (seekState.dragFraction * currentDuration.toFloat()).toLong()
                    seekState.onDraggingChange(false, seekState.dragFraction)
                    seekState.onSeekTriggered(targetMs)
                },
                colors = SliderDefaults.colors(
                    thumbColor = activeTrackColor,
                    activeTrackColor = activeTrackColor,
                    inactiveTrackColor = inactiveTrackColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("material3_slider")
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        ProgressTimeRow(
            displayedPositionMs = displayedPositionMs,
            currentDuration = currentDuration,
            activeColor = activeColor
        )
    }
}
