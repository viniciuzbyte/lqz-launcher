package com.lqz.launcher.ui

import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

val ClockFontNames = listOf("Standar", "Serif", "Mono", "Tulisan", "Ramping")
val ClockStyleNames = listOf("Tipis", "Tebal", "Bertumpuk", "Besar", "Analog")

fun clockFontFamily(kind: Int): FontFamily = when (kind) {
    1 -> FontFamily.Serif
    2 -> FontFamily.Monospace
    3 -> FontFamily.Cursive
    4 -> FontFamily(Typeface.create("sans-serif-condensed", Typeface.NORMAL))
    else -> FontFamily.SansSerif
}

@Composable
fun rememberNow(seconds: Boolean): State<LocalDateTime> = produceState(LocalDateTime.now(), seconds) {
    val step = if (seconds) 1000L else 60000L
    while (true) {
        value = LocalDateTime.now()
        delay(step - System.currentTimeMillis() % step + 5)
    }
}

@Composable
fun AnalogClock(modifier: Modifier, color: Color, accent: Color, showSeconds: Boolean) {
    val now by rememberNow(showSeconds)
    val hour = now.hour % 12
    val minute = now.minute
    val second = if (showSeconds) now.second else 0
    Canvas(modifier) {
        val radius = size.minDimension / 2f
        val c = center
        for (i in 0 until 12) {
            val angle = Math.toRadians(i * 30.0 - 90.0)
            val major = i % 3 == 0
            val outer = radius * 0.94f
            val inner = outer - radius * (if (major) 0.14f else 0.07f)
            drawLine(
                color = color.copy(alpha = if (major) 0.9f else 0.45f),
                start = Offset(c.x + cos(angle).toFloat() * inner, c.y + sin(angle).toFloat() * inner),
                end = Offset(c.x + cos(angle).toFloat() * outer, c.y + sin(angle).toFloat() * outer),
                strokeWidth = radius * (if (major) 0.045f else 0.022f),
                cap = StrokeCap.Round
            )
        }
        fun hand(degrees: Double, length: Float, width: Float, tint: Color) {
            val a = Math.toRadians(degrees - 90.0)
            drawLine(
                color = tint,
                start = c,
                end = Offset(c.x + cos(a).toFloat() * radius * length, c.y + sin(a).toFloat() * radius * length),
                strokeWidth = radius * width,
                cap = StrokeCap.Round
            )
        }
        hand((hour + minute / 60.0) * 30.0, 0.5f, 0.07f, color)
        hand((minute + second / 60.0) * 6.0, 0.76f, 0.045f, color)
        if (showSeconds) hand(second * 6.0, 0.86f, 0.022f, accent)
        drawCircle(accent, radius * 0.06f, c)
    }
}

@Composable
fun ClockView(
    style: Int,
    scale: Float,
    use24h: Boolean,
    showSeconds: Boolean,
    color: Color,
    accent: Color,
    showDate: Boolean,
    horizontal: Alignment.Horizontal,
    font: Int,
    modifier: Modifier = Modifier
) {
    val now by rememberNow(showSeconds && style != 4)
    val hour = if (use24h) "%02d".format(now.hour) else (now.hour % 12).let { if (it == 0) 12 else it }.toString()
    val minute = "%02d".format(now.minute)
    val second = "%02d".format(now.second)
    val date = now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))
    val shadow = Shadow(Color.Black.copy(alpha = 0.22f), Offset(0f, 3f), 12f)
    val family = clockFontFamily(font)
    val k = if (showSeconds) 0.82f else 1f
    val timeText = if (showSeconds) "$hour:$minute:$second" else "$hour:$minute"

    Column(modifier, horizontalAlignment = horizontal) {
        if (style == 3 && showDate) {
            Text(
                date,
                style = TextStyle(
                    color = accent,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = family,
                    shadow = shadow
                )
            )
        }
        when (style) {
            1 -> Text(
                timeText,
                maxLines = 1,
                softWrap = false,
                style = TextStyle(
                    color = color,
                    fontSize = (66f * scale * k).sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = family,
                    letterSpacing = (-1).sp,
                    shadow = shadow
                )
            )
            2 -> {
                val size = (104f * scale * k).sp
                Text(
                    hour,
                    style = TextStyle(
                        color = color,
                        fontSize = size,
                        lineHeight = size * 0.92f,
                        fontWeight = FontWeight.W300,
                        fontFamily = family,
                        shadow = shadow
                    )
                )
                Text(
                    minute,
                    style = TextStyle(
                        color = color.copy(alpha = 0.7f),
                        fontSize = size,
                        lineHeight = size * 0.92f,
                        fontWeight = FontWeight.W300,
                        fontFamily = family,
                        shadow = shadow
                    )
                )
                if (showSeconds) {
                    Text(
                        second,
                        style = TextStyle(
                            color = color.copy(alpha = 0.5f),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Light,
                            fontFamily = family
                        )
                    )
                }
            }
            3 -> Text(
                timeText,
                maxLines = 1,
                softWrap = false,
                style = TextStyle(
                    brush = Brush.verticalGradient(listOf(color, color.copy(alpha = 0.55f))),
                    fontSize = (92f * scale * k).sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = family,
                    letterSpacing = (-2).sp,
                    shadow = shadow
                )
            )
            4 -> AnalogClock(
                modifier = Modifier.size((150f * scale).dp),
                color = color,
                accent = accent,
                showSeconds = showSeconds
            )
            else -> Text(
                timeText,
                maxLines = 1,
                softWrap = false,
                style = TextStyle(
                    color = color,
                    fontSize = (76f * scale * k).sp,
                    fontWeight = FontWeight.W200,
                    fontFamily = family,
                    letterSpacing = (-1).sp,
                    shadow = shadow
                )
            )
        }
        if (style != 3 && showDate) {
            Spacer(Modifier.height(4.dp))
            Text(
                date,
                style = TextStyle(
                    color = accent,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = family,
                    shadow = shadow
                )
            )
        }
    }
}
