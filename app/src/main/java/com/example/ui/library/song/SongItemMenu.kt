package com.example.ui.library.song

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Queue
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.core.model.Song
import com.example.ui.library.core.SongAction

@Composable
fun SongActionMenuItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(text, style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        },
        onClick = onClick
    )
}

@Composable
fun SongItemDropdownMenu(
    song: Song,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onSongClick: (Song) -> Unit,
    onActionClick: ((SongAction, Song) -> Unit)?
) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss
    ) {
        // Playback & Queue
        SongActionMenuItem("Play", Icons.Rounded.PlayArrow) {
            onDismiss()
            if (onActionClick != null) {
                onActionClick.invoke(SongAction.PLAY, song)
            } else {
                onSongClick(song)
            }
        }
        SongActionMenuItem("Play Next", Icons.Rounded.QueueMusic) {
            onDismiss()
            onActionClick?.invoke(SongAction.PLAY_NEXT, song)
        }
        SongActionMenuItem("Add to Queue", Icons.Rounded.Queue) {
            onDismiss()
            onActionClick?.invoke(SongAction.ADD_TO_QUEUE, song)
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        // Collection
        SongActionMenuItem("Add to Playlist", Icons.Rounded.PlaylistAdd) {
            onDismiss()
            onActionClick?.invoke(SongAction.ADD_TO_PLAYLIST, song)
        }
        if (isFavorite) {
            SongActionMenuItem("Remove from Favorites", Icons.Rounded.Favorite) {
                onDismiss()
                onActionClick?.invoke(SongAction.REMOVE_FROM_FAVORITES, song)
            }
        } else {
            SongActionMenuItem("Add to Favorites", Icons.Rounded.FavoriteBorder) {
                onDismiss()
                onActionClick?.invoke(SongAction.TOGGLE_FAVORITE, song)
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        // Navigation
        SongActionMenuItem("Go to Artist", Icons.Rounded.Person) {
            onDismiss()
            onActionClick?.invoke(SongAction.GO_TO_ARTIST, song)
        }
        SongActionMenuItem("Go to Album", Icons.Rounded.Album) {
            onDismiss()
            onActionClick?.invoke(SongAction.GO_TO_ALBUM, song)
        }
        SongActionMenuItem("Go to Folder", Icons.Rounded.Folder) {
            onDismiss()
            onActionClick?.invoke(SongAction.GO_TO_FOLDER, song)
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        // Details & Management
        SongActionMenuItem("Song Info", Icons.Rounded.Info) {
            onDismiss()
            onActionClick?.invoke(SongAction.SONG_INFO, song)
        }
        SongActionMenuItem("Delete", Icons.Rounded.Delete) {
            onDismiss()
            onActionClick?.invoke(SongAction.DELETE, song)
        }
    }
}
