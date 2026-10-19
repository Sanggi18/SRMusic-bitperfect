package com.example.ui.settings.appearance
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.NowPlayingProgressStyle
import com.example.core.model.NowPlayingTheme
import com.example.core.model.PlaybackProgress
import com.example.core.model.Song
import com.example.core.model.TrackInfoAlignment
import com.example.ui.settings.appearance.components.PlayerThemePreviewCard
import com.example.ui.settings.appearance.components.ProgressStyleCard
import com.example.ui.settings.appearance.components.SectionHeaderTitle
import com.example.ui.settings.appearance.components.SegmentedAlignmentSelector
import kotlinx.coroutines.delay



private const val PREVIEW_TOTAL_DURATION_MS = 273000L // 04:33
private const val PREVIEW_START_POSITION_MS = 61000L   // 01:01

/**
 * Now Playing Style Screen
 *
 * Visual, interactive styling screen inspired by modern Android design:
 * - Real-time Interactive Hero Preview Card (live album art, alignment, and animated progress bar)
 * - Visual Progress Style Selection Cards with crisp mini-graphical illustrations
 * - 2x2 Visual Player Theme Miniature Preview Cards with checkmark badges
 * - Segmented Pill Control for Track Info Alignment (Left, Center, Right)
 * - Full existing logic and state integration preserved
 */
@Composable
fun NowPlayingStyleScreen(
    currentProgressStyle: NowPlayingProgressStyle,
    onProgressStyleChange: (NowPlayingProgressStyle) -> Unit,
    currentTheme: NowPlayingTheme,
    onThemeChange: (NowPlayingTheme) -> Unit,
    currentAlignment: TrackInfoAlignment,
    onAlignmentChange: (TrackInfoAlignment) -> Unit,
    previewSong: Song? = null,
    modifier: Modifier = Modifier
) {
    val displaySong = previewSong ?: Song(
        id = 0L,
        title = "Track Title",
        artist = "Artist Name",
        album = "Album Title",
        duration = PREVIEW_TOTAL_DURATION_MS,
        albumId = 0L,
        contentUri = Uri.EMPTY,
        artworkUri = Uri.EMPTY,
        displayName = "Track Title",
        size = 0L
    )

    // Dynamic self-advancing preview position (starts at 1:01, loops smoothly)
    var currentPositionMs by remember { mutableLongStateOf(PREVIEW_START_POSITION_MS) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(100L)
            currentPositionMs += 100L
            if (currentPositionMs >= PREVIEW_TOTAL_DURATION_MS) {
                currentPositionMs = PREVIEW_START_POSITION_MS
            }
        }
    }

    val previewProgress = PlaybackProgress(
        currentPosition = currentPositionMs,
        duration = PREVIEW_TOTAL_DURATION_MS
    )

    // Dynamic background styling for the live preview card based on selected theme
    val previewCardModifier = when (currentTheme) {
        NowPlayingTheme.DEFAULT -> Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
        NowPlayingTheme.COLOR -> Modifier.background(
            Brush.linearGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.40f),
                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.50f)
                )
            )
        )
        NowPlayingTheme.BLUR -> Modifier.background(
            Brush.radialGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.surfaceContainerHighest,
                    MaterialTheme.colorScheme.surfaceContainer
                )
            )
        )
        NowPlayingTheme.BLUR_2 -> Modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
                    MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(bottom = 64.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ==========================================
        // 1. TOP HERO LIVE PREVIEW CARD
        // ==========================================
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .then(previewCardModifier)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(28.dp)
                )
                .testTag("now_playing_preview_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val previewArtworkBg = MaterialTheme.colorScheme.primaryContainer
                val previewArtworkTint = MaterialTheme.colorScheme.onPrimaryContainer

                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(previewArtworkBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = previewArtworkTint,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Track Title, Artist, Album Title aligned per currentAlignment
                val textAlignAlignment = when (currentAlignment) {
                    TrackInfoAlignment.LEFT -> Alignment.Start
                    TrackInfoAlignment.CENTER -> Alignment.CenterHorizontally
                    TrackInfoAlignment.RIGHT -> Alignment.End
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = textAlignAlignment
                ) {
                    Text(
                        text = displaySong.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = displaySong.artist,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = displaySong.album,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f),
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Live animated progress bar with timestamps mengikuti currentProgressStyle wrapped in 96.dp Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp),
                    contentAlignment = Alignment.Center
                ) {
                    com.example.ui.player.style.progress.PlayerProgressContainer(
                        progress = previewProgress,
                        isPlaying = true,
                        progressStyle = currentProgressStyle,
                        onSeek = { newPositionMs -> currentPositionMs = newPositionMs },
                        song = displaySong,
                        activeColor = null,
                        inactiveColor = null,
                        isPreview = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // ==========================================
        // 2. SECTION: PROGRESS STYLE
        // ==========================================
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeaderTitle("Progress style")

            val styles = NowPlayingProgressStyle.entries
            val row1 = styles.take(3)
            val row2 = styles.drop(3)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row1.forEach { style ->
                        ProgressStyleCard(
                            style = style,
                            isSelected = currentProgressStyle == style,
                            onClick = { onProgressStyleChange(style) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row2.forEach { style ->
                        ProgressStyleCard(
                            style = style,
                            isSelected = currentProgressStyle == style,
                            onClick = { onProgressStyleChange(style) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // ==========================================
        // 3. SECTION: PLAYER THEME
        // ==========================================
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeaderTitle("Player theme")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NowPlayingTheme.entries.forEach { theme ->
                    PlayerThemePreviewCard(
                        theme = theme,
                        isSelected = currentTheme == theme,
                        onClick = { onThemeChange(theme) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ==========================================
        // 4. SECTION: TRACK INFO ALIGNMENT
        // ==========================================
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeaderTitle("Track info alignment")

            SegmentedAlignmentSelector(
                currentAlignment = currentAlignment,
                onAlignmentSelected = onAlignmentChange
            )
        }
    }
}

