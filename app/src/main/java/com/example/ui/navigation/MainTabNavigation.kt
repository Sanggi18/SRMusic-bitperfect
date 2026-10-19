package com.example.ui.navigation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.LaunchedEffect
import com.example.ui.common.theme.ExpressiveMotion
import com.example.ui.common.theme.LocalSpacing
import com.example.ui.navigation.components.TabItem
import com.example.ui.navigation.components.getTabIcon
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.example.ui.sidebar.core.CollectionAction
import com.example.ui.sidebar.core.CollectionUiState
import com.example.ui.home.core.HomeAction
import com.example.ui.home.core.HomeChildScreen
import com.example.ui.home.HomeScreen
import com.example.ui.home.core.HomeUiState
import com.example.ui.library.core.LibraryAction
import com.example.ui.library.core.LibraryUiState
import com.example.ui.library.core.SongAction
import com.example.ui.library.album.AlbumScreen
import com.example.ui.library.artist.ArtistScreen
import com.example.ui.library.components.CollectionDetailScreen
import com.example.ui.library.components.albumDetailConfig
import com.example.ui.library.components.artistDetailConfig
import com.example.ui.library.folder.FolderScreen
import com.example.ui.library.song.SongScreen
import com.example.ui.player.core.PlayerUiState
import com.example.core.model.Song

@Composable
private fun RowScope.NavPillItem(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .weight(1f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            icon()
            label()
        }
    }
}

@Composable
fun MainBottomNavigationBar(
    enabledTabs: List<TabItem>,
    selectedTab: TabItem,
    pagerState: PagerState? = null,
    expansionProgress: Float = 0f,
    isEqualizerOpen: Boolean,
    onTabSelected: (TabItem) -> Unit
) {
    if (isEqualizerOpen || expansionProgress >= 0.999f) return

    val spacing = LocalSpacing.current
    val navBarAlpha = (1f - expansionProgress * 2.0f).coerceIn(0f, 1f)
    val navBarBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val navBarTotalHeight = spacing.navigationBarHeight + navBarBottomInset + 16.dp
    val navBarTotalHeightPx = with(LocalDensity.current) { navBarTotalHeight.toPx() }

    if (navBarAlpha > 0f) {
        val pillShape = RoundedCornerShape(28.dp)

        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = navBarBottomInset + 8.dp)
                .graphicsLayer {
                    translationY = expansionProgress * navBarTotalHeightPx
                    alpha = navBarAlpha
                }
                .clip(pillShape)
                .border(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    shape = pillShape
                ),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 0.dp,
            windowInsets = WindowInsets(0, 0, 0, 0)
        ) {
            val tabCount = enabledTabs.size.coerceAtLeast(1)
            val isManualSwipe = pagerState != null && pagerState.currentPageOffsetFraction != 0f
            val activeTab = if (!isManualSwipe) {
                selectedTab
            } else {
                enabledTabs.getOrNull(pagerState.targetPage) ?: selectedTab
            }
            var parentWidthPx by remember { mutableFloatStateOf(0f) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .onGloballyPositioned { parentWidthPx = it.size.width.toFloat() }
            ) {
                val itemWidthPx = if (tabCount > 0) parentWidthPx / tabCount else 0f

                // Target posisi pill (dalam satuan index tab, bisa fraksi saat swipe):
                // - Swipe manual: ikuti pager persis (currentPage + fraction).
                // - Settle/tap: pakai pagerState.currentPage (sinkron, akurat).
                //   JANGAN pakai indexOf(selectedTab): update-nya async dan lag
                //   sehingga pill loncat balik sesaat (glitch).
                val pillTargetPos = if (pagerState != null) {
                    if (isManualSwipe) {
                        (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                            .coerceIn(0f, (tabCount - 1).coerceAtLeast(0).toFloat())
                    } else {
                        pagerState.currentPage.coerceIn(0, (tabCount - 1).coerceAtLeast(0)).toFloat()
                    }
                } else {
                    enabledTabs.indexOf(selectedTab).coerceAtLeast(0).toFloat()
                }

                // Animatable posisi pill:
                // - isManualSwipe = true  -> snapTo (ikuti jari 1:1, tanpa lag/animasi).
                // - isManualSwipe = false -> animateTo spring (tap/settle geser halus).
                val pillAnimatable = remember { Animatable(pillTargetPos) }
                LaunchedEffect(pillTargetPos, isManualSwipe) {
                    if (isManualSwipe) {
                        pillAnimatable.snapTo(pillTargetPos)
                    } else {
                        pillAnimatable.animateTo(
                            pillTargetPos,
                            animationSpec = spring(dampingRatio = 1f, stiffness = 400f)
                        )
                    }
                }

                Box(modifier = Modifier.matchParentSize()) {
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset((itemWidthPx * pillAnimatable.value).roundToInt(), 0)
                            }
                            .fillMaxWidth(1f / tabCount)
                            .padding(horizontal = 8.dp)
                            .fillMaxHeight()
                            .padding(vertical = 2.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    enabledTabs.forEach { tab ->
                        val isSelected = activeTab == tab
                        val iconScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.15f else 1.0f,
                            animationSpec = ExpressiveMotion.SnappySpring,
                            label = "tab_icon_scale"
                        )

                        NavPillItem(
                            onClick = { onTabSelected(tab) },
                            icon = {
                                Icon(
                                    imageVector = getTabIcon(tab),
                                    contentDescription = tab.label,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                    }
                                )
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainTabContent(
    pagerState: PagerState,
    enabledTabs: List<TabItem>,
    selectedTab: TabItem,
    hasDetailOpen: Boolean,
    isEqualizerOpen: Boolean,
    innerPadding: PaddingValues,
    homeUiState: HomeUiState,
    onHomeAction: (HomeAction) -> Unit,
    libraryUiState: LibraryUiState,
    onLibraryAction: (LibraryAction) -> Unit,
    collectionUiState: CollectionUiState,
    onCollectionAction: (CollectionAction) -> Unit,
    playerUiState: PlayerUiState,
    onSongAction: (SongAction, Song) -> Unit,
    onSongClick: (Song, List<Song>?) -> Unit,
    onArtistSelect: (String) -> Unit,
    onAlbumSelect: (String) -> Unit,
    onMenuClick: () -> Unit,
    onRequestPermission: () -> Unit,
    onRefresh: () -> Unit,
    onBackFromDetail: () -> Unit,
    artistListState: LazyListState,
    artistGridState: LazyGridState,
    albumListState: LazyListState,
    albumGridState: LazyGridState,
    activeHomeChildScreen: HomeChildScreen = HomeChildScreen.NONE,
    onHomeChildScreenChange: (HomeChildScreen) -> Unit = {}
) {
    val favoriteSongIds = remember(collectionUiState.favoriteSongs) {
        collectionUiState.favoriteSongs.map { it.id }.toSet()
    }
    val currentPlayingSongId = playerUiState.playbackInfo.currentSong?.id
    val isPlaying = playerUiState.playbackInfo.isPlaying
    val coroutineScope = rememberCoroutineScope()

    val bottomPadding = innerPadding.calculateBottomPadding()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (enabledTabs.contains(selectedTab)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = !hasDetailOpen &&
                    !isEqualizerOpen &&
                    activeHomeChildScreen == HomeChildScreen.NONE,
                beyondViewportPageCount = 1,
                key = { page -> enabledTabs.getOrNull(page)?.id ?: page }
            ) { page ->
                val currentTab = enabledTabs.getOrNull(page)
                when (currentTab) {
                    TabItem.HOME -> {
                        val adjustedHomeUiState = homeUiState.copy(isLoading = libraryUiState.isLoading)
                        HomeScreen(
                            uiState = adjustedHomeUiState,
                            currentPlayingSongId = currentPlayingSongId,
                            isPlaying = isPlaying,
                            onArtistClick = onArtistSelect,
                            onAlbumClick = onAlbumSelect,
                            onMenuClick = onMenuClick,
                            onAction = onHomeAction,
                            activeChildScreen = activeHomeChildScreen,
                            onChildScreenChange = onHomeChildScreenChange
                        )
                    }
                    TabItem.SONG -> {
                        SongScreen(
                            uiState = libraryUiState,
                            currentPlayingSongId = currentPlayingSongId,
                            isPlaying = isPlaying,
                            onSongClick = onSongClick,
                            onSongAction = onSongAction,
                            onMenuClick = onMenuClick,
                            searchQuery = libraryUiState.searchQuery,
                            onSearchQueryChange = { onLibraryAction(LibraryAction.Search(it)) },
                            sortCriteria = libraryUiState.songSortCriteria,
                            onSortCriteriaChange = { onLibraryAction(LibraryAction.UpdateSongSort(it, libraryUiState.songIsAscending)) },
                            isAscending = libraryUiState.songIsAscending,
                            onAscendingChange = { onLibraryAction(LibraryAction.UpdateSongSort(libraryUiState.songSortCriteria, it)) },
                            favoriteSongIds = favoriteSongIds,
                            onToggleFavorite = { song -> onCollectionAction(CollectionAction.ToggleFavorite(song.id)) }
                        )
                    }
                    TabItem.FOLDER -> {
                        FolderScreen(
                            uiState = libraryUiState,
                            currentPlayingSongId = currentPlayingSongId,
                            onSongClick = onSongClick,
                            onMenuClick = onMenuClick,
                            searchQuery = libraryUiState.searchQuery,
                            onSearchQueryChange = { onLibraryAction(LibraryAction.Search(it)) },
                            rootFolderDisplay = libraryUiState.rootFolderDisplay,
                            currentRelativePath = libraryUiState.currentFolderRelativePath,
                            onRelativePathChange = { onLibraryAction(LibraryAction.SelectFolder(it)) },
                            onCurrentRelativePathChange = { onLibraryAction(LibraryAction.SelectFolder(it)) },
                            folderStyle = libraryUiState.folderStyle,
                            onFolderStyleChange = { onLibraryAction(LibraryAction.ChangeFolderStyle(it)) },
                            onPlayNextBatch = { songs -> songs.forEach { onSongAction(SongAction.PLAY_NEXT, it) } },
                            onAddToQueueBatch = { songs -> songs.forEach { onSongAction(SongAction.ADD_TO_QUEUE, it) } },
                            onSongAction = onSongAction,
                            favoriteSongIds = favoriteSongIds,
                            onToggleFavorite = { song -> onCollectionAction(CollectionAction.ToggleFavorite(song.id)) }
                        )
                    }
                    TabItem.ARTIST -> {
                        ArtistScreen(
                            uiState = libraryUiState,
                            onMenuClick = onMenuClick,
                            searchQuery = libraryUiState.searchQuery,
                            onSearchQueryChange = { onLibraryAction(LibraryAction.Search(it)) },
                            selectedArtistName = libraryUiState.selectedArtistName,
                            onSelectedArtistChange = { onLibraryAction(LibraryAction.SelectArtist(it)) },
                            sortCriteria = libraryUiState.artistSortCriteria,
                            onSortCriteriaChange = { onLibraryAction(LibraryAction.UpdateArtistSort(it, libraryUiState.artistIsAscending)) },
                            isAscending = libraryUiState.artistIsAscending,
                            onAscendingChange = { onLibraryAction(LibraryAction.UpdateArtistSort(libraryUiState.artistSortCriteria, it)) },
                            gridMode = libraryUiState.artistGridMode,
                            onGridModeChange = { onLibraryAction(LibraryAction.UpdateArtistGridMode(it)) },
                            listState = artistListState,
                            gridState = artistGridState
                        )
                    }
                    TabItem.ALBUM -> {
                        AlbumScreen(
                            uiState = libraryUiState,
                            onMenuClick = onMenuClick,
                            searchQuery = libraryUiState.searchQuery,
                            onSearchQueryChange = { onLibraryAction(LibraryAction.Search(it)) },
                            selectedAlbumName = libraryUiState.selectedAlbumName,
                            onSelectedAlbumChange = { onLibraryAction(LibraryAction.SelectAlbum(it)) },
                            sortCriteria = libraryUiState.albumSortCriteria,
                            onSortCriteriaChange = { onLibraryAction(LibraryAction.UpdateAlbumSort(it, libraryUiState.albumIsAscending)) },
                            isAscending = libraryUiState.albumIsAscending,
                            onAscendingChange = { onLibraryAction(LibraryAction.UpdateAlbumSort(libraryUiState.albumSortCriteria, it)) },
                            gridMode = libraryUiState.albumGridMode,
                            onGridModeChange = { onLibraryAction(LibraryAction.UpdateAlbumGridMode(it)) },
                            listState = albumListState,
                            gridState = albumGridState
                        )
                    }
                    null -> {}
                }
            }
        }

        AnimatedVisibility(
            visible = libraryUiState.selectedArtistName != null,
            enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(250)),
            exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(200))
        ) {
            libraryUiState.selectedArtistName?.let { artistName ->
                CollectionDetailScreen(
                    title = artistName,
                    config = artistDetailConfig(artistName),
                    songs = libraryUiState.songs,
                    currentPlayingSongId = currentPlayingSongId,
                    onSongClick = onSongClick,
                    onBackClick = onBackFromDetail,
                    onSongAction = onSongAction,
                    favoriteSongIds = favoriteSongIds,
                    onToggleFavorite = { song -> onCollectionAction(CollectionAction.ToggleFavorite(song.id)) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = innerPadding.calculateBottomPadding())
                        .background(MaterialTheme.colorScheme.background)
                )
            }
        }

        AnimatedVisibility(
            visible = libraryUiState.selectedAlbumName != null,
            enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(250)),
            exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(200))
        ) {
            libraryUiState.selectedAlbumName?.let { albumName ->
                CollectionDetailScreen(
                    title = albumName,
                    config = albumDetailConfig(albumName),
                    songs = libraryUiState.songs,
                    currentPlayingSongId = currentPlayingSongId,
                    onSongClick = onSongClick,
                    onBackClick = onBackFromDetail,
                    onSongAction = onSongAction,
                    favoriteSongIds = favoriteSongIds,
                    onToggleFavorite = { song -> onCollectionAction(CollectionAction.ToggleFavorite(song.id)) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = innerPadding.calculateBottomPadding())
                        .background(MaterialTheme.colorScheme.background)
                )
            }
        }
    }
}
