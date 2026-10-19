package com.example.ui.common.components

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.core.model.Song
import com.example.ui.common.ArtworkPlaceholderType
import com.example.ui.common.ArtworkShape
import com.example.ui.common.components.ArtworkCard

@Composable
fun DynamicArtworkHero(
    songs: List<Song>,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    currentPlayingSong: Song? = null,
    placeholderType: ArtworkPlaceholderType = ArtworkPlaceholderType.ALBUM,
    extraAction: (@Composable () -> Unit)? = null
) {
    val view = LocalView.current
    val isAppInLightMode = MaterialTheme.colorScheme.background.luminance() > 0.5f
    DisposableEffect(isAppInLightMode) {
        val window = (view.context as? Activity)?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, view) }
        insetsController?.isAppearanceLightStatusBars = false
        onDispose {
            insetsController?.isAppearanceLightStatusBars = isAppInLightMode
        }
    }

    val displaySong = remember(songs, currentPlayingSong) {
        currentPlayingSong?.takeIf { playing -> songs.any { it.id == playing.id } }
            ?: songs.firstOrNull { it.artworkUri != null || !it.dataPath.isNullOrBlank() }
            ?: songs.firstOrNull()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        ArtworkCard(
            artworkUri = displaySong?.artworkUri,
            contentUri = displaySong?.contentUri,
            title = displaySong?.title.orEmpty(),
            size = 420.dp,
            shape = ArtworkShape.ROUNDED_SQUARE,
            cornerRadius = 0.dp,
            placeholderType = placeholderType,
            topCrop = true,
            modifier = Modifier.fillMaxSize()
        )

        // Top dark gradient scrim (sama persis seperti Album / Artist)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.70f),
                            Color.Black.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Bottom gradient fade menyatu ke background (sama persis seperti Album / Artist)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.50f to Color.Transparent,
                            0.65f to MaterialTheme.colorScheme.background.copy(alpha = 0.20f),
                            0.78f to MaterialTheme.colorScheme.background.copy(alpha = 0.55f),
                            0.90f to MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                            1.0f to MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        // Back button (sama persis seperti Album / Artist)
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 8.dp, start = 8.dp)
                .align(Alignment.TopStart)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f))
                .testTag("hero_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBackIos,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        if (extraAction != null) {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 8.dp, end = 8.dp)
                    .align(Alignment.TopEnd)
            ) {
                extraAction()
            }
        }
    }
}
