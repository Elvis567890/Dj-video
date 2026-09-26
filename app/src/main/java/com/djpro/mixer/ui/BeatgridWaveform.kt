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
fun BeatgridWaveform(accent: Color, modifier: Modifier = Modifier, barCount: Int = 140, seed: Int = 42) {
    val bars = remember(seed, barCount) {
        val rnd = Random(seed)
        FloatArray(barCount) { i ->
            val pos = i.toFloat() / barCount
            val env = 1f - abs(pos - 0.5f) * 1.6f
            val beat = if (i % 8 == 0) 1.0f else if (i % 4 == 0) 0.7f else 0.4f
            (0.15f + 0.85f * (env * 0.5f + 0.5f) * beat * (0.5f + 0.5f * rnd.nextFloat())).coerceIn(0.05f, 1f)
        }
    }
    Canvas(modifier = modifier.fillMaxWidth().height(52.dp)) {
        val w = size.width; val h = size.height
        val step = w / barCount
        val barW = (step * 0.62f).coerceAtLeast(1.0f)
        val mid = h / 2f
        for (g in 0..4) { val y = h * g / 4f; drawLine(accent.copy(alpha = 0.06f), Offset(0f, y), Offset(w, y), 0.8f) }
        var i = 0
        while (i < barCount) { val x = i * step; drawLine(accent.copy(alpha = 0.35f), Offset(x, 0f), Offset(x, h), 1f); i += 8 }
        for (i in 0 until barCount) {
            val amp = bars[i] * (h * 0.46f); val x = i * step + step / 2f
            drawLine(accent, Offset(x, mid - amp), Offset(x, mid + amp), barW, StrokeCap.Round)
        }
    }
}
