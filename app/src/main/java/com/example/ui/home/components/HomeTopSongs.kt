package com.example.ui.home.components

import com.example.ui.home.core.HomeContentGenerator
import com.example.ui.home.core.BillboardCategory
import com.example.ui.home.core.BillboardFilter
import com.example.ui.home.core.BillboardItemData
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.Song
import com.example.ui.common.ArtworkPlaceholderType
import com.example.ui.common.ArtworkShape
import com.example.ui.common.components.ArtworkCard

@Composable
fun TopSongsSlideCard(
    categoryLabel: String,
    title: String,
    subtitle: String,
    artworkSong: Song?,
    isArtist: Boolean,
    cardColor: Color,
    cardTextColor: Color,
    cardSubtextColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = cardColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = categoryLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = cardSubtextColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = cardTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = cardSubtextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            ArtworkCard(
                artworkUri = artworkSong?.artworkUri,
                contentUri = artworkSong?.contentUri,
                title = artworkSong?.title ?: title,
                size = 54.dp,
                shape = ArtworkShape.ROUNDED_SQUARE,
                cornerRadius = 12.dp,
                placeholderType = if (isArtist) ArtworkPlaceholderType.ARTIST else ArtworkPlaceholderType.ALBUM
            )
        }
    }
}

@Composable
fun BillboardRankScreen(
    category: BillboardCategory,
    allSongs: List<Song>,
    songPlayCounts: Map<Long, Int>,
    currentPlayingSongId: Long?,
    onBackClick: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onArtistClick: (String) -> Unit,
    onAlbumClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = true) {
        onBackClick()
    }

    var selectedCategory by remember { mutableStateOf(category) }
    val hasPlays = remember(songPlayCounts) { songPlayCounts.values.any { it > 0 } }
    var selectedFilter by remember { mutableStateOf(if (hasPlays) BillboardFilter.BY_PLAYING else BillboardFilter.BY_SONGS) }

    val categoryTitle = remember(selectedCategory) {
        when (selectedCategory) {
            BillboardCategory.TOP_ARTIST -> "Top Artists"
            BillboardCategory.TOP_ALBUM -> "Top Albums"
            BillboardCategory.TOP_SONGS -> "Top Songs"
        }
    }

    // Compute ranked list based on category & filter using generator
    val rankedItems: List<BillboardItemData> = remember(selectedCategory, selectedFilter, allSongs, songPlayCounts) {
        HomeContentGenerator.computeRankedBillboardItems(selectedCategory, selectedFilter, allSongs, songPlayCounts)
    }

    val top1 = rankedItems.getOrNull(0)
    val top2 = rankedItems.getOrNull(1)
    val top3 = rankedItems.getOrNull(2)

    val maxMetricVal = remember(rankedItems) {
        rankedItems.mapNotNull {
            val digits = it.detailMetric.filter { char -> char.isDigit() }
            digits.toFloatOrNull()
        }.maxOrNull() ?: 1f
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("billboard_rank_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("billboard_back_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Billboard Charts",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$categoryTitle • ${rankedItems.size} Entries",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Category Segmented Pill Switcher (Artists / Albums / Songs)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val categories = listOf(
                        BillboardCategory.TOP_ARTIST to "Artists",
                        BillboardCategory.TOP_ALBUM to "Albums",
                        BillboardCategory.TOP_SONGS to "Songs"
                    )
                    categories.forEach { (cat, label) ->
                        val isSelected = selectedCategory == cat
                        Surface(
                            onClick = { selectedCategory = cat },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Sub-filter Chips: By Songs vs By Playing
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    BillboardFilter.BY_PLAYING to "Most Played",
                    BillboardFilter.BY_SONGS to "Most Tracks"
                ).forEach { (filter, label) ->
                    val isSelected = selectedFilter == filter
                    Surface(
                        onClick = { selectedFilter = filter },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceContainer,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                // Podium Spotlight Section (Top 2 - Top 1 - Top 3)
                if (top1 != null) {
                    item(key = "billboard_podium_spotlight") {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "PODIUM SPOTLIGHT",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.2.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    // Rank 2 (Kiri)
                                    PodiumPillar(
                                        item = top2,
                                        rank = 2,
                                        rankColor = Color(0xFFC0C0C0),
                                        podiumHeight = 85.dp,
                                        artworkSize = 56.dp,
                                        onClick = {
                                            if (top2 != null) {
                                                when (selectedCategory) {
                                                    BillboardCategory.TOP_ARTIST -> onArtistClick(top2.title)
                                                    BillboardCategory.TOP_ALBUM -> onAlbumClick(top2.title)
                                                    BillboardCategory.TOP_SONGS -> top2.artworkSong?.let { onSongClick(it, allSongs) }
                                                }
                                            }
                                        }
                                    )

                                    // Rank 1 (Tengah - Paling Tinggi)
                                    PodiumPillar(
                                        item = top1,
                                        rank = 1,
                                        rankColor = Color(0xFFFFD700),
                                        podiumHeight = 115.dp,
                                        artworkSize = 72.dp,
                                        onClick = {
                                            when (selectedCategory) {
                                                BillboardCategory.TOP_ARTIST -> onArtistClick(top1.title)
                                                BillboardCategory.TOP_ALBUM -> onAlbumClick(top1.title)
                                                BillboardCategory.TOP_SONGS -> top1.artworkSong?.let { onSongClick(it, allSongs) }
                                            }
                                        }
                                    )

                                    // Rank 3 (Kanan)
                                    PodiumPillar(
                                        item = top3,
                                        rank = 3,
                                        rankColor = Color(0xFFCD7F32),
                                        podiumHeight = 70.dp,
                                        artworkSize = 52.dp,
                                        onClick = {
                                            if (top3 != null) {
                                                when (selectedCategory) {
                                                    BillboardCategory.TOP_ARTIST -> onArtistClick(top3.title)
                                                    BillboardCategory.TOP_ALBUM -> onAlbumClick(top3.title)
                                                    BillboardCategory.TOP_SONGS -> top3.artworkSong?.let { onSongClick(it, allSongs) }
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "rankings_header") {
                    Text(
                        text = "All Rankings",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                itemsIndexed(rankedItems, key = { index, item -> "${item.id}_$index" }) { index, item ->
                    val rankNumber = index + 1
                    val rankColor = when (rankNumber) {
                        1 -> Color(0xFFFFD700)
                        2 -> Color(0xFFC0C0C0)
                        3 -> Color(0xFFCD7F32)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    }

                    val metricVal = item.detailMetric.filter { it.isDigit() }.toFloatOrNull() ?: 1f
                    val progressRatio = (metricVal / maxMetricVal).coerceIn(0.08f, 1f)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                when (selectedCategory) {
                                    BillboardCategory.TOP_ARTIST -> onArtistClick(item.title)
                                    BillboardCategory.TOP_ALBUM -> onAlbumClick(item.title)
                                    BillboardCategory.TOP_SONGS -> {
                                        val song = item.artworkSong
                                        if (song != null) onSongClick(song, allSongs)
                                    }
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.width(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "#$rankNumber",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = if (rankNumber <= 3) FontWeight.Black else FontWeight.Bold,
                                    fontSize = if (rankNumber <= 3) 16.sp else 14.sp
                                ),
                                color = rankColor
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        ArtworkCard(
                            artworkUri = item.artworkSong?.artworkUri,
                            contentUri = item.artworkSong?.contentUri,
                            title = item.title,
                            size = 46.dp,
                            shape = ArtworkShape.ROUNDED_SQUARE,
                            cornerRadius = 10.dp
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.subtitle,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            // Popularity Progress Bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progressRatio)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (rankNumber <= 3) rankColor else MaterialTheme.colorScheme.primary)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = item.detailMetric,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PodiumPillar(
    item: BillboardItemData?,
    rank: Int,
    rankColor: Color,
    podiumHeight: Dp,
    artworkSize: Dp,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(96.dp)
            .clickable(enabled = item != null, onClick = onClick)
    ) {
        if (item != null) {
            Box(contentAlignment = Alignment.TopEnd) {
                ArtworkCard(
                    artworkUri = item.artworkSong?.artworkUri,
                    contentUri = item.artworkSong?.contentUri,
                    title = item.title,
                    size = artworkSize,
                    shape = ArtworkShape.ROUNDED_SQUARE,
                    cornerRadius = 14.dp
                )
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(rankColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$rank",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = Color(0xFF101018)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.detailMetric,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = rankColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            Spacer(modifier = Modifier.height(artworkSize + 28.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Undakan Balok Podium
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(podiumHeight),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "#$rank",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp
                    ),
                    color = rankColor.copy(alpha = 0.85f)
                )
            }
        }
    }
}
