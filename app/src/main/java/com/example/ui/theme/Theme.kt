package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Gold,
    onPrimary = NavyBlue,
    secondary = GoldenOrange,
    background = DarkBg,
    surface = SurfaceDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    primaryContainer = NavyBlue,
    onPrimaryContainer = Gold,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = TextSecondaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = NavyBlue,
    onPrimary = Color.White,
    secondary = Gold,
    onSecondary = NavyBlue,
    background = LightBg,
    surface = SurfaceLight,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    primaryContainer = Color(0xFFE2E8F0),
    onPrimaryContainer = NavyBlue,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = TextSecondaryLight
)

@Composable
fun NexoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
