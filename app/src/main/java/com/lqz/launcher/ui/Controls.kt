package com.lqz.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .panel(RoundedCornerShape(26.dp), strength = 0.8f, elevation = 6.dp)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            title.uppercase(),
            color = ThemeAccent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
        content()
    }
}

@Composable
internal fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = ThemeText, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = ThemeAccent,
                checkedThumbColor = Color.White,
                checkedBorderColor = Color.Transparent,
                uncheckedTrackColor = ThemeText.copy(alpha = 0.22f),
                uncheckedThumbColor = Color.White,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
internal fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueText: String,
    onChange: (Float) -> Unit
) {
    Column {
        Row(Modifier.fillMaxWidth()) {
            Text(label, color = ThemeText, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Text(valueText, color = ThemeTextSoft, fontSize = 14.sp)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = ThemeAccent,
                activeTrackColor = ThemeAccent,
                inactiveTrackColor = ThemeText.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
internal fun ChipRow(label: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = ThemeText, fontSize = 16.sp)
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEachIndexed { i, option ->
                val isSelected = i == selected
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (isSelected) ThemeAccent else ThemeText.copy(alpha = 0.12f))
                        .clickable { onSelect(i) }
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                ) {
                    Text(
                        option,
                        color = if (isSelected) Color.White else ThemeText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
internal fun ColorRow(label: String, colors: List<Color>, selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = ThemeText, fontSize = 16.sp)
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            colors.forEachIndexed { i, color ->
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            if (i == selected) 3.dp else 1.dp,
                            if (i == selected) ThemeText else ThemeText.copy(alpha = 0.3f),
                            CircleShape
                        )
                        .clickable { onSelect(i) }
                )
            }
        }
    }
}

@Composable
internal fun TextInput(value: String, placeholder: String, onChange: (String) -> Unit) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        textStyle = TextStyle(color = ThemeText, fontSize = 16.sp),
        cursorBrush = SolidColor(ThemeAccent),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(placeholder, color = ThemeTextSoft, fontSize = 16.sp)
                inner()
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .panel(RoundedCornerShape(16.dp), strength = 0.7f, elevation = 2.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}
