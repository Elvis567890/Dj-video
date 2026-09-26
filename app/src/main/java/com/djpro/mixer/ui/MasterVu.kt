package com.djpro.mixer.ui
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.sin
@Composable
fun MasterVu(playing: Boolean, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "vu")
    val phase by infinite.animateFloat(0f, (2 * Math.PI).toFloat() * 20f,
        infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Restart), label = "phase")
    Canvas(modifier = modifier.width(18.dp).fillMaxHeight()) {
        val w = size.width; val h = size.height
        val segments = 20
        val segH = (h - (segments - 1) * 2f) / segments
        for (i in 0 until segments) {
            val y = i * (segH + 2f)
            val t = 1f - i.toFloat() / segments
            val level = if (playing) (0.55f + 0.45f * sin(phase + i * 0.6f)).coerceIn(0.20f, 1f) else 0.10f
            val lit = t < level
            val color = when {
                t > 0.90f -> Color(0xFFFF3B5C)
                t > 0.75f -> Color(0xFFFFD54A)
                t > 0.55f -> Color(0xFF9CFF2B)
                else -> Color(0xFF00E5FF)
            }
            drawRect(color = color.copy(alpha = if (lit) 1f else 0.12f), topLeft = Offset(0f, y), size = Size(w, segH))
        }
    }
}
