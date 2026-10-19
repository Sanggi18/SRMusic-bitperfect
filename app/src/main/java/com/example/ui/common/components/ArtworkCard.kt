package com.example.ui.common.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.common.ArtworkPlaceholderType
import com.example.ui.common.ArtworkShape

@Composable
fun ArtworkCard(
    artworkUri: Any? = null,
    contentUri: Uri? = null,
    modifier: Modifier = Modifier,
    title: String = "",
    size: Dp = 48.dp,
    shape: ArtworkShape = ArtworkShape.ROUNDED_SQUARE,
    cornerRadius: Dp = 12.dp,
    placeholderType: ArtworkPlaceholderType = ArtworkPlaceholderType.MUSIC,
    isLoading: Boolean = false,
    decodeSizePx: Int? = null,
    topCrop: Boolean = false
) {
    val clipShape = remember(shape, cornerRadius) {
        if (shape == ArtworkShape.CIRCLE) CircleShape else RoundedCornerShape(cornerRadius)
    }

    val placeholderIcon: ImageVector = remember(placeholderType) {
        when (placeholderType) {
            ArtworkPlaceholderType.MUSIC -> Icons.Rounded.MusicNote
            ArtworkPlaceholderType.ARTIST -> Icons.Rounded.Person
            ArtworkPlaceholderType.ALBUM -> Icons.Rounded.Album
            ArtworkPlaceholderType.FOLDER -> Icons.Rounded.Folder
            ArtworkPlaceholderType.AUDIO_FILE -> Icons.Rounded.AudioFile
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(clipShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            Icon(
                imageVector = placeholderIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                modifier = Modifier.size(size * 0.45f)
            )
        } else {
            val primaryUri = contentUri?.takeIf { it != Uri.EMPTY }
                ?: (artworkUri as? Uri)?.takeIf { it != Uri.EMPTY }
            AlbumArtwork(
                artworkUri = primaryUri,
                fallbackText = title,
                contentDescription = if (title.isNotBlank()) "$title artwork" else null,
                modifier = Modifier.fillMaxSize(),
                size = size,
                targetSizePx = decodeSizePx,
                shape = clipShape,
                placeholderType = placeholderType,
                topCrop = topCrop,
            )
        }
    }
}
