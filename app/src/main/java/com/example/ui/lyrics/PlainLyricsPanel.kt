package com.example.ui.lyrics

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PlainLyricsPanel(
    text: String,
    modifier: Modifier = Modifier
) {
    val lines = remember(text) {
        text.lines().map { it.trimEnd() }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp, start = 24.dp, end = 24.dp)
    ) {
        itemsIndexed(
            items = lines,
            key = { index, _ -> "plain_line_$index" },
            contentType = { _, _ -> "plain_lyric_line" }
        ) { _, line ->
            Text(
                text = if (line.isBlank()) " " else line,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 20.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.90f),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
