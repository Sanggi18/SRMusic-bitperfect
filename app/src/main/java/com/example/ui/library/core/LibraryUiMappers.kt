package com.example.ui.library.core

import com.example.core.model.LibraryScanProgress
import com.example.ui.library.components.ScanProgressState

fun LibraryScanProgress.toUi(): ScanProgressState = ScanProgressState(
    isScanning = isScanning,
    currentCount = currentCount,
    isFinished = isFinished,
    title = title,
    description = description
)
