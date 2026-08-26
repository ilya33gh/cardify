package com.cardify.app.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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

    // Subtle natural Google press feedback (scale = 0.96f)
    val PressScale = 0.96f

    val BouncySpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val BouncySpringDp = spring<Dp>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val BouncySpringIntOffset = spring<IntOffset>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val TactileSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val TactileSpringDp = spring<Dp>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
}
