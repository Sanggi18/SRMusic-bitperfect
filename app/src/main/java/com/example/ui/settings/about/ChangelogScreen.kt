package com.example.ui.settings.about

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class ChangelogRelease(
    val version: String,
    val date: String,
    val badge: String,
    val isLatest: Boolean,
    val sections: List<ChangelogSection>
)

private data class ChangelogSection(
    val title: String,
    val icon: ImageVector,
    val items: List<String>
)

@Composable
fun ChangelogScreen(modifier: Modifier = Modifier) {
    val releases = listOf(
        ChangelogRelease(
            version = "1.1.0-beta4",
            date = "Oktober 2026",
            badge = "Terbaru",
            isLatest = true,
            sections = listOf(
                ChangelogSection(
                    title = "Pembaruan Versi",
                    icon = Icons.Rounded.NewReleases,
                    items = listOf(
                        "Pembaruan rilis ke versi 1.1.0-beta4.",
                        "Optimalisasi kestabilan pemutaran audio dan performa antarmuka."
                    )
                ),
                ChangelogSection(
                    title = "Pembaruan Desain & UI",
                    icon = Icons.Rounded.CheckCircle,
                    items = listOf(
                        "Desain baru halaman Tentang Aplikasi bergaya Material 3 Expressive minimalis.",
                        "Ikon aplikasi baru dengan bentuk squircle elegan (#25145D) dan glyph proporsional.",
                        "Gaya grouped cards bertepi halus pada menu pengaturan dan dialog informasi."
                    )
                )
            )
        ),
        ChangelogRelease(
            version = "1.1.0-beta3",
            date = "Oktober 2026",
            badge = "Sebelumnya",
            isLatest = false,
            sections = listOf(
                ChangelogSection(
                    title = "Pembaruan Desain & UI",
                    icon = Icons.Rounded.NewReleases,
                    items = listOf(
                        "Desain baru halaman Tentang Aplikasi bergaya Material 3 Expressive minimalis.",
                        "Ikon aplikasi baru dengan bentuk squircle elegan (#25145D) dan glyph proporsional.",
                        "Gaya grouped cards bertepi halus pada menu pengaturan dan dialog informasi."
                    )
                ),
                ChangelogSection(
                    title = "Peningkatan & Pembersihan",
                    icon = Icons.Rounded.CheckCircle,
                    items = listOf(
                        "Optimalisasi performa pemutaran dan deduplikasi logika progress seeker.",
                        "Penyelarasan palet warna dinamis dan perbaikan transisi navigasi."
                    )
                )
            )
        ),
        ChangelogRelease(
            version = "1.0.0",
            date = "Rilis Perdana",
            badge = "Stabil",
            isLatest = false,
            sections = listOf(
                ChangelogSection(
                    title = "Fitur Utama",
                    icon = Icons.Rounded.NewReleases,
                    items = listOf(
                        "Arsitektur Pengaturan Terkategori: Tampilan, Audio & Pemutaran, Library, dan Aplikasi.",
                        "Tema Material 3 & Dynamic Color: Mode Sistem, Gelap, Terang, AMOLED, dan 5 preset custom.",
                        "Tab Navigasi Fleksibel: Kustomisasi urutan dan visibilitas tab navigasi bawah.",
                        "Navigasi Folder Hirarkis: Penjelajahan folder audio langsung dari penyimpanan perangkat.",
                        "Equalizer 10-Band Presisi: Pengaturan fader vertikal, mode Bit Perfect, dan routing output audio.",
                        "Now Playing Layar Penuh: Dukungan lirik tersinkronisasi dan visualizer audio."
                    )
                )
            )
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Langsung tampil kartu pertama tanpa header/keterangan lagi
        releases.forEach { release ->
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Header Card (Versi + Badge + Tanggal)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "v${release.version}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = if (release.isLatest)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                            ) {
                                Text(
                                    text = release.badge,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (release.isLatest)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(
                            text = release.date,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Sections
                    release.sections.forEachIndexed { index, section ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 14.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                thickness = 0.5.dp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = section.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = section.title,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            section.items.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "•",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text(
                                        text = item,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
