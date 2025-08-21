package com.commerin.telemetri.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// Theme state management
val LocalThemeState = compositionLocalOf<ThemeState> { error("No ThemeState provided") }

data class ThemeState(
    val isDarkTheme: Boolean,
    val toggleTheme: () -> Unit
)

@Composable
fun rememberThemeState(initialDarkTheme: Boolean = isSystemInDarkTheme()): ThemeState {
    var isDarkTheme by remember { mutableStateOf(initialDarkTheme) }

    return ThemeState(
        isDarkTheme = isDarkTheme,
        toggleTheme = { isDarkTheme = !isDarkTheme }
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = DarkMutedBlue,
    secondary = DarkMutedTeal,
    tertiary = DarkMutedOrange,
    background = DarkPastelBackground,
    surface = DarkPastelSurface,
    surfaceVariant = DarkPastelSurfaceVariant,
    error = DarkMutedPink,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color(0xFFE2E8F0),
    onSurface = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFFA0AEC0),
    onError = Color.White,
    primaryContainer = DarkMutedBlue.copy(alpha = 0.3f),
    secondaryContainer = DarkMutedTeal.copy(alpha = 0.3f),
    tertiaryContainer = DarkMutedOrange.copy(alpha = 0.3f),
    errorContainer = DarkMutedPink.copy(alpha = 0.3f),
    onPrimaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = Color(0xFFE2E8F0),
    onTertiaryContainer = Color(0xFFE2E8F0),
    onErrorContainer = Color(0xFFE2E8F0)
)

private val LightColorScheme = lightColorScheme(
    primary = MutedBlue,
    secondary = MutedTeal,
    tertiary = MutedOrange,
    background = LightPastelBackground,
    surface = LightPastelSurface,
    surfaceVariant = LightPastelSurfaceVariant,
    error = MutedPink,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF2D3748),
    onSurface = Color(0xFF2D3748),
    onSurfaceVariant = Color(0xFF4A5568),
    onError = Color.White,
    primaryContainer = MutedBlue.copy(alpha = 0.15f),
    secondaryContainer = MutedTeal.copy(alpha = 0.15f),
    tertiaryContainer = MutedOrange.copy(alpha = 0.15f),
    errorContainer = MutedPink.copy(alpha = 0.15f),
    onPrimaryContainer = Color(0xFF2D3748),
    onSecondaryContainer = Color(0xFF2D3748),
    onTertiaryContainer = Color(0xFF2D3748),
    onErrorContainer = Color(0xFF2D3748)
)

@Composable
fun TelemetriTheme(
    themeState: ThemeState = rememberThemeState(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        themeState.isDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalThemeState provides themeState) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
