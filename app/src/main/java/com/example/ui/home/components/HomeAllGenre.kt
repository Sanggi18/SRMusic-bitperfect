package com.example.ui.home.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.data.mediastore.tags.GenreUtils
import com.example.core.model.Song
import com.example.ui.common.ArtworkPlaceholderType
import com.example.ui.common.components.DynamicArtworkHero
import com.example.ui.library.core.SongAction
import com.example.ui.library.song.SongItem

enum class AllGenreSortMode {
    BY_NAME,
    BY_SONG_COUNT
}

@Composable
fun AllGenreScreen(
    allSongs: List<Song>,
    currentPlayingSongId: Long?,
    onBackClick: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onSongAction: (SongAction, Song) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = true) {
        onBackClick()
    }

    var sortMode by remember { mutableStateOf(AllGenreSortMode.BY_NAME) }
    var expandedGenres by remember { mutableStateOf(emptySet<String>()) }
    var genreSongLimits by remember { mutableStateOf(mapOf<String, Int>()) }
    val listState = rememberLazyListState()

    val genreGroups = remember(allSongs, sortMode) {
        val grouped = mutableMapOf<String, MutableList<Song>>()
        val unknownList = mutableListOf<Song>()
        for (song in allSongs) {
            val genre = song.genre
            if (GenreUtils.isValidGenre(genre)) {
                val clean = GenreUtils.cleanGenre(genre!!)
                grouped.getOrPut(clean) { mutableListOf() }.add(song)
            } else {
                unknownList.add(song)
            }
        }
        val sortedKeys = when (sortMode) {
            AllGenreSortMode.BY_NAME -> grouped.keys.sortedBy { it.lowercase() }
            AllGenreSortMode.BY_SONG_COUNT -> grouped.keys.sortedWith(
                compareByDescending<String> { grouped[it]?.size ?: 0 }
                    .thenBy { it.lowercase() }
            )
        }
        val result = mutableListOf<Pair<String, List<Song>>>()
        for (genre in sortedKeys) {
            val songsInGenre = grouped[genre]?.sortedBy { it.title.lowercase() } ?: emptyList()
            result.add(genre to songsInGenre)
        }
        if (unknownList.isNotEmpty()) {
            result.add("Other" to unknownList.sortedBy { it.title.lowercase() })
        }
        result
    }

    val flattenedSortedSongs = remember(genreGroups) {
        genreGroups.flatMap { it.second }
    }

    val currentPlayingSong = remember(allSongs, currentPlayingSongId) {
        allSongs.firstOrNull { it.id == currentPlayingSongId }
    }

    val activeHeroSongs = remember(allSongs, currentPlayingSong, expandedGenres, genreGroups) {
        if (currentPlayingSong != null) {
            genreGroups.firstOrNull { (_, songs) -> songs.any { it.id == currentPlayingSong.id } }?.second ?: allSongs
        } else if (expandedGenres.isNotEmpty()) {
            val firstExpandedGenre = expandedGenres.first()
            genreGroups.firstOrNull { it.first == firstExpandedGenre }?.second ?: allSongs
        } else {
            allSongs
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("all_genre_screen")
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item(key = "all_genre_hero") {
                DynamicArtworkHero(
                    songs = activeHeroSongs,
                    onBackClick = onBackClick,
                    currentPlayingSong = currentPlayingSong,
                    placeholderType = ArtworkPlaceholderType.ALBUM
                )
            }

            item(key = "all_genre_info") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "All Genre",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${allSongs.size} Songs • ${genreGroups.size} Genres",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (flattenedSortedSongs.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    val first = flattenedSortedSongs.firstOrNull()
                                    if (first != null) onSongClick(first, flattenedSortedSongs)
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("all_genre_play_all_btn")
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Play All", fontWeight = FontWeight.SemiBold)
                            }

                            FilledTonalButton(
                                onClick = {
                                    sortMode = if (sortMode == AllGenreSortMode.BY_NAME) {
                                        AllGenreSortMode.BY_SONG_COUNT
                                    } else {
                                        AllGenreSortMode.BY_NAME
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("all_genre_sort_btn")
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (sortMode == AllGenreSortMode.BY_NAME) "Name" else "Song Count",
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            itemsIndexed(genreGroups, key = { _, pair -> "genre_group_${pair.first}" }) { _, (genre, songsInGenre) ->
                val isExpanded = expandedGenres.contains(genre)
                val arrowRotation by animateFloatAsState(
                    targetValue = if (isExpanded) 180f else 0f,
                    label = "arrow_rotation_$genre"
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("genre_group_$genre")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                expandedGenres = if (isExpanded) expandedGenres - genre else expandedGenres + genre
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.width(124.dp)
                        ) {
                            Text(
                                text = genre,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = if (isExpanded) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.75f),
                                modifier = Modifier.widthIn(min = 42.dp)
                            ) {
                                Text(
                                    text = "${songsInGenre.size}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    maxLines = 1
                                )
                            }

                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(24.dp)
                                    .rotate(arrowRotation)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            val visibleLimit = genreSongLimits[genre] ?: 30
                            songsInGenre.take(visibleLimit).forEachIndexed { index, song ->
                                SongItem(
                                    song = song,
                                    isCurrentSong = song.id == currentPlayingSongId,
                                    onSongClick = { onSongClick(song, songsInGenre) },
                                    onActionClick = onSongAction,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 2.dp)
                                        .testTag("all_genre_song_${genre}_$index")
                                )
                            }
                            if (songsInGenre.size > visibleLimit) {
                                Text(
                                    text = "Show ${songsInGenre.size - visibleLimit} more",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            genreSongLimits = genreSongLimits + (genre to (visibleLimit + 30))
                                        }
                                        .padding(vertical = 10.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}
