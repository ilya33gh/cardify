package com.cardify.app.ui.components

import android.app.Activity
import android.provider.Settings
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import com.cardify.app.R
import com.cardify.app.ui.theme.ManropeFamily
import com.cardify.app.ui.theme.OnestFamily

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
        if (clamped >= 0.99f && lastHapticValue < 0.99f) {
            hapticHelper.performClick()
            lastHapticValue = clamped
        } else if (kotlin.math.abs(clamped - lastHapticValue) >= 0.012f) {
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

    var isInteracting by remember { mutableStateOf(false) }

    val activeTrackHeight = 22.dp
    val inactiveTrackHeight = 14.dp
    val thumbWidth = 5.dp
    val thumbHeight = 36.dp
    val pillSize = 54.dp

    val activeTrackColor = MaterialTheme.colorScheme.primary
    val inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val thumbColor = MaterialTheme.colorScheme.primary

    val brightnessPercent = (sliderValue * 100).toInt()

    val pillAlpha by animateFloatAsState(
        targetValue = if (isInteracting) 1f else 0f,
        animationSpec = tween(durationMillis = 140),
        label = "brightnessPillAlpha"
    )
    val pillScale by animateFloatAsState(
        targetValue = if (isInteracting) 1f else 0.6f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow),
        label = "brightnessPillScale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Brightness Header (Positioned higher, with track right underneath)
        Text(
            text = stringResource(R.string.brightness_label),
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 2.dp, top = 8.dp, bottom = 2.dp)
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(thumbHeight),
            contentAlignment = Alignment.CenterStart
        ) {
            val totalWidthPx = constraints.maxWidth.toFloat()
            val availableWidth = maxWidth

            // Thumb and Pill positioning: strictly aligned to the vertical thumb center
            val thumbOffsetDp = (availableWidth - thumbWidth) * sliderValue
            val thumbCenterDp = thumbOffsetDp + (thumbWidth / 2)
            val badgeOffsetDp = thumbCenterDp - (pillSize / 2)

            // 1. Floating Value Bubble Badge (Higher vertically, floats above thumb, no % sign)
            if (pillAlpha > 0.01f) {
                Surface(
                    modifier = Modifier
                        .offset(x = badgeOffsetDp, y = (-48).dp)
                        .size(pillSize)
                        .graphicsLayer {
                            alpha = pillAlpha
                            scaleX = pillScale
                            scaleY = pillScale
                        },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shadowElevation = 6.dp,
                    border = null
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$brightnessPercent",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = OnestFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // 2. Track & Thumb Gesture Area with unified touch & continuous drag detection
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(thumbHeight)
                    .pointerInput(totalWidthPx) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            isInteracting = true
                            val initialFraction = (down.position.x / totalWidthPx).coerceIn(0f, 1f)
                            applyBrightness(initialFraction)

                            val pointerId = down.id
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.find { it.id == pointerId } ?: break
                                if (!change.pressed) {
                                    break
                                }
                                if (change.positionChanged()) {
                                    change.consume()
                                    val currentFraction = (change.position.x / totalWidthPx).coerceIn(0f, 1f)
                                    applyBrightness(currentFraction)
                                }
                            }
                            isInteracting = false
                        }
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                // Asymmetric Track Container: Active track (thicker, 22.dp), Inactive track (standard, 14.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(activeTrackHeight),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Active Track (Thick 22.dp Capsule with left dot)
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(sliderValue.coerceAtLeast(0.01f))
                            .clip(RoundedCornerShape(topStart = 11.dp, bottomStart = 11.dp, topEnd = 4.dp, bottomEnd = 4.dp))
                            .background(activeTrackColor),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(start = 7.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                        )
                    }

                    Spacer(modifier = Modifier.width(3.dp))

                    // Inactive Track (Standard 14.dp Capsule with right dot)
                    Box(
                        modifier = Modifier
                            .height(inactiveTrackHeight)
                            .weight((1f - sliderValue).coerceAtLeast(0.01f))
                            .clip(RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 7.dp, bottomEnd = 7.dp))
                            .background(inactiveTrackColor),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                }

                // Vertical Thumb Pill Handle (Photo 6)
                Box(
                    modifier = Modifier
                        .offset(x = thumbOffsetDp)
                        .width(thumbWidth)
                        .height(thumbHeight)
                        .clip(RoundedCornerShape(2.5.dp))
                        .background(thumbColor)
                )
            }
        }
    }
}
