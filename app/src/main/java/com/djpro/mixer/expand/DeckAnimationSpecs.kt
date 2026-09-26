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
    val fade = tween<Float>(durationMillis = 220)
    val pipEnter: EnterTransition =
        fadeIn(fade) + scaleIn(initialScale = 0.6f)
    val pipExit: ExitTransition =
        fadeOut(fade) + scaleOut(targetScale = 0.6f)
}
