package br.com.joaoduartemariucio.kmais_app_android.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors =
    lightColorScheme(
        primary = Color(0xFF007AFF),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD6E8FF),
        onPrimaryContainer = Color(0xFF001C3B),
        secondary = Color(0xFF5856D6),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFD6E8FF),
        onSecondaryContainer = Color(0xFF001C3B),
        tertiary = Color(0xFF30B0C7),
        background = Color(0xFFF2F2F7),
        onBackground = Color(0xFF1C1C1E),
        surface = Color(0xFFF2F2F7),
        onSurface = Color(0xFF1C1C1E),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color.White,
        surfaceContainer = Color(0xFFF2F2F7),
        surfaceContainerHigh = Color(0xFFE5E5EA),
        surfaceContainerHighest = Color(0xFFE5E5EA),
        surfaceVariant = Color(0xFFE5E5EA),
        onSurfaceVariant = Color(0xFF6C6C70),
        outline = Color(0xFFC6C6C8),
        outlineVariant = Color(0xFFE5E5EA),
        error = Color(0xFFFF3B30),
    )

private val DarkColors =
    darkColorScheme(
        primary = Color(0xFF0A84FF),
        onPrimary = Color.White,
        primaryContainer = Color(0xFF00315F),
        onPrimaryContainer = Color(0xFFD6E8FF),
        secondary = Color(0xFF5E5CE6),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFF00315F),
        onSecondaryContainer = Color(0xFFD6E8FF),
        tertiary = Color(0xFF40C8E0),
        background = Color.Black,
        onBackground = Color.White,
        surface = Color.Black,
        onSurface = Color.White,
        surfaceContainerLowest = Color(0xFF1C1C1E),
        surfaceContainerLow = Color(0xFF1C1C1E),
        surfaceContainer = Color.Black,
        surfaceContainerHigh = Color(0xFF2C2C2E),
        surfaceContainerHighest = Color(0xFF3A3A3C),
        surfaceVariant = Color(0xFF2C2C2E),
        onSurfaceVariant = Color(0xFF98989F),
        outline = Color(0xFF545458),
        outlineVariant = Color(0xFF38383A),
        error = Color(0xFFFF453A),
    )

@Composable
fun KmaisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
