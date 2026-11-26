package com.lqz.launcher.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class SettingsStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("lqz_launcher", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<LauncherSettings> = _settings.asStateFlow()

    private val _favorites = MutableStateFlow(readFavorites())
    val favorites: StateFlow<List<String>?> = _favorites.asStateFlow()

    private val _widgets = MutableStateFlow(readWidgets())
    val widgets: StateFlow<List<Int>> = _widgets.asStateFlow()

    private val _customWidgets = MutableStateFlow(readCustomWidgets())
    val customWidgets: StateFlow<List<CustomWidget>> = _customWidgets.asStateFlow()

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

    fun setCustomWidgets(list: List<CustomWidget>) {
        _customWidgets.value = list
        val array = JSONArray()
        list.forEach { w ->
            array.put(
                JSONObject()
                    .put("id", w.id)
                    .put("type", w.type)
                    .put("cols", w.cols)
                    .put("rows", w.rows)
                    .put("bgColor", w.bgColor)
                    .put("bgAlpha", w.bgAlpha.toDouble())
                    .put("radius", w.radius)
                    .put("textColor", w.textColor)
                    .put("imagePath", w.imagePath)
                    .put("text", w.text)
                    .put("use24h", w.use24h)
                    .put("showSeconds", w.showSeconds)
                    .put("font", w.font)
            )
        }
        prefs.edit().putString("customWidgets", array.toString()).apply()
    }

    private fun readFavorites(): List<String>? =
        prefs.getString("favorites", null)?.split("|")?.filter { it.isNotBlank() }

    private fun readWidgets(): List<Int> =
        prefs.getString("widgets", "").orEmpty().split(",").mapNotNull { it.toIntOrNull() }

    private fun readCustomWidgets(): List<CustomWidget> = runCatching {
        val array = JSONArray(prefs.getString("customWidgets", "[]"))
        (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            CustomWidget(
                id = o.getLong("id"),
                type = o.getInt("type"),
                cols = o.getInt("cols"),
                rows = o.getInt("rows"),
                bgColor = o.getInt("bgColor"),
                bgAlpha = o.getDouble("bgAlpha").toFloat(),
                radius = o.getInt("radius"),
                textColor = o.getInt("textColor"),
                imagePath = o.optString("imagePath", ""),
                text = o.optString("text", ""),
                use24h = o.optBoolean("use24h", true),
                showSeconds = o.optBoolean("showSeconds", false),
                font = o.optInt("font", 0)
            )
        }
    }.getOrDefault(emptyList())

    private fun read(): LauncherSettings {
        val d = LauncherSettings()
        return LauncherSettings(
            themeMode = prefs.getInt("themeMode", d.themeMode),
            panelStyle = prefs.getInt("panelStyle", d.panelStyle),
            accent = prefs.getInt("accent", d.accent),
            glassOpacity = prefs.getFloat("glassOpacity", d.glassOpacity),
            blurRadius = prefs.getInt("blurRadius", d.blurRadius),
            iconSize = prefs.getInt("iconSize", d.iconSize),
            iconShape = prefs.getInt("iconShape", d.iconShape),
            homeLayout = prefs.getInt("homeLayout", d.homeLayout),
            homeAlign = prefs.getInt("homeAlign", d.homeAlign),
            showFavoriteIcons = prefs.getBoolean("showFavoriteIcons", d.showFavoriteIcons),
            homeClockStyle = prefs.getInt("homeClockStyle", d.homeClockStyle),
            homeClockScale = prefs.getFloat("homeClockScale", d.homeClockScale),
            homeClockColor = prefs.getInt("homeClockColor", d.homeClockColor),
            clockFont = prefs.getInt("clockFont", d.clockFont),
            use24h = prefs.getBoolean("use24h", d.use24h),
            showSeconds = prefs.getBoolean("showSeconds", d.showSeconds),
            lockEnabled = prefs.getBoolean("lockEnabled", d.lockEnabled),
            lockClockStyle = prefs.getInt("lockClockStyle", d.lockClockStyle),
            lockClockScale = prefs.getFloat("lockClockScale", d.lockClockScale),
            lockClockPosition = prefs.getInt("lockClockPosition", d.lockClockPosition),
            lockClockColor = prefs.getInt("lockClockColor", d.lockClockColor),
            lockShowDate = prefs.getBoolean("lockShowDate", d.lockShowDate),
            lockShowNotifications = prefs.getBoolean("lockShowNotifications", d.lockShowNotifications),
            aodEnabled = prefs.getBoolean("aodEnabled", d.aodEnabled),
            aodTimeout = prefs.getInt("aodTimeout", d.aodTimeout),
            aodBrightness = prefs.getFloat("aodBrightness", d.aodBrightness),
            aodClockStyle = prefs.getInt("aodClockStyle", d.aodClockStyle),
            aodShowNotifs = prefs.getBoolean("aodShowNotifs", d.aodShowNotifs)
        )
    }

    private fun write(s: LauncherSettings) {
        prefs.edit()
            .putInt("themeMode", s.themeMode)
            .putInt("panelStyle", s.panelStyle)
            .putInt("accent", s.accent)
            .putFloat("glassOpacity", s.glassOpacity)
            .putInt("blurRadius", s.blurRadius)
            .putInt("iconSize", s.iconSize)
            .putInt("iconShape", s.iconShape)
            .putInt("homeLayout", s.homeLayout)
            .putInt("homeAlign", s.homeAlign)
            .putBoolean("showFavoriteIcons", s.showFavoriteIcons)
            .putInt("homeClockStyle", s.homeClockStyle)
            .putFloat("homeClockScale", s.homeClockScale)
            .putInt("homeClockColor", s.homeClockColor)
            .putInt("clockFont", s.clockFont)
            .putBoolean("use24h", s.use24h)
            .putBoolean("showSeconds", s.showSeconds)
            .putBoolean("lockEnabled", s.lockEnabled)
            .putInt("lockClockStyle", s.lockClockStyle)
            .putFloat("lockClockScale", s.lockClockScale)
            .putInt("lockClockPosition", s.lockClockPosition)
            .putInt("lockClockColor", s.lockClockColor)
            .putBoolean("lockShowDate", s.lockShowDate)
            .putBoolean("lockShowNotifications", s.lockShowNotifications)
            .putBoolean("aodEnabled", s.aodEnabled)
            .putInt("aodTimeout", s.aodTimeout)
            .putFloat("aodBrightness", s.aodBrightness)
            .putInt("aodClockStyle", s.aodClockStyle)
            .putBoolean("aodShowNotifs", s.aodShowNotifs)
            .apply()
    }
}
