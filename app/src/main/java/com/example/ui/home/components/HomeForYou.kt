package com.example.ui.home.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.Song
import com.example.ui.common.ArtworkPlaceholderType
import com.example.ui.common.components.ArtworkCard
import com.example.ui.common.components.DynamicArtworkHero
import com.example.ui.common.components.PlayShuffleButtonRow
import com.example.ui.library.core.SongAction
import com.example.ui.library.song.SongItem

@Composable
fun ForYouCard(
    fourArtworks: List<Song>,
    cardColor: Color,
    cardTextColor: Color,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items = remember(fourArtworks) { fourArtworks.take(4) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(cardColor)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { onCardClick() }
            .padding(14.dp)
            .testTag("for_you_card")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "For You",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp
                ),
                color = cardTextColor
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 2x2 collage artwork grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val row1 = items.take(2)
                val row2 = items.drop(2).take(2)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (song in row1) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(10.dp))
                        ) {
                            ArtworkCard(
                                artworkUri = song.artworkUri,
                                contentUri = song.contentUri,
                                title = song.title,
                                cornerRadius = 10.dp,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    if (row1.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight())
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (song in row2) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(10.dp))
                        ) {
                            ArtworkCard(
                                artworkUri = song.artworkUri,
                                contentUri = song.contentUri,
                                title = song.title,
                                cornerRadius = 10.dp,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ForYouRemixScreen(
    remixSongs: List<Song>,
    currentPlayingSongId: Long?,
    onBackClick: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onSongAction: (SongAction, Song) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = true) {
        onBackClick()
    }

    val currentPlayingSong = remember(remixSongs, currentPlayingSongId) {
        remixSongs.firstOrNull { it.id == currentPlayingSongId }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("for_you_remix_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Immersive Hero Header with Artwork Crossfade
            item(key = "for_you_hero_banner") {
                DynamicArtworkHero(
                    songs = remixSongs,
                    onBackClick = onBackClick,
                    currentPlayingSong = currentPlayingSong,
                    placeholderType = ArtworkPlaceholderType.ALBUM
                )
            }

            // Info and Action Buttons Section
            item(key = "for_you_info") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "For You",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Remix Today • ${remixSongs.size} Songs",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (remixSongs.isNotEmpty()) {
                        PlayShuffleButtonRow(
                            onPlayAll = {
                                val first = remixSongs.firstOrNull()
                                if (first != null) onSongClick(first, remixSongs)
                            },
                            onShuffle = {
                                val shuffled = remixSongs.shuffled()
                                val first = shuffled.firstOrNull()
                                if (first != null) onSongClick(first, shuffled)
                            },
                            playAllTestTag = "for_you_play_all_btn",
                            shuffleTestTag = "for_you_shuffle_btn"
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Tracks in Queue (${remixSongs.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            // Songs List with 16.dp horizontal padding
            itemsIndexed(remixSongs, key = { index, song -> "${song.id}_$index" }) { index, song ->
                SongItem(
                    song = song,
                    isCurrentSong = song.id == currentPlayingSongId,
                    onSongClick = { onSongClick(song, remixSongs) },
                    onActionClick = onSongAction,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .testTag("remix_song_item_$index")
                )
            }
        }
    }
}
