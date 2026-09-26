package com.djpro.mixer.expand

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.djpro.mixer.Neon
import kotlin.math.roundToInt

@Composable
fun VideoMixPiP(modifier: Modifier = Modifier) {
    var dx by remember { mutableStateOf(0f) }
    var dy by remember { mutableStateOf(0f) }
    Box(
        modifier = modifier
            .offset { IntOffset(dx.roundToInt(), dy.roundToInt()) }
            .size(180.dp, 100.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black)
            .border(1.5.dp, Neon.CYAN.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
            .pointerInput(Unit) {
                detectDragGestures { change: PointerInputChange, drag: Offset ->
                    change.consume(); dx += drag.x; dy += drag.y
                }
            },
        contentAlignment = Alignment.BottomStart
    ) {
        Text("VIDEO MIX", color = Neon.CYAN.copy(alpha = 0.8f), fontSize = 9.sp, modifier = Modifier.padding(6.dp))
    }
}
