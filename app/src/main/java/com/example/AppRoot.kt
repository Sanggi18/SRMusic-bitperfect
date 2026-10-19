package com.example

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.ui.setup.AnimatedSplashScreen
import com.example.ui.setup.FirstLaunchSetupScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.sidebar.core.CollectionViewModel
import com.example.ui.equalizer.core.EqualizerAction
import com.example.ui.equalizer.core.EqualizerViewModel
import com.example.ui.home.core.HomeViewModel
import com.example.ui.library.core.LibraryAction
import com.example.ui.library.core.LibraryViewModel
import com.example.ui.library.core.toUi
import com.example.ui.player.core.PlayerViewModel
import com.example.ui.settings.core.SettingsViewModel
import com.example.ui.settings.core.toComposeColor
import com.example.ui.settings.core.toUiThemeMode
import com.example.ui.library.components.ScanProgressDialog
import com.example.ui.navigation.AppNavigation
import com.example.ui.common.theme.SRMusicTheme

/**
 * Application Composition Root.
 *
 * Responsibilities:
 * - Obtains dependencies from SRMusicApp (Application scope)
 * - Injects repositories & preferences into feature ViewModel factories
 * - Scopes all feature ViewModels to the Activity
 * - Collects feature StateFlows
 * - Handles audio permission requests and library synchronization
 * - Sets up cross-feature event flows (Player event streams)
 * - Resolves application-level theme configuration
 * - Passes state and event dispatchers to AppNavigation
 */
@Composable
private inline fun <reified VM : ViewModel> appViewModel(
    activity: ComponentActivity?,
    factory: ViewModelProvider.Factory
): VM {
    return if (activity != null) {
        viewModel<VM>(
            viewModelStoreOwner = activity,
            factory = factory
        )
    } else {
        viewModel<VM>(factory = factory)
    }
}

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val app = context.applicationContext as SRMusicApp
    val activity = context as? ComponentActivity

    val settingsViewModel: SettingsViewModel = appViewModel(
        activity,
        SettingsViewModel.provideFactory(
            app.themePreferences,
            app.appPreferences,
            app.libraryPreferences
        )
    )

    val playerViewModel: PlayerViewModel = appViewModel(
        activity,
        PlayerViewModel.provideFactory(
            app.playbackManager,
            app.favoritesRepository,
            app.appPreferences
        )
    )

    val libraryViewModel: LibraryViewModel = appViewModel(
        activity,
        LibraryViewModel.provideFactory(
            app,
            app.songRepository,
            app.libraryRepository,
            app.favoritesRepository,
            app.appPreferences
        )
    )

    val homeViewModel: HomeViewModel = appViewModel(
        activity,
        HomeViewModel.provideFactory(
            app.songRepository,
            app.historyRepository,
            app.favoritesRepository,
            app.appPreferences,
            app.playbackManager
        )
    )

    val collectionViewModel: CollectionViewModel = appViewModel(
        activity,
        CollectionViewModel.provideFactory(
            app.favoritesRepository,
            app.playlistRepository,
            app.historyRepository,
            app.songRepository
        )
    )

    val equalizerViewModel: EqualizerViewModel = appViewModel(
        activity,
        EqualizerViewModel.provideFactory(app.equalizerPreferences)
    )

    var isFirstLaunchSetupCompleted by remember {
        mutableStateOf(app.appPreferences.isFirstLaunchSetupCompleted())
    }

    // Permission launcher in AppRoot
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        libraryViewModel.onAction(LibraryAction.OnPermissionResult(isGranted))
    }

    // Initial permission check on mount (only if first launch setup was already completed)
    LaunchedEffect(isFirstLaunchSetupCompleted) {
        if (isFirstLaunchSetupCompleted && !libraryViewModel.hasAudioPermission()) {
            permissionLauncher.launch(libraryViewModel.getRequiredAudioPermission())
        }
    }

    // Lifecycle resume handler: load songs if permission is granted but songs are empty
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, isFirstLaunchSetupCompleted) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && isFirstLaunchSetupCompleted) {
                if (libraryViewModel.hasAudioPermission() && libraryViewModel.uiState.value.songs.isEmpty() && !libraryViewModel.uiState.value.isLoading) {
                    libraryViewModel.onAction(LibraryAction.LoadSongs(false))
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Cross-feature Player Event Streams
    LaunchedEffect(Unit) {
        libraryViewModel.playerEvents.collect { action ->
            playerViewModel.onAction(action)
        }
    }
    LaunchedEffect(Unit) {
        homeViewModel.playerEvents.collect { action ->
            playerViewModel.onAction(action)
        }
    }

    // State Collection
    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val playerUiState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val libraryUiState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val collectionUiState by collectionViewModel.uiState.collectAsStateWithLifecycle()
    val equalizerUiState by equalizerViewModel.uiState.collectAsStateWithLifecycle()

    var artworkAccentColor by remember { mutableStateOf<Color?>(null) }
    val currentSong = playerUiState.playbackInfo.currentSong
    val systemInDarkTheme = isSystemInDarkTheme()
    val isAppDark = when (settingsUiState.themeMode) {
        com.example.core.model.AppThemeMode.AMOLED,
        com.example.core.model.AppThemeMode.PITCH_BLACK,
        com.example.core.model.AppThemeMode.DARK -> true
        com.example.core.model.AppThemeMode.LIGHT -> false
        com.example.core.model.AppThemeMode.SYSTEM -> systemInDarkTheme
        com.example.core.model.AppThemeMode.CUSTOM -> {
            val bg = settingsUiState.customBackgroundColorArgb
            if (bg != null) {
                val r = (bg shr 16 and 0xFF) / 255f
                val g = (bg shr 8 and 0xFF) / 255f
                val b = (bg and 0xFF) / 255f
                (r * 0.299f + g * 0.587f + b * 0.114f) < 0.5f
            } else true
        }
    }

    LaunchedEffect(currentSong?.id, settingsUiState.isArtworkAccent, isAppDark) {
        if (settingsUiState.isArtworkAccent && currentSong != null) {
            val colors = com.example.core.util.ArtworkPaletteExtractor.extractColors(
                context = context,
                song = currentSong,
                isDarkTheme = isAppDark
            )
            artworkAccentColor = colors.primary
        } else {
            artworkAccentColor = null
        }
    }

    val activeAccent = if (settingsUiState.isArtworkAccent && artworkAccentColor != null) {
        artworkAccentColor!!
    } else {
        settingsUiState.accentColorArgb.toComposeColor()
    }

    val activeDynamicColor = if (settingsUiState.isArtworkAccent && artworkAccentColor != null) {
        false // Defer to artwork-extracted accent palette
    } else {
        settingsUiState.isDynamicColor
    }

    val isCustomTheme = settingsUiState.themeMode == com.example.core.model.AppThemeMode.CUSTOM
    val effectiveCustomBg = if (isCustomTheme) settingsUiState.customBackgroundColorArgb?.toComposeColor() else null
    val effectiveCustomFont = if (isCustomTheme) settingsUiState.customFontColorArgb?.toComposeColor() else null

    var showSplashScreen by remember {
        mutableStateOf(!app.hasShownColdStartSplash)
    }

    SRMusicTheme(
        themeMode = settingsUiState.themeMode.toUiThemeMode(),
        isDynamicColor = activeDynamicColor,
        customAccent = activeAccent,
        customBackgroundColor = effectiveCustomBg,
        customFontColor = effectiveCustomFont,
        customThemePresetId = settingsUiState.customThemePresetId,
        customFontPath = settingsUiState.customFontPath
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (isFirstLaunchSetupCompleted) {
                    AppNavigation(
                        settingsUiState = settingsUiState,
                        onSettingsAction = settingsViewModel::onAction,
                        equalizerUiState = equalizerUiState,
                        onEqualizerAction = { action ->
                            equalizerViewModel.onAction(action)
                            when (action) {
                                is EqualizerAction.SetAudioOutputApi -> {
                                    app.playbackManager.onAudioOutputApiChanged(action.api)
                                }
                                is EqualizerAction.SetBitPerfectEnabled -> {
                                    app.playbackManager.onBitPerfectChanged(action.enabled)
                                }
                                else -> {}
                            }
                        },
                        homeUiState = homeUiState,
                        onHomeAction = homeViewModel::onAction,
                        libraryUiState = libraryUiState,
                        onLibraryAction = libraryViewModel::onAction,
                        collectionUiState = collectionUiState,
                        onCollectionAction = collectionViewModel::onAction,
                        playerUiState = playerUiState,
                        onPlayerAction = playerViewModel::onAction,
                        playbackProgressFlow = playerViewModel.playbackProgress,
                        onRequestPermission = {
                            permissionLauncher.launch(libraryViewModel.getRequiredAudioPermission())
                        },
                        onRefreshMedia = {
                            libraryViewModel.onAction(
                                LibraryAction.LoadSongs(
                                    isRefreshing = true,
                                    showProgressDialog = true,
                                    progressTitle = "Scanning Media"
                                )
                            )
                        }
                    )

                    ScanProgressDialog(
                        state = libraryUiState.scanProgress.toUi(),
                        onDismiss = {
                            libraryViewModel.onAction(LibraryAction.DismissScanProgress)
                        }
                    )
                } else {
                    FirstLaunchSetupScreen(
                        onContinue = {
                            app.appPreferences.setFirstLaunchSetupCompleted(true)
                            isFirstLaunchSetupCompleted = true
                            libraryViewModel.onAction(
                                LibraryAction.OnPermissionResult(libraryViewModel.hasAudioPermission())
                            )
                        }
                    )
                }

                if (showSplashScreen) {
                    AnimatedSplashScreen(
                        onAnimationFinished = {
                            app.hasShownColdStartSplash = true
                            showSplashScreen = false
                        }
                    )
                }
            }
        }
    }
}
