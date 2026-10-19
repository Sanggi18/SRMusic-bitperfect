package com.example.ui.player.style.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.core.model.NowPlayingTheme
import com.example.core.model.Song
import com.example.core.util.ArtworkColors
import com.example.core.util.ArtworkPaletteExtractor
import com.example.ui.common.components.AlbumArtwork

@Composable
fun NowPlayingThemeContainer(
    theme: NowPlayingTheme,
    song: Song?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val bgColor = MaterialTheme.colorScheme.background
    val defaultPrimary = MaterialTheme.colorScheme.primary
    val isDark = bgColor.luminance() <= 0.5f

    val initialColors = remember(song?.id, isDark, defaultPrimary, bgColor) {
        ArtworkPaletteExtractor.getCachedColors(song?.id, isDark)
            ?: ArtworkPaletteExtractor.defaultColors(
                primary = defaultPrimary,
                darkBg = bgColor,
                lightBg = bgColor,
                isDark = isDark
            )
    }

    val artworkColors = produceState(
        initialValue = initialColors,
        key1 = song?.id,
        key2 = theme,
        key3 = isDark
    ) {
        if (theme == NowPlayingTheme.COLOR && song != null) {
            value = ArtworkPaletteExtractor.extractColors(context, song, isDarkTheme = isDark)
        }
    }

    val currentContainerBg = when (theme) {
        NowPlayingTheme.DEFAULT -> Color.Transparent
        NowPlayingTheme.COLOR -> artworkColors.value.getSurface(isDark)
        NowPlayingTheme.BLUR, NowPlayingTheme.BLUR_2 -> bgColor
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(currentContainerBg)
    ) {
        when (theme) {
            NowPlayingTheme.DEFAULT, NowPlayingTheme.COLOR -> Unit
            NowPlayingTheme.BLUR -> {
                if (song != null) {
                    val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.fillMaxSize().blur(32.dp)
                    } else {
                        Modifier.fillMaxSize()
                    }

                    AlbumArtwork(
                        artworkUri = song.contentUri,
                        contentDescription = null,
                        modifier = blurModifier,
                        targetSizePx = 500,
                        shape = RectangleShape
                    )

                    val scrimAlpha = if (isDark) 0.55f else 0.60f
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(bgColor.copy(alpha = scrimAlpha))
                    )
                }
            }
            NowPlayingTheme.BLUR_2 -> {
                if (song != null) {
                    val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.fillMaxSize().blur(45.dp)
                    } else {
                        Modifier.fillMaxSize()
                    }

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        AlbumArtwork(
                            artworkUri = song.contentUri,
                            contentDescription = null,
                            modifier = blurModifier,
                            targetSizePx = 500,
                            shape = RectangleShape
                        )
                    }

                    val scrimColors = if (isDark) {
                        listOf(
                            bgColor.copy(alpha = 0.15f),
                            bgColor.copy(alpha = 0.30f),
                            bgColor.copy(alpha = 0.50f)
                        )
                    } else {
                        listOf(
                            bgColor.copy(alpha = 0.20f),
                            bgColor.copy(alpha = 0.35f),
                            bgColor.copy(alpha = 0.55f)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(scrimColors))
                    )
                }
            }
        }

        content()
    }
}
