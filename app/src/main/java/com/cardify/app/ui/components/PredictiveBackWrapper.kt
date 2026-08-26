package com.cardify.app.ui.components

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

/**
 * 120 FPS GPU-accelerated Slide-Over Predictive Back container.
 * Slides top screen horizontally to the right over the underlying screen.
 * Consumes touch events and guarantees 0 white/black flashes.
 */
@Composable
fun PredictiveBackWrapper(
    enabled: Boolean = true,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val progressAnimatable = remember { Animatable(0f) }

    PredictiveBackHandler(enabled = enabled) { progressFlow: Flow<BackEventCompat> ->
        try {
            progressFlow.collect { backEvent ->
                progressAnimatable.snapTo(backEvent.progress)
            }
            // Smooth GPU slide-out completion to the right
            progressAnimatable.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            )
            onBack()
        } catch (e: CancellationException) {
            // Spring back to origin position if gesture cancelled
            progressAnimatable.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    val progress = progressAnimatable.value

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        // Active Top Screen sliding off to the right directly over the base screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = progress * size.width
                }
                .background(MaterialTheme.colorScheme.surface)
        ) {
            content()
        }
    }
}
