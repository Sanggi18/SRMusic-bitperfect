package com.example.ui.sidebar

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.audio.AudioOutputType
import com.example.ui.player.components.rememberAudioOutputInfo
import com.example.core.util.formatBytes
import com.example.ui.common.theme.springPress
import kotlinx.coroutines.launch
import kotlin.math.roundToInt



/**
 * Overload for host integration (e.g. NavigationOverlays)
 */
@Composable
fun SidebarDrawer(
    onScanMedia: () -> Unit,
    onOpenSettings: () -> Unit,
    totalSongs: Int,
    totalSizeBytes: Long,
    modifier: Modifier = Modifier,
    onOpenPlaylists: () -> Unit = {},
    onOpenFavorites: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onDonateClick: () -> Unit = {},
    isPlaying: Boolean = false,
    hasCurrentSong: Boolean = false
) {
    SidebarDrawerPanel(
        totalSongsCount = totalSongs,
        totalStorageText = formatBytes(totalSizeBytes),
        onScanMedia = onScanMedia,
        onPlaylistClick = onOpenPlaylists,
        onFavoritesClick = onOpenFavorites,
        onHistoryClick = onOpenHistory,
        onSettingsClick = onOpenSettings,
        onDonateClick = onDonateClick,
        isPlaying = isPlaying,
        hasCurrentSong = hasCurrentSong,
        modifier = modifier
    )
}

@Composable
fun SidebarDrawerPanel(
    totalSongsCount: Int,
    totalStorageText: String,
    onScanMedia: () -> Unit,
    onPlaylistClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDonateClick: () -> Unit,
    isPlaying: Boolean = false,
    hasCurrentSong: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Header Card (Floating surface with SRMusic glyph)
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        com.example.ui.common.components.SRMusicGlyphIcon(
                            modifier = Modifier.size(50.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "SRMusic",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$totalSongsCount lagu",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (totalStorageText.isNotBlank()) {
                                Text(
                                    text = totalStorageText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // 2. Four Individual Floating Menu Cards
                SidebarMenuItemCard(
                    icon = Icons.Rounded.Refresh,
                    title = "Scan Media",
                    onClick = onScanMedia
                )

                SidebarMenuItemCard(
                    icon = Icons.Rounded.QueueMusic,
                    title = "Playlist",
                    onClick = onPlaylistClick
                )

                SidebarMenuItemCard(
                    icon = Icons.Rounded.Favorite,
                    title = "Favorites",
                    onClick = onFavoritesClick
                )

                SidebarMenuItemCard(
                    icon = Icons.Rounded.History,
                    title = "History",
                    onClick = onHistoryClick
                )
            }

            // 3. Footer Bottom Menu: Donate & Settings (Floating pills) + Copyright
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val audioOutputInfo = rememberAudioOutputInfo()
                val outputIcon = when (audioOutputInfo.type) {
                    AudioOutputType.USB -> Icons.Rounded.Usb
                    AudioOutputType.BLUETOOTH -> Icons.Rounded.Bluetooth
                    AudioOutputType.WIRED -> Icons.Rounded.Headphones
                    AudioOutputType.SPEAKER -> Icons.Rounded.Speaker
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        Icon(
                            imageVector = outputIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Audio Output",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.align(Alignment.CenterStart)
                                )
                                val dotColor = when {
                                    isPlaying -> Color(0xFF4CAF50)
                                    hasCurrentSong -> Color(0xFF9E9E9E)
                                    else -> Color(0xFFF44336)
                                }
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                            }
                            Text(
                                text = audioOutputInfo.displayName,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        onClick = onDonateClick,
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Donate",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Surface(
                        onClick = onSettingsClick,
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Settings",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Text(
                    text = "© SRMusic App",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun SidebarMenuItemCard(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .springPress(interactionSource, pressedScale = 0.98f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
