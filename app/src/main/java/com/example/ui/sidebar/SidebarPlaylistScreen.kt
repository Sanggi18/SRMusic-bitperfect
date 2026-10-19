package com.example.ui.sidebar

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.Song
import com.example.ui.common.ArtworkPlaceholderType
import com.example.ui.common.components.ArtworkCard
import com.example.ui.library.core.SongAction
import com.example.ui.library.song.SongItem
import com.example.ui.sidebar.core.CollectionAction
import com.example.ui.sidebar.core.CollectionUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SidebarPlaylistChildScreen(
    uiState: CollectionUiState,
    allSongs: List<Song>,
    currentPlayingSongId: Long?,
    isPlaying: Boolean,
    onBackClick: () -> Unit,
    onAction: (CollectionAction) -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onSongAction: (SongAction, Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var expandedPlaylistIds by remember { mutableStateOf(emptySet<Long>()) }

    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    var editingPlaylistId by remember { mutableStateOf<Long?>(null) }
    var editingPlaylistName by remember { mutableStateOf("") }

    // Mode Delete dengan Checkbox langsung pada Card Playlist
    var isDeleteMode by remember { mutableStateOf(false) }
    var selectedForDeleteIds by remember { mutableStateOf(emptySet<Long>()) }

    // Popup Tambah Lagu ke Playlist (dengan Search)
    var addSongsTargetPlaylistId by remember { mutableStateOf<Long?>(null) }
    var addSongsSearchQuery by remember { mutableStateOf("") }
    var selectedSongIdsToAdd by remember { mutableStateOf(emptySet<Long>()) }

    var pendingExportSongs by remember { mutableStateOf<List<Song>>(emptyList()) }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            onAction(CollectionAction.ClearStatusMessage)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onAction(CollectionAction.ImportPlaylist(uri, allSongs))
    }

    val exportM3u8Launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.apple.mpegurl")
    ) { uri ->
        if (uri != null && pendingExportSongs.isNotEmpty()) {
            onAction(CollectionAction.ExportPlaylist(uri, pendingExportSongs))
        }
    }

    val exportM3uLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("audio/x-mpegurl")
    ) { uri ->
        if (uri != null && pendingExportSongs.isNotEmpty()) {
            onAction(CollectionAction.ExportPlaylist(uri, pendingExportSongs))
        }
    }

    BackHandler(enabled = true) {
        if (isDeleteMode) {
            isDeleteMode = false
            selectedForDeleteIds = emptySet()
        } else {
            onBackClick()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Playlists",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            navigationIcon = {
                IconButton(onClick = {
                    if (isDeleteMode) {
                        isDeleteMode = false
                        selectedForDeleteIds = emptySet()
                    } else {
                        onBackClick()
                    }
                }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        // Baris 3 Card Tombol di Atas dengan Padding Rapi
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                onClick = {
                    newPlaylistName = ""
                    showCreateDialog = true
                },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
                    Text(
                        text = "New Playlist",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                onClick = {
                    importLauncher.launch(
                        arrayOf("audio/x-mpegurl", "application/vnd.apple.mpegurl", "audio/mpegurl", "text/plain", "*/*")
                    )
                },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
                    Text(
                        text = "Import Playlist",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                onClick = {
                    if (uiState.playlists.isEmpty()) return@Surface
                    if (!isDeleteMode) {
                        isDeleteMode = true
                        selectedForDeleteIds = emptySet()
                    } else {
                        if (selectedForDeleteIds.isNotEmpty()) {
                            onAction(CollectionAction.DeleteMultiplePlaylists(selectedForDeleteIds.toList()))
                        }
                        isDeleteMode = false
                        selectedForDeleteIds = emptySet()
                    }
                },
                shape = RoundedCornerShape(14.dp),
                color = if (isDeleteMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                modifier = Modifier
                    .weight(0.85f)
                    .height(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
                    val deleteLabel = when {
                        !isDeleteMode -> "Delete"
                        selectedForDeleteIds.isEmpty() -> "Cancel"
                        else -> "Delete (${selectedForDeleteIds.size})"
                    }
                    Text(
                        text = deleteLabel,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isDeleteMode) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onErrorContainer,
                        maxLines = 1
                    )
                }
            }
        }

        // Daftar Card Playlist Accordion dengan jarak atas-bawah yang rapi
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(uiState.playlists, key = { _, pl -> "playlist_card_${pl.id}" }) { _, playlist ->
                val songsInPlaylist = uiState.playlistSongsMap[playlist.id].orEmpty()
                val isExpanded = expandedPlaylistIds.contains(playlist.id)
                val isCheckedForDelete = selectedForDeleteIds.contains(playlist.id)
                var showExportMenu by remember { mutableStateOf(false) }

                val arrowRotation by animateFloatAsState(
                    targetValue = if (isExpanded) 180f else 0f,
                    label = "playlist_arrow_${playlist.id}"
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    if (isDeleteMode) {
                                        selectedForDeleteIds = if (isCheckedForDelete) {
                                            selectedForDeleteIds - playlist.id
                                        } else {
                                            selectedForDeleteIds + playlist.id
                                        }
                                    } else {
                                        expandedPlaylistIds = if (isExpanded) {
                                            expandedPlaylistIds - playlist.id
                                        } else {
                                            expandedPlaylistIds + playlist.id
                                        }
                                    }
                                }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isDeleteMode) {
                                    Checkbox(
                                        checked = isCheckedForDelete,
                                        onCheckedChange = {
                                            selectedForDeleteIds = if (isCheckedForDelete) {
                                                selectedForDeleteIds - playlist.id
                                            } else {
                                                selectedForDeleteIds + playlist.id
                                            }
                                        },
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = playlist.name,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        ),
                                        color = if (isExpanded) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Text(
                                    text = "${songsInPlaylist.size} ${if (songsInPlaylist.size == 1) "Song" else "Songs"}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        editingPlaylistId = playlist.id
                                        editingPlaylistName = playlist.name
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = "Sunting Nama Playlist",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .rotate(arrowRotation)
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (songsInPlaylist.isEmpty()) {
                                    // Jika playlist baru & belum ada isinya: tombol Tambah Lagu untuk Playlist
                                    OutlinedButton(
                                        onClick = {
                                            addSongsTargetPlaylistId = playlist.id
                                            addSongsSearchQuery = ""
                                            selectedSongIdsToAdd = emptySet()
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(42.dp)
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Tambah Lagu ke Playlist", fontWeight = FontWeight.SemiBold)
                                    }
                                } else {
                                    // Baris Aksi: Play | Shuffle | Titik 3 (Export .m3u8 & .m3u + Tambah Lagu)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = { onSongClick(songsInPlaylist.first(), songsInPlaylist) },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(38.dp)
                                        ) {
                                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Play", style = MaterialTheme.typography.labelLarge)
                                        }

                                        FilledTonalButton(
                                            onClick = {
                                                val shuffled = songsInPlaylist.shuffled()
                                                onSongClick(shuffled.first(), shuffled)
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(38.dp)
                                        ) {
                                            Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Shuffle", style = MaterialTheme.typography.labelLarge)
                                        }

                                        Box {
                                            FilledTonalIconButton(
                                                onClick = { showExportMenu = true },
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.MoreVert,
                                                    contentDescription = "More Options",
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            DropdownMenu(
                                                expanded = showExportMenu,
                                                onDismissRequest = { showExportMenu = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Tambah Lagu") },
                                                    leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                                                    onClick = {
                                                        showExportMenu = false
                                                        addSongsTargetPlaylistId = playlist.id
                                                        addSongsSearchQuery = ""
                                                        selectedSongIdsToAdd = emptySet()
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Export .m3u8") },
                                                    leadingIcon = { Icon(Icons.Rounded.FileUpload, contentDescription = null) },
                                                    onClick = {
                                                        showExportMenu = false
                                                        pendingExportSongs = songsInPlaylist
                                                        exportM3u8Launcher.launch("${playlist.name}.m3u8")
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Export .m3u") },
                                                    leadingIcon = { Icon(Icons.Rounded.FileUpload, contentDescription = null) },
                                                    onClick = {
                                                        showExportMenu = false
                                                        pendingExportSongs = songsInPlaylist
                                                        exportM3uLauncher.launch("${playlist.name}.m3u")
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    songsInPlaylist.forEach { song ->
                                        SongItem(
                                            song = song,
                                            isCurrentSong = song.id == currentPlayingSongId,
                                            onSongClick = { onSongClick(song, songsInPlaylist) },
                                            onActionClick = onSongAction,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Popup Tambah Lagu ke Playlist (dengan Search)
    if (addSongsTargetPlaylistId != null) {
        val targetPlaylistId = addSongsTargetPlaylistId!!
        val existingIds = remember(targetPlaylistId, uiState.playlistSongsMap) {
            uiState.playlistSongsMap[targetPlaylistId].orEmpty().map { it.id }.toSet()
        }
        val filteredSongs = remember(allSongs, addSongsSearchQuery, existingIds) {
            val q = addSongsSearchQuery.trim().lowercase()
            allSongs.filter { song ->
                !existingIds.contains(song.id) &&
                    (q.isEmpty() || song.title.lowercase().contains(q) || song.artist.lowercase().contains(q))
            }
        }

        AlertDialog(
            onDismissRequest = { addSongsTargetPlaylistId = null },
            title = {
                Text("Tambah Lagu ke Playlist", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = addSongsSearchQuery,
                        onValueChange = { addSongsSearchQuery = it },
                        placeholder = { Text("Cari lagu atau artis...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredSongs, key = { it.id }) { song ->
                            val isSelected = selectedSongIdsToAdd.contains(song.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        selectedSongIdsToAdd = if (isSelected) {
                                            selectedSongIdsToAdd - song.id
                                        } else {
                                            selectedSongIdsToAdd + song.id
                                        }
                                    }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = {
                                        selectedSongIdsToAdd = if (isSelected) {
                                            selectedSongIdsToAdd - song.id
                                        } else {
                                            selectedSongIdsToAdd + song.id
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                ArtworkCard(
                                    artworkUri = song.artworkUri,
                                    contentUri = song.contentUri,
                                    title = song.title,
                                    size = 38.dp,
                                    cornerRadius = 8.dp,
                                    placeholderType = ArtworkPlaceholderType.MUSIC
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = song.artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = selectedSongIdsToAdd.isNotEmpty(),
                    onClick = {
                        onAction(CollectionAction.AddSongsToPlaylist(targetPlaylistId, selectedSongIdsToAdd.toList()))
                        addSongsTargetPlaylistId = null
                    }
                ) {
                    Text("Tambah (${selectedSongIdsToAdd.size})")
                }
            },
            dismissButton = {
                TextButton(onClick = { addSongsTargetPlaylistId = null }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Playlist", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    placeholder = { Text("Nama playlist") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = newPlaylistName.trim()
                        if (trimmed.isNotBlank()) {
                            onAction(CollectionAction.CreatePlaylist(trimmed))
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (editingPlaylistId != null) {
        val targetId = editingPlaylistId!!
        AlertDialog(
            onDismissRequest = { editingPlaylistId = null },
            title = { Text("Sunting Nama Playlist", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editingPlaylistName,
                    onValueChange = { editingPlaylistName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = editingPlaylistName.trim()
                        if (trimmed.isNotBlank()) {
                            onAction(CollectionAction.RenamePlaylist(targetId, trimmed))
                            editingPlaylistId = null
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingPlaylistId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
