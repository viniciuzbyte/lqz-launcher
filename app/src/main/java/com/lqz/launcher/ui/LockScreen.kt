package com.lqz.launcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lqz.launcher.data.LauncherSettings
import com.lqz.launcher.notif.NotifItem
import com.lqz.launcher.notif.NotificationStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun LockScreen(
    settings: LauncherSettings,
    notifications: List<NotifItem>,
    notifAccess: Boolean,
    onUnlock: () -> Unit,
    onRequestAccess: () -> Unit,
    onDim: (Float?) -> Unit
) {
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    var heightPx by remember { mutableFloatStateOf(1f) }
    var aod by remember { mutableStateOf(false) }
    var lastTouch by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val palette = LocalPalette.current
    val color = LockColors[settings.lockClockColor.coerceIn(0, LockColors.lastIndex)]
    val hint = rememberInfiniteTransition(label = "lockHint")
    val bob by hint.animateFloat(
        initialValue = 0f,
        targetValue = -12f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "lockBob"
    )

    LaunchedEffect(settings.aodEnabled, settings.aodTimeout, lastTouch, aod) {
        if (settings.aodEnabled && !aod) {
            delay(settings.aodTimeout * 1000L)
            aod = true
        }
    }
    LaunchedEffect(aod, settings.aodBrightness) {
        onDim(if (aod) settings.aodBrightness else null)
    }
    DisposableEffect(Unit) {
        onDispose { onDim(null) }
    }

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
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    lastTouch = System.currentTimeMillis()
                }
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
                    listOf(Color(0xE60B1020), Color(0xB30B1020), Color(0xE61B0F2E))
                )
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (settings.lockClockPosition == 1) Spacer(Modifier.weight(1f)) else Spacer(Modifier.height(64.dp))
            ClockView(
                style = settings.lockClockStyle,
                scale = settings.lockClockScale,
                use24h = settings.use24h,
                showSeconds = settings.showSeconds,
                color = color,
                accent = palette.accent,
                showDate = settings.lockShowDate,
                horizontal = Alignment.CenterHorizontally,
                font = settings.clockFont
            )
            Spacer(Modifier.height(20.dp))
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                if (settings.lockShowNotifications) {
                    if (!notifAccess) {
                        GlassButton("Izinkan akses notifikasi", accent = true, onClick = onRequestAccess)
                    } else if (notifications.isNotEmpty()) {
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            notifications.take(3).forEach { item ->
                                NotificationCard(
                                    item = item,
                                    onOpen = {
                                        runCatching { item.intent?.send() }
                                        onUnlock()
                                    },
                                    onDismiss = { NotificationStore.dismiss(item.key) }
                                )
                            }
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                if (notifications.size > 3) {
                                    Text(
                                        "+${notifications.size - 3} notifikasi lainnya",
                                        color = ThemeTextSoft,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(Modifier.weight(1f))
                                }
                                Text(
                                    "Hapus semua",
                                    color = ThemeAccent,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clickable { NotificationStore.dismissAll() }
                                        .padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
            Text(
                "⌃",
                color = ThemeText.copy(alpha = 0.8f),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer { translationY = bob }
            )
            Text(
                "Geser ke atas untuk membuka",
                color = ThemeTextSoft,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
            )
        }

        AnimatedVisibility(
            visible = aod,
            enter = fadeIn(tween(600)),
            exit = fadeOut(tween(150))
        ) {
            AodLayer(
                settings = settings,
                notifications = if (settings.aodShowNotifs && settings.lockShowNotifications) notifications else emptyList(),
                onWake = {
                    aod = false
                    lastTouch = System.currentTimeMillis()
                }
            )
        }
    }
}

@Composable
private fun NotificationCard(item: NotifItem, onOpen: () -> Unit, onDismiss: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .bounceClick(onClick = onOpen)
            .panel(RoundedCornerShape(20.dp), strength = 1f, elevation = 4.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = item.icon
        if (icon != null) {
            Image(icon, item.appName, Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)))
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(item.appName, color = ThemeTextSoft, fontSize = 11.sp, maxLines = 1)
            if (item.title.isNotBlank()) {
                Text(item.title, color = ThemeText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
            if (item.text.isNotBlank()) {
                Text(item.text, color = ThemeTextSoft, fontSize = 14.sp, maxLines = 2)
            }
        }
        Text(
            "✕",
            color = ThemeTextSoft,
            fontSize = 16.sp,
            modifier = Modifier
                .clickable { onDismiss() }
                .padding(8.dp)
        )
    }
}

@Composable
private fun AodLayer(settings: LauncherSettings, notifications: List<NotifItem>, onWake: () -> Unit) {
    val now by rememberNow(false)
    val dx = remember(now.minute) { Random.nextInt(-28, 29) }
    val dy = remember(now.minute) { Random.nextInt(-48, 49) }
    val gray = remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    onWake()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(Modifier.offset(dx.dp, dy.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ClockView(
                style = settings.aodClockStyle,
                scale = 0.9f,
                use24h = settings.use24h,
                showSeconds = false,
                color = Color.White.copy(alpha = 0.82f),
                accent = Color.White.copy(alpha = 0.6f),
                showDate = true,
                horizontal = Alignment.CenterHorizontally,
                font = settings.clockFont
            )
            if (notifications.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    notifications.take(6).forEach { item ->
                        val icon = item.icon
                        if (icon != null) {
                            Image(
                                icon,
                                null,
                                Modifier.size(26.dp).clip(CircleShape),
                                alpha = 0.7f,
                                colorFilter = gray
                            )
                        }
                    }
                }
            }
        }
    }
}
