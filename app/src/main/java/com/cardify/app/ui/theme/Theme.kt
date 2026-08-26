package com.cardify.app.ui.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.cardify.app.data.local.ThemeMode

private val ExpressiveDarkColorScheme = darkColorScheme(
    primary = Color(0xFFAEC6FF),
    onPrimary = Color(0xFF002E69),
    primaryContainer = Color(0xFF004494),
    onPrimaryContainer = Color(0xFFD8E2FF),
    secondary = Color(0xFFBFC6DC),
    onSecondary = Color(0xFF293041),
    secondaryContainer = Color(0xFF3F4759),
    onSecondaryContainer = Color(0xFFDBE2F9),
    tertiary = Color(0xFFDFBCDF),
    onTertiary = Color(0xFF402843),
    tertiaryContainer = Color(0xFF583E5B),
    onTertiaryContainer = Color(0xFFFBD7FC),
    background = Color(0xFF0E1015),
    onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF0E1015),
    onSurface = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFF1F232D),
    onSurfaceVariant = Color(0xFFC4C6D0),
    surfaceContainer = Color(0xFF171A21),
    surfaceContainerHigh = Color(0xFF20242D),
    surfaceContainerHighest = Color(0xFF2B303C),
    surfaceContainerLow = Color(0xFF121419),
    surfaceContainerLowest = Color(0xFF090A0D),
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF323744),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val ExpressiveOledColorScheme = darkColorScheme(
    primary = Color(0xFFAEC6FF),
    onPrimary = Color(0xFF002E69),
    primaryContainer = Color(0xFF004494),
    onPrimaryContainer = Color(0xFFD8E2FF),
    secondary = Color(0xFFBFC6DC),
    onSecondary = Color(0xFF293041),
    secondaryContainer = Color(0xFF242730),
    onSecondaryContainer = Color(0xFFDBE2F9),
    tertiary = Color(0xFFDFBCDF),
    onTertiary = Color(0xFF402843),
    tertiaryContainer = Color(0xFF583E5B),
    onTertiaryContainer = Color(0xFFFBD7FC),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF1F2228),
    onSurfaceVariant = Color(0xFFC4C6D0),
    surfaceContainer = Color(0xFF16181D),
    surfaceContainerHigh = Color(0xFF1F2228),
    surfaceContainerHighest = Color(0xFF2B2E36),
    surfaceContainerLow = Color(0xFF101216),
    surfaceContainerLowest = Color(0xFF000000),
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF333742),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val ExpressiveLightColorScheme = lightColorScheme(
    primary = Color(0xFF005AC1),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD8E2FF),
    onPrimaryContainer = Color(0xFF001A41),
    secondary = Color(0xFF435E91),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD9E2FF),
    onSecondaryContainer = Color(0xFF001943),
    tertiary = Color(0xFF764B8D),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF3DAFF),
    onTertiaryContainer = Color(0xFF2E004E),
    background = Color(0xFFF8F9FE),
    onSurface = Color(0xFF191C20),
    surface = Color(0xFFF8F9FE),
    onBackground = Color(0xFF191C20),
    surfaceVariant = Color(0xFFC8D0DC),
    onSurfaceVariant = Color(0xFF1E2128),
    surfaceContainer = Color(0xFFEBEEF6),
    surfaceContainerHigh = Color(0xFFDCE2EE),
    surfaceContainerHighest = Color(0xFFC8D0DC),
    surfaceContainerLow = Color(0xFFF1F4FC),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    outline = Color(0xFF6E727A),
    outlineVariant = Color(0xFFAEB7C6),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private fun getExpressiveDynamicLightColorScheme(context: Context): ColorScheme {
    val dynamic = dynamicLightColorScheme(context)
    val darkerTone = Color(
        red = (dynamic.surfaceVariant.red * 0.85f).coerceIn(0f, 1f),
        green = (dynamic.surfaceVariant.green * 0.85f).coerceIn(0f, 1f),
        blue = (dynamic.surfaceVariant.blue * 0.85f).coerceIn(0f, 1f),
        alpha = 1f
    )
    return dynamic.copy(
        surfaceContainerHighest = darkerTone,
        surfaceVariant = darkerTone
    )
}

@Composable
fun CardifyTheme(
    themeMode: ThemeMode = ThemeMode.AUTO,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.AUTO -> isSystemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.OLED -> true
    }

    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            when (themeMode) {
                ThemeMode.OLED -> {
                    // Dynamic OLED: derive Monet dynamic wallpaper accents while keeping pitch-black surface and dark grey container
                    dynamicDarkColorScheme(context).copy(
                        background = Color(0xFF000000),
                        onBackground = Color(0xFFFFFFFF),
                        surface = Color(0xFF000000),
                        onSurface = Color(0xFFFFFFFF),
                        surfaceVariant = Color(0xFF1F2228),
                        surfaceContainerLowest = Color(0xFF000000),
                        surfaceContainerLow = Color(0xFF101216),
                        surfaceContainer = Color(0xFF16181D),
                        surfaceContainerHigh = Color(0xFF1F2228),
                        surfaceContainerHighest = Color(0xFF2B2E36),
                        outlineVariant = Color(0xFF333742)
                    )
                }
                ThemeMode.LIGHT -> getExpressiveDynamicLightColorScheme(context)
                ThemeMode.DARK -> dynamicDarkColorScheme(context)
                ThemeMode.AUTO -> if (isSystemDark) dynamicDarkColorScheme(context) else getExpressiveDynamicLightColorScheme(context)
            }
        }
        themeMode == ThemeMode.OLED -> ExpressiveOledColorScheme
        isDark -> ExpressiveDarkColorScheme
        else -> ExpressiveLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ExpressiveTypography,
        shapes = ExpressiveShapes,
        content = content
    )
}
