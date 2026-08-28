package com.cardify.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Material 3 Expressive Morphing Barcode Loading Indicator.
 *
 * Cycles smoothly through 3 expressive shapes:
 * 1. 4-lobed smooth clover/blob (Photo 1)
 * 2. 12-lobed scalloped flower/sun (Photo 2)
 * 3. Rounded 5-sided pentagon (Photo 3)
 *
 * Features non-linear rotation physics: accelerates during shape change and decelerates/settles after.
 * Uses dynamic Material You Monet colors (primaryContainer for outer circle, primary for inner morphing shape).
 */
@Composable
fun ExpressiveMorphLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    shapeContentColor: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "morphInfiniteTransition")

    // Total cycle: 3 stages * 1100ms = 3300ms
    val totalTimeMs by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "morphStageProgress"
    )

    // Non-linear easing for morphing: smooth acceleration and deceleration
    val morphEasing = remember { CubicBezierEasing(0.38f, 0.0f, 0.22f, 1.0f) }
    // Rotation easing: sharp acceleration surge in the middle of morph and gentle settle
    val rotationEasing = remember { CubicBezierEasing(0.42f, 0.0f, 0.12f, 1.0f) }

    val currentStage = totalTimeMs.toInt() % 3
    val nextStage = (currentStage + 1) % 3
    val rawFraction = (totalTimeMs - totalTimeMs.toInt()).coerceIn(0f, 1f)

    val morphFraction = morphEasing.transform(rawFraction)
    val rotStepFraction = rotationEasing.transform(rawFraction)

    // Each shape transition rotates by 120 degrees with non-linear acceleration surge
    val baseRotation = currentStage * 120f
    val rotationAngle = baseRotation + (rotStepFraction * 120f)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val canvasWidth = this.size.width
            val canvasHeight = this.size.height
            val centerOffset = androidx.compose.ui.geometry.Offset(canvasWidth / 2f, canvasHeight / 2f)
            val outerRadius = minOf(canvasWidth, canvasHeight) / 2f
            val shapeBaseRadius = outerRadius * 0.72f

            // 1. Outer Monet Circular Backdrop
            drawCircle(
                color = containerColor,
                radius = outerRadius,
                center = centerOffset
            )

            // 2. Compute Morphing Shape Path
            val pointCount = 180
            val path = Path()

            for (i in 0 until pointCount) {
                val theta = (i.toFloat() / pointCount) * (2f * PI.toFloat())

                // Shape 1 (4 lobes - Photo 1): r1(theta)
                val r1 = shapeBaseRadius * (0.64f + 0.28f * cos(4f * theta))

                // Shape 2 (12 lobes - Photo 2): r2(theta)
                val r2 = shapeBaseRadius * (0.76f + 0.16f * cos(12f * theta))

                // Shape 3 (Rounded Pentagon - Photo 3): r3(theta)
                val pentagonAngle = 5f * (theta - (PI.toFloat() / 2f))
                val r3 = shapeBaseRadius * (0.75f + 0.18f * cos(pentagonAngle) + 0.04f * cos(2f * pentagonAngle))

                val rStart = when (currentStage) {
                    0 -> r1
                    1 -> r2
                    else -> r3
                }

                val rEnd = when (nextStage) {
                    0 -> r1
                    1 -> r2
                    else -> r3
                }

                val currentR = rStart + (rEnd - rStart) * morphFraction

                val x = centerOffset.x + currentR * cos(theta)
                val y = centerOffset.y + currentR * sin(theta)

                if (i == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            path.close()

            // 3. Draw Inner Morphing Shape with Non-Linear Rotation
            rotate(degrees = rotationAngle, pivot = centerOffset) {
                drawPath(
                    path = path,
                    color = shapeContentColor
                )
            }
        }
    }
}
