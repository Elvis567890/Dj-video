package com.ultimate.dj.ui
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultimate.dj.Neon
import kotlin.math.abs
import kotlin.random.Random
@Composable
fun ScratchOverlay(accent: Color, rate: Float, velocity: Float, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "scratch")
    val phase by infinite.animateFloat(0f, 1000f,
        infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Restart), label = "phase")
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            val intensity = (abs(velocity) * 1.2f).coerceIn(0f, 1.5f)
            val rnd = Random((phase.toInt() * 7919))
            val sliceCount = (10 + (intensity * 20)).toInt()
            for (i in 0 until sliceCount) {
                val y = rnd.nextFloat() * h
                val sliceH = 1f + rnd.nextFloat() * (3f + 8f * intensity)
                val xOff = (rnd.nextFloat() - 0.5f) * 60f * intensity
                val alpha = 0.15f + 0.4f * intensity * rnd.nextFloat()
                drawRect(color = if (rnd.nextBoolean()) accent.copy(alpha = alpha) else Neon.AMBER.copy(alpha = alpha),
                    topLeft = Offset(xOff, y), size = Size(w, sliceH))
            }
        }
        val displayRate = if (rate < 0f) "REV ${"%.2f".format(abs(rate))}"
                          else "${"%.2f".format(rate)}"
        Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp)
            .clip(RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = 0.7f))
            .border(1.dp, accent, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)) {
            Text("SCRATCH $displayRate", color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}
