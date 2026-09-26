package com.djpro.mixer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun VerticalFader(accent: Color, defaultValue: Float = 0.5f, onValueChange: (Float) -> Unit = {}, modifier: Modifier = Modifier) {
    var value by remember { mutableFloatStateOf(defaultValue) }
    Box(modifier = modifier.width(18.dp).fillMaxHeight().pointerInput(Unit) {
        detectDragGestures { change: PointerInputChange, drag: Offset ->
            change.consume()
            value = (value + (-drag.y / size.height)).coerceIn(0f, 1f)
            onValueChange(value)
        }
    }) {
        Canvas(modifier = Modifier.fillMaxHeight().width(18.dp)) {
            val cx = size.width / 2f; val h = size.height
            drawLine(Color(0xFF2A2A38), Offset(cx, 0f), Offset(cx, h), 4f, StrokeCap.Round)
            val y = h - value * (h - 20f) - 10f
            drawLine(accent.copy(alpha = 0.6f), Offset(cx, h), Offset(cx, y), 4f, StrokeCap.Round)
            drawCircle(Color(0xFF16161E), 8f, Offset(cx, y))
            drawCircle(accent, 8f, Offset(cx, y), style = Stroke(width = 2f))
        }
    }
}
