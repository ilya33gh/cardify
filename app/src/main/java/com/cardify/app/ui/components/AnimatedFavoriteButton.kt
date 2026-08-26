package com.cardify.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.cardify.app.ui.theme.SquircleShape

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

val ExpressivePink = Color(0xFFEC407A)

/**
 * Expressive Squircle Pink Favorite Button with Synchronized Double Heartbeat Animation & Haptics.
 */
@Composable
fun AnimatedFavoriteIconButton(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val heartbeatScale = remember { Animatable(1f) }
    val coroutineScope = rememberCoroutineScope()
    val hapticHelper = rememberHapticHelper()

    val containerColor by animateColorAsState(
        targetValue = if (isFavorite)
            ExpressivePink
        else
            MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "favoriteContainerColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isFavorite)
            Color.White
        else
            ExpressivePink,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "favoriteContentColor"
    )

    Surface(
        onClick = {
            if (!isFavorite) {
                hapticHelper.performHeartbeat()
                coroutineScope.launch {
                    heartbeatScale.snapTo(1.0f)
                    heartbeatScale.animateTo(1.36f, tween(90, easing = FastOutSlowInEasing))
                    heartbeatScale.animateTo(1.14f, tween(60, easing = LinearEasing))
                    heartbeatScale.animateTo(1.42f, tween(110, easing = FastOutSlowInEasing))
                    heartbeatScale.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
                }
            } else {
                hapticHelper.performClick()
                coroutineScope.launch {
                    heartbeatScale.snapTo(1.0f)
                    heartbeatScale.animateTo(0.85f, tween(80))
                    heartbeatScale.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
                }
            }
            onToggle()
        },
        shape = SquircleShape,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = if (isFavorite) 4.dp else 0.dp,
        modifier = modifier
            .zIndex(10f)
            .size(44.dp)
            .scale(heartbeatScale.value)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Favorite",
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
