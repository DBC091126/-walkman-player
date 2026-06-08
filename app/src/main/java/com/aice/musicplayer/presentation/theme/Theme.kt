package com.aice.musicplayer.presentation.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val WalkmanDarkColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = BlackPure,
    primaryContainer = GoldDark,
    onPrimaryContainer = WhitePure,
    secondary = GoldMuted,
    onSecondary = BlackPure,
    secondaryContainer = BlackElevated,
    onSecondaryContainer = GoldLight,
    tertiary = WhiteSecondary,
    onTertiary = BlackPure,
    background = BlackPure,
    onBackground = WhiteText,
    surface = BlackSurface,
    onSurface = WhiteText,
    surfaceVariant = BlackCard,
    onSurfaceVariant = WhiteSecondary,
    error = RedError,
    onError = BlackPure,
    outline = WhiteMuted
)

@Composable
fun WalkmanPlayerTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = WalkmanDarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = WalkmanTypography,
        content = content
    )
}
