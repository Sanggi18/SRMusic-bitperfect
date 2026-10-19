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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.Song
import com.example.core.util.TimeUtils
import com.example.ui.common.ArtworkPlaceholderType
import com.example.ui.common.components.ArtworkCard
import com.example.ui.common.components.DynamicArtworkHero
import com.example.ui.common.components.PlayShuffleButtonRow
import com.example.ui.library.core.SongAction
import java.util.Calendar

@Composable
fun StackedArtworkPreview(
    songs: List<Song>,
    modifier: Modifier = Modifier
) {
    val items = remember(songs) { songs.take(3) }
    val configs = listOf(
        Triple(50.dp, 0.dp, 1f),
        Triple(46.dp, (-13).dp, 0.85f),
        Triple(42.dp, (-26).dp, 0.6f)
    )
    Box(
        modifier = modifier.width(76.dp).height(54.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        for (i in (items.size - 1) downTo 0) {
            val song = items[i]
            val (size, offsetX, alphaVal) = configs[i]
            ArtworkCard(
                artworkUri = song.artworkUri,
                contentUri = song.contentUri,
                title = song.title,
                size = size,
                cornerRadius = 8.dp,
                modifier = Modifier.offset(x = offsetX).alpha(alphaVal)
            )
        }
    }
}

@Composable
fun NewReleaseCard(
    songs: List<Song>,
    cardColor: Color,
    cardTextColor: Color,
    cardSubtextColor: Color,
    onCardClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val latestYear = remember(songs, currentYear) {
        if (songs.any { (it.year ?: 0) == currentYear }) currentYear
        else songs.mapNotNull { it.year }.filter { it > 0 }.maxOrNull() ?: currentYear
    }
    val thisYearSongs = remember(songs, latestYear) {
        songs.filter { (it.year ?: 0) == latestYear }
    }
    val stackAllSongs = remember(songs) {
        if (songs.size <= 3) songs else List(3) { songs.random() }.distinctBy { it.id }.ifEmpty { songs.take(3) }
    }
    val stackNewSongs = remember(thisYearSongs, stackAllSongs) {
        thisYearSongs.take(3).ifEmpty { stackAllSongs }
    }

    HomePagerCard(
        cardColor = cardColor,
        cardTextColor = cardTextColor,
        testTag = "home_new_release_card",
        pageContent = { actualPage ->
            val isNewReleaseSlide = actualPage == 0
            val title = if (isNewReleaseSlide) "New Release" else "All Release"
            val subtitle = if (isNewReleaseSlide) "$latestYear • ${thisYearSongs.size} Songs" else "${songs.size} Songs • By Year"

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onCardClick(actualPage) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                        color = cardTextColor,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = cardSubtextColor,
                        maxLines = 1
                    )
                }
                StackedArtworkPreview(songs = if (isNewReleaseSlide) stackNewSongs else stackAllSongs)
            }
        },
        modifier = modifier
    )
}

@Composable
fun NewReleaseScreen(
    allSongs: List<Song>,
    currentPlayingSongId: Long?,
    onBackClick: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onSongAction: (SongAction, Song) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = true, onBack = onBackClick)

    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val latestYear = remember(allSongs, currentYear) {
        if (allSongs.any { (it.year ?: 0) == currentYear }) currentYear
        else allSongs.mapNotNull { it.year }.filter { it > 0 }.maxOrNull() ?: currentYear
    }
    val thisYearSongs = remember(allSongs, latestYear) {
        allSongs.filter { (it.year ?: 0) == latestYear }.sortedBy { it.title.lowercase() }
    }
    val currentPlayingSong = remember(thisYearSongs, currentPlayingSongId) {
        thisYearSongs.firstOrNull { it.id == currentPlayingSongId }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("new_release_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item(key = "this_year_hero") {
                DynamicArtworkHero(
                    songs = thisYearSongs.ifEmpty { allSongs },
                    onBackClick = onBackClick,
                    currentPlayingSong = currentPlayingSong,
                    placeholderType = ArtworkPlaceholderType.ALBUM
                )
            }
            item(key = "this_year_info") {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "New Release",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 26.sp),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$latestYear • ${thisYearSongs.size} Songs",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium, fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (thisYearSongs.isNotEmpty()) {
                        PlayShuffleButtonRow(
                            onPlayAll = { onSongClick(thisYearSongs.first(), thisYearSongs) },
                            onShuffle = {
                                val shuffled = thisYearSongs.shuffled()
                                onSongClick(shuffled.first(), shuffled)
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Released in $latestYear (${thisYearSongs.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
            itemsIndexed(thisYearSongs, key = { index, song -> "thisyear_${song.id}_$index" }) { index, song ->
                NewReleaseSongRow(
                    song = song,
                    isCurrentSong = song.id == currentPlayingSongId,
                    onSongClick = { onSongClick(song, thisYearSongs) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp).testTag("this_year_song_$index")
                )
            }
        }
    }
}

@Composable
fun NewReleaseSongRow(
    song: Song,
    isCurrentSong: Boolean,
    onSongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val releaseDateText by produceState(
        initialValue = remember(song.id, song.year, song.dateAdded, song.dateModified) {
            TimeUtils.formatQuickSongReleaseDate(song, shortMonth = true)
        },
        key1 = song.id
    ) {
        value = com.example.core.data.mediastore.tags.ReleaseDateExtractor.getCachedReleaseDate(song.id)
            ?: kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                TimeUtils.formatSongReleaseDate(context, song, shortMonth = true)
            }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onSongClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArtworkCard(
            artworkUri = song.artworkUri,
            contentUri = song.contentUri,
            title = song.title,
            size = 42.dp,
            cornerRadius = 8.dp,
            placeholderType = ArtworkPlaceholderType.MUSIC
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 13.5.sp
                ),
                color = if (isCurrentSong) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (releaseDateText.isNotBlank()) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = releaseDateText,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, fontSize = 11.5.sp),
                color = if (isCurrentSong) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun HomeRecentlyAddedCard(
    songs: List<Song>,
    currentPlayingSongId: Long?,
    cardColor: Color,
    cardTextColor: Color,
    cardSubtextColor: Color,
    onSongClick: (Song, List<Song>) -> Unit,
    onSongAction: ((SongAction, Song) -> Unit)?,
    modifier: Modifier = Modifier
) {
    var songLimit by remember { mutableIntStateOf(20) }
    val displaySongs = remember(songs, songLimit) { songs.take(songLimit) }

    Surface(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).testTag("home_recently_added_card"),
        shape = RoundedCornerShape(16.dp),
        color = cardColor,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENTLY ADDED",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, letterSpacing = 1.sp),
                    color = cardTextColor.copy(alpha = 0.7f)
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${displaySongs.size} songs",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, fontSize = 11.sp),
                        color = cardSubtextColor
                    )
                    if (songLimit > 20) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = cardTextColor.copy(alpha = 0.08f),
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { songLimit = 20 }.testTag("recent_songs_reset_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Rounded.RestartAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
                                Text("Reset", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (displaySongs.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("No songs added yet", style = MaterialTheme.typography.bodySmall, color = cardSubtextColor, textAlign = TextAlign.Center)
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    displaySongs.forEach { song ->
                        HomeRecentCardSongRow(
                            song = song,
                            isCurrentSong = song.id == currentPlayingSongId,
                            onSongClick = { onSongClick(song, displaySongs) },
                            onSongAction = onSongAction
                        )
                    }
                }
                if (songs.size > songLimit && songLimit < 100) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { songLimit = minOf(songLimit + 10, 100) }
                            .padding(vertical = 8.dp)
                            .testTag("recent_songs_show_more_btn"),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show More", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun HomeRecentCardSongRow(
    song: Song,
    isCurrentSong: Boolean,
    onSongClick: (Song) -> Unit,
    onSongAction: ((SongAction, Song) -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onSongClick(song) }
            .padding(start = 4.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArtworkCard(
            artworkUri = song.artworkUri,
            contentUri = song.contentUri,
            title = song.title,
            size = 38.dp,
            cornerRadius = 6.dp,
            placeholderType = ArtworkPlaceholderType.MUSIC
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                color = if (isCurrentSong) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = TimeUtils.formatDuration(song.duration),
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
            color = if (isCurrentSong) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (onSongAction != null) {
            IconButton(
                onClick = { onSongAction(SongAction.ADD_TO_PLAYLIST, song) },
                modifier = Modifier.size(28.dp).testTag("home_card_song_more_${song.id}")
            ) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "More", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }
    }
}
