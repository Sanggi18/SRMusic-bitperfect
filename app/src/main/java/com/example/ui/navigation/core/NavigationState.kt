package com.example.ui.navigation.core
import com.example.ui.navigation.components.*
import com.example.ui.common.components.*

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.core.model.Song
import com.example.ui.home.core.HomeChildScreen

sealed interface NavigationAction {
    data class SelectTab(val tab: TabItem, val animate: Boolean = true) : NavigationAction
    data class OpenArtist(val artistName: String, val sourceTab: TabItem? = null) : NavigationAction
    data class OpenAlbum(val albumName: String, val sourceTab: TabItem? = null) : NavigationAction
    data class OpenFolder(val folderPath: String) : NavigationAction
    data class SetSettingsOpen(val isOpen: Boolean) : NavigationAction
    data class SetEqualizerOpen(val isOpen: Boolean) : NavigationAction
    data class SetLyricsOpen(val isOpen: Boolean) : NavigationAction
    data class SetHomeChildScreen(val child: HomeChildScreen) : NavigationAction
    data class SetSidebarChildScreen(val child: com.example.ui.sidebar.core.SidebarChildScreen) : NavigationAction
    data class ShowAddToPlaylist(val song: Song?) : NavigationAction
    data object DismissAddToPlaylist : NavigationAction
    data object ClearDetailSourceTab : NavigationAction
}

@Stable
class NavigationState(
    initialTab: TabItem = TabItem.HOME
) {
    var selectedTab by mutableStateOf(initialTab)
        private set

    val tabBackStack = mutableStateListOf<TabItem>()

    var detailSourceTab by mutableStateOf<TabItem?>(null)
        private set

    var isSettingsOpen by mutableStateOf(false)
        private set

    var isEqualizerOpen by mutableStateOf(false)
        private set

    var isLyricsOpen by mutableStateOf(false)
        private set

    var activeHomeChildScreen by mutableStateOf(HomeChildScreen.NONE)
        private set

    var activeSidebarChildScreen by mutableStateOf(com.example.ui.sidebar.core.SidebarChildScreen.NONE)

    var songForAddToPlaylist by mutableStateOf<Song?>(null)
        private set

    var isBackNavigating by mutableStateOf(false)

    fun onAction(action: NavigationAction) {
        when (action) {
            is NavigationAction.SelectTab -> navigateToTab(action.tab)
            is NavigationAction.OpenArtist -> {
                detailSourceTab = action.sourceTab ?: selectedTab
                navigateToTab(TabItem.ARTIST)
            }
            is NavigationAction.OpenAlbum -> {
                detailSourceTab = action.sourceTab ?: selectedTab
                navigateToTab(TabItem.ALBUM)
            }
            is NavigationAction.OpenFolder -> {
                detailSourceTab = selectedTab
                navigateToTab(TabItem.FOLDER)
            }
            is NavigationAction.SetSettingsOpen -> isSettingsOpen = action.isOpen
            is NavigationAction.SetEqualizerOpen -> isEqualizerOpen = action.isOpen
            is NavigationAction.SetLyricsOpen -> isLyricsOpen = action.isOpen
            is NavigationAction.SetHomeChildScreen -> activeHomeChildScreen = action.child
            is NavigationAction.SetSidebarChildScreen -> activeSidebarChildScreen = action.child
            is NavigationAction.ShowAddToPlaylist -> songForAddToPlaylist = action.song
            NavigationAction.DismissAddToPlaylist -> songForAddToPlaylist = null
            NavigationAction.ClearDetailSourceTab -> detailSourceTab = null
        }
    }

    private fun navigateToTab(tab: TabItem) {
        if (selectedTab != tab) {
            activeHomeChildScreen = HomeChildScreen.NONE
            if (!isBackNavigating) {
                if (tabBackStack.isEmpty() || tabBackStack.last() != selectedTab) {
                    tabBackStack.add(selectedTab)
                }
            }
            selectedTab = tab
            if (tab == TabItem.HOME) {
                tabBackStack.clear()
            }
        }
    }

    fun popTabBackStack(enabledTabs: List<TabItem>, firstTab: TabItem): TabItem? {
        while (tabBackStack.isNotEmpty() && (!enabledTabs.contains(tabBackStack.last()) || tabBackStack.last() == selectedTab)) {
            tabBackStack.removeAt(tabBackStack.lastIndex)
        }
        val targetTab = if (tabBackStack.isNotEmpty()) {
            tabBackStack.removeAt(tabBackStack.lastIndex)
        } else if (selectedTab != firstTab && selectedTab != TabItem.HOME) {
            firstTab
        } else {
            null
        }
        if (targetTab != null && targetTab != selectedTab) {
            isBackNavigating = true
            selectedTab = targetTab
            if (targetTab == TabItem.HOME || targetTab == firstTab) {
                tabBackStack.clear()
            }
            return targetTab
        }
        return null
    }
}

@Composable
fun rememberNavigationState(initialTab: TabItem = TabItem.HOME): NavigationState {
    return remember { NavigationState(initialTab) }
}

@Composable
fun BackNavigationHandler(
    isDrawerOpen: Boolean = false,
    onCloseDrawer: () -> Unit = {},
    isSettingsOpen: Boolean,
    onCloseSettings: () -> Unit,
    isEqualizerOpen: Boolean,
    onCloseEqualizer: () -> Unit,
    isLyricsOpen: Boolean = false,
    onCloseLyrics: () -> Unit = {},
    hasDetailOpen: Boolean,
    onBackFromDetail: () -> Unit,
    activeHomeChildScreen: HomeChildScreen = HomeChildScreen.NONE,
    onCloseHomeChildScreen: () -> Unit = {},
    activeSidebarChildScreen: com.example.ui.sidebar.core.SidebarChildScreen = com.example.ui.sidebar.core.SidebarChildScreen.NONE,
    onCloseSidebarChildScreen: () -> Unit = {},
    canGoBackTab: Boolean,
    onBackTab: () -> Unit
) {
    val context = LocalContext.current
    var lastBackPressTime by remember { mutableStateOf(0L) }

    BackHandler(enabled = true) {
        when {
            isDrawerOpen -> onCloseDrawer()
            isSettingsOpen -> onCloseSettings()
            isLyricsOpen -> onCloseLyrics()
            isEqualizerOpen -> onCloseEqualizer()
            activeSidebarChildScreen != com.example.ui.sidebar.core.SidebarChildScreen.NONE -> onCloseSidebarChildScreen()
            hasDetailOpen -> onBackFromDetail()
            activeHomeChildScreen != HomeChildScreen.NONE -> onCloseHomeChildScreen()
            canGoBackTab -> onBackTab()
            else -> {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastBackPressTime < 2000L) {
                    (context as? Activity)?.finish()
                } else {
                    lastBackPressTime = currentTime
                    Toast.makeText(context, "Tekan sekali lagi untuk keluar", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
