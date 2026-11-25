package com.lqz.launcher.data

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    val store = SettingsStore(application)

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()

    val settings = store.settings
    val favorites = store.favorites
    val widgets = store.widgets
    val locked = MutableStateFlow(false)
    val homeEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            reload()
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(application, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        reload()
    }

    fun reload() {
        viewModelScope.launch(Dispatchers.Default) {
            val list = AppRepository.load(getApplication())
            _apps.value = list
            syncFavorites(list)
        }
    }

    private fun syncFavorites(list: List<AppEntry>) {
        val current = store.favorites.value
        if (current == null) {
            val pm = getApplication<Application>().packageManager
            val intents = listOf(
                Intent(Intent.ACTION_DIAL),
                Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")),
                Intent(Intent.ACTION_VIEW, Uri.parse("https://")),
                Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            )
            val keys = intents
                .mapNotNull { pm.resolveActivity(it, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName }
                .distinct()
                .mapNotNull { pkg -> list.firstOrNull { it.packageName == pkg }?.key }
            store.setFavorites(keys)
        } else {
            val valid = list.mapTo(HashSet()) { it.key }
            val cleaned = current.filter { it in valid }
            if (cleaned.size != current.size && list.isNotEmpty()) store.setFavorites(cleaned)
        }
    }

    fun toggleFavorite(app: AppEntry) {
        val current = store.favorites.value.orEmpty()
        when {
            app.key in current -> store.setFavorites(current - app.key)
            current.size < MAX_FAVORITES -> store.setFavorites(current + app.key)
        }
    }

    fun updateSettings(block: (LauncherSettings) -> LauncherSettings) = store.update(block)

    fun addWidget(id: Int) = store.setWidgets(store.widgets.value + id)

    fun removeWidget(id: Int) = store.setWidgets(store.widgets.value - id)

    fun lockNow() {
        locked.value = true
    }

    fun unlock() {
        locked.value = false
    }

    override fun onCleared() {
        runCatching { getApplication<Application>().unregisterReceiver(receiver) }
    }

    companion object {
        const val MAX_FAVORITES = 7
    }
}
