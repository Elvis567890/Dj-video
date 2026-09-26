package com.djpro.mixer.expand

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.djpro.mixer.audio.DeckPlayer
import com.djpro.mixer.ui.ScratchOverlay
import kotlin.random.Random

enum class VideoTransition { CROSSFADE, ZOOM, WIPE }

@Composable
fun VideoDeckView(
    deck: DeckPlayer,
    alpha: Float,
    transition: VideoTransition = VideoTransition.CROSSFADE,
    accentColor: androidx.compose.ui.graphics.Color,
    showScratchOverlay: Boolean = true,
    modifier: Modifier = Modifier
) {
    val scaleTarget = when (transition) {
        VideoTransition.ZOOM -> 0.85f + 0.15f * alpha
        else -> 1f
    }
    val scale by animateFloatAsState(scaleTarget, tween(220), label = "vscale")
    val wipeX by animateFloatAsState(
        when (transition) { VideoTransition.WIPE -> (1f - alpha) * 1200f; else -> 0f },
        tween(220), label = "wipe"
    )

    // Live scratch state
    val scratching = deck.scratching
    val scratchVelocity = deck.scratchVelocity
    val scratchRate = deck.scratchRate

    // Jitter offsets when scratching
    val jitterX: Float
    val jitterY: Float
    val stretchX: Float
    if (scratching) {
        val rnd = remember(scratchVelocity) { Random((scratchVelocity * 1000).toInt()) }
        jitterX = (rnd.nextFloat() - 0.5f) * 30f * (kotlin.math.abs(scratchVelocity) + 0.3f)
        jitterY = (rnd.nextFloat() - 0.5f) * 10f * (kotlin.math.abs(scratchVelocity) + 0.3f)
        stretchX = 1f + (scratchVelocity * 0.05f).coerceIn(-0.08f, 0.08f)
    } else {
        jitterX = 0f; jitterY = 0f; stretchX = 1f
    }

    Box(modifier = modifier.clipToBounds(), contentAlignment = Alignment.Center) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    this.alpha = alpha.coerceIn(0f, 1f)
                    scaleX = scale * stretchX
                    scaleY = scale
                    translationX = wipeX + jitterX
                    translationY = jitterY
                },
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = deck.player
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                }
            }
        )

        // Scratch visual feedback drawn on top of this deck's video
        if (showScratchOverlay && scratching && alpha > 0.3f) {
            ScratchOverlay(
                accent = accentColor,
                rate = scratchRate,
                velocity = scratchVelocity,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { this.alpha = (alpha * 1f).coerceIn(0f, 1f) }
            )
        }
    }
}
