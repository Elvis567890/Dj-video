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
    Row(modifier = modifier.fillMaxWidth().height(40.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        decks.forEachIndexed { idx, deck ->
            val accent = Neon.DECK_COLORS[idx % Neon.DECK_COLORS.size]
            val isA = idx == activeA; val isB = idx == activeB
            val loaded = deck.isLoaded
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        when {
                            !loaded -> Color(0x08FFFFFF)
                            isA || isB -> accent.copy(alpha = 0.20f)
                            else -> Neon.PANEL
                        }
                    )
                    .border(
                        width = if (isA || isB) 1.dp else 0.5.dp,
                        color = when {
                            !loaded -> Color(0x15FFFFFF)
                            isA || isB -> accent
                            else -> Color(0x20FFFFFF)
                        },
                        shape = RoundedCornerShape(4.dp)
                    )
                    .clickable { if (loaded) onSelect(idx) else onLoadInto(idx) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("D${idx + 1}",
                        color = if (loaded) accent else Color(0x40FFFFFF),
                        fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    if (loaded) {
                        Text(if (isA) "A" else if (isB) "B" else "•",
                            color = if (isA || isB) Color.White else accent.copy(alpha = 0.5f),
                            fontSize = 7.sp, fontWeight = FontWeight.Black)
                    } else {
                        Text("+", color = Color(0x33FFFFFF), fontSize = 11.sp, fontWeight = FontWeight.Light)
                    }
                }
            }
        }
    }
}
