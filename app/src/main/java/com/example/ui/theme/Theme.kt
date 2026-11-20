package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val GiyaniDarkColorScheme = darkColorScheme(
    primary = Emerald500,
    onPrimary = Obsidian950,
    primaryContainer = Emerald900,
    onPrimaryContainer = Emerald400,
    secondary = Cyan400,
    onSecondary = Obsidian950,
    secondaryContainer = Cyan900,
    onSecondaryContainer = Cyan400,
    tertiary = Amber400,
    onTertiary = Obsidian950,
    tertiaryContainer = Amber900,
    onTertiaryContainer = Amber400,
    error = Rose400,
    onError = Obsidian950,
    errorContainer = Rose900,
    onErrorContainer = Rose400,
    background = Obsidian950,
    onBackground = Slate100,
    surface = Obsidian900,
    onSurface = Slate100,
    surfaceVariant = Obsidian800,
    onSurfaceVariant = Slate300,
    outline = Obsidian700,
    outlineVariant = Color(0xFF263244)
)

private val GiyaniLightColorScheme = lightColorScheme(
    primary = Emerald600,
    onPrimary = Color.White,
    primaryContainer = Emerald100,
    onPrimaryContainer = Emerald700,
    secondary = Cyan700,
    onSecondary = Color.White,
    secondaryContainer = Cyan100,
    onSecondaryContainer = Cyan700,
    tertiary = Amber700,
    onTertiary = Color.White,
    tertiaryContainer = Amber100,
    onTertiaryContainer = Amber700,
    error = Rose700,
    onError = Color.White,
    errorContainer = Rose100,
    onErrorContainer = Rose700,
    background = DaylightBackground,
    onBackground = DaylightOnBackground,
    surface = DaylightSurface,
    onSurface = DaylightOnBackground,
    surfaceVariant = DaylightSurfaceVariant,
    onSurfaceVariant = DaylightOnSurfaceVariant,
    outline = DaylightOutline,
    outlineVariant = DaylightOutlineVariant
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) GiyaniDarkColorScheme else GiyaniLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
