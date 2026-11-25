package com.lqz.launcher.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun rememberNow(seconds: Boolean): State<LocalDateTime> = produceState(LocalDateTime.now(), seconds) {
    val step = if (seconds) 1000L else 60000L
    while (true) {
        value = LocalDateTime.now()
        delay(step - System.currentTimeMillis() % step + 5)
    }
}

@Composable
fun ClockView(
    style: Int,
    scale: Float,
    use24h: Boolean,
    showSeconds: Boolean,
    color: Color,
    showDate: Boolean,
    horizontal: Alignment.Horizontal,
    modifier: Modifier = Modifier
) {
    val now by rememberNow(showSeconds)
    val hour = if (use24h) "%02d".format(now.hour) else (now.hour % 12).let { if (it == 0) 12 else it }.toString()
    val minute = "%02d".format(now.minute)
    val second = "%02d".format(now.second)
    val date = now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))
    val shadow = Shadow(Color.Black.copy(alpha = 0.28f), Offset(0f, 4f), 16f)
    val k = if (showSeconds) 0.82f else 1f
    val timeText = if (showSeconds) "$hour:$minute:$second" else "$hour:$minute"

    Column(modifier, horizontalAlignment = horizontal) {
        if (style == 3 && showDate) {
            Text(
                date,
                style = TextStyle(
                    color = color.copy(alpha = 0.88f),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
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
                        shadow = shadow
                    )
                )
                if (showSeconds) {
                    Text(
                        second,
                        style = TextStyle(color = color.copy(alpha = 0.5f), fontSize = 28.sp, fontWeight = FontWeight.Light)
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
                    letterSpacing = (-2).sp,
                    shadow = shadow
                )
            )
            else -> Text(
                timeText,
                maxLines = 1,
                softWrap = false,
                style = TextStyle(
                    color = color,
                    fontSize = (76f * scale * k).sp,
                    fontWeight = FontWeight.W200,
                    letterSpacing = (-1).sp,
                    shadow = shadow
                )
            )
        }
        if (style != 3 && showDate) {
            Spacer(Modifier.height(2.dp))
            Text(
                date,
                style = TextStyle(
                    color = color.copy(alpha = 0.85f),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    shadow = shadow
                )
            )
        }
    }
}
