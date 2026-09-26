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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.djpro.mixer.Neon
import kotlin.math.abs
import kotlin.random.Random

/**
 * Visual overlay shown when a deck is being scratched.
 * Renders:
 *  - animated glitch bars
 *  - speed/rate badge ("SCRATCH ×1.4")
 *  - corner neon brackets
 *  - scanning line based on scratch velocity
 */
@Composable
fun ScratchOverlay(
    accent: Color,
    rate: Float,
    velocity: Float,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "scratch")
    val phase by infinite.animateFloat(
        0f, 1000f,
        infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Glitch bars canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val intensity = (abs(velocity) * 1.2f).coerceIn(0f, 1.5f)

            // Random horizontal slices
            val rnd = Random((phase.toInt() * 7919))
            val sliceCount = (10 + (intensity * 20)).toInt()
            for (i in 0 until sliceCount) {
                val y = rnd.nextFloat() * h
                val sliceH = 1f + rnd.nextFloat() * (3f + 8f * intensity)
                val xOff = (rnd.nextFloat() - 0.5f) * 60f * intensity
                val alpha = 0.15f + 0.4f * intensity * rnd.nextFloat()
                drawRect(
                    color = if (rnd.nextBoolean()) accent.copy(alpha = alpha)
                            else Neon.MAGENTA.copy(alpha = alpha),
                    topLeft = Offset(xOff, y),
                    size = androidx.compose.ui.geometry.Size(w, sliceH)
                )
            }

            // Horizontal scan line sweeping based on velocity direction
            val scanY = ((phase * 2f * (if (velocity >= 0) 1f else -1f)) % h + h) % h
            drawLine(
                color = accent.copy(alpha = 0.7f),
                start = Offset(0f, scanY),
                end = Offset(w, scanY),
                strokeWidth = 2f
            )

            // Vignette glow when scratching
            val glowAlpha = 0.25f + 0.35f * intensity
            drawRect(color = accent.copy(alpha = glowAlpha * 0.15f))
        }

        // Corner brackets (Top-left)
        CornerBrackets(accent)

        // Rate badge (top-center)
        val displayRate = if (rate < 0f) "REV ×${"%.2f".format(abs(rate))}"
                          else "×${"%.2f".format(rate)}"
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.7f))
                .border(1.5.dp, accent, RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 4.dp)
        ) {
            Text(
                "SCRATCH $displayRate",
                color = accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CornerBrackets(accent: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val arm = 24f
        val thick = 3f
        // TL
        drawLine(accent, Offset(0f, 0f), Offset(arm, 0f), thick)
        drawLine(accent, Offset(0f, 0f), Offset(0f, arm), thick)
        // TR
        drawLine(accent, Offset(w, 0f), Offset(w - arm, 0f), thick)
        drawLine(accent, Offset(w, 0f), Offset(w, arm), thick)
        // BL
        drawLine(accent, Offset(0f, h), Offset(arm, h), thick)
        drawLine(accent, Offset(0f, h), Offset(0f, h - arm), thick)
        // BR
        drawLine(accent, Offset(w, h), Offset(w - arm, h), thick)
        drawLine(accent, Offset(w, h), Offset(w, h - arm), thick)
    }
}
