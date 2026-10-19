package com.example.ui.navigation.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.core.data.database.playlist.PlaylistEntity
import com.example.core.model.Song
import com.example.ui.settings.core.SettingsAction
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.core.SettingsUiState
import com.example.ui.settings.SettingsDestination
import com.example.ui.sidebar.SidebarDrawer
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun SettingsOverlay(
    isOpen: Boolean,
    onClose: () -> Unit,
    uiState: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
    onNavigateToEqualizer: () -> Unit,
    onRefreshLibrary: () -> Unit,
    initialDestination: SettingsDestination = SettingsDestination.Main
) {
    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn(animationSpec = tween(durationMillis = 280)),
        exit = fadeOut(animationSpec = tween(durationMillis = 220))
    ) {
        SettingsScreen(
            uiState = uiState,
            onAction = onAction,
            onClose = onClose,
            onNavigateToEqualizer = onNavigateToEqualizer,
            onRefreshLibrary = onRefreshLibrary,
            initialDestination = initialDestination
        )
    }
}

@Composable
fun DrawerHost(
    drawerState: DrawerState,
    gesturesEnabled: Boolean,
    onScanMedia: () -> Unit,
    onOpenSettings: () -> Unit,
    totalSongs: Int,
    totalSizeBytes: Long,
    onOpenPlaylists: () -> Unit = {},
    onOpenFavorites: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    isPlaying: Boolean = false,
    hasCurrentSong: Boolean = false,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val drawerWidth = 320.dp
    val drawerWidthPx = remember(density) { with(density) { drawerWidth.toPx() } }
    val scope = rememberCoroutineScope()
    val progressAnim = remember {
        Animatable(if (drawerState.currentValue == DrawerValue.Open) 1f else 0f)
    }

    // Sinkronkan ketika drawerState.open() atau drawerState.close() dipanggil
    LaunchedEffect(drawerState.targetValue, drawerState.currentValue) {
        val target = if (drawerState.isOpen || drawerState.targetValue == DrawerValue.Open) 1f else 0f
        if (abs(progressAnim.value - target) > 0.005f) {
            progressAnim.animateTo(
                targetValue = target,
                animationSpec = tween(
                    durationMillis = 240,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }

    val isDrawerVisible by remember {
        derivedStateOf { progressAnim.value > 0.001f }
    }

    fun closeDrawer() {
        scope.launch {
            progressAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(220, easing = FastOutSlowInEasing)
            )
            drawerState.snapTo(DrawerValue.Closed)
        }
    }

    BackHandler(enabled = isDrawerVisible) {
        closeDrawer()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (gesturesEnabled) {
                    Modifier.pointerInput(drawerWidthPx) {
                        var isEdgeDrag = false
                        detectHorizontalDragGestures(
                            onDragStart = { startOffset ->
                                val edgeZonePx = 36.dp.toPx()
                                isEdgeDrag = progressAnim.value > 0.02f || startOffset.x <= edgeZonePx
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                if (isEdgeDrag) {
                                    change.consume()
                                    val next = (progressAnim.value + dragAmount / drawerWidthPx).coerceIn(0f, 1f)
                                    scope.launch { progressAnim.snapTo(next) }
                                }
                            },
                            onDragEnd = {
                                if (isEdgeDrag) {
                                    isEdgeDrag = false
                                    val shouldOpen = progressAnim.value >= 0.35f
                                    val target = if (shouldOpen) 1f else 0f
                                    scope.launch {
                                        progressAnim.animateTo(
                                            targetValue = target,
                                            animationSpec = tween(
                                                durationMillis = 220,
                                                easing = FastOutSlowInEasing
                                            )
                                        )
                                        drawerState.snapTo(
                                            if (shouldOpen) DrawerValue.Open
                                            else DrawerValue.Closed
                                        )
                                    }
                                }
                            },
                            onDragCancel = {
                                if (isEdgeDrag) {
                                    isEdgeDrag = false
                                    val shouldOpen = progressAnim.value >= 0.5f
                                    val target = if (shouldOpen) 1f else 0f
                                    scope.launch {
                                        progressAnim.animateTo(
                                            targetValue = target,
                                            animationSpec = tween(200)
                                        )
                                        drawerState.snapTo(
                                            if (shouldOpen) DrawerValue.Open
                                            else DrawerValue.Closed
                                        )
                                    }
                                }
                            }
                        )
                    }
                } else Modifier
            )
    ) {
        content()

        if (isDrawerVisible) {
            // Scrim digambar langsung di fase Draw (0 recomposition)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRect(Color.Black.copy(alpha = 0.45f * progressAnim.value))
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        closeDrawer()
                    }
            )
        }

        // Sidebar di-mount dengan batasan lebar tepat 320.dp dan digeser murni via GPU graphicsLayer translationX
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(drawerWidth)
                .graphicsLayer {
                    translationX = -drawerWidthPx * (1f - progressAnim.value)
                    alpha = if (progressAnim.value > 0.001f) 1f else 0f
                }
        ) {
            SidebarDrawer(
                onScanMedia = onScanMedia,
                onOpenSettings = onOpenSettings,
                totalSongs = totalSongs,
                totalSizeBytes = totalSizeBytes,
                onOpenPlaylists = onOpenPlaylists,
                onOpenFavorites = onOpenFavorites,
                onOpenHistory = onOpenHistory,
                isPlaying = isPlaying,
                hasCurrentSong = hasCurrentSong
            )
        }
    }
}

@Composable
fun AddToPlaylistOverlay(
    song: Song?,
    playlists: List<PlaylistEntity>,
    onDismiss: () -> Unit,
    onAddSongToPlaylist: (Long, Long) -> Unit,
    onCreatePlaylist: (String, ((Long) -> Unit)?) -> Unit
) {
    if (song != null) {
        AddToPlaylistDialog(
            song = song,
            playlists = playlists,
            onDismiss = onDismiss,
            onAddToPlaylist = { playlistId, songId ->
                onAddSongToPlaylist(playlistId, songId)
                onDismiss()
            },
            onCreateAndAdd = { playlistName, songId ->
                onCreatePlaylist(playlistName) { newPlaylistId ->
                    onAddSongToPlaylist(newPlaylistId, songId)
                }
                onDismiss()
            }
        )
    }
}
