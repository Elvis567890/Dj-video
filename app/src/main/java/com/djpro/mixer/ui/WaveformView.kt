package com.djpro.mixer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.random.Random

@Composable
fun WaveformView(accent: Color, modifier: Modifier = Modifier, barCount: Int = 90, seed: Int = 42) {
    val bars = remember(seed, barCount) {
        val rnd = Random(seed)
        FloatArray(barCount) { i ->
            val pos = i.toFloat() / barCount
            val env = 1f - abs(pos - 0.5f) * 1.3f
            (0.25f + 0.75f * env * rnd.nextFloat()).coerceIn(0.1f, 1f)
        }
    }
    Canvas(modifier = modifier.fillMaxWidth().height(28.dp)) {
        val w = size.width; val h = size.height
        val step = w / barCount
        val barW = (step * 0.55f).coerceAtLeast(1.2f)
        val mid = h / 2f
        for (i in 0 until barCount) {
            val amp = bars[i] * (h * 0.46f)
            val x = i * step + step / 2f
            drawLine(accent.copy(alpha = 0.85f), Offset(x, mid - amp), Offset(x, mid + amp), barW, StrokeCap.Round)
            drawLine(accent.copy(alpha = 0.25f), Offset(x, mid - amp * 1.35f), Offset(x, mid + amp * 1.35f), barW * 0.4f, StrokeCap.Round)
        }
        drawLine(accent.copy(alpha = 0.3f), Offset(0f, mid), Offset(w, mid), 0.8f)
    }
}
