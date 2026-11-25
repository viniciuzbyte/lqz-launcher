package com.lqz.launcher.data

import androidx.compose.ui.graphics.ImageBitmap

data class AppEntry(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: ImageBitmap,
    val letter: Char
) {
    val key: String get() = "$packageName/$activityName"
}

data class LauncherSettings(
    val homeClockStyle: Int = 0,
    val homeClockScale: Float = 1f,
    val use24h: Boolean = true,
    val showSeconds: Boolean = false,
    val lockEnabled: Boolean = false,
    val lockClockStyle: Int = 3,
    val lockClockScale: Float = 1f,
    val lockClockPosition: Int = 0,
    val lockClockColor: Int = 0,
    val lockShowDate: Boolean = true,
    val glassOpacity: Float = 0.18f,
    val blurRadius: Int = 60,
    val iconSize: Int = 56,
    val iconShape: Int = 0,
    val accent: Int = 0
)
