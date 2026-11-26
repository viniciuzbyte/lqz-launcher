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
    val themeMode: Int = 0,
    val panelStyle: Int = 0,
    val accent: Int = 0,
    val glassOpacity: Float = 0.18f,
    val blurRadius: Int = 60,
    val iconSize: Int = 56,
    val iconShape: Int = 0,
    val homeLayout: Int = 0,
    val homeAlign: Int = 0,
    val showFavoriteIcons: Boolean = true,
    val homeClockStyle: Int = 0,
    val homeClockScale: Float = 1f,
    val homeClockColor: Int = 0,
    val clockFont: Int = 0,
    val use24h: Boolean = true,
    val showSeconds: Boolean = false,
    val lockEnabled: Boolean = false,
    val lockClockStyle: Int = 3,
    val lockClockScale: Float = 1f,
    val lockClockPosition: Int = 0,
    val lockClockColor: Int = 0,
    val lockShowDate: Boolean = true,
    val lockShowNotifications: Boolean = true,
    val aodEnabled: Boolean = false,
    val aodTimeout: Int = 15,
    val aodBrightness: Float = 0.05f,
    val aodClockStyle: Int = 0,
    val aodShowNotifs: Boolean = true
)

data class CustomWidget(
    val id: Long,
    val type: Int,
    val cols: Int,
    val rows: Int,
    val bgColor: Int,
    val bgAlpha: Float,
    val radius: Int,
    val textColor: Int,
    val imagePath: String,
    val text: String,
    val use24h: Boolean,
    val showSeconds: Boolean,
    val font: Int
)
