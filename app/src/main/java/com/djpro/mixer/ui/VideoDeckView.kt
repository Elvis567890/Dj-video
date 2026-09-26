package com.djpro.mixer.ui
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.djpro.mixer.audio.DeckPlayer
enum class VideoTransition { CROSSFADE, ZOOM, WIPE }
@Composable
fun VideoDeckView(
    deck: DeckPlayer, alpha: Float,
    transition: VideoTransition = VideoTransition.CROSSFADE,
    accentColor: Color = Color.Cyan,
    showScratchOverlay: Boolean = true,
    modifier: Modifier = Modifier
) {
    val scaleTarget = when (transition) { VideoTransition.ZOOM -> 0.85f + 0.15f * alpha; else -> 1f }
    val scale by animateFloatAsState(scaleTarget, tween(220), label = "vscale")
    val wipeX by animateFloatAsState(
        when (transition) { VideoTransition.WIPE -> (1f - alpha) * 1200f; else -> 0f },
        tween(220), label = "wipe")
    val scratching = deck.scratching
    val vel = deck.scratchVelocity
    val stretchX = if (scratching) (1f + (vel * 0.03f).coerceIn(-0.06f, 0.06f)) else 1f
    val jitterX = if (scratching) (vel * 4f).coerceIn(-14f, 14f) else 0f
    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize().graphicsLayer {
                this.alpha = alpha.coerceIn(0f, 1f)
                scaleX = scale * stretchX
                scaleY = scale
                translationX = wipeX + jitterX
            },
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = deck.player
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                }
            }
        )
        if (showScratchOverlay && scratching && alpha > 0.3f) {
            ScratchOverlay(accent = accentColor, rate = deck.scratchRate, velocity = vel,
                modifier = Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha.coerceIn(0f, 1f) })
        }
    }
}
