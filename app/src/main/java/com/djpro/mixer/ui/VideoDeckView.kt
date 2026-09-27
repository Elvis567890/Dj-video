package com.djpro.mixer.ui

import android.view.TextureView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.viewinterop.AndroidView
import com.djpro.mixer.audio.DeckPlayer
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

enum class VideoTransition {
    CROSSFADE, ZOOM, WIPE, SLIDE, FLIP, GLITCH, STROBE, SPIN, BURN
}

@Composable
fun VideoDeckView(
    deck: DeckPlayer,
    progress: Float,
    isIncoming: Boolean,
    transition: VideoTransition = VideoTransition.CROSSFADE,
    accentColor: Color = Color.Cyan,
    showScratchOverlay: Boolean = true,
    modifier: Modifier = Modifier
) {
    val p = if (isIncoming) progress else (1f - progress)
    val t = 1f - abs(progress - 0.5f) * 2f

    val smoothP by animateFloatAsState(p, tween(80), label = "vp")
    val smoothT by animateFloatAsState(t, tween(120), label = "vt")

    var alpha = smoothP
    var sx = 1f
    var sy = 1f
    var tx = 0f
    var ty = 0f
    var rotY = 0f
    var rotZ = 0f
    var camDist = 12f

    when (transition) {
        VideoTransition.CROSSFADE -> { alpha = smoothP }
        VideoTransition.ZOOM -> {
            alpha = smoothP
            val s = 0.80f + 0.20f * smoothP
            sx = s; sy = s
        }
        VideoTransition.WIPE -> {
            alpha = smoothP
            if (isIncoming) tx = (1f - progress) * 3000f
        }
        VideoTransition.SLIDE -> {
            alpha = if (p > 0.02f) 1f else 0f
            tx = if (isIncoming) -(1f - progress) * 3000f else progress * 3000f
        }
        VideoTransition.FLIP -> {
            rotY = if (isIncoming) (progress - 1f) * 180f else progress * 180f
            alpha = if (abs(rotY) < 89f) 1f else 0f
            camDist = 8f
        }
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
            val flick = if (smoothT > 0.02f) {
                val s = sin(progress * 95f)
                if (s > 0f) 1f else 0.15f
            } else 1f
            alpha = smoothP * flick
        }
        VideoTransition.SPIN -> {
            alpha = smoothP
            val dir = if (isIncoming) 1f else -1f
            rotZ = (1f - smoothP) * 360f * dir
            val s = 0.55f + 0.45f * smoothP
            sx = s; sy = s
        }
        VideoTransition.BURN -> {
            alpha = smoothP
            val s = 1f + smoothT * 0.18f
            sx = s; sy = s
        }
    }

    Box(modifier = modifier.clipToBounds()) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    this.alpha = alpha.coerceIn(0f, 1f)
                    scaleX = sx
                    scaleY = sy
                    translationX = tx
                    translationY = ty
                    rotationY = rotY
                    rotationZ = rotZ
                    cameraDistance = camDist * density
                },
            factory = { ctx ->
                TextureView(ctx).apply {
                    isOpaque = true
                    deck.player.setVideoTextureView(this)
                }
            }
        )

        if (smoothT > 0.05f && alpha > 0.15f) {
            when (transition) {
                VideoTransition.GLITCH -> GlitchOverlay(accentColor, smoothT, progress)
                VideoTransition.STROBE -> StrobeOverlay(accentColor, smoothT, progress)
                VideoTransition.BURN -> BurnOverlay(accentColor, smoothT)
                else -> {}
            }
        }

        val scratching = deck.scratching
        if (showScratchOverlay && scratching && alpha > 0.3f) {
            ScratchOverlay(
                accent = accentColor,
                rate = deck.scratchRate,
                velocity = deck.scratchVelocity,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { this.alpha = alpha.coerceIn(0f, 1f) }
            )
        }
    }
}

@Composable
private fun GlitchOverlay(accent: Color, intensity: Float, progress: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        val rnd = Random((progress * 10000f).toInt() * 7919)
        val bars = (8 + intensity * 22).toInt()
        for (i in 0 until bars) {
            val y = rnd.nextFloat() * h
            val barH = 1f + rnd.nextFloat() * (2f + 10f * intensity)
            val xOff = (rnd.nextFloat() - 0.5f) * 90f * intensity
            val a = 0.20f + 0.55f * intensity * rnd.nextFloat()
            val col = when (rnd.nextInt(3)) {
                0 -> accent.copy(alpha = a)
                1 -> Color(0xFFFF0044).copy(alpha = a * 0.8f)
                else -> Color(0xFF00FF88).copy(alpha = a * 0.6f)
            }
            drawRect(color = col, topLeft = Offset(xOff, y), size = Size(w, barH))
        }
        if (intensity > 0.6f) {
            drawRect(color = Color.Red.copy(alpha = 0.18f * intensity), topLeft = Offset(-6f, 0f), size = Size(w * 0.4f, h))
            drawRect(color = Color.Cyan.copy(alpha = 0.18f * intensity), topLeft = Offset(w * 0.6f + 6f, 0f), size = Size(w * 0.4f, h))
        }
    }
}

@Composable
private fun StrobeOverlay(accent: Color, intensity: Float, progress: Float) {
    val flash = (sin(progress * 95f) * 0.5f + 0.5f).coerceIn(0f, 1f)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val a = flash * intensity * 0.55f
        drawRect(color = Color.White.copy(alpha = a))
        drawRect(color = accent.copy(alpha = a * 0.35f))
    }
}

@Composable
private fun BurnOverlay(accent: Color, intensity: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        drawRect(color = Color(0xFFFF6A00).copy(alpha = 0.18f * intensity))
        drawRect(color = Color.White.copy(alpha = 0.35f * intensity),
            topLeft = Offset(0f, h * 0.45f), size = Size(w, h * 0.1f))
    }
}
