package com.example.ui.library.song

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.Song
import com.example.core.util.TimeUtils
import com.example.ui.common.components.ArtworkCard
import com.example.ui.library.core.SongAction

@Composable
fun SongItem(
    song: Song,
    isCurrentSong: Boolean = false,
    onSongClick: (Song) -> Unit,
    onActionClick: ((SongAction, Song) -> Unit)? = null,
    modifier: Modifier = Modifier,
    isFastScrolling: Boolean = false,
    isHighSpeedScrolling: Boolean = false,
    showFavoriteHeart: Boolean = false,
    isFavorite: Boolean = false,
    onToggleFavorite: ((Song) -> Unit)? = null
) {
    var showActionMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 1.5.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onSongClick(song) }
            .testTag("song_item_${song.id}"),
        shape = RoundedCornerShape(14.dp),
        color = if (isCurrentSong) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        tonalElevation = if (isCurrentSong) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                ArtworkCard(
                    artworkUri = song.artworkUri,
                    contentUri = song.contentUri,
                    title = song.title,
                    size = 48.dp,
                    cornerRadius = 10.dp,
                    isLoading = isFastScrolling || isHighSpeedScrolling
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isCurrentSong) FontWeight.SemiBold else FontWeight.Medium,
                        fontSize = 14.sp
                    ),
                    color = if (isCurrentSong) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (song.album.isNotBlank() && song.album != "Unknown Album") "${song.artist} • ${song.album}" else song.artist,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = if (isCurrentSong) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (isFavorite || showFavoriteHeart) {
                IconButton(
                    onClick = { onToggleFavorite?.invoke(song) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(6.dp))
            }

            Text(
                text = TimeUtils.formatDuration(song.duration),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                ),
                color = if (isCurrentSong) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Trailing 3-dots action menu
            Box {
                IconButton(
                    onClick = { showActionMenu = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Song options",
                        tint = if (isCurrentSong) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (showActionMenu) {
                    SongItemDropdownMenu(
                        song = song,
                        isFavorite = isFavorite,
                        onDismiss = { showActionMenu = false },
                        onSongClick = onSongClick,
                        onActionClick = onActionClick
                    )
                }
            }
        }
    }
}
