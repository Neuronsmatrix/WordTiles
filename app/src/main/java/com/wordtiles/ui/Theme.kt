package com.wordtiles.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF286454), onPrimary = Color.White,
    primaryContainer = Color(0xFFD9EBC2), onPrimaryContainer = Color(0xFF153D30),
    secondary = Color(0xFF736047), secondaryContainer = Color(0xFFF3E7D3),
    background = Color(0xFFF7F8F2), surface = Color(0xFFF7F8F2),
    surfaceContainer = Color(0xFFEDF0E7), surfaceContainerHigh = Color(0xFFE6EBDF),
    onSurface = Color(0xFF202C25), onSurfaceVariant = Color(0xFF5A685E),
    outlineVariant = Color(0xFFD6DDD1),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB6D797), onPrimary = Color(0xFF163E35),
    primaryContainer = Color(0xFF284F40), onPrimaryContainer = Color(0xFFD9EBC2),
    secondary = Color(0xFFD6C1A3), secondaryContainer = Color(0xFF4C4131),
    background = Color(0xFF121B17), surface = Color(0xFF121B17),
    surfaceContainer = Color(0xFF1D2922), surfaceContainerHigh = Color(0xFF28362D),
    onSurface = Color(0xFFE4EADF), onSurfaceVariant = Color(0xFFB6C3B6),
    outlineVariant = Color(0xFF3D4D41),
)

@Composable
fun WordTilesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = Typography(
            displaySmall = TextStyle(fontFamily = FontFamily.Serif, fontSize = 36.sp, lineHeight = 42.sp),
            headlineLarge = TextStyle(fontFamily = FontFamily.Serif, fontSize = 32.sp, lineHeight = 38.sp),
            headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontSize = 28.sp, lineHeight = 34.sp),
            titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),
        ),
        content = content,
    )
}
