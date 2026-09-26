package com.djpro.mixer.ui
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
fun KnobSmall(
    label: String, accent: Color, initialValue: Float = 0f,
    minValue: Float = -26f, maxValue: Float = 6f,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    onValueChange: (Float) -> Unit = {}, modifier: Modifier = Modifier
) {
    var value by remember { mutableFloatStateOf(initialValue) }
    Box(modifier = modifier.size(size).pointerInput(Unit) {
        detectDragGestures { change: PointerInputChange, drag: Offset ->
            change.consume()
            val range = maxValue - minValue
            value = (value + (-drag.y / 3f) * range / 30f).coerceIn(minValue, maxValue)
            onValueChange(value)
        }
    }, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val cx = this.size.width / 2f; val cy = this.size.height / 2f
            val r = this.size.minDimension / 2f - 2f
            drawCircle(Brush.radialGradient(listOf(Color(0xFF2E2E3C), Color(0xFF12121C)), Offset(cx, cy), r), r, Offset(cx, cy))
            drawCircle(accent.copy(alpha = 0.45f), r, Offset(cx, cy), style = Stroke(width = 1f))
            val start = 135f; val sweep = 270f
            drawArc(accent.copy(alpha = 0.22f), start, sweep, false, Offset(cx - r, cy - r), Size(r * 2, r * 2), style = Stroke(1.5f, cap = StrokeCap.Round))
            val range = maxValue - minValue
            val n = ((value - minValue) / range).coerceIn(0f, 1f)
            drawArc(accent, start, sweep * n, false, Offset(cx - r, cy - r), Size(r * 2, r * 2), style = Stroke(2.5f, cap = StrokeCap.Round))
            val aRad = Math.toRadians((start + sweep * n).toDouble())
            drawLine(Color.White, Offset(cx, cy), Offset(cx + (r - 5f) * cos(aRad).toFloat(), cy + (r - 5f) * sin(aRad).toFloat()), 2f, StrokeCap.Round)
            drawCircle(Color(0xFF1A1A24), 3f, Offset(cx, cy))
        }
        Text(label, color = accent.copy(alpha = 0.85f), fontSize = 7.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomCenter))
    }
}
