package com.heartandbrain.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Background = Color(0xFF0E0E0E)
val Surface = Color(0xFF1C1C1E)
val SurfaceVariant = Color(0xFF252528)
val Primary = Color(0xFF7C5CFC)
val OnBackground = Color(0xFFF2F2F2)
val OnSurface = Color(0xFFE0E0E0)
val Subtle = Color(0xFF888888)

private val DarkColorScheme = darkColorScheme(
    background = Background,
    surface = Surface,
    surfaceVariant = SurfaceVariant,
    primary = Primary,
    onBackground = OnBackground,
    onSurface = OnSurface,
    onPrimary = Color.White,
)

@Composable
fun HeartAndBrainTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content,
    )
}
