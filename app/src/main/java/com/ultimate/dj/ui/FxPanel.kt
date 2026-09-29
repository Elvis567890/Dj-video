package com.ultimate.dj.ui
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultimate.dj.Neon
val FX_NAMES = listOf("ECHO", "FILTER", "REVERB", "FLANGER", "PHASER", "ROLL", "CENSOR", "DELAY")
@Composable
fun FxPanel(
    onFxChange: (slot: Int, fxName: String, amount: Float, enabled: Boolean, assignDeck: Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var slot1Name by remember { mutableStateOf("ECHO") }
    var slot2Name by remember { mutableStateOf("FILTER") }
    var slot1Amount by remember { mutableFloatStateOf(0.5f) }
    var slot2Amount by remember { mutableFloatStateOf(0.5f) }
    var slot1On by remember { mutableStateOf(false) }
    var slot2On by remember { mutableStateOf(false) }
    var slot1Deck by remember { mutableStateOf(0) }
    var slot2Deck by remember { mutableStateOf(1) }
    Column(modifier = modifier.fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(Neon.PANEL_GLASS)
        .border(0.5.dp, Neon.PURPLE.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
        .padding(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("FX RACK", color = Neon.PURPLE, fontSize = 10.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
            Spacer(Modifier.weight(1f))
            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
                .background(Neon.BTN_BG)
                .border(0.5.dp, Neon.TEXT_FAINT.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .clickable { onClose() }
                .padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text("CLOSE", color = Neon.TEXT_DIM, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FxSlot(1, slot1Name, slot1Amount, slot1On, slot1Deck,
                { slot1Name = it; onFxChange(0, it, slot1Amount, slot1On, slot1Deck) },
                { slot1Amount = it; onFxChange(0, slot1Name, it, slot1On, slot1Deck) },
                { slot1On = it; onFxChange(0, slot1Name, slot1Amount, it, slot1Deck) },
                { slot1Deck = it; onFxChange(0, slot1Name, slot1Amount, slot1On, it) },
                Modifier.weight(1f))
            FxSlot(2, slot2Name, slot2Amount, slot2On, slot2Deck,
                { slot2Name = it; onFxChange(1, it, slot2Amount, slot2On, slot2Deck) },
                { slot2Amount = it; onFxChange(1, slot2Name, it, slot2On, slot2Deck) },
                { slot2On = it; onFxChange(1, slot2Name, slot2Amount, it, slot2Deck) },
                { slot2Deck = it; onFxChange(1, slot2Name, slot2Amount, slot2On, it) },
                Modifier.weight(1f))
        }
    }
}
@Composable
private fun FxSlot(
    slotNum: Int,
    name: String, amount: Float, enabled: Boolean, deck: Int,
    onName: (String) -> Unit,
    onAmount: (Float) -> Unit,
    onEnabled: (Boolean) -> Unit,
    onDeck: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = if (slotNum == 1) Neon.PURPLE else Neon.BLUE
    Column(modifier = modifier
        .clip(RoundedCornerShape(6.dp))
        .background(Neon.PANEL_DARK)
        .border(0.5.dp, accent.copy(alpha = if (enabled) 0.9f else 0.25f), RoundedCornerShape(6.dp))
        .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("FX $slotNum", color = accent, fontSize = 8.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
                .background(if (enabled) accent.copy(alpha = 0.30f) else Neon.BTN_BG)
                .border(0.5.dp, accent.copy(alpha = if (enabled) 1f else 0.25f), RoundedCornerShape(4.dp))
                .clickable { onEnabled(!enabled) }
                .padding(horizontal = 8.dp, vertical = 3.dp)) {
                Text(if (enabled) "ON" else "OFF", color = accent, fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            for (fx in FX_NAMES) {
                val sel = fx == name
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(3.dp))
                    .background(if (sel) accent.copy(alpha = 0.25f) else Neon.BTN_BG)
                    .border(0.5.dp, accent.copy(alpha = if (sel) 0.9f else 0.15f), RoundedCornerShape(3.dp))
                    .clickable { onName(fx) }
                    .padding(vertical = 3.dp), contentAlignment = Alignment.Center) {
                    Text(fx.take(3), color = if (sel) accent else Neon.TEXT_FAINT,
                        fontSize = 6.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text(name, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("AMT", color = Neon.TEXT_FAINT, fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.width(4.dp))
            Slider(value = amount, onValueChange = onAmount,
                colors = SliderDefaults.colors(thumbColor = accent,
                    activeTrackColor = accent.copy(alpha = 0.6f),
                    inactiveTrackColor = Neon.BORDER_SOFT),
                modifier = Modifier.weight(1f).height(16.dp))
            Text("${(amount * 100).toInt()}", color = accent, fontSize = 8.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.width(24.dp))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (d in 0..1) {
                val sel = deck == d
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(3.dp))
                    .background(if (sel) accent.copy(alpha = 0.25f) else Neon.BTN_BG)
                    .border(0.5.dp, accent.copy(alpha = if (sel) 0.9f else 0.20f), RoundedCornerShape(3.dp))
                    .clickable { onDeck(d) }
                    .padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                    Text(if (d == 0) "DECK A" else "DECK B",
                        color = if (sel) accent else Neon.TEXT_FAINT,
                        fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }
    }
}
