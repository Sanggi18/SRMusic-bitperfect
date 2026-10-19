package com.example.ui.setup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.common.components.SRMusicGlyphIcon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * AnimatedSplashScreen tanpa background squircle:
 * 1. Note polos muncul di tengah.
 * 2. 3 pill audio masuk berurutan (bawah -> tengah -> atas).
 * 3. Teks "SRMusic" muncul halus.
 * 4. Transisi keluar halus menuju halaman utama.
 */
@Composable
fun AnimatedSplashScreen(
    onAnimationFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(true) }

    // 1. Note Polos
    val noteScale = remember { Animatable(0.5f) }
    val noteAlpha = remember { Animatable(0f) }

    // 2. Tiga Bar Audio (masuk berurutan: bawah -> tengah -> atas)
    val bar3OffsetX = remember { Animatable(35f) }
    val bar3Alpha = remember { Animatable(0f) }

    val bar2OffsetX = remember { Animatable(35f) }
    val bar2Alpha = remember { Animatable(0f) }

    val bar1OffsetX = remember { Animatable(35f) }
    val bar1Alpha = remember { Animatable(0f) }

    // 3. Teks Brand "SRMusic"
    val textAlpha = remember { Animatable(0f) }
    val textOffsetY = remember { Animatable(16f) }

    LaunchedEffect(Unit) {
        // --- Langkah 1: Note polos muncul di tengah ---
        launch {
            noteAlpha.animateTo(1f, tween(200, easing = LinearOutSlowInEasing))
        }
        launch {
            noteScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )
        }

        // --- Langkah 2: 3 Bar Audio masuk dari kanan ke kiri (bawah -> tengah -> atas) ---
        launch {
            delay(140L)
            launch { bar3Alpha.animateTo(1f, tween(180)) }
            bar3OffsetX.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
            )
        }

        launch {
            delay(220L)
            launch { bar2Alpha.animateTo(1f, tween(180)) }
            bar2OffsetX.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
            )
        }

        launch {
            delay(300L)
            launch { bar1Alpha.animateTo(1f, tween(180)) }
            bar1OffsetX.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
            )
        }

        // --- Langkah 3: Teks SRMusic muncul halus ---
        delay(480L)
        launch {
            textAlpha.animateTo(1f, tween(280, easing = FastOutSlowInEasing))
        }
        launch {
            textOffsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }

        // Tahan sejenak tampilan logo utuh
        delay(520L)

        // --- Langkah 4: Fade out keluar ke aplikasi ---
        isVisible = false
        delay(280L)
        onAnimationFinished()
    }

    AnimatedVisibility(
        visible = isVisible,
        exit = fadeOut(animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)),
        modifier = modifier.fillMaxSize()
    ) {
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        val glyphColor = if (isDark) Color.White else Color.Black

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(112.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(96.dp)) {
                        val w = size.width
                        val h = size.height

                        // 1. Gambar note polos (Bulatan Nada & Tiang Nada)
                        val nScale = noteScale.value
                        val nAlpha = noteAlpha.value
                        if (nAlpha > 0f) {
                            val activeNoteColor = glyphColor.copy(alpha = nAlpha)
                            val noteRadius = w * 0.115f * nScale
                            drawCircle(
                                color = activeNoteColor,
                                radius = noteRadius,
                                center = Offset(w * 0.34f, h * 0.65f)
                            )

                            val stemTopY = h * 0.64f - (h * (0.64f - 0.24f) * nScale)
                            drawLine(
                                color = activeNoteColor,
                                start = Offset(w * 0.422f, stemTopY),
                                end = Offset(w * 0.422f, h * 0.64f),
                                strokeWidth = w * 0.068f,
                                cap = StrokeCap.Round
                            )
                        }

                        // 2. Gambar 3 Bar Audio di kanan, masuk dari kanan ke kiri
                        val barStroke = w * 0.068f

                        // Bar 3 (Bawah)
                        val b3Alpha = bar3Alpha.value
                        if (b3Alpha > 0f) {
                            val shiftX = bar3OffsetX.value.dp.toPx()
                            drawLine(
                                color = glyphColor.copy(alpha = b3Alpha),
                                start = Offset(w * 0.54f + shiftX, h * 0.52f),
                                end = Offset(w * 0.63f + shiftX, h * 0.52f),
                                strokeWidth = barStroke,
                                cap = StrokeCap.Round
                            )
                        }

                        // Bar 2 (Tengah)
                        val b2Alpha = bar2Alpha.value
                        if (b2Alpha > 0f) {
                            val shiftX = bar2OffsetX.value.dp.toPx()
                            drawLine(
                                color = glyphColor.copy(alpha = b2Alpha),
                                start = Offset(w * 0.54f + shiftX, h * 0.40f),
                                end = Offset(w * 0.70f + shiftX, h * 0.40f),
                                strokeWidth = barStroke,
                                cap = StrokeCap.Round
                            )
                        }

                        // Bar 1 (Atas)
                        val b1Alpha = bar1Alpha.value
                        if (b1Alpha > 0f) {
                            val shiftX = bar1OffsetX.value.dp.toPx()
                            drawLine(
                                color = glyphColor.copy(alpha = b1Alpha),
                                start = Offset(w * 0.54f + shiftX, h * 0.28f),
                                end = Offset(w * 0.76f + shiftX, h * 0.28f),
                                strokeWidth = barStroke,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "SRMusic",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = textAlpha.value
                            translationY = textOffsetY.value.dp.toPx()
                        }
                )
            }
        }
    }
}
