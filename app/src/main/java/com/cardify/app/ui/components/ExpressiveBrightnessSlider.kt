package com.cardify.app.ui.components

import android.app.Activity
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessHigh
import androidx.compose.material.icons.outlined.BrightnessLow
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import com.cardify.app.R
import com.cardify.app.ui.theme.InterFamily

/**
 * Material 3 Expressive Brightness Slider with title, icon, percentage display, and high-frequency tactile haptics.
 */
@Composable
fun ExpressiveBrightnessSlider(
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val isDark = isSystemInDarkTheme()

    // Find active Window (ModalBottomSheet DialogWindowProvider or Activity Window)
    val window = remember(view) {
        var currentView: Any? = view
        var foundWindow: android.view.Window? = null
        while (currentView != null) {
            if (currentView is DialogWindowProvider) {
                foundWindow = currentView.window
                break
            }
            if (currentView is android.view.View) {
                currentView = currentView.parent
            } else {
                break
            }
        }
        foundWindow ?: (view.context as? Activity)?.window
    }

    // Query initial brightness (0f to 1f)
    var sliderValue by remember {
        mutableStateOf(
            window?.attributes?.screenBrightness?.takeIf { it >= 0f }
                ?: try {
                    Settings.System.getInt(
                        view.context.contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS
                    ) / 255f
                } catch (e: Exception) {
                    0.7f
                }
        )
    }

    val hapticHelper = rememberHapticHelper()
    var lastHapticValue by remember { mutableFloatStateOf(sliderValue) }

    // Function to apply brightness to active Window & Activity with high-frequency tactile haptics
    fun applyBrightness(newVal: Float) {
        val clamped = newVal.coerceIn(0.05f, 1.0f)
        if (kotlin.math.abs(clamped - lastHapticValue) >= 0.012f) {
            hapticHelper.performTick()
            lastHapticValue = clamped
        }
        sliderValue = clamped
        
        // 1. Update Sheet Dialog Window
        window?.let { win ->
            val lp = win.attributes
            lp.screenBrightness = clamped
            win.attributes = lp
        }

        // 2. Also update parent Activity Window if distinct
        val act = view.context as? Activity
        if (act != null && act.window != window) {
            val actLp = act.window.attributes
            actLp.screenBrightness = clamped
            act.window.attributes = actLp
        }
    }

    val trackHeight = 14.dp
    val thumbWidth = 6.dp
    val thumbHeight = 36.dp

    val activeTrackColor = MaterialTheme.colorScheme.primary
    val inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val thumbColor = MaterialTheme.colorScheme.primary

    val brightnessPercent = (sliderValue * 100).toInt()
    val brightnessIcon = when {
        sliderValue < 0.35f -> Icons.Outlined.BrightnessLow
        sliderValue < 0.70f -> Icons.Outlined.BrightnessMedium
        else -> Icons.Outlined.BrightnessHigh
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Brightness Header: Icon + Label (Left), Percentage (Right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = brightnessIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.brightness_label),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "$brightnessPercent%",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val totalWidthPx = constraints.maxWidth.toFloat()
            val availableWidth = maxWidth

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(thumbHeight)
                    .pointerInput(totalWidthPx) {
                        detectTapGestures { offset ->
                            val newFraction = (offset.x / totalWidthPx).coerceIn(0f, 1f)
                            applyBrightness(newFraction)
                        }
                    }
                    .pointerInput(totalWidthPx) {
                        detectHorizontalDragGestures { change, _ ->
                            change.consume()
                            val newFraction = (change.position.x / totalWidthPx).coerceIn(0f, 1f)
                            applyBrightness(newFraction)
                        }
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                // Track Container (Thin 14.dp Capsule Line)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(trackHeight)
                        .clip(RoundedCornerShape(7.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Active Track (Left)
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(sliderValue.coerceAtLeast(0.01f))
                            .clip(RoundedCornerShape(topStart = 7.dp, bottomStart = 7.dp, topEnd = 4.dp, bottomEnd = 4.dp))
                        .background(activeTrackColor)
                    )

                    Spacer(modifier = Modifier.width(3.dp))

                    // Inactive Track (Right) with end dot
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight((1f - sliderValue).coerceAtLeast(0.01f))
                            .clip(RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 7.dp, bottomEnd = 7.dp))
                            .background(inactiveTrackColor),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(activeTrackColor.copy(alpha = 0.5f))
                        )
                    }
                }

                // Thumb Handle (Vertical Pill sliding along track)
                val thumbOffsetDp = (availableWidth - thumbWidth) * sliderValue
                Box(
                    modifier = Modifier
                        .offset(x = thumbOffsetDp)
                        .width(thumbWidth)
                        .height(thumbHeight)
                        .clip(RoundedCornerShape(3.dp))
                        .background(thumbColor)
                )
            }
        }
    }
}
