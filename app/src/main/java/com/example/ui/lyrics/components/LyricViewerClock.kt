package com.example.ui.lyrics.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import com.example.lyrics.LyricLine

@Composable
internal fun rememberLyricClock(
    lyrics: List<LyricLine>,
    positionMs: Long,
    isPlaying: Boolean,
): MutableLongState {
    val lyricClock = remember(lyrics) {
        mutableLongStateOf(positionMs.coerceAtLeast(0L))
    }

    var clockAnchorPositionMs by remember(lyrics) {
        mutableLongStateOf(positionMs.coerceAtLeast(0L))
    }

    var clockAnchorFrameTimeNanos by remember(lyrics) {
        mutableLongStateOf(0L)
    }

    var hasClockAnchor by remember(lyrics) {
        mutableStateOf(false)
    }

    LaunchedEffect(lyrics, positionMs, isPlaying) {
        val frameTimeNanos = withFrameNanos { it }

        clockAnchorPositionMs = positionMs.coerceAtLeast(0L)
        clockAnchorFrameTimeNanos = frameTimeNanos
        hasClockAnchor = true

        if (!isPlaying) {
            lyricClock.longValue = positionMs.coerceAtLeast(0L)
        }
    }

    /*
     * Drive word reveal & floating words dari frame clock, bukan dari poll player.
     */
    LaunchedEffect(lyrics, isPlaying) {
        if (!isPlaying) {
            lyricClock.longValue = positionMs.coerceAtLeast(0L)
            return@LaunchedEffect
        }

        while (true) {
            withFrameNanos { frameTimeNanos ->
                if (!hasClockAnchor) {
                    lyricClock.longValue = positionMs.coerceAtLeast(0L)
                } else {
                    val elapsedNanos = (frameTimeNanos - clockAnchorFrameTimeNanos)
                        .coerceAtLeast(0L)
                    val elapsedMs = elapsedNanos / 1_000_000L

                    lyricClock.longValue = (clockAnchorPositionMs + elapsedMs)
                        .coerceAtLeast(0L)
                }
            }
        }
    }

    return lyricClock
}
