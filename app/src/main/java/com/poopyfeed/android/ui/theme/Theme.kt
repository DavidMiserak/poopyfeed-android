package com.poopyfeed.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme =
    lightColorScheme(
        primary = Rose400,
        onPrimary = White,
        primaryContainer = Rose50,
        onPrimaryContainer = Slate800,
        secondary = Orange400,
        onSecondary = White,
        secondaryContainer = Orange50,
        onSecondaryContainer = Slate800,
        tertiary = Amber400,
        onTertiary = White,
        tertiaryContainer = Amber50,
        onTertiaryContainer = Slate800,
        background = White,
        onBackground = Slate800,
        surface = White,
        onSurface = Slate800,
        onSurfaceVariant = Slate600,
        outline = Rose200,
        error = Red500,
        onError = White,
        errorContainer = Red50,
        onErrorContainer = Red700,
    )

private val DarkColorScheme =
    darkColorScheme(
        primary = Rose400,
        onPrimary = White,
        secondary = Orange400,
        onSecondary = White,
        tertiary = Amber400,
        onTertiary = White,
        error = Red500,
        onError = White,
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
        content = content,
    )
}
