package com.lqz.launcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lqz.launcher.data.AppEntry
import com.lqz.launcher.data.LauncherSettings
import kotlinx.coroutines.delay

val AccentColors = listOf(
    Color(0xFF0A84FF),
    Color(0xFFBF5AF2),
    Color(0xFFFF375F),
    Color(0xFF30D158),
    Color(0xFFFF9F0A),
    Color(0xFF64D2FF)
)

val LockColors = listOf(
    Color.White,
    Color(0xFFFFD60A),
    Color(0xFF64D2FF),
    Color(0xFFFF9FB2),
    Color(0xFFB4F8C8)
)

val LocalSettings = staticCompositionLocalOf { LauncherSettings() }

fun iconShape(kind: Int): Shape = when (kind) {
    1 -> CircleShape
    2 -> RoundedCornerShape(14)
    else -> RoundedCornerShape(27)
}

fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(28.dp),
    tint: Color = Color.White,
    strength: Float = 1f,
    elevation: Dp = 12.dp
): Modifier = composed {
    val base = LocalSettings.current.glassOpacity * strength
    this
        .shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = 0.35f),
            spotColor = Color.Black.copy(alpha = 0.35f)
        )
        .clip(shape)
        .background(
            Brush.linearGradient(
                listOf(
                    tint.copy(alpha = (base + 0.14f).coerceAtMost(1f)),
                    tint.copy(alpha = (base * 0.55f).coerceAtMost(1f)),
                    tint.copy(alpha = (base + 0.06f).coerceAtMost(1f))
                )
            )
        )
        .drawWithContent {
            drawContent()
            drawRect(
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.30f),
                    0.35f to Color.White.copy(alpha = 0.04f),
                    1f to Color.Transparent
                )
            )
            drawRect(
                Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(size.width * 0.12f, size.height * 0.05f),
                    radius = size.maxDimension * 0.6f
                )
            )
            drawRect(
                Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(size.width * 0.95f, size.height * 1.02f),
                    radius = size.maxDimension * 0.5f
                )
            )
        }
        .border(
            1.dp,
            Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.75f),
                    Color.White.copy(alpha = 0.08f),
                    Color.White.copy(alpha = 0.35f),
                    Color.White.copy(alpha = 0.55f)
                )
            ),
            shape
        )
}

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.bounceClick(onLongClick: (() -> Unit)? = null, onClick: () -> Unit): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "bounce"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .combinedClickable(
            interactionSource = interaction,
            indication = null,
            onLongClick = onLongClick,
            onClick = onClick
        )
}

@Composable
fun Modifier.staggerIn(index: Int): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 50L)
        progress.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 180f))
    }
    return this.graphicsLayer {
        alpha = progress.value.coerceIn(0f, 1f)
        translationY = (1f - progress.value) * 48f
    }
}

@Composable
fun AppIcon(app: AppEntry, modifier: Modifier = Modifier, scale: Float = 1f) {
    val settings = LocalSettings.current
    val shape = iconShape(settings.iconShape)
    Image(
        bitmap = app.icon,
        contentDescription = app.label,
        modifier = modifier
            .size((settings.iconSize * scale).dp)
            .shadow(6.dp, shape, clip = false)
            .clip(shape)
    )
}

@Composable
fun GlassButton(text: String, modifier: Modifier = Modifier, accent: Boolean = false, onClick: () -> Unit) {
    val color = AccentColors[LocalSettings.current.accent.coerceIn(0, AccentColors.lastIndex)]
    Box(
        modifier
            .bounceClick(onClick = onClick)
            .liquidGlass(
                shape = RoundedCornerShape(50),
                tint = if (accent) color else Color.White,
                strength = 0.9f,
                elevation = 6.dp
            )
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun GlassPopup(
    visible: Boolean,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.9f),
        exit = fadeOut() + scaleOut(targetScale = 0.95f)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .pointerInput(Unit) { detectTapGestures { onDismiss() } },
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier
                    .padding(32.dp)
                    .liquidGlass(RoundedCornerShape(30.dp), strength = 1.4f)
                    .pointerInput(Unit) { detectTapGestures { } }
                    .padding(10.dp),
                content = content
            )
        }
    }
}

@Composable
fun PopupAction(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .bounceClick(onClick = onClick)
            .defaultMinSize(minWidth = 240.dp)
            .padding(horizontal = 14.dp, vertical = 13.dp)
    ) {
        Text(text, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium)
    }
}
