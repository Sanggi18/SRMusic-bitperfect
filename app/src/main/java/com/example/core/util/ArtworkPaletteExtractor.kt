package com.example.core.util

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.collection.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.example.core.data.mediastore.tags.TaglibHelper
import com.example.core.model.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class ArtworkColors(
    val primary: Color,
    val secondary: Color,
    val darkMuted: Color,
    val lightSurface: Color,
    val darkSurface: Color,
    val activeProgress: Color,
    val inactiveProgress: Color,
    val controlButton: Color,
    val onControlButton: Color,
    val controlIcon: Color
) {
    fun getSurface(isDark: Boolean): Color = if (isDark) darkSurface else lightSurface
}

object ArtworkPaletteExtractor {
    private data class CacheKey(val songId: Long, val isDark: Boolean)
    private val colorCache = LruCache<CacheKey, ArtworkColors>(128)
    private val lock = Any()
    private val inFlightRequests = HashMap<CacheKey, CompletableDeferred<ArtworkColors>>()
    private var currentCacheEpoch = 0L

    fun getCachedColors(songId: Long?, isDarkTheme: Boolean): ArtworkColors? {
        if (songId == null) return null
        return synchronized(lock) {
            colorCache[CacheKey(songId, isDarkTheme)]
        }
    }

    fun defaultColors(
        primary: Color = Color(0xFF2196F3),
        darkBg: Color = Color(0xFF121214),
        lightBg: Color = Color(0xFFF8F9FC),
        isDark: Boolean = false
    ): ArtworkColors {
        val surface = if (isDark) darkBg else lightBg
        val onSurface = if (isDark) Color(0xFFF0F0F5) else Color(0xFF14161C)
        return ArtworkColors(
            primary = primary,
            secondary = primary,
            darkMuted = darkBg,
            lightSurface = lightBg,
            darkSurface = darkBg,
            activeProgress = primary,
            inactiveProgress = if (isDark) Color.White.copy(alpha = 0.24f) else Color.Black.copy(alpha = 0.16f),
            controlButton = primary.copy(alpha = 0.50f),
            onControlButton = Color.White,
            controlIcon = onSurface
        )
    }

    fun normalizeSafeAccentColor(rgb: Int, isDarkTheme: Boolean): Color {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(rgb, hsl)
        val hue = hsl[0]
        var sat = hsl[1]
        var light = hsl[2]

        // Achromatic / nearly grayscale (e.g. pure black, dark grey, pure white album covers)
        if (sat < 0.15f || (isDarkTheme && light < 0.18f) || (!isDarkTheme && light > 0.85f)) {
            return if (isDarkTheme) {
                // Soft non-flashy pastel light blue with crisp contrast on dark canvas
                Color(0xFF64B5F6)
            } else {
                // Rich, deep readable blue with crisp contrast on light canvas
                Color(0xFF1E6EB8)
            }
        }

        if (isDarkTheme) {
            // Guarantee luminous readable primary against dark surfaces (never black or invisible)
            light = light.coerceIn(0.62f, 0.82f)
            sat = sat.coerceAtLeast(0.50f)
        } else {
            // Guarantee deep readable primary against light surfaces (never white or washed out)
            light = light.coerceIn(0.26f, 0.42f)
            sat = sat.coerceAtLeast(0.60f)
        }

        val safeRgb = ColorUtils.HSLToColor(floatArrayOf(hue, sat, light))
        return Color(safeRgb)
    }

    suspend fun extractColors(
        context: Context,
        song: Song?,
        isDarkTheme: Boolean = false
    ): ArtworkColors = withContext(Dispatchers.IO) {
        if (song == null) {
            return@withContext defaultColors(isDark = isDarkTheme)
        }

        val cacheKey = CacheKey(song.id, isDarkTheme)
        val (deferred, isLeader, epoch) = synchronized(lock) {
            val cached = colorCache[cacheKey]
            if (cached != null) {
                return@withContext cached
            }
            val existing = inFlightRequests[cacheKey]
            if (existing != null) {
                Triple(existing, false, currentCacheEpoch)
            } else {
                val newDeferred = CompletableDeferred<ArtworkColors>()
                inFlightRequests[cacheKey] = newDeferred
                Triple(newDeferred, true, currentCacheEpoch)
            }
        }

        if (!isLeader) {
            return@withContext deferred.await()
        }

        try {
            val colors = performExtraction(context, song, isDarkTheme, cacheKey, epoch)
            deferred.complete(colors)
            colors
        } catch (ce: CancellationException) {
            deferred.cancel(ce)
            throw ce
        } catch (t: Throwable) {
            deferred.completeExceptionally(t)
            throw t
        } finally {
            synchronized(lock) {
                if (inFlightRequests[cacheKey] === deferred) {
                    inFlightRequests.remove(cacheKey)
                }
            }
        }
    }

    private fun performExtraction(
        context: Context,
        song: Song,
        isDarkTheme: Boolean,
        cacheKey: CacheKey,
        epoch: Long
    ): ArtworkColors {
        val bitmap = extractBitmap(context, song)

        if (bitmap == null) {
            val fallback = defaultColors(isDark = isDarkTheme)
            synchronized(lock) {
                if (currentCacheEpoch == epoch) {
                    colorCache.put(cacheKey, fallback)
                }
            }
            return fallback
        }

        val (rawPrimaryRgb, darkRgb) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val wallpaperColors = try {
                android.app.WallpaperColors.fromBitmap(bitmap)
            } catch (_: Exception) {
                null
            }
            if (wallpaperColors != null) {
                val primaryRgb = wallpaperColors.primaryColor.toArgb()
                val secondaryRgb = wallpaperColors.secondaryColor?.toArgb() ?: primaryRgb
                Pair(primaryRgb, secondaryRgb)
            } else {
                extractRgbFromPalette(bitmap)
            }
        } else {
            extractRgbFromPalette(bitmap)
        }

        val colors = if (rawPrimaryRgb != null) {
            val safePrimaryColor = normalizeSafeAccentColor(rawPrimaryRgb, isDarkTheme)
            val primaryRgb = safePrimaryColor.toArgb()
            val effectiveDarkRgb = darkRgb ?: 0xFF121212.toInt()

            // Calculate AyraMusic clean pastel surface for light theme & rich surface for dark theme
            val lightSurface = calculateTintedSurface(primaryRgb, isDark = false)
            val darkSurface = calculateTintedSurface(primaryRgb, isDark = true)

            // Dynamic progress & control colors (Ayra style logic)
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(primaryRgb, hsl)
            val hue = hsl[0]

            // Active progress color with guaranteed saturation and readable lightness
            val activeProgressLightness = if (isDarkTheme) 0.68f else 0.38f
            val activeProgressRgb = ColorUtils.HSLToColor(floatArrayOf(hue, 0.85f, activeProgressLightness))
            val activeProgressColor = Color(activeProgressRgb)

            // Inactive progress track color with guaranteed visible contrast against light & dark canvases
            val inactiveProgressColor = if (isDarkTheme) {
                Color.White.copy(alpha = 0.24f)
            } else {
                Color.Black.copy(alpha = 0.16f)
            }

            // Control button color (Play / Pause pill/circle)
            val buttonLightness = if (isDarkTheme) 0.60f else 0.40f
            val buttonRgb = ColorUtils.HSLToColor(floatArrayOf(hue, 0.70f, buttonLightness))
            val controlButtonColor = Color(buttonRgb)

            val onControlButtonColor = if (buttonLightness > 0.50f && !isDarkTheme) Color.Black else Color.White

            // Control icons
            val iconLightness = if (isDarkTheme) 0.95f else 0.12f
            val iconRgb = ColorUtils.HSLToColor(floatArrayOf(hue, 0.20f, iconLightness))
            val controlIconColor = Color(iconRgb)

            ArtworkColors(
                primary = safePrimaryColor,
                secondary = safePrimaryColor,
                darkMuted = Color(effectiveDarkRgb),
                lightSurface = lightSurface,
                darkSurface = darkSurface,
                activeProgress = activeProgressColor,
                inactiveProgress = inactiveProgressColor,
                controlButton = controlButtonColor.copy(alpha = 0.50f),
                onControlButton = onControlButtonColor,
                controlIcon = controlIconColor
            )
        } else {
            defaultColors(isDark = isDarkTheme)
        }

        synchronized(lock) {
            if (currentCacheEpoch == epoch) {
                colorCache.put(cacheKey, colors)
            }
        }
        return colors
    }

    private fun extractRgbFromPalette(bitmap: Bitmap): Pair<Int?, Int?> {
        val palette = try {
            Palette.from(bitmap).generate()
        } catch (_: Exception) {
            null
        } ?: return Pair(null, null)

        val dominant = palette.dominantSwatch
        val vibrant = palette.vibrantSwatch ?: palette.lightVibrantSwatch ?: dominant
        val darkMuted = palette.darkMutedSwatch ?: palette.darkVibrantSwatch ?: dominant

        val primaryRgb = vibrant?.rgb ?: dominant?.rgb
        val darkRgb = darkMuted?.rgb ?: dominant?.rgb
        return Pair(primaryRgb, darkRgb)
    }

    private fun calculateTintedSurface(rgb: Int, isDark: Boolean): Color {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(rgb, hsl)
        val hue = hsl[0]
        val saturation = hsl[1].coerceIn(0.12f, 0.38f)
        val lightness = if (isDark) 0.11f else 0.94f
        val resultInt = ColorUtils.HSLToColor(floatArrayOf(hue, saturation, lightness))
        return Color(resultInt)
    }

    private fun extractBitmap(context: Context, song: Song): Bitmap? {
        // 1. Try ContentResolver.loadThumbnail (64x64 sudah sangat cukup untuk ekstraksi warna Palette)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val targetUri = if (song.artworkUri != Uri.EMPTY) song.artworkUri else song.contentUri
            if (targetUri != Uri.EMPTY) {
                try {
                    val bmp = context.contentResolver.loadThumbnail(targetUri, Size(64, 64), null)
                    if (bmp != null) return bmp
                } catch (_: Exception) {}
            }
        }

        // 2. Try ContentResolver via MediaStore Album Art URI
        if (song.albumId != -1L) {
            try {
                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    song.albumId
                )
                context.contentResolver.openInputStream(albumArtUri)?.use { stream ->
                    val opts = BitmapFactory.Options().apply {
                        inSampleSize = 4
                    }
                    val bmp = BitmapFactory.decodeStream(stream, null, opts)
                    if (bmp != null) return bmp
                }
            } catch (_: Exception) {}
        }

        // 3. Fallback ke TagLib / MediaMetadataRetriever jika dataPath tersedia
        val dp = song.dataPath
        if (!dp.isNullOrBlank()) {
            val file = File(dp)
            if (file.exists()) {
                val taglibArt = TaglibHelper.getArtworkBytes(file)
                if (taglibArt != null && taglibArt.isNotEmpty()) {
                    val opts = BitmapFactory.Options().apply {
                        inSampleSize = 4
                    }
                    val bmp = BitmapFactory.decodeByteArray(taglibArt, 0, taglibArt.size, opts)
                    if (bmp != null) return bmp
                }

                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(dp)
                    val picture = retriever.embeddedPicture
                    if (picture != null && picture.isNotEmpty()) {
                        val opts = BitmapFactory.Options().apply {
                            inSampleSize = 4
                        }
                        val bmp = BitmapFactory.decodeByteArray(picture, 0, picture.size, opts)
                        if (bmp != null) return bmp
                    }
                } catch (_: Exception) {
                } finally {
                    try { retriever.release() } catch (_: Exception) {}
                }
            }
        }

        return null
    }

    fun clearCache() {
        synchronized(lock) {
            currentCacheEpoch++
            colorCache.evictAll()
            inFlightRequests.clear()
        }
    }
}
