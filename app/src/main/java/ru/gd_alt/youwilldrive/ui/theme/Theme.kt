package ru.gd_alt.youwilldrive.ui.theme

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
    primary = BrandYellow80,
    secondary = BrandYellowGrey80,
    tertiary = Orange80,
    primaryContainer = BrandYellow80.copy(alpha = 0.7f),
    surface = Color(0xFF1D1B16),
    surfaceVariant = BrandYellowGrey80.copy(alpha = 0.3f),
    error = Orange80,
    onPrimaryContainer = Color.Black,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = BrandYellow40,
    secondary = BrandYellowGrey40,
    tertiary = Orange40,
    primaryContainer = BrandYellow40.copy(alpha = 0.7f),
    surface = Color(0xFFFFFDF5),
    surfaceVariant = BrandYellowGrey40.copy(alpha = 0.5f),
    error = Orange40,
    onPrimaryContainer = Color.Black,
    onSurface = Color.Black
)

@Composable
fun YouWillDriveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}