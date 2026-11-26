package com.lqz.launcher.notif

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.app.NotificationManagerCompat
import com.lqz.launcher.data.toIconBitmap
import kotlinx.coroutines.flow.MutableStateFlow

data class NotifItem(
    val key: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val time: Long,
    val intent: PendingIntent?,
    val icon: ImageBitmap?
)

object NotificationStore {
    val items = MutableStateFlow<List<NotifItem>>(emptyList())

    @Volatile
    var service: NotificationListenerService? = null

    fun dismiss(key: String) {
        runCatching { service?.cancelNotification(key) }
    }

    fun dismissAll() {
        runCatching { service?.cancelAllNotifications() }
    }

    fun isEnabled(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
}

class LqzNotificationService : NotificationListenerService() {

    override fun onListenerConnected() {
        NotificationStore.service = this
        refresh()
    }

    override fun onListenerDisconnected() {
        NotificationStore.service = null
        NotificationStore.items.value = emptyList()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refresh()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refresh()
    }

    private fun refresh() {
        val active = runCatching { activeNotifications?.toList().orEmpty() }.getOrDefault(emptyList())
        NotificationStore.items.value = active
            .filter { it.packageName != packageName }
            .filter { !it.isOngoing }
            .filter { (it.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0 }
            .mapNotNull { toItem(it) }
            .sortedByDescending { it.time }
    }

    private fun toItem(sbn: StatusBarNotification): NotifItem? {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return null
        val pm = packageManager
        val appName = runCatching {
            pm.getApplicationLabel(pm.getApplicationInfo(sbn.packageName, 0)).toString()
        }.getOrDefault(sbn.packageName)
        val icon = runCatching {
            pm.getApplicationIcon(sbn.packageName).toIconBitmap(96).asImageBitmap()
        }.getOrNull()
        return NotifItem(
            key = sbn.key,
            packageName = sbn.packageName,
            appName = appName,
            title = title,
            text = text,
            time = sbn.postTime,
            intent = sbn.notification.contentIntent,
            icon = icon
        )
    }
}
