package com.cardify.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.cardify.app.ui.theme.OnestFamily

/**
 * Material 3 Expressive Large Collapsing Header with 120fps fluid morphing.
 *
 * Expanded state:
 * - Back button & Actions at top row (56.dp).
 * - Title (34.sp, FontWeight.Black) strictly centered vertically between back button and content.
 *
 * Collapsed state:
 * - Top bar has height (56.dp + statusBarHeight) with surface background.
 * - Back button in distinct circle with subtle border so it never blends with the background.
 * - Title (20.sp, FontWeight.Bold) aligned strictly on the same vertical center line as the back button.
 */
@Composable
fun M3ExpressiveCollapsingHeader(
    title: String,
    onNavigateBack: () -> Unit,
    collapseFraction: Float,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val topBarContentHeight = 56.dp
    val expandedExtraHeight = 88.dp
    val totalHeight = topBarContentHeight + (expandedExtraHeight * (1f - collapseFraction)) + statusBarHeight

    val titleFontSize = (34 - (14 * collapseFraction)).sp
    val titleStartX = (20 + (44 * collapseFraction)).dp

    // In expanded state (t=0): Y = statusBarHeight + 56.dp + (88.dp - 44.dp) / 2 = statusBarHeight + 78.dp
    // In collapsed state (t=1): Y = statusBarHeight + (56.dp - 28.dp) / 2 = statusBarHeight + 14.dp
    val titleTopY = statusBarHeight + (78.dp * (1f - collapseFraction)) + (14.dp * collapseFraction)

    val containerColor = MaterialTheme.colorScheme.surfaceContainer
    val bgAlpha = collapseFraction.coerceIn(0f, 1f)

    // Circle background for back button: smoothly lerped directly with collapseFraction (darker in dark/OLED mode)
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val collapsedCircleColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val backButtonBg = androidx.compose.ui.graphics.lerp(
        MaterialTheme.colorScheme.surfaceContainerHighest,
        collapsedCircleColor,
        collapseFraction
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeight),
        color = containerColor.copy(alpha = bgAlpha),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = null
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = statusBarHeight)
        ) {
            // Distinct Circular Back Button (no shadow, no border)
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .padding(start = 12.dp, top = 6.dp)
                    .size(44.dp)
                    .align(Alignment.TopStart)
            ) {
                Surface(
                    shape = CircleShape,
                    color = backButtonBg,
                    border = null,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Top-Right Actions (e.g. Favorite Heart Button)
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 12.dp, top = 6.dp)
                    .height(44.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                content = actions
            )

            // Interpolated Title
            Text(
                text = title,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = OnestFamily,
                    fontWeight = if (collapseFraction > 0.6f) FontWeight.Bold else FontWeight.Black,
                    fontSize = titleFontSize
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .offset(x = titleStartX, y = titleTopY - statusBarHeight)
            )
        }
    }
}
