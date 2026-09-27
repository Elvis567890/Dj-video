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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

/**
 * Animated audio visualizer. Shows when a deck has audio-only content.
 * Looks like a spectrum analyzer with flowing bars and glowing centre line.
 */
@Composable
fun AudioVisualizer(
    accent: Color,
    isPlaying: Boolean,
    trackName: String,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "viz")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Seeded bar envelope (per-deck look, static shape)
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

        // Radial glow background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = if (isPlaying) 0.25f * pulse else 0.08f),
                        Color.Transparent
                    ),
                    center = c,
                    radius = size.minDimension * 0.7f
                ),
                radius = size.minDimension * 0.7f,
                center = c
            )
        }

        // Spectrum bars
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val mid = h / 2f
            val n = bars.size
            val step = w / n
            val barW = (step * 0.7f).coerceAtLeast(1.5f)

            for (i in 0 until n) {
                val base = bars[i]
                // Animate amplitude using sine offset
                val anim = if (isPlaying)
                    (0.4f + 0.6f * abs(sin(phase * 0.06f + i * 0.35f)))
                else 0.15f

                val amp = base * anim * (h * 0.42f)

                val x = i * step + step / 2f

                // Main bar
                drawLine(
                    color = accent.copy(alpha = 0.95f),
                    start = Offset(x, mid - amp),
                    end = Offset(x, mid + amp),
                    strokeWidth = barW,
                    cap = StrokeCap.Round
                )

                // Soft glow above/below
                drawLine(
                    color = accent.copy(alpha = 0.25f),
                    start = Offset(x, mid - amp * 1.15f),
                    end = Offset(x, mid + amp * 1.15f),
                    strokeWidth = barW * 0.4f,
                    cap = StrokeCap.Round
                )

                // Peak dot
                drawCircle(
                    color = Color.White.copy(alpha = 0.7f),
                    radius = 1.5f,
                    center = Offset(x, mid - amp - 3f)
                )
            }

            // Centre scan line
            drawLine(
                color = accent.copy(alpha = 0.5f),
                start = Offset(0f, mid),
                end = Offset(w, mid),
                strokeWidth = 1.5f
            )
        }

        // Corner brackets
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            val arm = 26f; val thick = 3f
            drawLine(accent.copy(alpha = 0.8f), Offset(0f, 0f), Offset(arm, 0f), thick)
            drawLine(accent.copy(alpha = 0.8f), Offset(0f, 0f), Offset(0f, arm), thick)
            drawLine(accent.copy(alpha = 0.8f), Offset(w, 0f), Offset(w - arm, 0f), thick)
            drawLine(accent.copy(alpha = 0.8f), Offset(w, 0f), Offset(w, h * 0f + arm), thick)
            drawLine(accent.copy(alpha = 0.8f), Offset(0f, h), Offset(arm, h), thick)
            drawLine(accent.copy(alpha = 0.8f), Offset(0f, h), Offset(0f, h - arm), thick)
            drawLine(accent.copy(alpha = 0.8f), Offset(w, h), Offset(w - arm, h), thick)
            drawLine(accent.copy(alpha = 0.8f), Offset(w, h), Offset(w, h - arm), thick)
        }

        // Top pill: "AUDIO"
        Box(modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
            .background(accent.copy(alpha = 0.15f), androidx.compose.foundation.shape.RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = "AUDIO",
                color = accent,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp
            )
        }

        // Bottom pill: track name
        if (trackName.isNotEmpty()) {
            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
                .background(Color.Black.copy(alpha = 0.6f), androidx.compose.foundation.shape.RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Text(
                    text = trackName.uppercase().take(40),
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
        }

        // LIVE dot top-right
        if (isPlaying) {
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(color = Neon.LIVE, radius = 5f)
                }
            }
        }
    }
}

private fun <T> remember(key: Any?, calculation: () -> T): T =
    androidx.compose.runtime.remember(key, calculation)
