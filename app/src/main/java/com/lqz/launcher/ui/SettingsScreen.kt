package com.lqz.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lqz.launcher.data.LauncherSettings

@Composable
fun SettingsScreen(
    settings: LauncherSettings,
    onChange: ((LauncherSettings) -> LauncherSettings) -> Unit,
    onClose: () -> Unit,
    onWallpaper: () -> Unit,
    onLockNow: () -> Unit
) {
    val accent = AccentColors[settings.accent.coerceIn(0, AccentColors.lastIndex)]
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.42f))
            .pointerInput(Unit) { detectTapGestures { } }
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 18.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Pengaturan",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            GlassButton("Selesai", accent = true, onClick = onClose)
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Section("Jam Beranda") {
                ChipRow("Gaya jam", listOf("Tipis", "Tebal", "Bertumpuk", "Kaca"), settings.homeClockStyle, accent) { i ->
                    onChange { it.copy(homeClockStyle = i) }
                }
                SliderRow("Ukuran", settings.homeClockScale, 0.7f..1.4f, "${(settings.homeClockScale * 100).toInt()}%", accent) { v ->
                    onChange { it.copy(homeClockScale = v) }
                }
                SwitchRow("Format 24 jam", settings.use24h, accent) { v -> onChange { it.copy(use24h = v) } }
                SwitchRow("Tampilkan detik", settings.showSeconds, accent) { v -> onChange { it.copy(showSeconds = v) } }
            }

            Section("Layar Kunci LQZ") {
                SwitchRow("Aktifkan layar kunci", settings.lockEnabled, accent) { v -> onChange { it.copy(lockEnabled = v) } }
                ChipRow("Gaya jam", listOf("Tipis", "Tebal", "Bertumpuk", "Kaca"), settings.lockClockStyle, accent) { i ->
                    onChange { it.copy(lockClockStyle = i) }
                }
                ChipRow("Posisi jam", listOf("Atas", "Tengah"), settings.lockClockPosition, accent) { i ->
                    onChange { it.copy(lockClockPosition = i) }
                }
                ColorRow("Warna jam", LockColors, settings.lockClockColor) { i ->
                    onChange { it.copy(lockClockColor = i) }
                }
                SliderRow("Ukuran", settings.lockClockScale, 0.7f..1.4f, "${(settings.lockClockScale * 100).toInt()}%", accent) { v ->
                    onChange { it.copy(lockClockScale = v) }
                }
                SwitchRow("Tampilkan tanggal", settings.lockShowDate, accent) { v -> onChange { it.copy(lockShowDate = v) } }
                GlassButton("Kunci sekarang", accent = true, onClick = onLockNow)
            }

            Section("Tampilan") {
                SliderRow("Kekuatan kaca", settings.glassOpacity, 0.05f..0.40f, "${(settings.glassOpacity * 100).toInt()}%", accent) { v ->
                    onChange { it.copy(glassOpacity = v) }
                }
                SliderRow("Blur latar (Android 12+)", settings.blurRadius.toFloat(), 0f..120f, "${settings.blurRadius}", accent) { v ->
                    onChange { it.copy(blurRadius = v.toInt()) }
                }
                SliderRow("Ukuran ikon", settings.iconSize.toFloat(), 44f..72f, "${settings.iconSize}dp", accent) { v ->
                    onChange { it.copy(iconSize = v.toInt()) }
                }
                ChipRow("Bentuk ikon", listOf("Squircle", "Bulat", "Kotak"), settings.iconShape, accent) { i ->
                    onChange { it.copy(iconShape = i) }
                }
                ColorRow("Warna aksen", AccentColors, settings.accent) { i ->
                    onChange { it.copy(accent = i) }
                }
            }

            Section("Lainnya") {
                GlassButton("Ganti wallpaper", onClick = onWallpaper)
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .liquidGlass(RoundedCornerShape(28.dp), strength = 0.8f)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            title.uppercase(),
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
        content()
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, accent: Color, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = accent,
                checkedThumbColor = Color.White,
                checkedBorderColor = Color.Transparent,
                uncheckedTrackColor = Color.White.copy(alpha = 0.2f),
                uncheckedThumbColor = Color.White,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueText: String,
    accent: Color,
    onChange: (Float) -> Unit
) {
    Column {
        Row(Modifier.fillMaxWidth()) {
            Text(label, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Text(valueText, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = accent,
                inactiveTrackColor = Color.White.copy(alpha = 0.22f)
            )
        )
    }
}

@Composable
private fun ChipRow(label: String, options: List<String>, selected: Int, accent: Color, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = Color.White, fontSize = 16.sp)
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEachIndexed { i, option ->
                val isSelected = i == selected
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (isSelected) accent else Color.White.copy(alpha = 0.14f))
                        .clickable { onSelect(i) }
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                ) {
                    Text(option, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ColorRow(label: String, colors: List<Color>, selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = Color.White, fontSize = 16.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            colors.forEachIndexed { i, color ->
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(color)
                        .then(
                            if (i == selected) Modifier.border(3.dp, Color.White.copy(alpha = 0.9f), CircleShape)
                            else Modifier.border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        )
                        .clickable { onSelect(i) }
                )
            }
        }
    }
}
