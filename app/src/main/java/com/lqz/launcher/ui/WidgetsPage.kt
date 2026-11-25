package com.lqz.launcher.ui

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.lqz.launcher.data.toBitmapScaled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class WidgetEnv(
    val host: AppWidgetHost,
    val manager: AppWidgetManager,
    val request: (AppWidgetProviderInfo) -> Unit,
    val remove: (Int) -> Unit
)

@Composable
fun WidgetsPage(widgetIds: List<Int>, env: WidgetEnv, onAdd: () -> Unit) {
    var edit by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Widget",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            GlassButton(if (edit) "Selesai" else "Edit", accent = edit) { edit = !edit }
            Spacer(Modifier.width(8.dp))
            GlassButton("＋ Tambah", accent = true, onClick = onAdd)
        }
        LazyColumn(
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "battery") { BatteryCard() }
            items(widgetIds, key = { it }) { id ->
                HostedWidget(id, env, edit)
            }
            if (widgetIds.isEmpty()) {
                item(key = "empty") {
                    Text(
                        "Belum ada widget. Ketuk ＋ Tambah untuk memasang widget dari aplikasi lain.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 15.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BatteryCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val accent = AccentColors[LocalSettings.current.accent.coerceIn(0, AccentColors.lastIndex)]
    val state by produceState(Pair(0, false)) {
        while (true) {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (intent != null) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
                value = Pair(level * 100 / scale, charging)
            }
            delay(15000)
        }
    }
    val sweep by animateFloatAsState(state.first / 100f, spring(stiffness = 60f), label = "battery")

    Row(
        modifier
            .fillMaxWidth()
            .liquidGlass()
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(76.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                val inset = 5.dp.toPx()
                val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                drawArc(Color.White.copy(alpha = 0.18f), -90f, 360f, false, topLeft = androidx.compose.ui.geometry.Offset(inset, inset), size = arcSize, style = stroke)
                drawArc(accent, -90f, 360f * sweep, false, topLeft = androidx.compose.ui.geometry.Offset(inset, inset), size = arcSize, style = stroke)
            }
            Text("${state.first}%", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(18.dp))
        Column {
            Text("Baterai", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text(
                if (state.second) "Sedang mengisi daya" else "Tidak mengisi daya",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
    }
}

@Suppress("DEPRECATION")
@Composable
fun HostedWidget(id: Int, env: WidgetEnv, edit: Boolean, modifier: Modifier = Modifier) {
    val info = remember(id) { env.manager.getAppWidgetInfo(id) } ?: return
    val density = LocalDensity.current
    val height = with(density) { maxOf(info.minHeight, info.minResizeHeight).toDp() }.coerceAtLeast(110.dp)
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(height)
            .liquidGlass(RoundedCornerShape(28.dp), strength = 0.6f)
    ) {
        val widthDp = maxWidth.value.toInt()
        val heightDp = height.value.toInt()
        AndroidView(
            factory = { ctx -> env.host.createView(ctx, id, info) },
            update = { view -> view.updateAppWidgetSize(Bundle(), widthDp, heightDp, widthDp, heightDp) },
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
                .clip(RoundedCornerShape(26.dp))
        )
        if (edit) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF453A))
                    .clickable { env.remove(id) },
                contentAlignment = Alignment.Center
            ) {
                Text("✕", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun WidgetPicker(env: WidgetEnv, onClose: () -> Unit) {
    val context = LocalContext.current
    val providers by produceState<List<AppWidgetProviderInfo>>(emptyList()) {
        value = withContext(Dispatchers.Default) {
            val pm = context.packageManager
            env.manager.installedProviders
                .filter { it.widgetCategory and AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN != 0 }
                .sortedBy { it.loadLabel(pm).lowercase() }
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .pointerInput(Unit) { detectTapGestures { } }
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Pilih widget",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            GlassButton("Tutup", onClick = onClose)
        }
        LazyColumn(
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(providers, key = { it.provider.flattenToString() }) { info ->
                WidgetOption(info) {
                    env.request(info)
                    onClose()
                }
            }
        }
    }
}

@Composable
private fun WidgetOption(info: AppWidgetProviderInfo, onPick: () -> Unit) {
    val context = LocalContext.current
    val dpi = context.resources.displayMetrics.densityDpi
    val preview by produceState<ImageBitmap?>(null, info) {
        value = withContext(Dispatchers.Default) {
            runCatching {
                (info.loadPreviewImage(context, 0) ?: info.loadIcon(context, dpi))
                    ?.toBitmapScaled(480)
                    ?.asImageBitmap()
            }.getOrNull()
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .bounceClick(onClick = onPick)
            .liquidGlass(RoundedCornerShape(24.dp), strength = 0.8f, elevation = 6.dp)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(width = 120.dp, height = 84.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            val bitmap = preview
            if (bitmap != null) {
                Image(bitmap, null, Modifier.fillMaxSize().padding(6.dp), contentScale = ContentScale.Fit)
            }
        }
        Spacer(Modifier.width(16.dp))
        Text(
            info.loadLabel(context.packageManager),
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2
        )
    }
}
