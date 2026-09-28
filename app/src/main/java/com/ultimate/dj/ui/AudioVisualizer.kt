package com.ultimate.dj.ui
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import kotlin.math.abs
import kotlin.math.sin
@Composable
fun AudioVisualizer(accent: Color, isPlaying: Boolean, trackName: String, waveform: FloatArray = FloatArray(0), modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "viz")
    val phase by infinite.animateFloat(0f, 1000f,
        infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart), label = "phase")
    val pulse by infinite.animateFloat(0.75f, 1f,
        infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse), label = "pulse")
    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = if (isPlaying) 0.20f * pulse else 0.06f), Color.Transparent),
                    center = c, radius = size.minDimension * 0.7f),
                radius = size.minDimension * 0.7f, center = c)
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height; val mid = h / 2f
            val bars = if (waveform.isNotEmpty()) waveform else FloatArray(64) { 0.3f }
            val n = bars.size; val step = w / n; val barW = (step * 0.65f).coerceAtLeast(1.2f)
            for (i in 0 until n) {
                val base = bars[i].coerceIn(0.02f, 1f)
                val anim = if (isPlaying) (0.5f + 0.5f * abs(sin(phase * 0.06f + i * 0.35f))) else 0.2f
                val amp = base * anim * (h * 0.40f); val x = i * step + step / 2f
                drawLine(accent.copy(alpha = 0.85f), Offset(x, mid - amp), Offset(x, mid + amp), barW, StrokeCap.Round)
            }
        }
    }
}
