package com.example.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun HomePagerCard(
    cardColor: Color,
    cardTextColor: Color,
    testTag: String,
    pageContent: @Composable (page: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val pageCount = 2
    val totalVirtualPages = 1000 * pageCount
    val initialVirtualPage = (totalVirtualPages / 2) - ((totalVirtualPages / 2) % pageCount)
    val pagerState = rememberPagerState(initialPage = initialVirtualPage) { totalVirtualPages }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        color = cardColor,
        tonalElevation = 1.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { virtualPage ->
                pageContent(virtualPage % pageCount)
            }
            // Dots indicator
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(pageCount) { index ->
                    val isSelected = (pagerState.currentPage % pageCount) == index
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) cardTextColor
                                else cardTextColor.copy(alpha = 0.4f)
                            )
                    )
                }
            }
        }
    }
}
