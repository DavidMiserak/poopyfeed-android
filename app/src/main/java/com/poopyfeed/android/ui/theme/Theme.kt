package com.poopyfeed.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * Light color scheme aligned with DESIGN_SYSTEM.md.
 * Primary: rose-400. Secondary/tertiary: orange/amber. Background/surface: white with slate text.
 */
private val LightColorScheme =
    lightColorScheme(
        primary = Rose400,
        onPrimary = White,
        primaryContainer = Rose50,
        onPrimaryContainer = Slate900,
        secondary = Orange400,
        onSecondary = White,
        secondaryContainer = Orange50,
        onSecondaryContainer = Slate900,
        tertiary = Amber400,
        onTertiary = White,
        tertiaryContainer = Amber50,
        onTertiaryContainer = Slate900,
        background = Slate50,
        onBackground = Slate900,
        surface = White,
        onSurface = Slate900,
        onSurfaceVariant = Slate600,
        outline = Slate200,
        outlineVariant = Rose200,
        error = Red500,
        onError = White,
        errorContainer = Red50,
        onErrorContainer = Red700,
    )

private val DarkColorScheme =
    darkColorScheme(
        primary = Rose400,
        onPrimary = White,
        primaryContainer = Rose600,
        onPrimaryContainer = White,
        secondary = Orange400,
        onSecondary = White,
        tertiary = Amber400,
        onTertiary = White,
        background = Slate900,
        onBackground = Slate50,
        surface = Slate800,
        onSurface = Slate50,
        onSurfaceVariant = Slate400,
        outline = Slate600,
        error = Red500,
        onError = White,
    )

private val AppMaterialShapes =
    Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = AppShapes.small,
        medium = AppShapes.medium,
        large = AppShapes.large,
        extraLarge = AppShapes.extraLarge,
    )

@Composable
fun PoopyFeedTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppMaterialShapes,
        content = content,
    )
}
