package com.djpro.mixer.ui
import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.djpro.mixer.Neon
import com.djpro.mixer.audio.SampleFilePlayer
import com.djpro.mixer.audio.SampleStore
import com.djpro.mixer.audio.SampleSynth
enum class PadMode { SAMPLER, HOTCUE, LOOP, STEMS }
private val PAD_NAMES = listOf("KICK", "SNARE", "HIHAT", "CLAP", "PERC", "RISER", "LASER", "SIREN")
@Composable
fun SamplerDrawer(
    context: Context,
    revision: Long,
    onRequestImport: (Int) -> Unit,
    onClearPad: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(Neon.PANEL_GLASS)
        .border(1.dp, Neon.MAGENTA.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
        .padding(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("SAMPLER", color = Neon.MAGENTA, fontSize = 10.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            Spacer(Modifier.weight(1f))
            Text("TAP = PLAY  •  LONG-PRESS = IMPORT",
                color = Neon.TEXT_FAINT, fontSize = 7.sp, fontWeight = FontWeight.Bold)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (i in 0 until 8) {
                PadSlot(context, i, revision, onRequestImport, onClearPad, Modifier.weight(1f))
            }
        }
    }
}
@Composable
private fun PadSlot(
    context: Context, index: Int, revision: Long,
    onRequestImport: (Int) -> Unit, onClearPad: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val name = PAD_NAMES[index]
    val accent = if (index % 2 == 0) Neon.MAGENTA else Neon.PURPLE
    val assigned = remember(index, revision) { SampleStore.getAssignedUri(context, index) }
    val isCustom = assigned != null
    Box(
        modifier = modifier.aspectRatio(1.5f)
            .clip(RoundedCornerShape(8.dp))
            .background(accent.copy(alpha = if (isCustom) 0.30f else 0.12f))
            .border(
                width = if (isCustom) 2.dp else 1.dp,
                color = accent.copy(alpha = if (isCustom) 1f else 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .pointerInput(index, revision) {
                detectTapGestures(
                    onTap = {
                        val uri = SampleStore.getAssignedUri(context, index)
                        if (uri != null) SampleFilePlayer.play(context, Uri.parse(uri))
                        else SampleSynth.play(name)
                    },
                    onLongPress = { onRequestImport(index) }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(name, color = accent, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text(if (isCustom) "CUSTOM" else "SYNTH",
                color = accent.copy(alpha = 0.7f), fontSize = 6.sp, fontWeight = FontWeight.Bold)
        }
        if (isCustom) {
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(2.dp)
                .size(14.dp).clip(CircleShape)
                .background(Neon.RED.copy(alpha = 0.7f))
                .clickable { onClearPad(index) },
                contentAlignment = Alignment.Center) {
                Text("×", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
