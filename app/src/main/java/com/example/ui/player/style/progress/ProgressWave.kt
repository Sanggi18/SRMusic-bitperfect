package com.example.ui.player.style.progress

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.model.PlaybackProgress
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun WaveMinimalProgress(
    progress: PlaybackProgress,
    isPlaying: Boolean,
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

    val wavePhase = remember { Animatable(0f) }

    val waveAmplitudeFactor by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(
            durationMillis = 280,
            easing = FastOutSlowInEasing
        ),
        label = "wave_amplitude"
    )

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            val twoPi = 2f * PI.toFloat()

            while (true) {
                wavePhase.animateTo(
                    targetValue = wavePhase.value + twoPi,
                    animationSpec = tween(
                        durationMillis = 1600,
                        easing = LinearEasing
                    )
                )
                wavePhase.snapTo(wavePhase.value % twoPi)
            }
        }
    }

    val trackActiveColor = activeColor ?: MaterialTheme.colorScheme.primary
    val trackInactiveColor = inactiveColor ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("now_playing_wave_slider")
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
                // Morphe WavyProgress styling: BarStrokeWidth = 6.dp, BarHeight = 16.dp
                val strokeWidthPx = 6.dp.toPx()
                val waveAmplitudePx = 5.dp.toPx() * waveAmplitudeFactor
                val wavelengthPx = 40.dp.toPx()

                val progressX = (canvasWidth * currentProgressValue).coerceIn(0f, canvasWidth)

                // 1. Draw Inactive Track only after the wave head, separated by M3's track gap
                val trackStartX = (progressX + 10.dp.toPx()).coerceAtMost(canvasWidth)
                if (trackStartX < canvasWidth) {
                    drawLine(
                        color = trackInactiveColor,
                        start = Offset(trackStartX, centerY),
                        end = Offset(canvasWidth, centerY),
                        strokeWidth = strokeWidthPx,
                        cap = StrokeCap.Round
                    )
                }

                // 2. Draw Active Wave Track
                if (progressX > 0f) {
                    val activePath = Path()
                    val phase = wavePhase.value
                    val k = (2f * PI.toFloat()) / wavelengthPx

                    val startY = centerY + (sin(phase) * waveAmplitudePx)
                    activePath.moveTo(0f, startY)

                    var x = 0f
                    val stepPx = 2f

                    while (x < progressX) {
                        x = (x + stepPx).coerceAtMost(progressX)
                        val angle = (k * x) + phase
                        val y = centerY + (sin(angle) * waveAmplitudePx)
                        activePath.lineTo(x, y)
                    }

                    drawPath(
                        path = activePath,
                        color = trackActiveColor,
                        style = Stroke(
                            width = strokeWidthPx,
                            cap = StrokeCap.Round
                        )
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
