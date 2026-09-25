package com.example.djvideomixer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun JogWheel(
    label: String,
    onScratch: (Float) -> Unit,
    onRelease: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(140.dp)
            .clip(CircleShape)
            .background(Color(0xFF222222))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, drag ->
                        change.consume()
                        val rate = 1f + (-drag.y / 200f)
                        onScratch(rate)
                    },
                    onDragEnd = { onRelease() },
                    onDragCancel = { onRelease() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White)
    }
}
