package com.djpro.mixer.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun JogWheel(
    accent: Color, size: Dp, isPlaying: Boolean,
    onScratch: (Float) -> Unit, onScratchEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var userAngle by remember { mutableFloatStateOf(0f) }
    val infinite = rememberInfiniteTransition(label = "jog")
    val spin by infinite.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "spin"
    )
    val displayedAngle = if (isPlaying) userAngle + spin else userAngle

    Box(
        modifier = modifier.size(size).pointerInput(Unit) {
            detectDragGestures(
                onDrag = { change: PointerInputChange, drag: Offset ->
                    change.consume()
                    userAngle += drag.y * 0.9f
                    onScratch(1f - (drag.y / 120f))
                },
                onDragEnd = { onScratchEnd() },
                onDragCancel = { onScratchEnd() }
            )
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val r = this.size.minDimension / 2f
            val c = Offset(cx, cy)

            drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.35f), Color.Transparent), c, r), r, c)
            drawCircle(Brush.linearGradient(listOf(Color(0xFF3A3A48), Color(0xFF14141C), Color(0xFF3A3A48)), Offset(0f, 0f), Offset(this.size.width, this.size.height)), r * 0.94f, c)
            drawCircle(accent, r * 0.94f, c, style = Stroke(width = 3f))
            drawCircle(accent.copy(alpha = 0.35f), r * 0.94f, c, style = Stroke(width = 8f))
            drawCircle(Brush.radialGradient(listOf(Color(0xFF232330), Color(0xFF0A0A12)), c, r * 0.78f), r * 0.78f, c)
            for (g in 1..4) drawCircle(accent.copy(alpha = 0.10f), r * (0.30f + g * 0.11f), c, style = Stroke(width = 1f))
            drawCircle(Color(0xFF1A1A24), r * 0.22f, c)
            drawCircle(accent, r * 0.22f, c, style = Stroke(width = 2f))
            drawCircle(accent, r * 0.05f, c)

            val rad = Math.toRadians(displayedAngle.toDouble())
            drawLine(accent, c, Offset(cx + (r * 0.72f) * cos(rad).toFloat(), cy + (r * 0.72f) * sin(rad).toFloat()), 3f, StrokeCap.Round)

            for (t in 0 until 60) {
                val a = Math.toRadians(t * 6.0)
                val rO = r * 0.90f
                val rI = if (t % 5 == 0) r * 0.80f else r * 0.85f
                drawLine(accent.copy(alpha = 0.5f),
                    Offset(cx + rI * cos(a).toFloat(), cy + rI * sin(a).toFloat()),
                    Offset(cx + rO * cos(a).toFloat(), cy + rO * sin(a).toFloat()),
                    if (t % 5 == 0) 2f else 1f)
            }
        }
    }
}
