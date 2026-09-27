package com.djpro.mixer.ui
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize          import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.djpro.mixer.Neon
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random
@Composable
fun AudioVisualizer(accent: Color, isPlaying: Boolean, trackName: String, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "viz")
    val phase by infinite.animateFloat(0f, 1000f,
        infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart), label = "phase")
    val pulse by infinite.animateFloat(0.75f, 1f,
        infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse), label = "pulse")
    val seed = trackName.hashCode().let { if (it == 0) 17 else it }
    val bars = remember(seed) {
        val rnd = Random(seed)
        FloatArray(48) { i ->
            val pos = i.toFloat() / 48
            val env = 1f - abs(pos - 0.5f) * 1.2f
            (0.25f + 0.75f * env * (0.4f + 0.6f * rnd.nextFloat())).coerceIn(0.1f, 1f)
        }
    }
    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = if (isPlaying) 0.25f * pulse else 0.08f), Color.Transparent),
                    center = c, radius = size.minDimension * 0.7f),
                radius = size.minDimension * 0.7f, center = c)
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height; val mid = h / 2f
            val n = bars.size; val step = w / n; val barW = (step * 0.7f).coerceAtLeast(1.5f)
            for (i in 0 until n) {
                val base = bars[i]
                val anim = if (isPlaying) (0.4f + 0.6f * abs(sin(phase * 0.06f + i * 0.35f))) else 0.15f
                val amp = base * anim * (h * 0.42f); val x = i * step + step / 2f
                drawLine(accent.copy(alpha = 0.95f), Offset(x, mid - amp), Offset(x, mid + amp), barW, StrokeCap.Round)
            }
            drawLine(accent.copy(alpha = 0.5f), Offset(0f, mid), Offset(w, mid), 1.5f)
        }
        Box(modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
            .clip(RoundedCornerShape(50)).background(accent.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 4.dp)) {
            Text("AUDIO", color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
        }
        if (trackName.isNotEmpty()) {
            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
                .clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 14.dp, vertical = 5.dp)) {
                Text(trackName.uppercase().take(40), color = Color.White.copy(alpha = 0.85f),
                    fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }
        }
        if (isPlaying) {
            Canvas(modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).size(10.dp)) {
                drawCircle(color = Neon.LIVE)
            }
        }
    }
}
