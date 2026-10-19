package com.example.ui.navigation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Canonical Navigation Tab item enum representing available and enabled
 * bottom navigation destinations in the application.
 */
enum class TabItem(val id: String, val label: String) {
    HOME("home", "Home"),
    SONG("song", "Song"),
    FOLDER("folder", "Folder"),
    ARTIST("artist", "Artist"),
    ALBUM("album", "Album");

    val title: String get() = label
}

fun getTabIcon(tab: TabItem): ImageVector {
    return when (tab) {
        TabItem.HOME -> Icons.Rounded.Home
        TabItem.SONG -> Icons.Rounded.MusicNote
        TabItem.FOLDER -> Icons.Rounded.Folder
        TabItem.ARTIST -> Icons.Rounded.Person
        TabItem.ALBUM -> Icons.Rounded.Album
    }
}
