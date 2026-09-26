package com.djpro.mixer.ui
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
@Composable
fun VolumeFader(accent: Color, initialValue: Float = 0.75f, onValueChange: (Float) -> Unit = {}, modifier: Modifier = Modifier) {
    var value by remember { mutableFloatStateOf(initialValue) }
    Box(modifier = modifier.width(28.dp).fillMaxHeight().pointerInput(Unit) {
        detectDragGestures { change: PointerInputChange, drag: Offset ->
            change.consume()
            value = (value + (-drag.y / size.height)).coerceIn(0f, 1f)
            onValueChange(value)
        }
    }) {
        Canvas(modifier = Modifier.fillMaxHeight().width(28.dp)) {
            val cx = size.width / 2f; val h = size.height
            drawLine(Color(0xFF2A2A38), Offset(cx, 10f), Offset(cx, h - 10f), 3f, StrokeCap.Round)
            val y = h - value * (h - 20f) - 10f
            drawLine(accent.copy(alpha = 0.55f), Offset(cx, h - 10f), Offset(cx, y), 3f, StrokeCap.Round)
            drawRoundRect(accent, Offset(cx - 12f, y - 6f), Size(24f, 12f), CornerRadius(3f))
            drawRoundRect(Color(0x40FFFFFF), Offset(cx - 12f, y - 6f), Size(24f, 4f), CornerRadius(3f))
            drawRoundRect(Color(0x60000000), Offset(cx - 12f, y - 6f), Size(24f, 12f), CornerRadius(3f), style = Stroke(width = 1f))
        }
    }
}
