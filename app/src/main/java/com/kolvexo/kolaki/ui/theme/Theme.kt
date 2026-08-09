package com.kolvexo.kolaki.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.kolvexo.kolaki.data.ThemeMode

private val LightColors = lightColorScheme(
    primary = Coral,
    onPrimary = Branco,
    secondary = Lime,
    onSecondary = Preto,
    tertiary = Roxo,
    background = OffWhite,
    onBackground = Preto,
    surface = Branco,
    onSurface = Preto,
    surfaceVariant = CinzaClaro,
    onSurfaceVariant = CinzaCafe,
    error = Erro,
)

private val DarkColors = darkColorScheme(
    primary = CoralDark,
    onPrimary = Branco,
    secondary = Lime,
    onSecondary = Preto,
    tertiary = Roxo,
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = DarkSecondary,
    error = Erro,
)

@Composable
fun KolaKiTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colors = if (dark) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = KolaKiTypography,
        content = content,
    )
}
