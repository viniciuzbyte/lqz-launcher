package com.lqz.launcher.ui

import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.os.BatteryManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lqz.launcher.data.CustomWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter
import java.util.Locale

val WidgetBgColors = listOf(
    Color(0xFF1D1E24),
    Color(0xFFFFFFFF),
    Color(0xFF000000),
    Color(0xFF4F8CFF),
    Color(0xFFB36BFF),
    Color(0xFFFF5C7A),
    Color(0xFF34D399),
    Color(0xFFFFB020),
    Color(0xFF22D3EE),
    Color(0xFF8E8E93)
)

val WidgetTextColors = listOf(
    Color.White,
    Color(0xFF111318),
    Color(0xFFFFD60A),
    Color(0xFF64D2FF),
    Color(0xFFFF9FB2),
    Color(0xFFB4F8C8)
)

val WidgetSizes = listOf(1 to 1, 2 to 1, 2 to 2, 4 to 1, 4 to 2)
val WidgetTypeNames = listOf("Jam", "Analog", "Foto", "Teks", "Tanggal", "Baterai")

fun newCustomWidget(): CustomWidget = CustomWidget(
    id = System.currentTimeMillis(),
    type = 0,
    cols = 2,
    rows = 1,
    bgColor = WidgetBgColors[0].toArgb(),
    bgAlpha = 0.9f,
    radius = 24,
    textColor = WidgetTextColors[0].toArgb(),
    imagePath = "",
    text = "",
    use24h = true,
    showSeconds = false,
    font = 0
)

@Composable
fun rememberBattery(): State<Pair<Int, Boolean>> {
    val context = LocalContext.current
    return produceState(Pair(0, false)) {
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
}

@Composable
private fun rememberWidgetBitmap(path: String): State<ImageBitmap?> =
    produceState<ImageBitmap?>(null, path) {
        value = if (path.isEmpty()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
            }
        }
    }

@Composable
fun CustomWidgetView(
    widget: CustomWidget,
    cell: Dp,
    gap: Dp,
    edit: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val width = cell * widget.cols + gap * (widget.cols - 1)
    val height = cell * widget.rows + gap * (widget.rows - 1)
    val shape = RoundedCornerShape(widget.radius.dp)
    val textColor = Color(widget.textColor)
    val bitmap by rememberWidgetBitmap(widget.imagePath)
    val image = bitmap

    Box(
        modifier
            .size(width, height)
            .clip(shape)
            .background(Color(widget.bgColor).copy(alpha = widget.bgAlpha))
            .then(if (edit) Modifier.clickable { onEdit() } else Modifier)
    ) {
        if (image != null) {
            Image(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            if (widget.type != 2) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.30f)))
            }
        }
        Box(Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.Center) {
            WidgetContent(widget, width - 16.dp, height - 16.dp, textColor)
        }
        if (edit) {
            Row(Modifier.align(Alignment.TopEnd).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RoundAction("✎", ThemeAccent, onEdit)
                RoundAction("✕", Color(0xFFFF453A), onDelete)
            }
        }
    }
}

@Composable
private fun RoundAction(symbol: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(color)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun WidgetContent(widget: CustomWidget, w: Dp, h: Dp, color: Color) {
    when (widget.type) {
        0 -> DigitalContent(widget, w, h, color)
        1 -> AnalogClock(
            modifier = Modifier.size(minOf(w, h)),
            color = color,
            accent = ThemeAccent,
            showSeconds = widget.showSeconds
        )
        2 -> if (widget.text.isNotBlank()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomStart) {
                Text(
                    widget.text,
                    color = color,
                    fontSize = (minOf(w.value, h.value) * 0.14f).coerceIn(11f, 22f).sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )
            }
        }
        3 -> Text(
            widget.text.ifBlank { "Ketuk edit untuk mengisi teks" },
            color = color,
            fontSize = (minOf(w.value, h.value) * 0.17f).coerceIn(12f, 28f).sp,
            fontFamily = clockFontFamily(widget.font),
            textAlign = TextAlign.Center,
            maxLines = 6
        )
        4 -> DateContent(w, h, color)
        else -> BatteryContent(w, h, color)
    }
}

@Composable
private fun DigitalContent(widget: CustomWidget, w: Dp, h: Dp, color: Color) {
    val now by rememberNow(widget.showSeconds)
    val hour = if (widget.use24h) "%02d".format(now.hour) else (now.hour % 12).let { if (it == 0) 12 else it }.toString()
    val minute = "%02d".format(now.minute)
    val family = clockFontFamily(widget.font)
    val stacked = widget.cols <= 2 && (widget.rows >= 2 || widget.cols == 1)
    val showDate = widget.rows >= 2 || widget.cols >= 4
    val date = now.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()))
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        if (stacked) {
            val size = minOf(w.value * 0.46f, h.value * (if (showDate) 0.30f else 0.40f))
            Text(
                hour,
                color = color,
                fontSize = size.sp,
                lineHeight = (size * 0.95f).sp,
                fontFamily = family,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                minute,
                color = color.copy(alpha = 0.75f),
                fontSize = size.sp,
                lineHeight = (size * 0.95f).sp,
                fontFamily = family,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        } else {
            val size = minOf(w.value * 0.2f, h.value * (if (showDate) 0.42f else 0.55f))
            Text(
                "$hour:$minute",
                color = color,
                fontSize = size.sp,
                fontFamily = family,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false
            )
        }
        if (showDate) {
            Text(
                date,
                color = color.copy(alpha = 0.8f),
                fontSize = minOf(w.value * 0.07f, 15f).sp,
                fontFamily = family,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DateContent(w: Dp, h: Dp, color: Color) {
    val now by rememberNow(false)
    val base = minOf(w.value, h.value)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(
            now.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault())).uppercase(),
            color = ThemeAccent,
            fontSize = (base * 0.15f).coerceAtLeast(10f).sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            now.dayOfMonth.toString(),
            color = color,
            fontSize = (base * 0.5f).sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        if (h > 100.dp) {
            Text(
                now.format(DateTimeFormatter.ofPattern("MMMM", Locale.getDefault())),
                color = color.copy(alpha = 0.8f),
                fontSize = (base * 0.13f).coerceAtLeast(10f).sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun BatteryContent(w: Dp, h: Dp, color: Color) {
    val battery by rememberBattery()
    val sweep by animateFloatAsState(battery.first / 100f, spring(stiffness = 60f), label = "widgetBattery")
    val ring = minOf(w, h) * 0.86f
    Box(Modifier.size(ring), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.12f
            val arc = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(stroke / 2f, stroke / 2f)
            drawArc(color.copy(alpha = 0.2f), -90f, 360f, false, topLeft = topLeft, size = arc, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(color, -90f, 360f * sweep, false, topLeft = topLeft, size = arc, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Text(
            "${battery.first}%",
            color = color,
            fontSize = (ring.value * 0.26f).sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
fun WidgetEditor(
    initial: CustomWidget,
    isNew: Boolean,
    onSave: (CustomWidget) -> Unit,
    onCancel: () -> Unit,
    onPickImage: ((String?) -> Unit) -> Unit
) {
    var w by remember(initial.id) { mutableStateOf(initial) }
    val sizeIndex = WidgetSizes.indexOf(w.cols to w.rows).coerceAtLeast(0)
    val bgIndex = WidgetBgColors.indexOfFirst { it.toArgb() == w.bgColor }
    val textIndex = WidgetTextColors.indexOfFirst { it.toArgb() == w.textColor }

    Column(
        Modifier
            .fillMaxSize()
            .background(ThemeOverlay)
            .pointerInput(Unit) { detectTapGestures { } }
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 18.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (isNew) "Widget baru" else "Edit widget",
                color = ThemeText,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            GlassButton("Batal", onClick = onCancel)
            Spacer(Modifier.width(8.dp))
            GlassButton("Simpan", accent = true) { onSave(w) }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.CenterStart) {
                CustomWidgetView(w, 70.dp, 10.dp, edit = false, onEdit = {}, onDelete = {})
            }
            Section("Jenis") {
                ChipRow("Tipe", WidgetTypeNames, w.type) { w = w.copy(type = it) }
                ChipRow("Ukuran", WidgetSizes.map { "${it.first}x${it.second}" }, sizeIndex) {
                    w = w.copy(cols = WidgetSizes[it].first, rows = WidgetSizes[it].second)
                }
            }
            Section("Tampilan") {
                ColorRow("Warna latar", WidgetBgColors, bgIndex) { w = w.copy(bgColor = WidgetBgColors[it].toArgb()) }
                SliderRow("Transparansi latar", w.bgAlpha, 0f..1f, "${(w.bgAlpha * 100).toInt()}%") {
                    w = w.copy(bgAlpha = it)
                }
                SliderRow("Sudut", w.radius.toFloat(), 0f..44f, "${w.radius}dp") { w = w.copy(radius = it.toInt()) }
                ColorRow("Warna teks", WidgetTextColors, textIndex) { w = w.copy(textColor = WidgetTextColors[it].toArgb()) }
            }
            if (w.type == 0 || w.type == 1) {
                Section("Jam") {
                    if (w.type == 0) {
                        ChipRow("Font", ClockFontNames, w.font) { w = w.copy(font = it) }
                        SwitchRow("Format 24 jam", w.use24h) { w = w.copy(use24h = it) }
                    }
                    SwitchRow("Tampilkan detik", w.showSeconds) { w = w.copy(showSeconds = it) }
                }
            }
            if (w.type == 3) {
                Section("Font teks") {
                    ChipRow("Font", ClockFontNames, w.font) { w = w.copy(font = it) }
                }
            }
            Section("Gambar") {
                if (w.type == 2 && w.imagePath.isEmpty()) {
                    Text("Pilih gambar untuk widget foto.", color = ThemeTextSoft, fontSize = 14.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassButton("Pilih gambar", accent = true) {
                        onPickImage { path -> if (path != null) w = w.copy(imagePath = path) }
                    }
                    if (w.imagePath.isNotEmpty()) {
                        GlassButton("Hapus gambar") { w = w.copy(imagePath = "") }
                    }
                }
            }
            if (w.type == 2 || w.type == 3) {
                Section(if (w.type == 2) "Keterangan" else "Isi teks") {
                    TextInput(w.text, if (w.type == 2) "Keterangan foto" else "Tulis sesuatu") { w = w.copy(text = it) }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}
