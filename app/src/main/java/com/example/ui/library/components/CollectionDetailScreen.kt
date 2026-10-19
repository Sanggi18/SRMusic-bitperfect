package com.example.ui.library.components

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.core.model.Song
import com.example.ui.common.components.PlayShuffleButtonRow
import com.example.core.util.ArtistHelper
import com.example.core.util.SortHelper
import com.example.ui.common.ArtworkPlaceholderType
import com.example.ui.common.ArtworkShape
import com.example.ui.common.components.ArtworkCard
import com.example.ui.library.core.SongAction
import com.example.ui.library.song.SongItem

data class CollectionDetailConfig(
    val filter: (Song) -> Boolean,
    val sort: Comparator<Song>,
    val subtitle: (tracks: List<Song>) -> String?,
    val infoLine: (tracks: List<Song>) -> String,
    val titleMarquee: Boolean,
)

fun albumDetailConfig(albumName: String) = CollectionDetailConfig(
    filter = { it.album.trim().equals(albumName.trim(), ignoreCase = true) },
    sort = Comparator { a, b ->
        val aTrack = a.trackNumber
        val bTrack = b.trackNumber
        val trackCmp = when {
            aTrack != null && bTrack != null -> aTrack.compareTo(bTrack)
            else -> 0
        }
        if (trackCmp != 0 && aTrack != null && bTrack != null) trackCmp
        else if (aTrack != null && bTrack == null) -1
        else if (aTrack == null && bTrack != null) 1
        else SortHelper.compareTitles(a.title, b.title)
    },
    subtitle = { tracks -> tracks.firstOrNull()?.artist ?: "Unknown Artist" },
    infoLine = { tracks -> "${tracks.size} Tracks" },
    titleMarquee = true,
)

fun artistDetailConfig(artistName: String) = CollectionDetailConfig(
    filter = {
        val main = ArtistHelper.extractMainArtist(it.artist)
        main.equals(artistName, ignoreCase = true) || it.artist.equals(artistName, ignoreCase = true)
    },
    sort = Comparator { a, b -> SortHelper.compareTitles(a.title, b.title) },
    subtitle = { null },
    infoLine = { tracks -> "${tracks.size} Tracks • ${tracks.map { it.album }.distinct().size} Albums" },
    titleMarquee = false,
)

@Composable
fun CollectionDetailScreen(
    title: String,
    config: CollectionDetailConfig,
    songs: List<Song>,
    currentPlayingSongId: Long?,
    onSongClick: (Song, List<Song>) -> Unit,
    onBackClick: () -> Unit,
    onSongAction: ((SongAction, Song) -> Unit)? = null,
    favoriteSongIds: Set<Long> = emptySet(),
    onToggleFavorite: ((Song) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val tracks = remember(songs, title) {
        songs.filter(config.filter).sortedWith(config.sort)
    }

    val randomSong = remember(tracks) {
        tracks.filter { it.artworkUri != null || !it.dataPath.isNullOrBlank() }.randomOrNull() ?: tracks.firstOrNull()
    }

    BackHandler(onBack = onBackClick)

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

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("collection_detail_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item(key = "collection_hero_banner") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                ) {
                    ArtworkCard(
                        artworkUri = randomSong?.artworkUri,
                        contentUri = randomSong?.contentUri,
                        title = title,
                        size = 420.dp,
                        shape = ArtworkShape.ROUNDED_SQUARE,
                        cornerRadius = 0.dp,
                        placeholderType = if (config.subtitle(tracks) != null) ArtworkPlaceholderType.ALBUM else ArtworkPlaceholderType.ARTIST,
                        topCrop = true,
                        modifier = Modifier.fillMaxSize()
                    )

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

                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(top = 8.dp, start = 8.dp)
                            .align(Alignment.TopStart)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.35f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBackIos,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            item(key = "detail_info") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = if (config.titleMarquee) TextOverflow.Clip else TextOverflow.Ellipsis,
                        modifier = if (config.titleMarquee) {
                            Modifier
                                .fillMaxWidth()
                                .basicMarquee()
                        } else {
                            Modifier.fillMaxWidth()
                        }
                    )

                    val subtitleText = remember(tracks) { config.subtitle(tracks) }
                    if (subtitleText != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee()
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = remember(tracks) { config.infoLine(tracks) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (tracks.isNotEmpty()) {
                        PlayShuffleButtonRow(
                            onPlayAll = { onSongClick(tracks.first(), tracks) },
                            onShuffle = {
                                val shuffled = tracks.shuffled()
                                onSongClick(shuffled.first(), shuffled)
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Songs",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            items(tracks, key = { it.id }) { song ->
                val isFav = favoriteSongIds.contains(song.id)
                SongItem(
                    song = song,
                    isCurrentSong = song.id == currentPlayingSongId,
                    onSongClick = { s -> onSongClick(s, tracks) },
                    onActionClick = onSongAction,
                    isFavorite = isFav,
                    onToggleFavorite = onToggleFavorite,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}
