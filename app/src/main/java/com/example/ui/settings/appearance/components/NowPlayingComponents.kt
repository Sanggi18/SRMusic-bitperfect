package com.example.ui.settings.appearance.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatAlignLeft
import androidx.compose.material.icons.automirrored.rounded.FormatAlignRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FormatAlignCenter
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.NowPlayingProgressStyle
import com.example.core.model.NowPlayingTheme
import com.example.core.model.TrackInfoAlignment

/**
 * Clean Section Header Title matching modern settings typography
 */
@Composable
fun SectionHeaderTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp)
    )
}

/**
 * Interactive card chip representing a progress style with custom graphic preview
 */
@Composable
fun ProgressStyleCard(
    style: NowPlayingProgressStyle,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "progress_card_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "progress_card_content"
    )
    val strokeColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
        label = "progress_card_stroke"
    )

    val interactionSource = remember { MutableInteractionSource() }
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(width = 1.dp, color = strokeColor, shape = RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag("progress_style_${style.name.lowercase()}")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Miniature graphical canvas preview of the progress style
                ProgressStyleGraphic(
                    style = style,
                    color = contentColor,
                    modifier = Modifier
                        .size(width = 38.dp, height = 22.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Clean concise label
                Text(
                    text = getProgressShortLabel(style),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.5.sp
                    ),
                    color = contentColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Draws an accurate mini-graphic representing each progress bar style
 */
@Composable
fun ProgressStyleGraphic(
    style: NowPlayingProgressStyle,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val midY = h / 2f

        when (style) {
            NowPlayingProgressStyle.LINE_THUMB -> {
                // Line with central thumb circle
                drawLine(
                    color = color.copy(alpha = 0.85f),
                    start = Offset(2f, midY),
                    end = Offset(w - 2f, midY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = color,
                    radius = 4.dp.toPx(),
                    center = Offset(w * 0.55f, midY)
                )
            }
            NowPlayingProgressStyle.CAPSULE_PILL -> {
                // Thin track + vertical pill thumb (sesuai CapsulePillProgress asli)
                val trackH = 4.dp.toPx()
                drawLine(
                    color = color.copy(alpha = 0.25f),
                    start = Offset(2f, midY),
                    end = Offset(w - 2f, midY),
                    strokeWidth = trackH,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(2f, midY),
                    end = Offset(w * 0.55f, midY),
                    strokeWidth = trackH,
                    cap = StrokeCap.Round
                )
                val pillW = 5.dp.toPx()
                val pillH = 14.dp.toPx()
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.55f - pillW / 2f, midY - pillH / 2f),
                    size = Size(pillW, pillH),
                    cornerRadius = CornerRadius(pillW / 2f, pillW / 2f)
                )
            }
            NowPlayingProgressStyle.ROUNDED_BAR -> {
                // Thick rounded bar + played/unplayed split (sesuai RoundedBarProgress asli)
                val barH = 8.dp.toPx()
                drawRoundRect(
                    color = color.copy(alpha = 0.25f),
                    topLeft = Offset(2f, midY - barH / 2f),
                    size = Size(w - 4f, barH),
                    cornerRadius = CornerRadius(barH / 2f, barH / 2f)
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(2f, midY - barH / 2f),
                    size = Size((w - 4f) * 0.55f, barH),
                    cornerRadius = CornerRadius(barH / 2f, barH / 2f)
                )
            }
            NowPlayingProgressStyle.WAVEFORM -> {
                // Equalizer vertical bars
                val barCount = 7
                val barWidth = 2.2.dp.toPx()
                val spacing = (w - (barCount * barWidth)) / (barCount + 1)
                val heights = listOf(0.4f, 0.75f, 0.55f, 1.0f, 0.70f, 0.85f, 0.45f)

                heights.forEachIndexed { i, factor ->
                    val barH = h * factor
                    val x = spacing + i * (barWidth + spacing)
                    drawRoundRect(
                        color = color.copy(alpha = 0.9f),
                        topLeft = Offset(x, midY - barH / 2f),
                        size = Size(barWidth, barH),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }
            }
            NowPlayingProgressStyle.WAVE_MINIMAL -> {
                // Smooth Morphe-style wavy bar preview
                val path = Path()
                path.moveTo(2f, midY)
                val waveWidth = (w - 4f) / 2f
                val amplitude = 5.dp.toPx()

                path.cubicTo(
                    2f + waveWidth * 0.25f, midY - amplitude,
                    2f + waveWidth * 0.75f, midY - amplitude,
                    2f + waveWidth, midY
                )
                path.cubicTo(
                    2f + waveWidth * 1.25f, midY + amplitude,
                    2f + waveWidth * 1.75f, midY + amplitude,
                    w - 2f, midY
                )

                drawPath(
                    path = path,
                    color = color.copy(alpha = 0.95f),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            NowPlayingProgressStyle.MATERIAL_3 -> {
                val trackHeight = 6.dp.toPx()
                val thumbX = w * 0.55f
                val thumbW = 2.2.dp.toPx()
                val thumbH = 13.dp.toPx()
                val gap = 2.5.dp.toPx()
                val activeEnd = (thumbX - thumbW / 2f - gap).coerceAtLeast(2f)
                drawRoundRect(
                    color = color,
                    topLeft = Offset(2f, midY - trackHeight / 2f),
                    size = Size((activeEnd - 2f).coerceAtLeast(0f), trackHeight),
                    cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(thumbX - thumbW / 2f, midY - thumbH / 2f),
                    size = Size(thumbW, thumbH),
                    cornerRadius = CornerRadius(thumbW / 2f, thumbW / 2f)
                )
                val inactiveStart = thumbX + thumbW / 2f + gap
                if (inactiveStart < w - 2f) {
                    drawRoundRect(
                        color = color.copy(alpha = 0.25f),
                        topLeft = Offset(inactiveStart, midY - trackHeight / 2f),
                        size = Size(w - 2f - inactiveStart, trackHeight),
                        cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
                    )
                    drawCircle(
                        color = color.copy(alpha = 0.6f),
                        radius = 1.5.dp.toPx(),
                        center = Offset(w - 5f, midY)
                    )
                }
            }
        }
    }
}

/**
 * Formats a clean short label for progress styles
 */
private fun getProgressShortLabel(style: NowPlayingProgressStyle): String {
    return when (style) {
        NowPlayingProgressStyle.LINE_THUMB -> "Line"
        NowPlayingProgressStyle.CAPSULE_PILL -> "Capsule"
        NowPlayingProgressStyle.ROUNDED_BAR -> "Bar"
        NowPlayingProgressStyle.WAVE_MINIMAL -> "Wave"
        NowPlayingProgressStyle.MATERIAL_3 -> "M3 Bar"
        NowPlayingProgressStyle.WAVEFORM -> "Waveform"
    }
}

/**
 * Player Theme Preview Card (Default, Color, Blur, Blur 2)
 * Vertical preview layout with theme background and artwork impression.
 */
@Composable
fun PlayerThemePreviewCard(
    theme: NowPlayingTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "theme_preview_border"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer
    val tertiaryContainer = MaterialTheme.colorScheme.tertiaryContainer
    val surfaceHigh = MaterialTheme.colorScheme.surfaceContainerHighest
    val surfaceHighest = MaterialTheme.colorScheme.surfaceContainerHigh
    val surfaceLow = MaterialTheme.colorScheme.surfaceContainerLow
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer

    val fgColor = when (theme) {
        NowPlayingTheme.DEFAULT -> onSurface
        NowPlayingTheme.COLOR -> onPrimaryContainer
        NowPlayingTheme.BLUR -> onSurface
        NowPlayingTheme.BLUR_2 -> onSurface
    }

    val themeLabel = when (theme) {
        NowPlayingTheme.DEFAULT -> "Default"
        NowPlayingTheme.COLOR -> "Color"
        NowPlayingTheme.BLUR -> "Blur"
        NowPlayingTheme.BLUR_2 -> "Blur 2"
    }

    val interactionSource = remember { MutableInteractionSource() }
    val cardShape = RoundedCornerShape(18.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag("theme_preview_${theme.name.lowercase()}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .clip(cardShape)
                .border(
                    width = if (isSelected) 2.2.dp else 1.dp,
                    color = if (isSelected) borderColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    shape = cardShape
                )
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // 1. Background sesuai karakter tema dengan warna Material You
                when (theme) {
                    NowPlayingTheme.DEFAULT -> {
                        drawRect(color = surfaceHigh)
                    }
                    NowPlayingTheme.COLOR -> {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(primaryContainer, secondaryContainer)
                            )
                        )
                    }
                    NowPlayingTheme.BLUR -> {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(surfaceHigh, surfaceLow)
                            )
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    tertiaryContainer.copy(alpha = 0.75f),
                                    Color.Transparent
                                ),
                                center = Offset(w * 0.46f, h * 0.48f),
                                radius = w * 0.72f
                            ),
                            center = Offset(w * 0.46f, h * 0.48f),
                            radius = w * 0.72f
                        )
                    }
                    NowPlayingTheme.BLUR_2 -> {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(surfaceHighest, surfaceLow)
                            )
                        )
                        // Lingkaran pink (kiri atas)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFEC407A).copy(alpha = 0.75f),
                                    Color.Transparent
                                ),
                                center = Offset(w * 0.30f, h * 0.32f),
                                radius = w * 0.55f
                            ),
                            center = Offset(w * 0.30f, h * 0.32f),
                            radius = w * 0.55f
                        )
                        // Lingkaran biru (kanan bawah)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF42A5F5).copy(alpha = 0.75f),
                                    Color.Transparent
                                ),
                                center = Offset(w * 0.72f, h * 0.68f),
                                radius = w * 0.55f
                            ),
                            center = Offset(w * 0.72f, h * 0.68f),
                            radius = w * 0.55f
                        )
                    }
                }

                // 2. Mini Now Playing UI Elements (Bagian Bawah Kartu)
                val padX = w * 0.15f
                val titleY = h * 0.65f
                val subtitleY = h * 0.715f
                val progressY = h * 0.80f
                val controlsY = h * 0.90f

                // Garis Judul Lagu (lebih panjang & tegas)
                drawLine(
                    color = fgColor.copy(alpha = 0.88f),
                    start = Offset(padX, titleY),
                    end = Offset(w * 0.68f, titleY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Garis Nama Artis (lebih pendek & lembut)
                drawLine(
                    color = fgColor.copy(alpha = 0.55f),
                    start = Offset(padX, subtitleY),
                    end = Offset(w * 0.52f, subtitleY),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Garis Progress Bar
                drawLine(
                    color = fgColor.copy(alpha = 0.75f),
                    start = Offset(padX, progressY),
                    end = Offset(w - padX, progressY),
                    strokeWidth = 1.6.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // 3 Titik Tombol Kontrol (Prev - Play - Next)
                val centerX = w * 0.5f
                val btnSpacing = w * 0.20f
                drawCircle(
                    color = fgColor.copy(alpha = 0.55f),
                    radius = 2.6.dp.toPx(),
                    center = Offset(centerX - btnSpacing, controlsY)
                )
                drawCircle(
                    color = primaryColor,
                    radius = 5.2.dp.toPx(),
                    center = Offset(centerX, controlsY)
                )
                drawCircle(
                    color = fgColor.copy(alpha = 0.55f),
                    radius = 2.6.dp.toPx(),
                    center = Offset(centerX + btnSpacing, controlsY)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = themeLabel,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 12.sp
            ),
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Connected Button Group for Track Info Alignment (Left, Center, Right)
 * M3 Connected Button Group: 2dp gap, outer corners 28dp, inner corners 4dp.
 * Selected = primary, unselected = secondaryContainer.
 */
@Composable
fun SegmentedAlignmentSelector(
    currentAlignment: TrackInfoAlignment,
    onAlignmentSelected: (TrackInfoAlignment) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("segmented_alignment_selector"),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlignmentSegmentItem(
            label = "Left",
            icon = Icons.AutoMirrored.Rounded.FormatAlignLeft,
            isSelected = currentAlignment == TrackInfoAlignment.LEFT,
            shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp, topEnd = 4.dp, bottomEnd = 4.dp),
            onClick = { onAlignmentSelected(TrackInfoAlignment.LEFT) },
            modifier = Modifier.weight(1f)
        )

        AlignmentSegmentItem(
            label = "Center",
            icon = Icons.Rounded.FormatAlignCenter,
            isSelected = currentAlignment == TrackInfoAlignment.CENTER,
            shape = RoundedCornerShape(4.dp),
            onClick = { onAlignmentSelected(TrackInfoAlignment.CENTER) },
            modifier = Modifier.weight(1f)
        )

        AlignmentSegmentItem(
            label = "Right",
            icon = Icons.AutoMirrored.Rounded.FormatAlignRight,
            isSelected = currentAlignment == TrackInfoAlignment.RIGHT,
            shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 28.dp, bottomEnd = 28.dp),
            onClick = { onAlignmentSelected(TrackInfoAlignment.RIGHT) },
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Single segment item in the connected alignment control
 */
@Composable
fun AlignmentSegmentItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    shape: Shape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "segment_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "segment_content"
    )

    Surface(
        onClick = onClick,
        shape = shape,
        color = bgColor,
        modifier = modifier
            .fillMaxHeight()
            .testTag("align_option_${label.lowercase()}")
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.5.sp
                ),
                color = contentColor
            )
        }
    }
}
