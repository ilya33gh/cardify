package com.cardify.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cardify.app.ui.theme.SquircleShape
import kotlinx.coroutines.launch

val ExpressivePink = Color(0xFFEC407A)

/**
 * Expressive Squircle Pink Favorite Button with Synchronized Double Heartbeat Animation & Haptics.
 * The icon scales expressively inside the stable squircle container, preventing any clipping or layer overlap.
 */
@Composable
fun AnimatedFavoriteIconButton(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconScale = remember { Animatable(1f) }
    val buttonScale = remember { Animatable(1f) }
    val coroutineScope = rememberCoroutineScope()
    val hapticHelper = rememberHapticHelper()

    val containerColor by animateColorAsState(
        targetValue = if (isFavorite)
            ExpressivePink
        else
            MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "favoriteContainerColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isFavorite)
            Color.White
        else
            ExpressivePink,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "favoriteContentColor"
    )

    Surface(
        onClick = {
            if (!isFavorite) {
                hapticHelper.performHeartbeat()
                coroutineScope.launch {
                    launch {
                        buttonScale.animateTo(0.92f, tween(60))
                        buttonScale.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
                    }
                    iconScale.snapTo(1.0f)
                    iconScale.animateTo(1.38f, tween(90, easing = FastOutSlowInEasing))
                    iconScale.animateTo(1.12f, tween(60, easing = LinearEasing))
                    iconScale.animateTo(1.45f, tween(110, easing = FastOutSlowInEasing))
                    iconScale.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
                }
            } else {
                hapticHelper.performClick()
                coroutineScope.launch {
                    launch {
                        buttonScale.animateTo(0.92f, tween(60))
                        buttonScale.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
                    }
                    iconScale.animateTo(0.75f, tween(70))
                    iconScale.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
                }
            }
            onToggle()
        },
        shape = androidx.compose.foundation.shape.CircleShape,
        color = containerColor,
        contentColor = contentColor,
        border = null,
        tonalElevation = 0.dp,
        modifier = modifier
            .size(40.dp)
            .scale(buttonScale.value)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = "Favorite",
                tint = contentColor,
                modifier = Modifier
                    .size(22.dp)
                    .scale(iconScale.value)
            )
        }
    }
}
