package com.ultimate.dj.ui
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
import com.ultimate.dj.Neon
import com.ultimate.dj.audio.SampleFilePlayer
import com.ultimate.dj.audio.SampleStore
import com.ultimate.dj.audio.SampleSynth
enum class PadMode { SAMPLER, HOTCUE, LOOP, STEMS }
private val PAD_NAMES = listOf("KICK", "SNARE", "HIHAT", "CLAP", "PERC", "RISER", "LASER", "SIREN")
@Composable
fun SamplerDrawer(
    context: Context,
    bank: Int,
    revision: Long,
    onRequestImport: (Int) -> Unit,
    onClearPad: (Int) -> Unit,
    onBankChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()
        .clip(RoundedCornerShape(6.dp))
        .background(Neon.PANEL_GLASS)
        .border(0.5.dp, Neon.AMBER.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
        .padding(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("SAMPLER", color = Neon.AMBER, fontSize = 9.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            Spacer(Modifier.width(12.dp))
            for (b in 0 until 4) {
                val sel = b == bank
                Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
                    .background(if (sel) Neon.AMBER.copy(alpha = 0.20f) else Neon.BTN_BG)
                    .border(0.5.dp, Neon.AMBER.copy(alpha = if (sel) 0.9f else 0.20f), RoundedCornerShape(4.dp))
                    .clickable { onBankChange(b) }
                    .padding(horizontal = 8.dp, vertical = 2.dp)) {
                    Text("B${b + 1}", color = if (sel) Neon.AMBER else Neon.TEXT_DIM, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(4.dp))
            }
            Spacer(Modifier.weight(1f))
            Text("TAP PLAY · HOLD IMPORT",
                color = Neon.TEXT_FAINT, fontSize = 7.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (i in 0 until 8) {
                PadSlot(context, bank * 8 + i, i, revision, onRequestImport, onClearPad, Modifier.weight(1f))
            }
        }
    }
}
@Composable
private fun PadSlot(
    context: Context, absoluteIndex: Int, displayIndex: Int, revision: Long,
    onRequestImport: (Int) -> Unit, onClearPad: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val name = PAD_NAMES[displayIndex]
    val accent = if (displayIndex % 2 == 0) Neon.AMBER else Neon.PURPLE
    val assigned = remember(absoluteIndex, revision) { SampleStore.getAssignedUri(context, absoluteIndex) }
    val isCustom = assigned != null
    Box(
        modifier = modifier.aspectRatio(1.5f)
            .clip(RoundedCornerShape(4.dp))
            .background(accent.copy(alpha = if (isCustom) 0.18f else 0.06f))
            .border(
                width = if (isCustom) 1.dp else 0.5.dp,
                color = accent.copy(alpha = if (isCustom) 0.85f else 0.30f),
                shape = RoundedCornerShape(4.dp)
            )
            .pointerInput(absoluteIndex, revision) {
                detectTapGestures(
                    onTap = {
                        val uri = SampleStore.getAssignedUri(context, absoluteIndex)
                        if (uri != null) SampleFilePlayer.play(context, Uri.parse(uri))
                        else SampleSynth.play(name)
                    },
                    onLongPress = { onRequestImport(absoluteIndex) }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(name, color = accent, fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            Text(if (isCustom) "USER" else "SYN",
                color = accent.copy(alpha = 0.5f), fontSize = 5.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
        }
        if (isCustom) {
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(2.dp)
                .size(12.dp).clip(CircleShape)
                .background(Neon.RED.copy(alpha = 0.6f))
                .clickable { onClearPad(absoluteIndex) },
                contentAlignment = Alignment.Center) {
                Text("×", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
