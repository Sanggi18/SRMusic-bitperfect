package com.example.ui.common.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Material Design 3 Expressive Motion Specifications.
 * Uses physics-based spring curves for organic, fluid, responsive animations.
 * Reference: https://m3.material.io/styles/motion/overview
 */
object ExpressiveMotion {

    /**
     * Expressive bouncy spring for energetic micro-interactions (presses, badges, toggles).
     */
    val BouncySpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    /**
     * Snappy spring for quick, responsive UI elements (switches, tabs, small sheets).
     */
    val SnappySpring = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val SpatialSpring = spring<Float>(
        dampingRatio = 0.7f,
        stiffness = 300f
    )

    val SpatialSpringDp = spring<androidx.compose.ui.unit.Dp>(
        dampingRatio = 0.7f,
        stiffness = 300f
    )

    val SnappySpringDp = spring<androidx.compose.ui.unit.Dp>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val SlideSpringIntOffset = spring<androidx.compose.ui.unit.IntOffset>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium
    )
}

/**
 * Applies only the spring press scale without consuming clicks (useful when child handles click).
 */
fun Modifier.springPress(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.94f
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = ExpressiveMotion.BouncySpring,
        label = "expressive_spring_press"
    )

    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
