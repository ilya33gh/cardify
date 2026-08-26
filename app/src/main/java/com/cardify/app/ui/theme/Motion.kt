package com.cardify.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

object ExpressiveMotion {
    // Official Google Smooth Spring Physics (Silky smooth, visible, zero jank)
    val GoogleSmooth = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 380f
    )

    val GoogleSmoothDp = spring<Dp>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 380f
    )

    val GoogleSmoothIntOffset = spring<IntOffset>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 380f
    )

    val GoogleSmoothIntSize = spring<IntSize>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 380f
    )

    // M3 Expressive Elastic Bouncy Motion
    val ElasticBouncy = spring<Float>(
        dampingRatio = 0.58f,
        stiffness = Spring.StiffnessMediumLow
    )

    val ElasticBouncyDp = spring<Dp>(
        dampingRatio = 0.58f,
        stiffness = Spring.StiffnessMediumLow
    )

    // M3 Expressive Snappy Fast Easing
    val SnappyEase = tween<Float>(
        durationMillis = 240,
        easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    )

    // Subtle natural Google press feedback (scale = 0.96f)
    val PressScale = 0.96f
    val HoldScale = 0.93f

    val BouncySpring = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val BouncySpringDp = spring<Dp>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val BouncySpringIntOffset = spring<IntOffset>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val TactileSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val TactileSpringDp = spring<Dp>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
}
