package com.example.ui.library.folder
import com.example.ui.library.core.*
import com.example.ui.library.components.*
import com.example.ui.common.components.*
import com.example.ui.library.ArtworkRef

data class FolderUiModel(
    val path: String,
    val name: String,
    val songCount: Int,
    val subfolderCount: Int = 0,
    val artwork: ArtworkRef = ArtworkRef()
)

enum class FolderAction {
    PLAY,
    SHUFFLE,
    PLAY_NEXT,
    ADD_TO_QUEUE
}
