package com.lqz.launcher.ui

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.os.Bundle
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.lqz.launcher.data.CustomWidget
import com.lqz.launcher.data.toBitmapScaled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WidgetEnv(
    val host: AppWidgetHost,
    val manager: AppWidgetManager,
    val request: (AppWidgetProviderInfo) -> Unit,
    val remove: (Int) -> Unit
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WidgetsPage(
    systemWidgets: List<Int>,
    customWidgets: List<CustomWidget>,
    env: WidgetEnv,
    onAddSystem: () -> Unit,
    onNewCustom: () -> Unit,
    onEditCustom: (CustomWidget) -> Unit,
    onDeleteCustom: (CustomWidget) -> Unit
) {
    var edit by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Widget",
                color = ThemeText,
                fontSize = 32.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.weight(1f)
            )
            GlassButton(if (edit) "Selesai" else "Edit", accent = edit) { edit = !edit }
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassButton("＋ Buat widget", accent = true, onClick = onNewCustom)
            GlassButton("＋ Widget sistem", onClick = onAddSystem)
        }
        BoxWithConstraints(Modifier.weight(1f)) {
            val gap = 10.dp
            val cell = (maxWidth - gap * 3) / 4
            LazyColumn(
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(key = "custom") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(gap),
                        verticalArrangement = Arrangement.spacedBy(gap)
                    ) {
                        customWidgets.forEach { widget ->
                            key(widget.id) {
                                CustomWidgetView(
                                    widget = widget,
                                    cell = cell,
                                    gap = gap,
                                    edit = edit,
                                    onEdit = { onEditCustom(widget) },
                                    onDelete = { onDeleteCustom(widget) }
                                )
                            }
                        }
                    }
                }
                items(systemWidgets, key = { it }) { id ->
                    HostedWidget(id, env, edit)
                }
                if (customWidgets.isEmpty() && systemWidgets.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            "Belum ada widget. Ketuk ＋ Buat widget untuk membuat jam, foto, teks, tanggal atau baterai dengan ukuran 1x1 sampai 4x2.",
                            color = ThemeTextSoft,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
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
            .panel(RoundedCornerShape(26.dp), strength = 0.6f, elevation = 6.dp)
    ) {
        val widthDp = maxWidth.value.toInt()
        val heightDp = height.value.toInt()
        AndroidView(
            factory = { ctx -> env.host.createView(ctx, id, info) },
            update = { view -> view.updateAppWidgetSize(Bundle(), widthDp, heightDp, widthDp, heightDp) },
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
                .clip(RoundedCornerShape(24.dp))
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
            .background(ThemeOverlay)
            .pointerInput(Unit) { detectTapGestures { } }
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Widget sistem",
                color = ThemeText,
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
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
            .panel(RoundedCornerShape(22.dp), strength = 0.8f, elevation = 4.dp)
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
            color = ThemeText,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2
        )
    }
}
