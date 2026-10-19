package com.example.ui.settings.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.core.model.NowPlayingProgressStyle

@Composable
fun StyleProgressPreview(
    style: NowPlayingProgressStyle,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val primary = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val inactive = if (isSelected) primary.copy(alpha = 0.22f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp),
        contentAlignment = Alignment.Center
    ) {
        when (style) {
            NowPlayingProgressStyle.WAVEFORM -> {
                MiniWaveformStylePreview(
                    activeColor = primary,
                    inactiveColor = inactive,
                    modifier = Modifier.fillMaxWidth().height(18.dp)
                )
            }
            NowPlayingProgressStyle.MATERIAL_3 -> {
                MiniMaterial3StylePreview(
                    activeColor = primary,
                    inactiveColor = inactive,
                    modifier = Modifier.fillMaxWidth().height(14.dp)
                )
            }
            NowPlayingProgressStyle.CAPSULE_PILL -> {
                MiniCapsulePillStylePreview(
                    activeColor = primary,
                    inactiveColor = inactive,
                    modifier = Modifier.fillMaxWidth().height(8.dp)
                )
            }
            NowPlayingProgressStyle.ROUNDED_BAR -> {
                MiniRoundedBarStylePreview(
                    activeColor = primary,
                    inactiveColor = inactive,
                    modifier = Modifier.fillMaxWidth().height(4.dp)
                )
            }
            NowPlayingProgressStyle.WAVE_MINIMAL -> {
                MiniWaveMinimalStylePreview(
                    activeColor = primary,
                    inactiveColor = inactive,
                    modifier = Modifier.fillMaxWidth().height(12.dp)
                )
            }
            NowPlayingProgressStyle.LINE_THUMB -> {
                MiniLineThumbStylePreview(
                    activeColor = primary,
                    inactiveColor = inactive,
                    modifier = Modifier.fillMaxWidth().height(12.dp)
                )
            }
        }
    }
}

@Composable
fun MiniWaveformStylePreview(
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier
) {
    val barAmplitudes = remember {
        listOf(0.35f, 0.6f, 0.85f, 0.5f, 0.95f, 0.7f, 0.4f, 0.8f, 1.0f, 0.65f, 0.45f, 0.75f, 0.9f, 0.55f, 0.35f, 0.7f, 0.5f, 0.3f)
    }

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val count = barAmplitudes.size
        val barWidthPx = 2.5.dp.toPx()
        val gap = (width - (count * barWidthPx)) / (count - 1).coerceAtLeast(1)

        val activeCutoff = count * 0.45f // 45% progres terputar

        for (i in 0 until count) {
            val left = i * (barWidthPx + gap)
            val amp = barAmplitudes[i]
            val bHeight = (height * amp).coerceIn(3.dp.toPx(), height)
            val isPlayed = i <= activeCutoff

            drawRoundRect(
                color = if (isPlayed) activeColor else inactiveColor,
                topLeft = androidx.compose.ui.geometry.Offset(left, centerY - bHeight / 2f),
                size = androidx.compose.ui.geometry.Size(barWidthPx, bHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
        }
    }
}

@Composable
fun MiniMaterial3StylePreview(
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val trackH = 8.dp.toPx()
        val trackTop = centerY - trackH / 2f
        val progressX = width * 0.45f

        drawRoundRect(
            color = inactiveColor,
            topLeft = androidx.compose.ui.geometry.Offset(0f, trackTop),
            size = androidx.compose.ui.geometry.Size(width, trackH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackH / 2f, trackH / 2f)
        )
        drawRoundRect(
            color = activeColor,
            topLeft = androidx.compose.ui.geometry.Offset(0f, trackTop),
            size = androidx.compose.ui.geometry.Size(progressX, trackH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackH / 2f, trackH / 2f)
        )
        val thumbW = 3.dp.toPx()
        val thumbH = 14.dp.toPx()
        drawRoundRect(
            color = activeColor,
            topLeft = androidx.compose.ui.geometry.Offset(progressX - thumbW / 2f, centerY - thumbH / 2f),
            size = androidx.compose.ui.geometry.Size(thumbW, thumbH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
        )
    }
}

@Composable
fun MiniCapsulePillStylePreview(
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val progressX = width * 0.45f

        drawRoundRect(
            color = inactiveColor,
            size = Size(width, height),
            cornerRadius = CornerRadius(height / 2f, height / 2f)
        )
        if (progressX > 0f) {
            drawRoundRect(
                color = activeColor,
                size = Size(progressX, height),
                cornerRadius = CornerRadius(height / 2f, height / 2f)
            )
        }
    }
}

@Composable
fun MiniRoundedBarStylePreview(
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val progressX = width * 0.45f

        drawRoundRect(
            color = inactiveColor,
            size = Size(width, height),
            cornerRadius = CornerRadius(height / 2f, height / 2f)
        )
        if (progressX > 0f) {
            drawRoundRect(
                color = activeColor,
                size = Size(progressX, height),
                cornerRadius = CornerRadius(height / 2f, height / 2f)
            )
        }
    }
}

@Composable
fun MiniWaveMinimalStylePreview(
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val progressX = width * 0.45f

        drawLine(
            color = inactiveColor,
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 2.5.dp.toPx()
        )
        if (progressX > 0f) {
            drawLine(
                color = activeColor,
                start = Offset(0f, centerY),
                end = Offset(progressX, centerY),
                strokeWidth = 2.5.dp.toPx()
            )
            drawCircle(
                color = activeColor,
                radius = 4.dp.toPx(),
                center = Offset(progressX, centerY)
            )
        }
    }
}

@Composable
fun MiniLineThumbStylePreview(
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val progressX = width * 0.45f

        drawLine(
            color = inactiveColor,
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 2.dp.toPx()
        )
        if (progressX > 0f) {
            drawLine(
                color = activeColor,
                start = Offset(0f, centerY),
                end = Offset(progressX, centerY),
                strokeWidth = 2.dp.toPx()
            )
            drawCircle(
                color = activeColor,
                radius = 4.5.dp.toPx(),
                center = Offset(progressX, centerY)
            )
        }
    }
}
