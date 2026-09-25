package com.djpro.mixer.expand

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut

object DeckAnim {
    val expandSpring = spring<Float>(dampingRatio = 0.7f, stiffness = 400f)
    val sizeSpring = spring<Int>(dampingRatio = 0.7f, stiffness = 400f)
    val fade = tween<Float>(durationMillis = 220)
    val pipEnter: EnterTransition = fadeIn(fade) + scaleIn(initialScale = 0.6f, animationSpec = expandSpring)
    val pipExit: ExitTransition = fadeOut(fade) + scaleOut(targetScale = 0.6f, animationSpec = expandSpring)
}
