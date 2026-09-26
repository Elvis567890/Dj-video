package com.djpro.mixer.ui
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
import com.djpro.mixer.Neon
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
                drawRect(color = if (rnd.nextBoolean()) accent.copy(alpha = alpha) else Neon.MAGENTA.copy(alpha = alpha),
                    topLeft = Offset(xOff, y), size = Size(w, sliceH))
            }
            val scanY = ((phase * 2f * (if (velocity >= 0) 1f else -1f)) % h + h) % h
            drawLine(accent.copy(alpha = 0.7f), Offset(0f, scanY), Offset(w, scanY), 2f)
            drawRect(color = accent.copy(alpha = (0.25f + 0.35f * intensity) * 0.15f))
        }
        CornerBrackets(accent)
        val displayRate = if (rate < 0f) "\u25C0 REV \u00D7${"%.2f".format(abs(rate))}"
                          else "\u25B6 \u00D7${"%.2f".format(rate)}"
        Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp)
            .clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.75f))
            .border(1.5.dp, accent, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 2.dp)
        ) { Text("SCRATCH $displayRate", color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
    }
}
@Composable
private fun CornerBrackets(accent: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height; val arm = 20f; val thick = 3f
        drawLine(accent, Offset(0f, 0f), Offset(arm, 0f), thick)
        drawLine(accent, Offset(0f, 0f), Offset(0f, arm), thick)
        drawLine(accent, Offset(w, 0f), Offset(w - arm, 0f), thick)
        drawLine(accent, Offset(w, 0f), Offset(w, arm), thick)
        drawLine(accent, Offset(0f, h), Offset(arm, h), thick)
        drawLine(accent, Offset(0f, h), Offset(0f, h - arm), thick)
        drawLine(accent, Offset(w, h), Offset(w - arm, h), thick)
        drawLine(accent, Offset(w, h), Offset(w, h - arm), thick)
    }
}
