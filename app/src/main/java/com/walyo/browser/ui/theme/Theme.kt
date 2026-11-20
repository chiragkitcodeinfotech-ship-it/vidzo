package com.walyo.browser.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.walyo.browser.data.ThemeMode

object WalyoColors {
    val Violet = Color(0xFF7C5CFF)
    val Blue = Color(0xFF5B7CFF)
    val Cyan = Color(0xFF22D3EE)
    val Amber = Color(0xFFF5A524)
    val Brand: Brush = Brush.linearGradient(listOf(Violet, Blue, Cyan))
}

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9F8BFF),
    onPrimary = Color.White,
    secondary = WalyoColors.Cyan,
    tertiary = WalyoColors.Amber,
    background = Color(0xFF0D0F14),
    surface = Color(0xFF0D0F14),
    surfaceVariant = Color(0xFF1B1E28),
    surfaceContainer = Color(0xFF14171F),
    surfaceContainerHigh = Color(0xFF1E2230),
    onBackground = Color(0xFFECEEF5),
    onSurface = Color(0xFFECEEF5),
    onSurfaceVariant = Color(0xFF9AA0B2),
    outline = Color(0xFF2A2E3B),
    error = Color(0xFFFF6B7A),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF6A4BFF),
    onPrimary = Color.White,
    secondary = Color(0xFF0891B2),
    tertiary = Color(0xFFD9870B),
    background = Color(0xFFF5F6FA),
    surface = Color(0xFFF5F6FA),
    surfaceVariant = Color(0xFFE8EAF2),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFEEF0F6),
    onBackground = Color(0xFF12141C),
    onSurface = Color(0xFF12141C),
    onSurfaceVariant = Color(0xFF5B6172),
    outline = Color(0xFFD7DAE4),
    error = Color(0xFFD92D43),
)

private val WalyoTypography = Typography().run {
    copy(
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun isDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun WalyoTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = WalyoTypography,
    ) {
        // Default color for icons and text that don't set one, so they follow light/dark mode.
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground, content = content)
    }
}
