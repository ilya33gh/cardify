package com.cardify.app.ui.theme

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Window width & height classification
 */
enum class WindowType {
    COMPACT,
    MEDIUM,
    EXPANDED
}

/**
 * Comprehensive Window Size & Adaptive Form-Factor Info
 */
data class WindowSizeInfo(
    val widthType: WindowType,
    val heightType: WindowType,
    val screenWidthDp: Dp,
    val screenHeightDp: Dp,
    val isLandscape: Boolean,
    val isFoldableUnfolded: Boolean,
    val isTablet: Boolean
) {
    val isWideScreen: Boolean
        get() = isTablet || isFoldableUnfolded || (isLandscape && screenWidthDp >= 600.dp)

    /**
     * Recommended outer horizontal margins based on form-factor
     */
    val horizontalPadding: Dp
        get() = when {
            isTablet -> 32.dp
            isFoldableUnfolded -> 24.dp
            isLandscape -> 24.dp
            else -> 18.dp
        }

    /**
     * Recommended maximum width for single-column form content or centered sheets
     */
    val maxFormWidth: Dp
        get() = when {
            isTablet -> 720.dp
            isFoldableUnfolded -> 620.dp
            else -> Dp.Unspecified
        }

    /**
     * Optimal number of columns for grid layouts
     */
    fun getAdaptiveGridColumns(isFullCardMode: Boolean = false): Int {
        return when {
            isTablet && isLandscape -> if (isFullCardMode) 3 else 4
            isTablet || isFoldableUnfolded -> if (isFullCardMode) 2 else 3
            isLandscape -> if (isFullCardMode) 2 else 3
            else -> if (isFullCardMode) 1 else 2
        }
    }
}

val LocalWindowSizeInfo: ProvidableCompositionLocal<WindowSizeInfo> = staticCompositionLocalOf {
    WindowSizeInfo(
        widthType = WindowType.COMPACT,
        heightType = WindowType.MEDIUM,
        screenWidthDp = 360.dp,
        screenHeightDp = 800.dp,
        isLandscape = false,
        isFoldableUnfolded = false,
        isTablet = false
    )
}

/**
 * Convenience accessor for current WindowSizeInfo
 */
val MaterialThemeAdaptive: WindowSizeInfo
    @Composable
    @ReadOnlyComposable
    get() = LocalWindowSizeInfo.current

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun rememberWindowSizeInfo(): WindowSizeInfo {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val activity = context as? Activity

    val windowSizeClass = if (activity != null) {
        calculateWindowSizeClass(activity)
    } else {
        null
    }

    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val widthType = when (windowSizeClass?.widthSizeClass) {
        WindowWidthSizeClass.Compact -> WindowType.COMPACT
        WindowWidthSizeClass.Medium -> WindowType.MEDIUM
        WindowWidthSizeClass.Expanded -> WindowType.EXPANDED
        else -> when {
            screenWidth < 600.dp -> WindowType.COMPACT
            screenWidth < 840.dp -> WindowType.MEDIUM
            else -> WindowType.EXPANDED
        }
    }

    val heightType = when (windowSizeClass?.heightSizeClass) {
        WindowHeightSizeClass.Compact -> WindowType.COMPACT
        WindowHeightSizeClass.Medium -> WindowType.MEDIUM
        WindowHeightSizeClass.Expanded -> WindowType.EXPANDED
        else -> when {
            screenHeight < 480.dp -> WindowType.COMPACT
            screenHeight < 900.dp -> WindowType.MEDIUM
            else -> WindowType.EXPANDED
        }
    }

    // Aspect ratio calculation for Foldable detection (e.g. Galaxy Fold unfolded ~4:3 or ~1:1 aspect ratio, min width 580dp)
    val aspectRatio = if (screenHeight.value > 0) screenWidth.value / screenHeight.value else 1f
    val isFoldableUnfolded = (widthType == WindowType.MEDIUM && heightType != WindowType.COMPACT) ||
            (screenWidth >= 580.dp && screenWidth < 900.dp && aspectRatio in 0.72f..1.38f)

    val isTablet = widthType == WindowType.EXPANDED || (widthType == WindowType.MEDIUM && isLandscape && screenWidth >= 700.dp)

    return remember(widthType, heightType, screenWidth, screenHeight, isLandscape, isFoldableUnfolded, isTablet) {
        WindowSizeInfo(
            widthType = widthType,
            heightType = heightType,
            screenWidthDp = screenWidth,
            screenHeightDp = screenHeight,
            isLandscape = isLandscape,
            isFoldableUnfolded = isFoldableUnfolded,
            isTablet = isTablet
        )
    }
}

/**
 * Modifier helper to constrain content width on wide screens and center it
 */
fun Modifier.responsiveContentWidth(maxWidth: Dp = 720.dp): Modifier = this
    .widthIn(max = maxWidth)
    .wrapContentWidth(Alignment.CenterHorizontally)
