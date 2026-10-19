package com.example.ui.player.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.foundation.interaction.MutableInteractionSource
import com.example.ui.common.theme.springPress

@Composable
fun PlayerActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String,
    isActive: Boolean = false,
    enabled: Boolean = true,
    iconTint: Color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .alpha(if (enabled) 1f else 0.38f)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
            ),
            color = iconTint
        )
    }
}

@Composable
fun PlayerBottomActions(
    onOpenQueue: () -> Unit,
    onNavigateToEqualizer: () -> Unit,
    onShowInfo: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenLyrics: (() -> Unit)? = null,
    iconTintColor: Color? = null,
    activeAccentColor: Color? = null
) {
    var showOverflowMenu by remember { mutableStateOf(false) }
    val defaultIconTint = iconTintColor ?: MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onOpenLyrics != null) {
            PlayerActionButton(
                icon = Icons.Rounded.Lyrics,
                label = "Lyrics",
                onClick = onOpenLyrics,
                testTag = "now_playing_action_lyrics",
                iconTint = defaultIconTint
            )
        }
        PlayerActionButton(
            icon = Icons.Rounded.QueueMusic,
            label = "Queue",
            onClick = onOpenQueue,
            testTag = "now_playing_action_queue",
            iconTint = defaultIconTint
        )
        PlayerActionButton(
            icon = Icons.Rounded.Equalizer,
            label = "Equalizer",
            onClick = onNavigateToEqualizer,
            testTag = "now_playing_action_equalizer",
            iconTint = defaultIconTint
        )
        Box {
            PlayerActionButton(
                icon = Icons.Rounded.MoreVert,
                label = "More",
                onClick = { showOverflowMenu = true },
                testTag = "now_playing_action_overflow",
                iconTint = defaultIconTint
            )
            DropdownMenu(
                expanded = showOverflowMenu,
                onDismissRequest = { showOverflowMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Song Info") },
                    leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null) },
                    onClick = {
                        showOverflowMenu = false
                        onShowInfo()
                    },
                    modifier = Modifier.testTag("now_playing_menu_info")
                )
            }
        }
    }
}

@Composable
fun PlayerAudioPill(
    audioPillText: String,
    modifier: Modifier = Modifier,
    pillBgColor: Color? = null,
    pillTextColor: Color? = null,
    onClick: (() -> Unit)? = null
) {
    val bg = pillBgColor ?: MaterialTheme.colorScheme.primaryContainer
    val textCol = pillTextColor ?: MaterialTheme.colorScheme.onPrimaryContainer

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = bg,
            modifier = Modifier.then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ) else Modifier
            )
        ) {
            Text(
                text = audioPillText,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = textCol
                ),
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 5.dp)
            )
        }
    }
}
