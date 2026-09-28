package com.ultimate.dj.ui
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultimate.dj.Neon
import com.ultimate.dj.audio.DeckPlayer
@Composable
fun DeckStrip(
    decks: List<DeckPlayer>,
    activeA: Int,
    activeB: Int,
    onSelect: (Int) -> Unit,
    onLoadInto: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth().height(52.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        decks.forEachIndexed { idx, deck ->
            val accent = Neon.DECK_COLORS[idx % Neon.DECK_COLORS.size]
            val isA = idx == activeA; val isB = idx == activeB
            val loaded = deck.isLoaded
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when {
                            !loaded -> Color(0x11FFFFFF)
                            isA -> accent.copy(alpha = 0.30f)
                            isB -> Neon.MAGENTA.copy(alpha = 0.30f)
                            else -> Neon.PANEL
                        }
                    )
                    .border(
                        width = if (loaded) 1.5.dp else 1.dp,
                        color = if (loaded) accent.copy(alpha = 0.9f) else Color(0x33FFFFFF),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { if (loaded) onSelect(idx) else onLoadInto(idx) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("D${idx + 1}",
                        color = if (loaded) accent else Color(0x66FFFFFF),
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    if (loaded) {
                        Text(if (deck.isPlaying) "▶" else "❚❚",
                            color = accent, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        if (isA || isB) {
                            Text(if (isA) "A" else "B", color = Color.White,
                                fontSize = 7.sp, fontWeight = FontWeight.Black)
                        }
                    } else {
                        Text("+", color = Color(0x88FFFFFF), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
