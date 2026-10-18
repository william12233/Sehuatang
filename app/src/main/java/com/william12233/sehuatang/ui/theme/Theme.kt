package com.william12233.sehuatang.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.william12233.sehuatang.data.model.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF8F4053),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9E0),
    onPrimaryContainer = Color(0xFF3B0718),
    secondary = Color(0xFF76565D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9E0),
    onSecondaryContainer = Color(0xFF2C151B),
    tertiary = Color(0xFF795831),
    background = Color(0xFFFFF8F8),
    surface = Color(0xFFFFF8F8),
    surfaceVariant = Color(0xFFF4DDE1),
    onSurfaceVariant = Color(0xFF514348),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB1C1),
    onPrimary = Color(0xFF570D27),
    primaryContainer = Color(0xFF722D42),
    onPrimaryContainer = Color(0xFFFFD9E0),
    secondary = Color(0xFFE5BFC5),
    onSecondary = Color(0xFF432930),
    secondaryContainer = Color(0xFF5C3F46),
    onSecondaryContainer = Color(0xFFFFD9E0),
    tertiary = Color(0xFFE9C18E),
    background = Color(0xFF1A1113),
    surface = Color(0xFF1A1113),
    surfaceVariant = Color(0xFF514348),
    onSurfaceVariant = Color(0xFFD8C2C6),
)

@Composable
fun SehuatangTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(),
        content = content,
    )
}
