package com.example.ui.common.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PlayShuffleButtonRow(
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    playAllTestTag: String = "",
    shuffleTestTag: String = "",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FilledTonalButton(
            onClick = onPlayAll,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .weight(1f)
                .height(46.dp)
                .testTag(playAllTestTag)
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Play All", fontWeight = FontWeight.SemiBold)
        }
        FilledTonalButton(
            onClick = onShuffle,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .weight(1f)
                .height(46.dp)
                .testTag(shuffleTestTag)
        ) {
            Icon(
                imageVector = Icons.Rounded.Shuffle,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Shuffle", fontWeight = FontWeight.SemiBold)
        }
    }
}
