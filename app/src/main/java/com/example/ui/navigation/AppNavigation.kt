package com.example.ui.navigation

import com.example.ui.navigation.components.AddToPlaylistOverlay
import com.example.ui.navigation.components.DrawerHost
import com.example.ui.navigation.components.SettingsOverlay
import com.example.ui.navigation.components.TabItem
import com.example.ui.navigation.core.BackNavigationHandler
import com.example.ui.navigation.core.NavigationAction
import com.example.ui.navigation.core.NavigationState
import com.example.ui.navigation.core.rememberNavigationState
import com.example.ui.common.theme.ExpressiveMotion
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.common.LocalMiniPlayerVisible
import com.example.ui.sidebar.core.CollectionAction
import com.example.ui.sidebar.core.CollectionUiState
import com.example.ui.equalizer.core.EqualizerAction
import com.example.ui.equalizer.EqualizerScreen
import com.example.ui.equalizer.core.EqualizerUiState
import com.example.ui.home.core.HomeAction
import com.example.ui.home.core.HomeChildScreen
import com.example.ui.home.core.HomeUiState
import com.example.ui.library.core.LibraryAction
import com.example.ui.library.core.LibraryUiState
import com.example.ui.library.core.SongAction
import com.example.ui.player.core.PlayerAction
import com.example.ui.player.PlayerHost
import com.example.ui.player.core.PlayerUiState
import com.example.ui.settings.core.SettingsAction
import com.example.ui.settings.core.SettingsUiState
import com.example.ui.settings.core.toTabItems
import com.example.ui.settings.SettingsDestination
import com.example.core.model.Song
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    settingsUiState: SettingsUiState,
    onSettingsAction: (SettingsAction) -> Unit,
    equalizerUiState: EqualizerUiState,
    onEqualizerAction: (EqualizerAction) -> Unit,
    homeUiState: HomeUiState,
    onHomeAction: (HomeAction) -> Unit,
    libraryUiState: LibraryUiState,
    onLibraryAction: (LibraryAction) -> Unit,
    collectionUiState: CollectionUiState,
    onCollectionAction: (CollectionAction) -> Unit,
    playerUiState: PlayerUiState,
    onPlayerAction: (PlayerAction) -> Unit,
    playbackProgressFlow: kotlinx.coroutines.flow.StateFlow<com.example.core.model.PlaybackProgress>? = null,
    onRequestPermission: () -> Unit,
    onRefreshMedia: () -> Unit,
    navigationState: NavigationState = rememberNavigationState(),
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var settingsInitialDestination: SettingsDestination by remember { mutableStateOf(SettingsDestination.Main) }

    val enabledTabs = remember(settingsUiState.enabledTabIds) {
        settingsUiState.enabledTabIds.toTabItems()
    }
    val totalSizeBytes = remember(homeUiState.songs) {
        homeUiState.songs.sumOf { it.size ?: 0L }
    }
    val effectivePlayerUiState = remember(playerUiState, settingsUiState.isArtworkAccent) {
        playerUiState.copy(isArtworkAccent = settingsUiState.isArtworkAccent)
    }
    val firstTab = enabledTabs.firstOrNull() ?: TabItem.HOME

    val artistListState = rememberLazyListState()
    val artistGridState = rememberLazyGridState()
    val albumListState = rememberLazyListState()
    val albumGridState = rememberLazyGridState()

    val pagerState = rememberPagerState(initialPage = 0) { enabledTabs.size }
    var programmaticTargetPage by remember { mutableStateOf<Int?>(null) }

    // Synchronize pager settled state to navigationState
    LaunchedEffect(pagerState, enabledTabs) {
        snapshotFlow { pagerState.settledPage }
            .collect { settledPage ->
                val targetTab = enabledTabs.getOrNull(settledPage)
                if (targetTab != null && targetTab != navigationState.selectedTab) {
                    navigationState.onAction(NavigationAction.SelectTab(targetTab))
                    onLibraryAction(LibraryAction.ClearDetail)
                    navigationState.onAction(NavigationAction.ClearDetailSourceTab)
                }
                navigationState.isBackNavigating = false
            }
    }

    // Keep tabs valid when enabledTabs changes in settings
    LaunchedEffect(enabledTabs) {
        navigationState.tabBackStack.retainAll(enabledTabs.toSet())
        val currentIndex = enabledTabs.indexOf(navigationState.selectedTab)
        if (currentIndex != -1) {
            if (pagerState.currentPage != currentIndex) {
                pagerState.scrollToPage(currentIndex)
            }
        } else if (enabledTabs.isNotEmpty()) {
            navigationState.onAction(NavigationAction.SelectTab(enabledTabs[0]))
            pagerState.scrollToPage(0)
        }
    }

    fun selectTab(tab: TabItem, animate: Boolean = true) {
        val targetIndex = enabledTabs.indexOf(tab)
        if (targetIndex != -1 && targetIndex != pagerState.currentPage) {
            if (libraryUiState.selectedArtistName != null || libraryUiState.selectedAlbumName != null) {
                onLibraryAction(LibraryAction.ClearDetail)
            }
            if (navigationState.detailSourceTab != null) {
                navigationState.onAction(NavigationAction.ClearDetailSourceTab)
            }
            navigationState.onAction(NavigationAction.SelectTab(tab))
            programmaticTargetPage = targetIndex
            val pageDiff = kotlin.math.abs(targetIndex - pagerState.currentPage)
            coroutineScope.launch {
                if (animate && pageDiff <= 1) {
                    pagerState.animateScrollToPage(
                        page = targetIndex,
                        animationSpec = tween(
                            durationMillis = 250,
                            easing = androidx.compose.animation.core.FastOutSlowInEasing
                        )
                    )
                } else {
                    pagerState.scrollToPage(targetIndex)
                }
                programmaticTargetPage = null
            }
        }
    }

    val onSongActionDispatcher: (SongAction, Song) -> Unit = { action, song ->
        when (action) {
            SongAction.PLAY -> {
                onPlayerAction(PlayerAction.PlaySong(song, listOf(song)))
            }
            SongAction.PLAY_NEXT -> {
                onPlayerAction(PlayerAction.PlayNext(song))
            }
            SongAction.ADD_TO_QUEUE -> {
                onPlayerAction(PlayerAction.AddToQueue(song))
            }
            SongAction.TOGGLE_FAVORITE,
            SongAction.REMOVE_FROM_FAVORITES -> {
                onCollectionAction(CollectionAction.ToggleFavorite(song.id))
            }
            SongAction.ADD_TO_PLAYLIST -> {
                navigationState.onAction(NavigationAction.ShowAddToPlaylist(song))
            }
            SongAction.GO_TO_ARTIST -> {
                song.artist.takeIf { it.isNotBlank() }?.let { artistName ->
                    navigationState.onAction(NavigationAction.OpenArtist(artistName, navigationState.selectedTab))
                    onLibraryAction(LibraryAction.SelectAlbum(null))
                    onLibraryAction(LibraryAction.SelectArtist(artistName))
                    val targetIndex = enabledTabs.indexOf(TabItem.ARTIST)
                    if (targetIndex != -1) {
                        coroutineScope.launch { pagerState.scrollToPage(targetIndex) }
                    }
                }
            }
            SongAction.GO_TO_ALBUM -> {
                song.album.takeIf { it.isNotBlank() }?.let { albumName ->
                    navigationState.onAction(NavigationAction.OpenAlbum(albumName, navigationState.selectedTab))
                    onLibraryAction(LibraryAction.SelectArtist(null))
                    onLibraryAction(LibraryAction.SelectAlbum(albumName))
                    val targetIndex = enabledTabs.indexOf(TabItem.ALBUM)
                    if (targetIndex != -1) {
                        coroutineScope.launch { pagerState.scrollToPage(targetIndex) }
                    }
                }
            }
            SongAction.GO_TO_FOLDER -> {
                navigationState.onAction(NavigationAction.OpenFolder(song.relativePath ?: ""))
                onLibraryAction(LibraryAction.ClearDetail)
                onLibraryAction(LibraryAction.HandleSongAction(SongAction.GO_TO_FOLDER, song))
                val targetIndex = enabledTabs.indexOf(TabItem.FOLDER)
                if (targetIndex != -1) {
                    coroutineScope.launch { pagerState.scrollToPage(targetIndex) }
                }
            }
            SongAction.SONG_INFO,
            SongAction.DELETE -> {}
        }
    }

    val hasDetailOpen = libraryUiState.selectedArtistName != null || libraryUiState.selectedAlbumName != null
    val isHomeChildOpen = navigationState.activeHomeChildScreen != HomeChildScreen.NONE
    val isSidebarChildOpen = navigationState.activeSidebarChildScreen != com.example.ui.sidebar.core.SidebarChildScreen.NONE
    val isBottomNavVisible = !hasDetailOpen && !isHomeChildOpen && !isSidebarChildOpen && !navigationState.isEqualizerOpen && !navigationState.isLyricsOpen && !navigationState.isSettingsOpen
    val canGoBackTab = navigationState.tabBackStack.isNotEmpty() || (navigationState.selectedTab != TabItem.HOME && navigationState.selectedTab != firstTab)

    // Canonical Back Handling
    BackNavigationHandler(
        isDrawerOpen = false,
        onCloseDrawer = { coroutineScope.launch { drawerState.close() } },
        isSettingsOpen = navigationState.isSettingsOpen,
        onCloseSettings = { navigationState.onAction(NavigationAction.SetSettingsOpen(false)) },
        isEqualizerOpen = navigationState.isEqualizerOpen,
        onCloseEqualizer = { navigationState.onAction(NavigationAction.SetEqualizerOpen(false)) },
        isLyricsOpen = navigationState.isLyricsOpen,
        onCloseLyrics = { navigationState.onAction(NavigationAction.SetLyricsOpen(false)) },
        hasDetailOpen = hasDetailOpen,
        onBackFromDetail = {
            onLibraryAction(LibraryAction.ClearDetail)
            navigationState.detailSourceTab?.let { origin ->
                navigationState.onAction(NavigationAction.ClearDetailSourceTab)
                selectTab(origin, animate = false)
            }
        },
        activeHomeChildScreen = navigationState.activeHomeChildScreen,
        onCloseHomeChildScreen = { navigationState.onAction(NavigationAction.SetHomeChildScreen(HomeChildScreen.NONE)) },
        activeSidebarChildScreen = navigationState.activeSidebarChildScreen,
        onCloseSidebarChildScreen = { navigationState.onAction(NavigationAction.SetSidebarChildScreen(com.example.ui.sidebar.core.SidebarChildScreen.NONE)) },
        canGoBackTab = canGoBackTab,
        onBackTab = {
            val targetTab = navigationState.popTabBackStack(enabledTabs, firstTab)
            if (targetTab != null) {
                onLibraryAction(LibraryAction.ClearDetail)
                navigationState.onAction(NavigationAction.ClearDetailSourceTab)
                val targetIndex = enabledTabs.indexOf(targetTab)
                if (targetIndex != -1) {
                    coroutineScope.launch { pagerState.animateScrollToPage(targetIndex) }
                }
            }
        }
    )

    val isMiniPlayerVisible = playerUiState.currentSong != null &&
        !playerUiState.isDismissed &&
        !navigationState.isEqualizerOpen &&
        !navigationState.isLyricsOpen &&
        !navigationState.isSettingsOpen

    val drawerGesturesEnabled = !hasDetailOpen && !isHomeChildOpen && !isSidebarChildOpen && !navigationState.isLyricsOpen && !navigationState.isEqualizerOpen && !navigationState.isSettingsOpen

    CompositionLocalProvider(
        LocalMiniPlayerVisible provides isMiniPlayerVisible
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            DrawerHost(
                drawerState = drawerState,
                gesturesEnabled = drawerGesturesEnabled,
                onScanMedia = {
                    coroutineScope.launch { drawerState.close() }
                    onRefreshMedia()
                },
                onOpenSettings = {
                    coroutineScope.launch { drawerState.close() }
                    settingsInitialDestination = SettingsDestination.Main
                    navigationState.onAction(NavigationAction.SetSettingsOpen(true))
                },
                totalSongs = homeUiState.songs.size,
                totalSizeBytes = totalSizeBytes,
                onOpenPlaylists = {
                    coroutineScope.launch { drawerState.close() }
                    navigationState.onAction(NavigationAction.SetSidebarChildScreen(com.example.ui.sidebar.core.SidebarChildScreen.PLAYLIST))
                },
                onOpenFavorites = {
                    coroutineScope.launch { drawerState.close() }
                    navigationState.onAction(NavigationAction.SetSidebarChildScreen(com.example.ui.sidebar.core.SidebarChildScreen.FAVORITES))
                },
                onOpenHistory = {
                    coroutineScope.launch { drawerState.close() }
                    navigationState.onAction(NavigationAction.SetSidebarChildScreen(com.example.ui.sidebar.core.SidebarChildScreen.HISTORY))
                },
                isPlaying = playerUiState.playbackInfo.isPlaying,
                hasCurrentSong = playerUiState.currentSong != null
            ) {
                Scaffold(
                    modifier = modifier.fillMaxSize(),
                    containerColor = Color.Transparent,
                    contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        if (isBottomNavVisible) {
                            MainBottomNavigationBar(
                                enabledTabs = enabledTabs,
                                selectedTab = navigationState.selectedTab,
                                pagerState = pagerState,
                                expansionProgress = 0f,
                                isEqualizerOpen = navigationState.isEqualizerOpen,
                                onTabSelected = { selectTab(it) }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        MainTabContent(
                            enabledTabs = enabledTabs,
                            selectedTab = navigationState.selectedTab,
                            pagerState = pagerState,
                            hasDetailOpen = hasDetailOpen,
                            isEqualizerOpen = navigationState.isEqualizerOpen,
                            innerPadding = innerPadding,
                            homeUiState = homeUiState,
                            onHomeAction = onHomeAction,
                            libraryUiState = libraryUiState,
                            onLibraryAction = onLibraryAction,
                            collectionUiState = collectionUiState,
                            onCollectionAction = onCollectionAction,
                            playerUiState = playerUiState,
                            onSongAction = onSongActionDispatcher,
                            onSongClick = { song, queue ->
                                onPlayerAction(PlayerAction.PlaySong(song, queue ?: emptyList()))
                            },
                            onArtistSelect = { artistName ->
                                navigationState.onAction(NavigationAction.OpenArtist(artistName, navigationState.selectedTab))
                                onLibraryAction(LibraryAction.SelectAlbum(null))
                                onLibraryAction(LibraryAction.SelectArtist(artistName))
                                val targetIndex = enabledTabs.indexOf(TabItem.ARTIST)
                                if (targetIndex != -1) {
                                    coroutineScope.launch { pagerState.scrollToPage(targetIndex) }
                                }
                            },
                            onAlbumSelect = { albumName ->
                                navigationState.onAction(NavigationAction.OpenAlbum(albumName, navigationState.selectedTab))
                                onLibraryAction(LibraryAction.SelectArtist(null))
                                onLibraryAction(LibraryAction.SelectAlbum(albumName))
                                val targetIndex = enabledTabs.indexOf(TabItem.ALBUM)
                                if (targetIndex != -1) {
                                    coroutineScope.launch { pagerState.scrollToPage(targetIndex) }
                                }
                            },
                            onMenuClick = { coroutineScope.launch { drawerState.open() } },
                            onRequestPermission = onRequestPermission,
                            onRefresh = onRefreshMedia,
                            onBackFromDetail = {
                                onLibraryAction(LibraryAction.ClearDetail)
                                navigationState.detailSourceTab?.let { origin ->
                                    navigationState.onAction(NavigationAction.ClearDetailSourceTab)
                                    selectTab(origin, animate = false)
                                }
                            },
                            artistListState = artistListState,
                            artistGridState = artistGridState,
                            albumListState = albumListState,
                            albumGridState = albumGridState,
                            activeHomeChildScreen = navigationState.activeHomeChildScreen,
                            onHomeChildScreenChange = { navigationState.onAction(NavigationAction.SetHomeChildScreen(it)) }
                        )

                        when (navigationState.activeSidebarChildScreen) {
                            com.example.ui.sidebar.core.SidebarChildScreen.PLAYLIST -> {
                                com.example.ui.sidebar.SidebarPlaylistChildScreen(
                                    uiState = collectionUiState,
                                    allSongs = homeUiState.songs,
                                    currentPlayingSongId = playerUiState.currentSong?.id,
                                    isPlaying = playerUiState.playbackInfo.isPlaying,
                                    onBackClick = { navigationState.onAction(NavigationAction.SetSidebarChildScreen(com.example.ui.sidebar.core.SidebarChildScreen.NONE)) },
                                    onAction = onCollectionAction,
                                    onSongClick = { song, queue -> onPlayerAction(PlayerAction.PlaySong(song, queue)) },
                                    onSongAction = onSongActionDispatcher,
                                    modifier = Modifier.fillMaxSize().padding(innerPadding)
                                )
                            }
                            com.example.ui.sidebar.core.SidebarChildScreen.FAVORITES -> {
                                com.example.ui.sidebar.SidebarFavoritesChildScreen(
                                    favoriteSongs = collectionUiState.favoriteSongs,
                                    currentPlayingSongId = playerUiState.currentSong?.id,
                                    isPlaying = playerUiState.playbackInfo.isPlaying,
                                    onBackClick = { navigationState.onAction(NavigationAction.SetSidebarChildScreen(com.example.ui.sidebar.core.SidebarChildScreen.NONE)) },
                                    onSongClick = { song, queue -> onPlayerAction(PlayerAction.PlaySong(song, queue)) },
                                    onSongAction = onSongActionDispatcher,
                                    modifier = Modifier.fillMaxSize().padding(innerPadding)
                                )
                            }
                            com.example.ui.sidebar.core.SidebarChildScreen.HISTORY -> {
                                com.example.ui.sidebar.SidebarHistoryChildScreen(
                                    historySongs = collectionUiState.historySongs,
                                    currentPlayingSongId = playerUiState.currentSong?.id,
                                    isPlaying = playerUiState.playbackInfo.isPlaying,
                                    onBackClick = { navigationState.onAction(NavigationAction.SetSidebarChildScreen(com.example.ui.sidebar.core.SidebarChildScreen.NONE)) },
                                    onClearHistory = { onCollectionAction(CollectionAction.ClearHistory) },
                                    onSongClick = { song, queue -> onPlayerAction(PlayerAction.PlaySong(song, queue)) },
                                    onSongAction = onSongActionDispatcher,
                                    modifier = Modifier.fillMaxSize().padding(innerPadding)
                                )
                            }
                            com.example.ui.sidebar.core.SidebarChildScreen.NONE -> Unit
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            visible = navigationState.isEqualizerOpen,
                            enter = androidx.compose.animation.slideInHorizontally(
                                animationSpec = com.example.ui.common.theme.ExpressiveMotion.SlideSpringIntOffset
                            ) { fullWidth -> (fullWidth * 0.35f).toInt() } + androidx.compose.animation.fadeIn(
                                animationSpec = com.example.ui.common.theme.ExpressiveMotion.SnappySpring
                            ),
                            exit = androidx.compose.animation.slideOutHorizontally(
                                animationSpec = com.example.ui.common.theme.ExpressiveMotion.SlideSpringIntOffset
                            ) { fullWidth -> (fullWidth * 0.35f).toInt() } + androidx.compose.animation.fadeOut(
                                animationSpec = com.example.ui.common.theme.ExpressiveMotion.SnappySpring
                            )
                        ) {
                            EqualizerScreen(
                                uiState = equalizerUiState,
                                onAction = onEqualizerAction,
                                onBackClick = { navigationState.onAction(NavigationAction.SetEqualizerOpen(false)) },
                                onMenuClick = { coroutineScope.launch { drawerState.open() } },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                                    .background(MaterialTheme.colorScheme.background)
                            )
                        }
                    }
                }

                // Interactive Expanding Player Surface
                PlayerHost(
                    uiState = effectivePlayerUiState,
                    onAction = onPlayerAction,
                    playbackProgressFlow = playbackProgressFlow,
                    isEqualizerOpen = navigationState.isEqualizerOpen,
                    isLyricsOpen = navigationState.isLyricsOpen,
                    isSettingsOpen = navigationState.isSettingsOpen,
                    isBottomBarVisible = isBottomNavVisible,
                    onNavigateToEqualizer = {
                        navigationState.onAction(NavigationAction.SetEqualizerOpen(true))
                    },
                    onNavigateToLyrics = {
                        navigationState.onAction(NavigationAction.SetLyricsOpen(true))
                    }
                )
            }

            // Settings Overlay with smooth spring animation
            SettingsOverlay(
                isOpen = navigationState.isSettingsOpen,
                onClose = {
                    navigationState.onAction(NavigationAction.SetSettingsOpen(false))
                    settingsInitialDestination = SettingsDestination.Main
                },
                uiState = settingsUiState,
                onAction = onSettingsAction,
                onNavigateToEqualizer = {
                    navigationState.onAction(NavigationAction.SetSettingsOpen(false))
                    navigationState.onAction(NavigationAction.SetEqualizerOpen(true))
                },
                onRefreshLibrary = onRefreshMedia,
                initialDestination = settingsInitialDestination
            )

            // Full-screen Lyrics Child Screen
            androidx.compose.animation.AnimatedVisibility(
                visible = navigationState.isLyricsOpen,
                enter = fadeIn(animationSpec = tween(320, easing = androidx.compose.animation.core.FastOutSlowInEasing)) +
                    slideInVertically(animationSpec = tween(360, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { it / 9 },
                exit = fadeOut(animationSpec = tween(240, easing = androidx.compose.animation.core.FastOutSlowInEasing)) +
                    slideOutVertically(animationSpec = tween(260, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { it / 9 }
            ) {
                val isBlurLyrics = playerUiState.nowPlayingTheme == com.example.core.model.NowPlayingTheme.BLUR ||
                    playerUiState.nowPlayingTheme == com.example.core.model.NowPlayingTheme.BLUR_2

                com.example.ui.player.style.theme.NowPlayingThemeContainer(
                    theme = playerUiState.nowPlayingTheme,
                    song = playerUiState.currentSong,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Khusus Lyrics: gelapkan backdrop saat tema BLUR / BLUR_2 agar lirik putih tetap terbaca jelas walau artwork berwarna putih/cerah
                        if (isBlurLyrics) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.48f))
                            )
                        }

                        com.example.ui.lyrics.LyricsChildScreen(
                            song = playerUiState.currentSong,
                            playbackProgressFlow = playbackProgressFlow,
                            isPlaying = playerUiState.playbackInfo.isPlaying,
                            forceLightContent = isBlurLyrics,
                            onBackClick = { navigationState.onAction(NavigationAction.SetLyricsOpen(false)) },
                            onSeekTo = { positionMs ->
                                onPlayerAction(PlayerAction.SeekTo(positionMs))
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Add to Playlist Overlay Dialog
            AddToPlaylistOverlay(
                song = navigationState.songForAddToPlaylist,
                playlists = collectionUiState.playlists,
                onDismiss = { navigationState.onAction(NavigationAction.DismissAddToPlaylist) },
                onAddSongToPlaylist = { playlistId, songId ->
                    onCollectionAction(CollectionAction.AddSongToPlaylist(playlistId, songId))
                },
                onCreatePlaylist = { name, callback ->
                    onCollectionAction(CollectionAction.CreatePlaylist(name, callback))
                }
            )
        }
    }
}
