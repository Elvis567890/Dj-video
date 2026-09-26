package com.djpro.mixer.ui
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
fun MetalJogWheel(
    accent: Color, size: Dp, isPlaying: Boolean,
    onScratchStart: () -> Unit, onScratchMove: (Float) -> Unit, onScratchEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var userAngle by remember { mutableFloatStateOf(0f) }
    val infinite = rememberInfiniteTransition(label = "jog")
    val spin by infinite.animateFloat(0f, 360f,
        infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart), label = "spin")
    val displayedAngle = if (isPlaying) userAngle + spin else userAngle
    Box(modifier = modifier.size(size).pointerInput(Unit) {
        detectDragGestures(
            onDragStart = { onScratchStart() },
            onDrag = { change: PointerInputChange, drag: Offset ->
                change.consume()
                userAngle += drag.y * 0.9f
                onScratchMove(drag.y)
            },
            onDragEnd = { onScratchEnd() },
            onDragCancel = { onScratchEnd() }
        )
    }, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = this.size.width / 2f; val cy = this.size.height / 2f
            val r = this.size.minDimension / 2f; val c = Offset(cx, cy)
            drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.30f), Color.Transparent), c, r), r, c)
            drawCircle(Brush.linearGradient(listOf(Color(0xFF2A2A36), Color(0xFF0A0A12), Color(0xFF2A2A36)),
                Offset(0f, 0f), Offset(this.size.width, this.size.height)), r * 0.98f, c)
            drawCircle(accent, r * 0.98f, c, style = Stroke(width = 3f))
            drawCircle(accent.copy(alpha = 0.30f), r * 0.98f, c, style = Stroke(width = 9f))
            drawCircle(Brush.radialGradient(
                listOf(Color(0xFFE8E8EC), Color(0xFFB8B8C2), Color(0xFF8A8A96), Color(0xFF5A5A66)),
                Offset(cx - r * 0.20f, cy - r * 0.25f), r * 1.10f), r * 0.78f, c)
            for (g in 1..6) drawCircle(Color(0x30FFFFFF), r * (0.30f + g * 0.075f), c, style = Stroke(width = 0.8f))
            drawCircle(Brush.radialGradient(listOf(Color.Transparent, Color(0x50000000)), c, r * 0.78f), r * 0.78f, c)
            val crossR = r * 0.15f
            drawCircle(Color(0xFF2A2A38), crossR, c)
            drawCircle(Color(0xFF5A5A66), crossR, c, style = Stroke(width = 1f))
            drawLine(Color(0xFF9A9AA6), Offset(cx - crossR * 0.6f, cy), Offset(cx + crossR * 0.6f, cy), 1.5f)
            drawLine(Color(0xFF9A9AA6), Offset(cx, cy - crossR * 0.6f), Offset(cx, cy + crossR * 0.6f), 1.5f)
            val rad = Math.toRadians(displayedAngle.toDouble())
            val dx = (r * 0.88f) * cos(rad).toFloat()
            val dy = (r * 0.88f) * sin(rad).toFloat()
            drawCircle(accent, 4f, Offset(cx + dx, cy + dy))
            drawLine(accent, c, Offset(cx + dx, cy + dy), 2f, StrokeCap.Round)
        }
    }
}
