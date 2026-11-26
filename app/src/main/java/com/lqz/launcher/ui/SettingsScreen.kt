package com.lqz.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lqz.launcher.data.LauncherSettings

@Composable
fun SettingsScreen(
    settings: LauncherSettings,
    notifAccess: Boolean,
    onChange: ((LauncherSettings) -> LauncherSettings) -> Unit,
    onClose: () -> Unit,
    onWallpaper: () -> Unit,
    onLockNow: () -> Unit,
    onNotifAccess: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(ThemeOverlay)
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
                color = ThemeText,
                fontSize = 30.sp,
                fontWeight = FontWeight.Light,
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
            Section("Tema") {
                ChipRow("Mode", listOf("Sistem", "Terang", "Gelap"), settings.themeMode) { i ->
                    onChange { it.copy(themeMode = i) }
                }
                ChipRow("Gaya panel", listOf("Solid", "Kaca"), settings.panelStyle) { i ->
                    onChange { it.copy(panelStyle = i) }
                }
                ColorRow("Warna aksen", AccentColors, settings.accent) { i ->
                    onChange { it.copy(accent = i) }
                }
                if (settings.panelStyle == 1) {
                    SliderRow("Kekuatan kaca", settings.glassOpacity, 0.05f..0.40f, "${(settings.glassOpacity * 100).toInt()}%") { v ->
                        onChange { it.copy(glassOpacity = v) }
                    }
                }
                SliderRow("Blur latar (Android 12+)", settings.blurRadius.toFloat(), 0f..120f, "${settings.blurRadius}") { v ->
                    onChange { it.copy(blurRadius = v.toInt()) }
                }
                SliderRow("Ukuran ikon", settings.iconSize.toFloat(), 44f..72f, "${settings.iconSize}dp") { v ->
                    onChange { it.copy(iconSize = v.toInt()) }
                }
                ChipRow("Bentuk ikon", listOf("Squircle", "Bulat", "Kotak"), settings.iconShape) { i ->
                    onChange { it.copy(iconShape = i) }
                }
            }

            Section("Beranda") {
                ChipRow("Tata letak", listOf("Daftar", "Grid", "Minimal"), settings.homeLayout) { i ->
                    onChange { it.copy(homeLayout = i) }
                }
                ChipRow("Perataan", listOf("Kiri", "Kanan", "Tengah"), settings.homeAlign) { i ->
                    onChange { it.copy(homeAlign = i) }
                }
                SwitchRow("Ikon di daftar favorit", settings.showFavoriteIcons) { v ->
                    onChange { it.copy(showFavoriteIcons = v) }
                }
            }

            Section("Jam") {
                ChipRow("Gaya jam", ClockStyleNames, settings.homeClockStyle) { i ->
                    onChange { it.copy(homeClockStyle = i) }
                }
                ChipRow("Font", ClockFontNames, settings.clockFont) { i ->
                    onChange { it.copy(clockFont = i) }
                }
                ColorRow("Warna jam beranda (kosong = otomatis)", ClockColorChoices, settings.homeClockColor) { i ->
                    onChange { it.copy(homeClockColor = i) }
                }
                SliderRow("Ukuran", settings.homeClockScale, 0.7f..1.4f, "${(settings.homeClockScale * 100).toInt()}%") { v ->
                    onChange { it.copy(homeClockScale = v) }
                }
                SwitchRow("Format 24 jam", settings.use24h) { v -> onChange { it.copy(use24h = v) } }
                SwitchRow("Tampilkan detik", settings.showSeconds) { v -> onChange { it.copy(showSeconds = v) } }
            }

            Section("Layar kunci LQZ") {
                SwitchRow("Aktifkan layar kunci", settings.lockEnabled) { v -> onChange { it.copy(lockEnabled = v) } }
                ChipRow("Gaya jam", ClockStyleNames, settings.lockClockStyle) { i ->
                    onChange { it.copy(lockClockStyle = i) }
                }
                ChipRow("Posisi jam", listOf("Atas", "Tengah"), settings.lockClockPosition) { i ->
                    onChange { it.copy(lockClockPosition = i) }
                }
                ColorRow("Warna jam", LockColors, settings.lockClockColor) { i ->
                    onChange { it.copy(lockClockColor = i) }
                }
                SliderRow("Ukuran", settings.lockClockScale, 0.7f..1.4f, "${(settings.lockClockScale * 100).toInt()}%") { v ->
                    onChange { it.copy(lockClockScale = v) }
                }
                SwitchRow("Tampilkan tanggal", settings.lockShowDate) { v -> onChange { it.copy(lockShowDate = v) } }
                SwitchRow("Tampilkan notifikasi", settings.lockShowNotifications) { v ->
                    onChange { it.copy(lockShowNotifications = v) }
                }
                Text(
                    if (notifAccess) "Akses notifikasi: aktif" else "Akses notifikasi: belum diberikan",
                    color = ThemeTextSoft,
                    fontSize = 14.sp
                )
                if (!notifAccess) GlassButton("Berikan akses notifikasi", accent = true, onClick = onNotifAccess)
                GlassButton("Kunci sekarang", onClick = onLockNow)
            }

            Section("AOD (layar redup di layar kunci)") {
                SwitchRow("Aktifkan AOD", settings.aodEnabled) { v -> onChange { it.copy(aodEnabled = v) } }
                SliderRow("Redupkan setelah", settings.aodTimeout.toFloat(), 5f..120f, "${settings.aodTimeout} dtk") { v ->
                    onChange { it.copy(aodTimeout = v.toInt()) }
                }
                SliderRow("Kecerahan AOD", settings.aodBrightness, 0.01f..0.30f, "${(settings.aodBrightness * 100).toInt()}%") { v ->
                    onChange { it.copy(aodBrightness = v) }
                }
                ChipRow("Gaya jam AOD", ClockStyleNames, settings.aodClockStyle) { i ->
                    onChange { it.copy(aodClockStyle = i) }
                }
                SwitchRow("Ikon notifikasi di AOD", settings.aodShowNotifs) { v ->
                    onChange { it.copy(aodShowNotifs = v) }
                }
                Text(
                    "AOD berjalan di dalam launcher: layar tetap menyala dengan kecerahan rendah saat layar kunci LQZ tidak disentuh.",
                    color = ThemeTextSoft,
                    fontSize = 13.sp
                )
            }

            Section("Lainnya") {
                GlassButton("Ganti wallpaper", onClick = onWallpaper)
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}
