package com.djpro.mixer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun EqKnob(label: String, accent: Color, initialValue: Float = 0f, onValueChange: (Float) -> Unit = {}, modifier: Modifier = Modifier) {
    var value by remember { mutableFloatStateOf(initialValue) }
    Box(modifier = modifier.size(44.dp).pointerInput(Unit) {
        detectDragGestures { change: PointerInputChange, drag: Offset ->
            change.consume()
            value = (value + (-drag.y / 4f)).coerceIn(-26f, 6f)
            onValueChange(value)
        }
    }, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(44.dp)) {
            val cx = size.width / 2f; val cy = size.height / 2f
            val r = size.minDimension / 2f - 2f
            drawCircle(Brush.radialGradient(listOf(Color(0xFF2A2A38), Color(0xFF0E0E16)), Offset(cx, cy), r), r, Offset(cx, cy))
            drawCircle(accent.copy(alpha = 0.5f), r, Offset(cx, cy), style = Stroke(width = 1.5f))
            val start = 135f; val sweep = 270f
            drawArc(accent.copy(alpha = 0.25f), start, sweep, false, Offset(cx - r, cy - r), Size(r * 2, r * 2), style = Stroke(2f, cap = StrokeCap.Round))
            val normalized = ((value + 26f) / 32f).coerceIn(0f, 1f)
            val angleRad = Math.toRadians((start + normalized * sweep).toDouble())
            drawLine(accent, Offset(cx, cy), Offset(cx + (r - 6f) * cos(angleRad).toFloat(), cy + (r - 6f) * sin(angleRad).toFloat()), 2.5f, StrokeCap.Round)
        }
        Text(label, color = accent, fontSize = 7.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomCenter))
    }
}
