package com.example.ui.lyrics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.model.PlaybackProgress
import com.example.core.model.Song
import com.example.ui.lyrics.components.LyricsEmpty
import com.example.ui.lyrics.components.LyricsLoading
import com.example.lyrics.LyricsContent
import kotlinx.coroutines.flow.StateFlow

@Composable
fun LyricsScreen(
    song: Song,
    playbackProgressFlow: StateFlow<PlaybackProgress>,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    forceLightContent: Boolean = false,
    onSeekTo: ((Long) -> Unit)? = null,
    viewModel: LyricsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(song.id, song.dateModified) {
        viewModel.load(song)
    }

    val contentColor = if (forceLightContent) Color.White else MaterialTheme.colorScheme.onBackground

    Box(modifier = modifier.fillMaxSize()) {
        when (val current = state) {
            LyricsUiState.Idle -> Unit

            is LyricsUiState.Loading -> {
                LyricsLoading()
            }

            is LyricsUiState.Ready -> {
                if (current.songId != song.id) {
                    LyricsLoading()
                    return@Box
                }

                when (val content = current.document.content) {
                    is LyricsContent.Synced -> {
                        val progress by playbackProgressFlow.collectAsStateWithLifecycle()

                        LyricViewer(
                            lyrics = content.lines,
                            positionMs = progress.currentPosition,
                            onSeek = { onSeekTo?.invoke(it) },
                            isPlaying = isPlaying,
                            activeColor = contentColor,
                            inactiveColor = contentColor.copy(alpha = 0.35f),
                            accentColor = MaterialTheme.colorScheme.primary,
                        )
                    }

                    is LyricsContent.Plain -> {
                        PlainLyricsPanel(content.text)
                    }

                    LyricsContent.None -> {
                        LyricsEmpty()
                    }
                }
            }
        }
    }
}
