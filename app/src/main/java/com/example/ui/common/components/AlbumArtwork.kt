package com.example.ui.common.components

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.common.ArtworkPlaceholderType
import androidx.collection.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private val artworkBitmapCache = object : LruCache<String, Bitmap>(20 * 1024 * 1024) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
}

// Negative cache untuk URI yang memang tidak memiliki artwork agar tidak di-load berulang-ulang saat scroll
private val missingArtworkKeys = java.util.Collections.synchronizedSet(
    object : LinkedHashSet<String>() {
        override fun add(element: String): Boolean {
            if (size >= 1000) {
                val it = iterator()
                if (it.hasNext()) {
                    it.next()
                    it.remove()
                }
            }
            return super.add(element)
        }
    }
)

@Composable
fun AlbumArtwork(
    artworkUri: Uri? = null,
    imageFile: File? = null,
    fallbackText: String = "",
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    targetSizePx: Int? = null,
    shape: Shape = RoundedCornerShape(12.dp),
    placeholderType: ArtworkPlaceholderType = ArtworkPlaceholderType.MUSIC,
    topCrop: Boolean = false,
    onLoadingChange: ((Boolean) -> Unit)? = null,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val px = remember(density, size, targetSizePx) {
        (targetSizePx ?: with(density) { size.roundToPx() }).coerceIn(1, 1024)
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

    val srcKey = remember(artworkUri, imageFile?.absolutePath) {
        artworkUri?.toString() ?: imageFile?.absolutePath
    }

    val cacheKey = remember(srcKey, px) {
        if (srcKey != null) "${srcKey}_$px" else null
    }

    val cachedBitmap = remember(cacheKey) {
        if (cacheKey != null) artworkBitmapCache.get(cacheKey)?.asImageBitmap() else null
    }

    val bitmap by produceState<ImageBitmap?>(
        initialValue = cachedBitmap,
        artworkUri, imageFile, px, cacheKey
    ) {
        if (cacheKey != null) {
            val memBmp = artworkBitmapCache.get(cacheKey)
            if (memBmp != null) {
                value = memBmp.asImageBitmap()
                return@produceState
            }
        }
        if (srcKey == null || missingArtworkKeys.contains(srcKey)) {
            value = null
            return@produceState
        }
        if (artworkUri == null && (imageFile == null || !imageFile.exists())) {
            value = null
            return@produceState
        }

        onLoadingChange?.invoke(true)
        val loaded = withContext(Dispatchers.IO) {
            runCatching {
                val decoded = when {
                    artworkUri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                        runCatching {
                            context.contentResolver.loadThumbnail(artworkUri, Size(px, px), null)
                        }.getOrNull() ?: decodeSampledStream(context.contentResolver, artworkUri, px)
                    }
                    artworkUri != null ->
                        decodeSampledStream(context.contentResolver, artworkUri, px)
                    imageFile != null && imageFile.exists() ->
                        decodeSampledFile(imageFile, px)
                    else -> null
                }
                if (decoded != null && cacheKey != null) {
                    artworkBitmapCache.put(cacheKey, decoded)
                } else if (decoded == null && srcKey != null) {
                    missingArtworkKeys.add(srcKey)
                }
                decoded
            }.getOrNull()
        }
        value = loaded?.asImageBitmap()
        onLoadingChange?.invoke(false)
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                alignment = if (topCrop) Alignment.TopCenter else Alignment.Center,
                contentScale = ContentScale.Crop,
            )
        } else if (fallbackText.isNotBlank()) {
            Text(
                text = fallbackText.trim().first().uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        } else {
            Icon(
                imageVector = placeholderIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(size * 0.45f),
            )
        }
    }
}

private fun calcSampleSize(w: Int, h: Int, reqPx: Int): Int {
    var s = 1
    val minSide = minOf(w, h)
    while (s * 2 <= minSide / reqPx) s *= 2
    return s.coerceAtLeast(1)
}

private fun decodeSampledStream(resolver: ContentResolver, uri: Uri, reqPx: Int): Bitmap? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val sample = calcSampleSize(bounds.outWidth, bounds.outHeight, reqPx)
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.HARDWARE
        }
        val bmp = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        bmp ?: resolver.openInputStream(uri)?.use {
            val fallbackOpts = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeStream(it, null, fallbackOpts)
        }
    } catch (_: Exception) { null }
}

private fun decodeSampledFile(file: File, reqPx: Int): Bitmap? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val sample = calcSampleSize(bounds.outWidth, bounds.outHeight, reqPx)
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.HARDWARE
        }
        BitmapFactory.decodeFile(file.absolutePath, opts) ?: BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sample }
        )
    } catch (_: Exception) { null }
}
