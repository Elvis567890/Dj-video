package com.djpro.mixer.expand

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.djpro.mixer.Neon
import kotlin.math.roundToInt

@Composable
fun VideoMixPiP(
    manager: ExpandableDeckManager,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var offset by remember { mutableStateOf(IntOffset.Zero) }
    Box(
        modifier = modifier
            .offset { offset }
            .size(width = 160.dp, height = 90.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black)
            .border(1.dp, Neon.CYAN.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .pointerInput(Unit) {
                detectDragGestures { change, drag ->
                    change.consume()
                    offset = IntOffset(
                        (offset.x + drag.x).roundToInt(),
                        (offset.y + drag.y).roundToInt(),
                    )
                }
            },
        contentAlignment = Alignment.BottomStart,
    ) {
        Text("VIDEO MIX", color = Neon.CYAN.copy(alpha = 0.7f),
            fontSize = 9.sp, modifier = Modifier.padding(6.dp))
    }
}
