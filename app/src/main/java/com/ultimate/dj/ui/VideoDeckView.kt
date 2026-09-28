package com.ultimate.dj.ui
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.ultimate.dj.R
import com.ultimate.dj.audio.DeckPlayer
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random
enum class VideoTransition { CROSSFADE, ZOOM, WIPE, SLIDE, FLIP, GLITCH, STROBE, SPIN, BURN }
@Composable
fun VideoDeckView(
    deck: DeckPlayer, progress: Float, isIncoming: Boolean,
    transition: VideoTransition = VideoTransition.CROSSFADE,
    accentColor: Color = Color.Cyan,
    showScratchOverlay: Boolean = true,
    modifier: Modifier = Modifier
) {
    val p = if (isIncoming) progress else (1f - progress)
    val t = 1f - abs(progress - 0.5f) * 2f
    val smoothP by animateFloatAsState(p, tween(80), label = "vp")
    val smoothT by animateFloatAsState(t, tween(120), label = "vt")
    var alpha = smoothP; var sx = 1f; var sy = 1f; var tx = 0f; var ty = 0f
    var rotY = 0f; var rotZ = 0f; var camDist = 12f
    when (transition) {
        VideoTransition.CROSSFADE -> { alpha = smoothP }
        VideoTransition.ZOOM -> { alpha = smoothP; val s = 0.80f + 0.20f * smoothP; sx = s; sy = s }
        VideoTransition.WIPE -> { alpha = smoothP; if (isIncoming) tx = (1f - progress) * 3000f }
        VideoTransition.SLIDE -> { alpha = if (p > 0.02f) 1f else 0f; tx = if (isIncoming) -(1f - progress) * 3000f else progress * 3000f }
        VideoTransition.FLIP -> { rotY = if (isIncoming) (progress - 1f) * 180f else progress * 180f; alpha = if (abs(rotY) < 89f) 1f else 0f; camDist = 8f }
        VideoTransition.GLITCH -> {
            alpha = smoothP
            val jitterAmp = smoothT * 32f
            val rnd = Random((progress * 1000f).toInt() * 7919)
            tx = (rnd.nextFloat() - 0.5f) * jitterAmp
            ty = (rnd.nextFloat() - 0.5f) * jitterAmp * 0.35f
            sx = 1f + smoothT * 0.04f * (rnd.nextFloat() - 0.5f)
            sy = 1f + smoothT * 0.04f * (rnd.nextFloat() - 0.5f)
        }
        VideoTransition.STROBE -> {
            val flick = if (smoothT > 0.02f) { val s = sin(progress * 95f); if (s > 0f) 1f else 0.15f } else 1f
            alpha = smoothP * flick
        }
        VideoTransition.SPIN -> {
            alpha = smoothP
            val dir = if (isIncoming) 1f else -1f
            rotZ = (1f - smoothP) * 360f * dir
            val s = 0.55f + 0.45f * smoothP; sx = s; sy = s
        }
        VideoTransition.BURN -> { alpha = smoothP; val s = 1f + smoothT * 0.18f; sx = s; sy = s }
    }
    Box(modifier = modifier.clipToBounds()) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    this.alpha = alpha.coerceIn(0f, 1f)
                    scaleX = sx; scaleY = sy
                    translationX = tx; translationY = ty
                    rotationY = rotY; rotationZ = rotZ
                    cameraDistance = camDist * density
                },
            factory = { c ->
                val view = android.view.LayoutInflater.from(c)
                    .inflate(R.layout.player_view, null) as PlayerView
                view.player = deck.player
                view
            },
            update = { view -> view.player = deck.player },
            onRelease = { view -> view.player = null }
        )
    }
}
