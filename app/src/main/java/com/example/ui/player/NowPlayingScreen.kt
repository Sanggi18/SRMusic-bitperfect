package com.example.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.player.components.rememberAudioOutputInfo
import com.example.core.model.NowPlayingTheme
import com.example.core.model.PlaybackProgress
import com.example.core.util.ArtworkPaletteExtractor
import com.example.core.util.TimeUtils
import com.example.ui.common.components.AlbumArtwork
import com.example.ui.common.theme.ExpressiveMotion
import com.example.ui.player.components.PlayerAudioPill
import com.example.ui.player.components.PlayerBottomActions
import com.example.ui.player.components.PlayerControlsRow
import com.example.ui.player.components.PlayerQueueSheet
import com.example.ui.player.core.PlayerAction
import com.example.ui.player.core.PlayerUiState
import com.example.ui.player.style.progress.PlayerProgressContainer
import com.example.ui.player.style.theme.NowPlayingThemeContainer
import com.example.ui.player.style.trackinfo.PlayerTrackInfo
import com.example.core.data.mediastore.tags.TaglibHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    uiState: PlayerUiState,
    onAction: (PlayerAction) -> Unit,
    playbackProgressFlow: StateFlow<PlaybackProgress>? = null,
    containerColor: Color = MaterialTheme.colorScheme.background,
    hideArtwork: Boolean = false,
    isSheetSettledExpanded: Boolean = true,
    onArtworkCoordinatesMeasured: ((LayoutCoordinates) -> Unit)? = null,
    onOpenLyrics: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val song = uiState.currentSong ?: return

    val currentProgress = if (playbackProgressFlow != null) {
        playbackProgressFlow.collectAsStateWithLifecycle().value
    } else {
        uiState.playbackProgress
    }

    var showInfoDialog by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showAudioOutput by remember { mutableStateOf(false) }
    val audioOutputInfo = rememberAudioOutputInfo()

    val context = LocalContext.current
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() <= 0.5f

    // Dynamic color extraction for COLOR, BLUR, and BLUR_2 themes (Ayra & dynamic harmony)
    val artworkColorsState = produceState(
        ArtworkPaletteExtractor.defaultColors(
            primary = MaterialTheme.colorScheme.primary,
            darkBg = MaterialTheme.colorScheme.background,
            lightBg = MaterialTheme.colorScheme.background,
            isDark = isDarkTheme
        ),
        song.id,
        uiState.nowPlayingTheme,
        isDarkTheme,
        uiState.isArtworkAccent
    ) {
        if (uiState.nowPlayingTheme != NowPlayingTheme.DEFAULT && uiState.isArtworkAccent) {
            value = ArtworkPaletteExtractor.extractColors(context, song, isDarkTheme)
        }
    }

    val showThemeBackground = uiState.nowPlayingTheme != NowPlayingTheme.DEFAULT && isSheetSettledExpanded
    val themeBackgroundAlpha by animateFloatAsState(
        targetValue = if (showThemeBackground) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (showThemeBackground) 420 else 180,
            easing = FastOutSlowInEasing
        ),
        label = "now_playing_theme_bg_alpha"
    )

    val isDynamicTheme = uiState.nowPlayingTheme != NowPlayingTheme.DEFAULT && uiState.isArtworkAccent
    val defaultActiveProgress = MaterialTheme.colorScheme.primary
    val defaultInactiveProgress = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
    val defaultPlayButtonColor = MaterialTheme.colorScheme.primaryContainer
    val defaultOnPlayButtonColor = MaterialTheme.colorScheme.onPrimaryContainer
    val defaultIconTintColor = MaterialTheme.colorScheme.onSurface
    val defaultActiveAccent = MaterialTheme.colorScheme.primary

    val targetActiveProgress = if (isDynamicTheme && showThemeBackground) artworkColorsState.value.activeProgress else defaultActiveProgress
    val targetInactiveProgress = if (isDynamicTheme && showThemeBackground) artworkColorsState.value.inactiveProgress else defaultInactiveProgress
    val targetPlayButtonColor = if (isDynamicTheme && showThemeBackground) artworkColorsState.value.controlButton else defaultPlayButtonColor
    val targetOnPlayButtonColor = if (isDynamicTheme && showThemeBackground) artworkColorsState.value.onControlButton else defaultOnPlayButtonColor
    val targetIconTintColor = if (isDynamicTheme && showThemeBackground) artworkColorsState.value.controlIcon else defaultIconTintColor
    val targetActiveAccentColor = if (isDynamicTheme && showThemeBackground) artworkColorsState.value.primary else defaultActiveAccent

    val dynamicActiveProgress by animateColorAsState(targetActiveProgress, tween(380), label = "activeProgress")
    val dynamicInactiveProgress by animateColorAsState(targetInactiveProgress, tween(380), label = "inactiveProgress")
    val dynamicPlayButtonColor by animateColorAsState(targetPlayButtonColor, tween(380), label = "playButton")
    val dynamicOnPlayButtonColor by animateColorAsState(targetOnPlayButtonColor, tween(380), label = "onPlayButton")
    val dynamicIconTintColor by animateColorAsState(targetIconTintColor, tween(380), label = "iconTint")
    val dynamicActiveAccentColor by animateColorAsState(targetActiveAccentColor, tween(380), label = "activeAccent")

    val streamPropsState = produceState<com.example.core.data.mediastore.tags.AudioStreamProps?>(
        initialValue = null,
        key1 = song.id
    ) {
        value = withContext(Dispatchers.IO) {
            TaglibHelper.getAudioStreamProps(song, context)
        }
    }

    val audioPillText = remember(song.id, uiState.technicalInfo, streamPropsState.value) {
        val techInfo = uiState.technicalInfo
        val streamProps = streamPropsState.value
        val sampleRateFormatted = streamProps?.formatSampleRate() ?: techInfo?.sampleRate?.takeIf { it.isNotBlank() }?.lowercase()

        if (techInfo != null && techInfo.bitrate.isNotBlank() && sampleRateFormatted != null) {
            val format = if (techInfo.format.contains("aac", ignoreCase = true)) "M4A" else techInfo.format.uppercase()
            "$format • ${techInfo.bitrate.lowercase()} • $sampleRateFormatted"
        } else if (streamProps != null) {
            val parts = mutableListOf<String>()
            parts.add(streamProps.format)
            if (streamProps.bitrateKbps != null && streamProps.bitrateKbps > 0) {
                parts.add("${streamProps.bitrateKbps} kbps")
            }
            if (sampleRateFormatted != null) {
                parts.add(sampleRateFormatted)
            }
            parts.joinToString(" • ")
        } else {
            var format = song.displayName?.let { dn ->
                if (dn.contains('.')) dn.substringAfterLast('.').uppercase() else null
            } ?: song.dataPath?.let { dp ->
                if (dp.contains('.')) dp.substringAfterLast('.').uppercase() else null
            } ?: "MP3"
            if (format.contains("AAC", ignoreCase = true) || format.contains("MP4", ignoreCase = true)) {
                format = "M4A"
            }
            if (song.duration > 0 && (song.size ?: 0L) > 0) {
                val calculatedKbps = (((song.size ?: 0L) * 8) / song.duration).toInt()
                "$format • ${calculatedKbps}kb/s"
            } else {
                format
            }
        }
    }

    val effectiveScaffoldColor = if (uiState.nowPlayingTheme == NowPlayingTheme.DEFAULT) containerColor else Color.Transparent

    // Material Expressive playback-reactive artwork spring physics
    val isPlaying = uiState.playbackInfo.isPlaying
    val artworkScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.89f,
        animationSpec = ExpressiveMotion.BouncySpring,
        label = "expressive_artwork_scale"
    )
    val artworkCornerRadius by animateDpAsState(
        targetValue = if (isPlaying) 28.dp else 22.dp,
        animationSpec = ExpressiveMotion.SpatialSpringDp,
        label = "expressive_artwork_corners"
    )
    val artworkElevation by animateDpAsState(
        targetValue = if (isPlaying) 16.dp else 6.dp,
        animationSpec = ExpressiveMotion.SnappySpringDp,
        label = "expressive_artwork_elevation"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (uiState.nowPlayingTheme != NowPlayingTheme.DEFAULT && themeBackgroundAlpha > 0.001f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = themeBackgroundAlpha }
            ) {
                NowPlayingThemeContainer(
                    theme = uiState.nowPlayingTheme,
                    song = song,
                    modifier = Modifier.fillMaxSize()
                ) {}
            }
        }

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .testTag("now_playing_screen"),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {},
                    windowInsets = WindowInsets.statusBars,
                    navigationIcon = {
                        IconButton(
                            onClick = { onAction(PlayerAction.Collapse) },
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .testTag("now_playing_dismiss")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = "Collapse Now Playing",
                                tint = dynamicIconTintColor,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    },
                    actions = {},
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(modifier = Modifier.height(2.dp))

                    val measuredModifier = if (onArtworkCoordinatesMeasured != null) {
                        Modifier.onGloballyPositioned { coordinates ->
                            onArtworkCoordinatesMeasured(coordinates)
                        }
                    } else Modifier

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .aspectRatio(1f)
                            .then(measuredModifier),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!hideArtwork) {
                            val shadowColor = dynamicActiveAccentColor?.copy(alpha = if (isPlaying) 0.42f else 0.2f)
                                ?: MaterialTheme.colorScheme.primary.copy(alpha = if (isPlaying) 0.32f else 0.15f)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = artworkScale
                                        scaleY = artworkScale
                                        shadowElevation = artworkElevation.toPx()
                                        shape = RoundedCornerShape(artworkCornerRadius)
                                        clip = true
                                        spotShadowColor = shadowColor
                                        ambientShadowColor = shadowColor
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                AlbumArtwork(
                                    artworkUri = song.contentUri,
                                    fallbackText = song.title,
                                    contentDescription = "Album art for ${song.title}",
                                    modifier = Modifier.fillMaxSize(),
                                    targetSizePx = 1000,
                                    shape = RoundedCornerShape(artworkCornerRadius)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    PlayerTrackInfo(
                        song = song,
                        alignment = uiState.trackInfoAlignment
                    )

                    PlayerProgressContainer(
                        progress = currentProgress,
                        isPlaying = uiState.playbackInfo.isPlaying,
                        progressStyle = uiState.progressStyle,
                        onSeek = { targetMs -> onAction(PlayerAction.SeekTo(targetMs)) },
                        song = song,
                        activeColor = dynamicActiveProgress,
                        inactiveColor = dynamicInactiveProgress
                    )

                    PlayerControlsRow(
                        isShuffle = uiState.isShuffle,
                        repeatMode = uiState.repeatMode,
                        playbackInfo = uiState.playbackInfo,
                        onAction = onAction,
                        playButtonColor = dynamicPlayButtonColor,
                        onPlayButtonColor = dynamicOnPlayButtonColor,
                        iconTintColor = dynamicIconTintColor,
                        activeAccentColor = dynamicActiveAccentColor
                    )

                    PlayerAudioPill(
                        audioPillText = if (showAudioOutput) audioOutputInfo.displayName else audioPillText,
                        pillBgColor = dynamicPlayButtonColor,
                        pillTextColor = dynamicOnPlayButtonColor,
                        onClick = { showAudioOutput = !showAudioOutput }
                    )

                    PlayerBottomActions(
                        onOpenLyrics = onOpenLyrics,
                        onOpenQueue = { showQueueSheet = true },
                        onNavigateToEqualizer = { onAction(PlayerAction.OpenEqualizer) },
                        onShowInfo = { showInfoDialog = true },
                        iconTintColor = dynamicIconTintColor,
                        activeAccentColor = dynamicActiveAccentColor
                    )
                }
            }
        }
    }

    if (showInfoDialog) {
        val techInfo = uiState.technicalInfo
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("Song Info", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Title: ${song.title}", style = MaterialTheme.typography.bodyMedium)
                    Text("Artist: ${song.artist}", style = MaterialTheme.typography.bodyMedium)
                    Text("Album: ${song.album}", style = MaterialTheme.typography.bodyMedium)
                    Text("Duration: ${TimeUtils.formatDuration(song.duration)}", style = MaterialTheme.typography.bodyMedium)
                    if (techInfo != null) {
                        Text("Format: ${techInfo.format}", style = MaterialTheme.typography.bodyMedium)
                        Text("Bit Depth & Sample Rate: ${techInfo.bitDepth} / ${techInfo.sampleRate}", style = MaterialTheme.typography.bodyMedium)
                        Text("Bitrate: ${techInfo.bitrate}", style = MaterialTheme.typography.bodyMedium)
                        Text("Channels: ${techInfo.channels}", style = MaterialTheme.typography.bodyMedium)
                        if (techInfo.fileSizeFormatted.isNotBlank()) {
                            Text("File Size: ${techInfo.fileSizeFormatted}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    val dp = song.dataPath
                    if (!dp.isNullOrBlank()) {
                        Text("Path: $dp", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showInfoDialog = false },
                    modifier = Modifier.testTag("song_info_dismiss")
                ) {
                    Text("Close")
                }
            }
        )
    }

    if (showQueueSheet) {
        PlayerQueueSheet(
            queue = uiState.currentQueue,
            currentSongId = song.id,
            onDismissRequest = { showQueueSheet = false },
            onSelectQueueItem = { index ->
                onAction(PlayerAction.SelectQueueItem(index))
                showQueueSheet = false
            }
        )
    }
}
