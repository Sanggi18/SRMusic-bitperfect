package com.example.ui.home.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.Song
import com.example.ui.common.ArtworkPlaceholderType
import com.example.ui.common.components.ArtworkCard
import com.example.ui.common.components.DynamicArtworkHero
import com.example.ui.common.components.PlayShuffleButtonRow
import com.example.ui.home.core.DailyGenreItem
import com.example.ui.library.core.SongAction
import com.example.ui.library.song.SongItem

@Composable
fun GenreTodayCard(
    dailyGenres: List<DailyGenreItem>,
    allSongs: List<Song>,
    cardColor: Color,
    cardTextColor: Color,
    cardSubtextColor: Color,
    onCardClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTodayGenre = dailyGenres.firstOrNull()
    val todayGenreName = activeTodayGenre?.name ?: "Music"
    val todaySongCount = activeTodayGenre?.songs?.size ?: allSongs.size
    val todaySampleSong = activeTodayGenre?.sampleSong ?: activeTodayGenre?.songs?.firstOrNull() ?: allSongs.firstOrNull()

    val totalGenreCount = remember(allSongs) {
        allSongs.mapNotNull { it.genre }
            .filter { com.example.core.data.mediastore.tags.GenreUtils.isValidGenre(it) }
            .map { com.example.core.data.mediastore.tags.GenreUtils.cleanGenre(it) }
            .distinct()
            .size
            .coerceAtLeast(1)
    }
    val randomAllGenreSong = remember(allSongs) {
        allSongs.randomOrNull()
    }

    HomePagerCard(
        cardColor = cardColor,
        cardTextColor = cardTextColor,
        testTag = "home_genre_today_card",
        pageContent = { actualPage ->
            val isTodaySlide = actualPage == 0
            val title = if (isTodaySlide) "Genre Today" else "All Genre"
            val subtitle = if (isTodaySlide) {
                "$todayGenreName • $todaySongCount Songs"
            } else {
                "$totalGenreCount Genres • ${allSongs.size} Songs"
            }
            val sampleSong = if (isTodaySlide) todaySampleSong else randomAllGenreSong

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onCardClick(actualPage) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Artwork di KIRI
                ArtworkCard(
                    artworkUri = sampleSong?.artworkUri,
                    contentUri = sampleSong?.contentUri,
                    title = sampleSong?.title ?: (if (isTodaySlide) todayGenreName else "All Genre"),
                    size = 48.dp,
                    cornerRadius = 10.dp,
                    placeholderType = ArtworkPlaceholderType.ALBUM
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Info Genre di KANAN
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = cardTextColor,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        ),
                        color = cardSubtextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        modifier = modifier
    )
}

@Composable
fun GenreTodayScreen(
    genreName: String,
    genreSongs: List<Song>,
    artists: List<String>,
    currentPlayingSongId: Long?,
    onBackClick: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onSongAction: (SongAction, Song) -> Unit,
    onRefreshGenre: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = true) {
        onBackClick()
    }

    val currentPlayingSong = remember(genreSongs, currentPlayingSongId) {
        genreSongs.firstOrNull { it.id == currentPlayingSongId }
    }

    val heroArtworkSong = remember(genreSongs, currentPlayingSong) {
        currentPlayingSong ?: genreSongs.firstOrNull()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("genre_today_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                DynamicArtworkHero(
                    songs = genreSongs,
                    onBackClick = onBackClick,
                    currentPlayingSong = currentPlayingSong,
                    placeholderType = ArtworkPlaceholderType.ALBUM,
                    extraAction = {
                        IconButton(
                            onClick = onRefreshGenre,
                            modifier = Modifier.testTag("genre_refresh_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Next Genre",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = genreName,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val artistSubtitle = if (artists.isNotEmpty()) {
                        "Featuring ${artists.take(3).joinToString(", ")} • ${genreSongs.size} Songs"
                    } else {
                        "Genre Today • ${genreSongs.size} Songs"
                    }
                    Text(
                        text = artistSubtitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (genreSongs.isNotEmpty()) {
                        PlayShuffleButtonRow(
                            onPlayAll = {
                                val first = genreSongs.firstOrNull()
                                if (first != null) onSongClick(first, genreSongs)
                            },
                            onShuffle = {
                                val shuffled = genreSongs.shuffled()
                                val first = shuffled.firstOrNull()
                                if (first != null) onSongClick(first, shuffled)
                            },
                            playAllTestTag = "genre_play_all_btn",
                            shuffleTestTag = "genre_shuffle_btn"
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Songs in $genreName (${genreSongs.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            itemsIndexed(genreSongs, key = { index, song -> "${song.id}_$index" }) { index, song ->
                SongItem(
                    song = song,
                    isCurrentSong = song.id == currentPlayingSongId,
                    onSongClick = { onSongClick(song, genreSongs) },
                    onActionClick = onSongAction,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .testTag("genre_song_item_$index")
                )
            }
        }
    }
}
