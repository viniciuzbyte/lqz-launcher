package com.lqz.launcher.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lqz.launcher.data.LauncherSettings
import kotlinx.coroutines.launch

@Composable
fun LockScreen(settings: LauncherSettings, onUnlock: () -> Unit) {
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    var heightPx by remember { mutableFloatStateOf(1f) }
    val color = LockColors[settings.lockClockColor.coerceIn(0, LockColors.lastIndex)]
    val hint = rememberInfiniteTransition(label = "lockHint")
    val bob by hint.animateFloat(
        initialValue = 0f,
        targetValue = -12f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "lockBob"
    )

    fun finish(velocity: Float) {
        scope.launch {
            if (-offset.value > heightPx * 0.22f || velocity < -1200f) {
                offset.animateTo(-heightPx, tween(260, easing = FastOutLinearInEasing))
                onUnlock()
            } else {
                offset.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 300f))
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { heightPx = it.height.toFloat().coerceAtLeast(1f) }
            .graphicsLayer {
                translationY = offset.value
                alpha = 1f - (-offset.value / heightPx).coerceIn(0f, 1f) * 0.6f
            }
            .draggable(
                state = rememberDraggableState { d ->
                    scope.launch { offset.snapTo((offset.value + d).coerceAtMost(0f)) }
                },
                orientation = Orientation.Vertical,
                onDragStopped = { velocity -> finish(velocity) }
            )
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xCC0B1020), Color(0x990B1020), Color(0xCC1B0F2E))
                )
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (settings.lockClockPosition == 1) Spacer(Modifier.weight(1f)) else Spacer(Modifier.height(72.dp))
            ClockView(
                style = settings.lockClockStyle,
                scale = settings.lockClockScale,
                use24h = settings.use24h,
                showSeconds = settings.showSeconds,
                color = color,
                showDate = settings.lockShowDate,
                horizontal = Alignment.CenterHorizontally
            )
            Spacer(Modifier.weight(1f))
            Text(
                "⌃",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer { translationY = bob }
            )
            Box(
                Modifier
                    .padding(top = 6.dp, bottom = 28.dp)
                    .liquidGlass(RoundedCornerShape(50), strength = 0.9f, elevation = 6.dp)
                    .padding(horizontal = 22.dp, vertical = 10.dp)
            ) {
                Text(
                    "Geser ke atas untuk membuka",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
