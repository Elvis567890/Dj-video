package com.djpro.mixer.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.djpro.mixer.Neon
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun MasterVuMeter(playing: Boolean, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "vu")
    val phase by infinite.animateFloat(0f, (2 * Math.PI).toFloat() * 20f,
        infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart), label = "phase")
    Canvas(modifier = modifier.fillMaxWidth().height(14.dp)) {
        val w = size.width; val h = size.height; val mid = h / 2f
        val bars = 120; val step = w / bars
        for (i in 0 until bars) {
            val t = i.toFloat() / bars
            val env = (1f - abs(t - 0.5f) * 1.6f).coerceAtLeast(0.05f)
            val anim = if (playing) (0.5f + 0.5f * sin(phase + i * 0.35f)).coerceIn(0.15f, 1f) else 0.15f
            val amp = env * anim * (h * 0.5f - 1f)
            val x = i * step + step / 2f
            val color = if (t < 0.5f) Neon.CYAN else Neon.MAGENTA
            drawLine(color.copy(alpha = 0.85f), Offset(x, mid - amp), Offset(x, mid + amp), (step * 0.6f).coerceAtLeast(1f))
        }
    }
}
