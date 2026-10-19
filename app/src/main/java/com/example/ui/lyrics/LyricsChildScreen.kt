package com.example.ui.lyrics

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.PlaybackProgress
import com.example.core.model.Song
import com.example.ui.common.ArtworkPlaceholderType
import com.example.ui.common.ArtworkShape
import com.example.ui.common.components.ArtworkCard
import com.example.ui.lyrics.components.LyricsEmpty
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsChildScreen(
    song: Song?,
    playbackProgressFlow: StateFlow<PlaybackProgress>?,
    isPlaying: Boolean,
    onBackClick: () -> Unit,
    forceLightContent: Boolean = false,
    onSeekTo: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBackClick()
    }

    val contentColor = if (forceLightContent) Color.White else MaterialTheme.colorScheme.onBackground
    val subtitleColor = if (forceLightContent) Color.White.copy(alpha = 0.70f) else MaterialTheme.colorScheme.onSurfaceVariant

    val openProgress = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        openProgress.animateTo(1f, androidx.compose.animation.core.tween(380, easing = androidx.compose.animation.core.FastOutSlowInEasing))
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    if (song != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(end = 16.dp)
                        ) {
                            ArtworkCard(
                                artworkUri = song.artworkUri,
                                contentUri = song.contentUri,
                                title = song.title,
                                size = 42.dp,
                                shape = ArtworkShape.ROUNDED_SQUARE,
                                cornerRadius = 8.dp,
                                placeholderType = ArtworkPlaceholderType.MUSIC,
                                modifier = Modifier.graphicsLayer {
                                    val p = openProgress.value
                                    val s = 2.0f - (1.0f * p)
                                    scaleX = s
                                    scaleY = s
                                    translationY = 96f * (1f - p)
                                }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.5.sp
                                    ),
                                    color = contentColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artist,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 13.sp
                                    ),
                                    color = subtitleColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Lyrics",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = contentColor
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("lyrics_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back to player",
                            tint = contentColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        if (song != null && playbackProgressFlow != null) {
            LyricsScreen(
                song = song,
                playbackProgressFlow = playbackProgressFlow,
                isPlaying = isPlaying,
                forceLightContent = forceLightContent,
                onSeekTo = onSeekTo,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .graphicsLayer {
                        val p = openProgress.value
                        alpha = p
                        translationY = 36f * (1f - p)
                    }
            )
        } else {
            LyricsEmpty(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}
