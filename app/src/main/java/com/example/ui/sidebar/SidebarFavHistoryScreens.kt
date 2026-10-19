package com.example.ui.sidebar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.model.Song
import com.example.ui.library.core.SongAction
import com.example.ui.library.song.SongItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SidebarFavoritesChildScreen(
    favoriteSongs: List<Song>,
    currentPlayingSongId: Long?,
    isPlaying: Boolean,
    onBackClick: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onSongAction: (SongAction, Song) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = true) { onBackClick() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Favorites",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${favoriteSongs.size} Liked Songs",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp)
        ) {
            if (favoriteSongs.isNotEmpty()) {
                item(key = "favorites_actions") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { onSongClick(favoriteSongs.first(), favoriteSongs) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play All", fontWeight = FontWeight.SemiBold)
                        }
                        FilledTonalButton(
                            onClick = {
                                val shuffled = favoriteSongs.shuffled()
                                onSongClick(shuffled.first(), shuffled)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Shuffle", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            itemsIndexed(favoriteSongs, key = { index, song -> "${song.id}_$index" }) { _, song ->
                SongItem(
                    song = song,
                    isCurrentSong = song.id == currentPlayingSongId,
                    onSongClick = { onSongClick(song, favoriteSongs) },
                    onActionClick = onSongAction,
                    isFavorite = true,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SidebarHistoryChildScreen(
    historySongs: List<Song>,
    currentPlayingSongId: Long?,
    isPlaying: Boolean,
    onBackClick: () -> Unit,
    onClearHistory: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onSongAction: (SongAction, Song) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = true) { onBackClick() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "History",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Riwayat Pemutaran • ${historySongs.size} Songs",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                if (historySongs.isNotEmpty()) {
                    IconButton(onClick = onClearHistory) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteSweep,
                            contentDescription = "Clear History",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp)
        ) {
            if (historySongs.isNotEmpty()) {
                item(key = "history_actions") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { onSongClick(historySongs.first(), historySongs) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play All", fontWeight = FontWeight.SemiBold)
                        }
                        FilledTonalButton(
                            onClick = {
                                val shuffled = historySongs.shuffled()
                                onSongClick(shuffled.first(), shuffled)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Shuffle", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            itemsIndexed(historySongs, key = { index, song -> "${song.id}_$index" }) { _, song ->
                SongItem(
                    song = song,
                    isCurrentSong = song.id == currentPlayingSongId,
                    onSongClick = { onSongClick(song, historySongs) },
                    onActionClick = onSongAction,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }
        }
    }
}
