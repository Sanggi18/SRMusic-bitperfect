package com.example.ui.common.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance

/**
 * Reusable icon glyph representing SRMusic: Note head + stem + 3 horizontal audio pills.
 * Transparent background, adapts automatically to background luminance.
 */
@Composable
fun SRMusicGlyphIcon(
    modifier: Modifier = Modifier,
    color: Color = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color.White else Color.Black
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.068f
        drawCircle(color = color, radius = w * 0.115f, center = Offset(w * 0.34f, h * 0.65f))
        drawLine(color = color, start = Offset(w * 0.422f, h * 0.24f), end = Offset(w * 0.422f, h * 0.64f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(w * 0.54f, h * 0.28f), end = Offset(w * 0.76f, h * 0.28f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(w * 0.54f, h * 0.40f), end = Offset(w * 0.70f, h * 0.40f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(w * 0.54f, h * 0.52f), end = Offset(w * 0.63f, h * 0.52f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}
