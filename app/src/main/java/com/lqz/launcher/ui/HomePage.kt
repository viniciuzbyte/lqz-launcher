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
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
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
            .padding(horizontal = 26.dp, vertical = 20.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        ClockView(
            style = settings.homeClockStyle,
            scale = settings.homeClockScale,
            use24h = settings.use24h,
            showSeconds = settings.showSeconds,
            color = Color.White,
            showDate = true,
            horizontal = Alignment.Start
        )
        Spacer(Modifier.weight(1f))

        if (favorites.isEmpty()) {
            Text(
                "Tekan lama aplikasi di daftar untuk menambahkan ke favorit",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 15.sp,
                style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.3f), Offset(0f, 2f), 8f))
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            favorites.forEachIndexed { index, app ->
                Row(
                    Modifier
                        .staggerIn(index)
                        .bounceClick(onLongClick = { onAppMenu(app) }) { launchApp(context, app) }
                        .liquidGlass(RoundedCornerShape(28.dp))
                        .defaultMinSize(minWidth = 220.dp)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIcon(app, scale = 0.82f)
                    Spacer(Modifier.width(14.dp))
                    Text(
                        app.label,
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    Spacer(Modifier.width(10.dp))
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                "⌃",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer { translationY = bob }
            )
        }
    }
}
