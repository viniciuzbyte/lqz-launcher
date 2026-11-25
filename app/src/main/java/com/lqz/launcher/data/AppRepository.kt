package com.lqz.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import java.text.Normalizer

object AppRepository {

    fun load(context: Context): List<AppEntry> {
        val pm = context.packageManager
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(query, 0)
            .filter { it.activityInfo.packageName != context.packageName }
            .map { info ->
                val label = info.loadLabel(pm).toString()
                AppEntry(
                    label = label,
                    packageName = info.activityInfo.packageName,
                    activityName = info.activityInfo.name,
                    icon = info.loadIcon(pm).toIconBitmap(160).asImageBitmap(),
                    letter = letterOf(label)
                )
            }
            .sortedWith(compareBy<AppEntry, String>(String.CASE_INSENSITIVE_ORDER) { it.label })
    }

    private fun letterOf(label: String): Char {
        val first = label.trim().take(1)
        if (first.isEmpty()) return '#'
        val base = Normalizer.normalize(first, Normalizer.Form.NFD).firstOrNull()?.uppercaseChar() ?: return '#'
        return if (base in 'A'..'Z') base else '#'
    }
}

fun Drawable.toIconBitmap(size: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    if (this is AdaptiveIconDrawable) {
        val layer = (size * 1.5f).toInt()
        val offset = -(layer - size) / 2
        background?.let {
            it.setBounds(offset, offset, offset + layer, offset + layer)
            it.draw(canvas)
        }
        foreground?.let {
            it.setBounds(offset, offset, offset + layer, offset + layer)
            it.draw(canvas)
        }
    } else {
        setBounds(0, 0, size, size)
        draw(canvas)
    }
    return bitmap
}

fun Drawable.toBitmapScaled(max: Int): Bitmap {
    val w = intrinsicWidth.coerceAtLeast(1)
    val h = intrinsicHeight.coerceAtLeast(1)
    val s = max.toFloat() / maxOf(w, h)
    return toBitmap((w * s).toInt().coerceAtLeast(1), (h * s).toInt().coerceAtLeast(1))
}

fun launchApp(context: Context, app: AppEntry) {
    val intent = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LAUNCHER)
        .setComponent(ComponentName(app.packageName, app.activityName))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
    runCatching { context.startActivity(intent) }
}

fun openAppInfo(context: Context, packageName: String) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

fun uninstallApp(context: Context, packageName: String) {
    val intent = Intent(Intent.ACTION_DELETE, Uri.fromParts("package", packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

fun openWallpaperPicker(context: Context) {
    val intent = Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Wallpaper")
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
