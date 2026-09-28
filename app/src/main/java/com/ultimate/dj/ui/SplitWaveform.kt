package com.ultimate.dj.ui
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
@Composable
fun SplitWaveform(
    accent: Color,
    waveform: FloatArray,
    bpm: Float,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier.fillMaxWidth().height(28.dp)) {
            val w = size.width; val h = size.height
            val n = if (waveform.isNotEmpty()) waveform.size else 140
            val step = w / n; val mid = h / 2f
            for (i in 0 until n) {
                val base = if (waveform.isNotEmpty()) waveform[i].coerceIn(0.02f, 1f) else 0.3f
                val amp = base * (h * 0.42f); val x = i * step + step / 2f
                drawLine(accent.copy(alpha = 0.85f), Offset(x, mid - amp), Offset(x, mid + amp), step.coerceAtLeast(1f), StrokeCap.Round)
            }
            val px = w * progress.coerceIn(0f, 1f)
            drawLine(Color.White, Offset(px, 0f), Offset(px, h), 2f)
        }
        Canvas(modifier = Modifier.fillMaxWidth().height(28.dp)) {
            val w = size.width; val h = size.height
            val n = if (waveform.isNotEmpty()) waveform.size else 140
            val zoom = 4
            val visible = n / zoom
            val center = (progress * n).toInt().coerceIn(0, n - 1)
            val start = (center - visible / 2).coerceIn(0, (n - visible).coerceAtLeast(0))
            val step = w / visible; val mid = h / 2f
            for (i in 0 until visible) {
                val idx = (start + i).coerceIn(0, n - 1)
                val base = if (waveform.isNotEmpty()) waveform[idx].coerceIn(0.02f, 1f) else 0.3f
                val amp = base * (h * 0.42f); val x = i * step + step / 2f
                drawLine(accent, Offset(x, mid - amp), Offset(x, mid + amp), (step * 0.7f).coerceAtLeast(1f), StrokeCap.Round)
            }
            drawLine(Color.White, Offset(w / 2f, 0f), Offset(w / 2f, h), 2f)
        }
    }
}
