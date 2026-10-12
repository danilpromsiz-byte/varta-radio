package com.safecomm.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = TelegramBlue,
    onPrimary = TelegramTextPrimary,
    primaryContainer = TelegramDarkSurface,
    onPrimaryContainer = TelegramTextPrimary,
    secondary = AlertFireOrange,
    onSecondary = TelegramTextPrimary,
    background = TelegramDarkBg,
    onBackground = TelegramTextPrimary,
    surface = TelegramDarkSurface,
    onSurface = TelegramTextPrimary,
    surfaceVariant = TelegramDarkCard,
    onSurfaceVariant = TelegramTextSecondary,
    error = AlertShellingRed,
    onError = TelegramTextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = TelegramBlueDark,
    onPrimary = TelegramTextPrimary,
    primaryContainer = TelegramDarkSurface,
    onPrimaryContainer = TelegramTextPrimary,
    secondary = AlertFireOrange,
    background = TelegramDarkBg,
    surface = TelegramDarkSurface,
    error = AlertShellingRed
)

@Composable
fun SafeCommTheme(
    darkTheme: Boolean = true, // Default to Telegram dark theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = TelegramDarkBg.toArgb()
            window.navigationBarColor = TelegramDarkBg.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
