package com.lqz.launcher.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lqz.launcher.data.AppEntry
import com.lqz.launcher.data.LauncherSettings
import com.lqz.launcher.data.launchApp

@Composable
fun HomePage(
    settings: LauncherSettings,
    favorites: List<AppEntry>,
    onDrag: (Float) -> Unit,
    onDragStopped: (Float) -> Unit,
    onLongPress: () -> Unit,
    onAppMenu: (AppEntry) -> Unit
) {
    val context = LocalContext.current
    val palette = LocalPalette.current
    val horizontal = when (settings.homeAlign) {
        1 -> Alignment.End
        2 -> Alignment.CenterHorizontally
        else -> Alignment.Start
    }
    val clockColor = if (settings.homeClockColor == 0) {
        palette.text
    } else {
        ClockColorChoices[settings.homeClockColor.coerceIn(1, ClockColorChoices.lastIndex)]
    }
    val hint = rememberInfiniteTransition(label = "hint")
    val bob by hint.animateFloat(
        initialValue = 0f,
        targetValue = -14f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob"
    )

    Column(
        Modifier
            .fillMaxSize()
            .draggable(
                state = rememberDraggableState { onDrag(it) },
                orientation = Orientation.Vertical,
                onDragStopped = { velocity -> onDragStopped(velocity) }
            )
            .pointerInput(Unit) { detectTapGestures(onLongPress = { onLongPress() }) }
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 28.dp, vertical = 20.dp),
        horizontalAlignment = horizontal
    ) {
        if (settings.homeLayout == 2) Spacer(Modifier.weight(1f)) else Spacer(Modifier.height(24.dp))

        ClockView(
            style = settings.homeClockStyle,
            scale = settings.homeClockScale,
            use24h = settings.use24h,
            showSeconds = settings.showSeconds,
            color = clockColor,
            accent = palette.accent,
            showDate = true,
            horizontal = horizontal,
            font = settings.clockFont
        )

        Spacer(Modifier.weight(1f))

        when (settings.homeLayout) {
            1 -> favorites.chunked(4).forEachIndexed { rowIndex, rowApps ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    rowApps.forEachIndexed { i, app ->
                        Column(
                            Modifier
                                .weight(1f)
                                .staggerIn(rowIndex * 4 + i)
                                .bounceClick(onLongClick = { onAppMenu(app) }) { launchApp(context, app) },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AppIcon(app)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                app.label,
                                color = ThemeText,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                    repeat(4 - rowApps.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            2 -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                favorites.take(4).forEachIndexed { index, app ->
                    Box(
                        Modifier
                            .staggerIn(index)
                            .bounceClick(onLongClick = { onAppMenu(app) }) { launchApp(context, app) }
                    ) {
                        AppIcon(app)
                    }
                }
            }
            else -> {
                if (favorites.isEmpty()) {
                    Text(
                        "Tekan lama aplikasi di daftar untuk menambahkan ke favorit",
                        color = ThemeTextSoft,
                        fontSize = 15.sp
                    )
                }
                favorites.forEachIndexed { index, app ->
                    val iconFirst = settings.homeAlign != 1
                    Row(
                        Modifier
                            .staggerIn(index)
                            .bounceClick(onLongClick = { onAppMenu(app) }) { launchApp(context, app) }
                            .padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (settings.showFavoriteIcons && iconFirst) {
                            AppIcon(app, scale = 0.62f)
                            Spacer(Modifier.width(16.dp))
                        }
                        Text(
                            app.label,
                            color = ThemeText,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Light,
                            maxLines = 1
                        )
                        if (settings.showFavoriteIcons && !iconFirst) {
                            Spacer(Modifier.width(16.dp))
                            AppIcon(app, scale = 0.62f)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                "⌃",
                color = palette.accent,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer { translationY = bob }
            )
        }
    }
}
