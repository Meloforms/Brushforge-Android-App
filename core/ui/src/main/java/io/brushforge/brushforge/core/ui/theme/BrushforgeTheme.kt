package io.brushforge.brushforge.core.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val BrushforgeDarkColorScheme = darkColorScheme(
    primary = Color(0xFFE6C469),
    onPrimary = Color(0xFF1E1300),
    primaryContainer = Color(0xFF3F2D0B),
    onPrimaryContainer = Color(0xFFF9E5B6),
    secondary = Color(0xFF8AA2BA),
    onSecondary = Color(0xFF0E1B2A),
    secondaryContainer = Color(0xFF27415B),
    onSecondaryContainer = Color(0xFFD1E6FF),
    tertiary = Color(0xFFE1A886),
    onTertiary = Color(0xFF3A1902),
    tertiaryContainer = Color(0xFF5A2A16),
    onTertiaryContainer = Color(0xFFFFDCC8),
    background = Color(0xFF0F1017),
    onBackground = Color(0xFFE2E4EF),
    surface = Color(0xFF12131B),
    onSurface = Color(0xFFE2E4EF),
    surfaceVariant = Color(0xFF1F2230),
    onSurfaceVariant = Color(0xFFC1C5D3),
    outline = Color(0xFF444856),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val BrushforgeLightColorScheme = lightColorScheme(
    primary = Color(0xFF3B2A07),
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFFF6F2EA),
    onBackground = Color(0xFF1E1B16),
    surface = Color(0xFFF6F2EA),
    onSurface = Color(0xFF1E1B16)
)

@Composable
fun BrushforgeTheme(
    useDarkTheme: Boolean = true,
    enableDynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        enableDynamicColor && useDarkTheme && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicDarkColorScheme(context)

        enableDynamicColor && !useDarkTheme && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicLightColorScheme(context)

        useDarkTheme -> BrushforgeDarkColorScheme
        else -> BrushforgeLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BrushforgeTypography,
        content = content
    )
}
