package com.example.ui.settings.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R
import com.example.ui.settings.components.OpenLinkConfirmDialog
import com.example.ui.settings.SettingsDestination

private const val DEVELOPER_URL = "https://t.me/Sanggi"
private const val SUPPORT_GROUP_URL = "https://t.me/SRMusicDiscus"

@Composable
fun AppSettingsScreen(
    onNavigate: (SettingsDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    var pendingUrl by remember { mutableStateOf<String?>(null) }

    fun openUrl(url: String) {
        pendingUrl = url
    }

    pendingUrl?.let { url ->
        OpenLinkConfirmDialog(
            url = url,
            onConfirm = {
                pendingUrl = null
                try {
                    uriHandler.openUri(url)
                } catch (_: Exception) {}
            },
            onDismiss = {
                pendingUrl = null
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                com.example.ui.common.components.SRMusicGlyphIcon(
                    modifier = Modifier.size(64.dp)
                )

                Spacer(Modifier.width(16.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "SRMusic",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Version ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        SectionHeader(title = "APLIKASI & PENGEMBANG")
        Spacer(Modifier.height(8.dp))
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
        ) {
            Column {
                GroupedSettingItem(
                    icon = Icons.Rounded.Person,
                    title = "Developer",
                    subtitle = "Sanggi",
                    onClick = { openUrl(DEVELOPER_URL) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 68.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 0.5.dp
                )
                GroupedSettingItem(
                    icon = Icons.Rounded.History,
                    title = "Changelog",
                    subtitle = "Riwayat pembaruan aplikasi",
                    onClick = { onNavigate(SettingsDestination.Changelog) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 68.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 0.5.dp
                )
                GroupedSettingItem(
                    icon = Icons.Rounded.Code,
                    title = "Credits",
                    subtitle = "Lisensi dan Open Source",
                    onClick = { onNavigate(SettingsDestination.Credits) }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        SectionHeader(title = "KOMUNITAS & DUKUNGAN")
        Spacer(Modifier.height(8.dp))
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
        ) {
            Column {
                GroupedSettingItem(
                    icon = Icons.Rounded.Group,
                    title = "Dukungan",
                    subtitle = "Grup Support (Tanya jawab & laporan)",
                    onClick = { openUrl(SUPPORT_GROUP_URL) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 68.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 0.5.dp
                )
                GroupedSettingItem(
                    icon = Icons.Rounded.Favorite,
                    title = "Donate",
                    subtitle = "Support Developer",
                    onClick = null
                )
            }
        }

        Spacer(Modifier.height(36.dp))

        Text(
            text = "Terima kasih telah menggunakan SRMusic",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp)
    )
}

@Composable
private fun GroupedSettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (onClick != null) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
