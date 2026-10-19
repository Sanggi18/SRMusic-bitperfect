package com.example.ui.player.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.model.PlaybackInfo
import com.example.ui.common.theme.LocalSpacing
import com.example.ui.common.theme.springPress
import com.example.ui.player.core.PlayerAction
import com.example.ui.player.core.RepeatMode

@Composable
fun PlayerControlsRow(
    isShuffle: Boolean,
    repeatMode: RepeatMode,
    playbackInfo: PlaybackInfo,
    onAction: (PlayerAction) -> Unit,
    modifier: Modifier = Modifier,
    playButtonColor: Color? = null,
    onPlayButtonColor: Color? = null,
    iconTintColor: Color? = null,
    activeAccentColor: Color? = null
) {
    val spacing = LocalSpacing.current
    val basePlayBg = playButtonColor ?: MaterialTheme.colorScheme.primaryContainer
    val playBg = basePlayBg
    val playOnBg = onPlayButtonColor ?: MaterialTheme.colorScheme.onPrimaryContainer
    val iconTint = iconTintColor ?: MaterialTheme.colorScheme.onSurface
    val activeTint = (activeAccentColor ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.50f)
    val inactiveTint = iconTintColor?.copy(alpha = 0.45f) ?: MaterialTheme.colorScheme.onSurfaceVariant

    val prevInteractionSource = remember { MutableInteractionSource() }
    val nextInteractionSource = remember { MutableInteractionSource() }
    val shuffleInteractionSource = remember { MutableInteractionSource() }
    val repeatInteractionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = spacing.small),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Shuffle Button
        IconButton(
            onClick = { onAction(PlayerAction.ToggleShuffle) },
            interactionSource = shuffleInteractionSource,
            modifier = Modifier
                .size(spacing.minTouchTarget)
                .springPress(shuffleInteractionSource, pressedScale = 0.86f)
                .testTag("now_playing_shuffle_btn")
        ) {
            Icon(
                imageVector = Icons.Rounded.Shuffle,
                contentDescription = "Shuffle",
                tint = if (activeAccentColor != null && isShuffle) activeTint else if (isShuffle) iconTint else inactiveTint,
                modifier = Modifier.size(22.dp)
            )
        }

        // 2. Previous Track Button
        IconButton(
            onClick = { onAction(PlayerAction.Previous) },
            interactionSource = prevInteractionSource,
            modifier = Modifier
                .size(52.dp)
                .springPress(prevInteractionSource, pressedScale = 0.86f)
                .testTag("now_playing_previous")
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = "Previous track",
                tint = iconTint,
                modifier = Modifier.size(34.dp)
            )
        }

        // 3. Central Play/Pause Action Button (72.dp bulat sempurna, tanpa efek klik)
        Surface(
            shape = CircleShape,
            color = playBg,
            shadowElevation = 0.dp,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onAction(PlayerAction.PlayPause) }
                )
                .testTag("now_playing_play_pause")
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                if (playbackInfo.isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp,
                        color = playOnBg
                    )
                } else {
                    Icon(
                        imageVector = if (playbackInfo.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (playbackInfo.isPlaying) "Pause" else "Play",
                        tint = playOnBg,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
        }

        // 4. Next Track Button
        IconButton(
            onClick = { onAction(PlayerAction.Next) },
            enabled = playbackInfo.hasNext,
            interactionSource = nextInteractionSource,
            modifier = Modifier
                .size(52.dp)
                .springPress(nextInteractionSource, pressedScale = 0.86f)
                .testTag("now_playing_next")
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = "Next track",
                tint = if (playbackInfo.hasNext) iconTint else iconTint.copy(alpha = 0.38f),
                modifier = Modifier.size(34.dp)
            )
        }

        // 5. Repeat Button
        IconButton(
            onClick = { onAction(PlayerAction.ToggleRepeat) },
            interactionSource = repeatInteractionSource,
            modifier = Modifier
                .size(spacing.minTouchTarget)
                .springPress(repeatInteractionSource, pressedScale = 0.86f)
                .testTag("now_playing_repeat_btn")
        ) {
            val repeatIcon = when (repeatMode) {
                RepeatMode.ONE -> Icons.Rounded.RepeatOne
                else -> Icons.Rounded.Repeat
            }
            val isRepeatActive = repeatMode != RepeatMode.OFF
            Icon(
                imageVector = repeatIcon,
                contentDescription = "Repeat",
                tint = if (activeAccentColor != null && isRepeatActive) activeTint else if (isRepeatActive) iconTint else inactiveTint,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
