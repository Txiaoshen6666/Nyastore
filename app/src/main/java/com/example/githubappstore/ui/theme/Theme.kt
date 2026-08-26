package com.example.githubappstore.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * GitStore theme — Material 3 Expressive.
 *  - minSdk 33, so dynamic (Material You) color is always available on supported devices.
 *  - Pure-black dark mode ([pureBlackDark]=true) forces #FF000000 background for OLED.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GitStoreTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    pureBlackDark: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val baseScheme = if (dynamicColor) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (darkTheme) DarkColorScheme else LightColorScheme
    }

    val colorScheme = if (darkTheme && pureBlackDark) {
        baseScheme.copy(
            background = Color(0xFF000000),
            onBackground = baseScheme.onBackground,
            surface = Color(0xFF050505),
            onSurface = baseScheme.onSurface,
            surfaceVariant = Color(0xFF0F0F12),
            onSurfaceVariant = baseScheme.onSurfaceVariant,
            surfaceContainer = Color(0xFF101013),
            surfaceContainerHigh = Color(0xFF18181C),
            surfaceContainerHighest = Color(0xFF202026)
        )
    } else baseScheme

    MaterialExpressiveTheme(colorScheme = colorScheme, typography = ExpressiveTypography, shapes = MaterialTheme.shapes, content = content)
}

private val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary, onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer, onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary, onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer, onSecondaryContainer = md_theme_light_onSecondaryContainer,
    background = md_theme_light_background, onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface, onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant, onSurfaceVariant = md_theme_light_onSurfaceVariant,
    error = md_theme_light_error
)

private val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary, onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer, onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary, onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer, onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    background = md_theme_dark_background, onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface, onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant, onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    error = md_theme_dark_error
)
