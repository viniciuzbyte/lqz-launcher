package com.lqz.launcher.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("lqz_launcher", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<LauncherSettings> = _settings.asStateFlow()

    private val _favorites = MutableStateFlow(readFavorites())
    val favorites: StateFlow<List<String>?> = _favorites.asStateFlow()

    private val _widgets = MutableStateFlow(readWidgets())
    val widgets: StateFlow<List<Int>> = _widgets.asStateFlow()

    fun update(block: (LauncherSettings) -> LauncherSettings) {
        val next = block(_settings.value)
        _settings.value = next
        write(next)
    }

    fun setFavorites(list: List<String>) {
        _favorites.value = list
        prefs.edit().putString("favorites", list.joinToString("|")).apply()
    }

    fun setWidgets(list: List<Int>) {
        _widgets.value = list
        prefs.edit().putString("widgets", list.joinToString(",")).apply()
    }

    private fun readFavorites(): List<String>? =
        prefs.getString("favorites", null)?.split("|")?.filter { it.isNotBlank() }

    private fun readWidgets(): List<Int> =
        prefs.getString("widgets", "").orEmpty().split(",").mapNotNull { it.toIntOrNull() }

    private fun read(): LauncherSettings {
        val d = LauncherSettings()
        return LauncherSettings(
            homeClockStyle = prefs.getInt("homeClockStyle", d.homeClockStyle),
            homeClockScale = prefs.getFloat("homeClockScale", d.homeClockScale),
            use24h = prefs.getBoolean("use24h", d.use24h),
            showSeconds = prefs.getBoolean("showSeconds", d.showSeconds),
            lockEnabled = prefs.getBoolean("lockEnabled", d.lockEnabled),
            lockClockStyle = prefs.getInt("lockClockStyle", d.lockClockStyle),
            lockClockScale = prefs.getFloat("lockClockScale", d.lockClockScale),
            lockClockPosition = prefs.getInt("lockClockPosition", d.lockClockPosition),
            lockClockColor = prefs.getInt("lockClockColor", d.lockClockColor),
            lockShowDate = prefs.getBoolean("lockShowDate", d.lockShowDate),
            glassOpacity = prefs.getFloat("glassOpacity", d.glassOpacity),
            blurRadius = prefs.getInt("blurRadius", d.blurRadius),
            iconSize = prefs.getInt("iconSize", d.iconSize),
            iconShape = prefs.getInt("iconShape", d.iconShape),
            accent = prefs.getInt("accent", d.accent)
        )
    }

    private fun write(s: LauncherSettings) {
        prefs.edit()
            .putInt("homeClockStyle", s.homeClockStyle)
            .putFloat("homeClockScale", s.homeClockScale)
            .putBoolean("use24h", s.use24h)
            .putBoolean("showSeconds", s.showSeconds)
            .putBoolean("lockEnabled", s.lockEnabled)
            .putInt("lockClockStyle", s.lockClockStyle)
            .putFloat("lockClockScale", s.lockClockScale)
            .putInt("lockClockPosition", s.lockClockPosition)
            .putInt("lockClockColor", s.lockClockColor)
            .putBoolean("lockShowDate", s.lockShowDate)
            .putFloat("glassOpacity", s.glassOpacity)
            .putInt("blurRadius", s.blurRadius)
            .putInt("iconSize", s.iconSize)
            .putInt("iconShape", s.iconShape)
            .putInt("accent", s.accent)
            .apply()
    }
}
