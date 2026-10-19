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
import com.example.core.model.Song
import com.example.ui.common.ArtworkPlaceholderType
import com.example.ui.common.components.DynamicArtworkHero
import com.example.ui.common.YearSortOrder
import com.example.ui.library.components.FastScrollBar
import com.example.ui.library.core.SongAction

@Composable
fun AllReleaseScreen(
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

    var sortOrder by remember { mutableStateOf(YearSortOrder.DESCENDING) }
    var expandedYears by remember { mutableStateOf(emptySet<Int>()) }
    var yearSongLimits by remember { mutableStateOf(mapOf<Int, Int>()) }
    var isFastScrolling by remember { mutableStateOf(false) }
    var isHighSpeedScrolling by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val yearGroups = remember(allSongs, sortOrder) {
        val grouped = allSongs.groupBy { it.year?.takeIf { y -> y > 0 } ?: 0 }
        val sortedYears = grouped.keys.filter { it > 0 }.let { validYears ->
            if (sortOrder == YearSortOrder.DESCENDING) validYears.sortedDescending() else validYears.sorted()
        }
        val result = mutableListOf<Pair<Int, List<Song>>>()
        for (year in sortedYears) {
            val songsInYear = grouped[year]?.sortedBy { it.title.lowercase() } ?: emptyList()
            result.add(year to songsInYear)
        }
        val unknownYearSongs = grouped[0]
        if (!unknownYearSongs.isNullOrEmpty()) {
            result.add(0 to unknownYearSongs.sortedBy { it.title.lowercase() })
        }
        result
    }

    val flattenedSortedSongs = remember(yearGroups) {
        yearGroups.flatMap { it.second }
    }

    val currentPlayingSong = remember(allSongs, currentPlayingSongId) {
        allSongs.firstOrNull { it.id == currentPlayingSongId }
    }

    val activeHeroSongs = remember(allSongs, currentPlayingSong, expandedYears, yearGroups) {
        if (currentPlayingSong != null) {
            val playingYear = currentPlayingSong.year?.takeIf { it > 0 } ?: 0
            yearGroups.firstOrNull { it.first == playingYear }?.second ?: allSongs
        } else if (expandedYears.isNotEmpty()) {
            val firstExpandedYear = expandedYears.first()
            yearGroups.firstOrNull { it.first == firstExpandedYear }?.second ?: allSongs
        } else {
            allSongs
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("all_release_screen")
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item(key = "all_release_hero") {
                DynamicArtworkHero(
                    songs = activeHeroSongs,
                    onBackClick = onBackClick,
                    currentPlayingSong = currentPlayingSong,
                    placeholderType = ArtworkPlaceholderType.ALBUM
                )
            }

            item(key = "all_release_info") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "All Release",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${allSongs.size} Songs • ${yearGroups.size} Release Years",
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
                                onClick = { onSongClick(flattenedSortedSongs.first(), flattenedSortedSongs) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(46.dp)
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Play All", fontWeight = FontWeight.SemiBold)
                            }

                            FilledTonalButton(
                                onClick = {
                                    sortOrder = if (sortOrder == YearSortOrder.DESCENDING) {
                                        YearSortOrder.ASCENDING
                                    } else {
                                        YearSortOrder.DESCENDING
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(46.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (sortOrder == YearSortOrder.DESCENDING) "Newest First" else "Oldest First",
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

            itemsIndexed(yearGroups, key = { _, pair -> "year_group_${pair.first}" }) { _, (year, songsInYear) ->
                val yearLabel = if (year > 0) "$year" else "Unknown"
                val isExpanded = expandedYears.contains(year)
                val arrowRotation by animateFloatAsState(
                    targetValue = if (isExpanded) 180f else 0f,
                    label = "arrow_rotation_$year"
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("year_group_$year")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                expandedYears = if (isExpanded) expandedYears - year else expandedYears + year
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.width(76.dp)
                        ) {
                            Text(
                                text = yearLabel,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = if (isExpanded) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
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
                                    text = "${songsInYear.size}",
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
                            val visibleLimit = yearSongLimits[year] ?: 30
                            songsInYear.take(visibleLimit).forEachIndexed { index, song ->
                                NewReleaseSongRow(
                                    song = song,
                                    isCurrentSong = song.id == currentPlayingSongId,
                                    onSongClick = { onSongClick(song, songsInYear) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 2.dp)
                                        .testTag("all_release_song_${year}_$index")
                                )
                            }
                            if (songsInYear.size > visibleLimit) {
                                Text(
                                    text = "Show ${songsInYear.size - visibleLimit} more",
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
                                            yearSongLimits = yearSongLimits + (year to (visibleLimit + 30))
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

        FastScrollBar(
            listState = listState,
            itemCount = yearGroups.size + 2,
            previewTextProvider = { index ->
                val yearIdx = index - 2
                if (yearIdx in yearGroups.indices) {
                    val y = yearGroups[yearIdx].first
                    if (y > 0) "$y" else "?"
                } else "AR"
            },
            onDraggingChange = { isFastScrolling = it },
            onHighSpeedChange = { isHighSpeedScrolling = it },
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}
