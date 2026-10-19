package com.example.ui.settings.appearance.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.common.ItemPosition
import com.example.ui.settings.components.groupItemShape

/**
 * SwitchAI Preference Item Card
 */
@Composable
fun SwitchAiPreferenceItem(
    position: ItemPosition,
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    testTag: String = "",
    control: @Composable () -> Unit
) {
    Card(
        shape = groupItemShape(position),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        ThemeOptionRow(
            title = title,
            subtitle = subtitle,
            icon = icon,
            isSelected = isSelected,
            onClick = onClick,
            testTag = testTag,
            control = control
        )
    }
}

/**
 * Left-aligned Section Header matching modern Android Settings
 */
@Composable
fun SectionHeaderLeft(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 6.dp, bottom = 4.dp)
    )
}

/**
 * Sleek Option Row inside a Grouped Card with icon, title, subtitle, and right control
 */
@Composable
fun ThemeOptionRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    testTag: String = "",
    control: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 15.sp
                ),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))
        control()
    }
}

/**
 * High-contrast switch colors for AMOLED, Dark, and Light canvases
 */
@Composable
fun highContrastSwitchColors(isDark: Boolean) = SwitchDefaults.colors(
    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
    checkedTrackColor = MaterialTheme.colorScheme.primary,
    checkedBorderColor = Color.Transparent,
    uncheckedThumbColor = if (isDark) Color(0xFFD4D4D8) else Color(0xFF71717A),
    uncheckedTrackColor = if (isDark) Color(0xFF27272A) else Color(0xFFE4E4E7),
    uncheckedBorderColor = if (isDark) Color(0xFF52525B) else Color(0xFFA1A1AA)
)

/**
 * Color Preview Capsule Pill
 */
@Composable
fun ColorPreviewCapsule(
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 44.dp, height = 24.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
    )
}
