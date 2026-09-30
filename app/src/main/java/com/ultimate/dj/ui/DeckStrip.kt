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
    onSelectA: (Int) -> Unit,
    onSelectB: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth().height(32.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        decks.forEachIndexed { idx, deck ->
            val accent = Neon.DECK_COLORS[idx % Neon.DECK_COLORS.size]
            val isA = idx == activeA; val isB = idx == activeB
            val active = isA || isB
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (active) accent.copy(alpha = 0.25f) else Neon.PANEL)
                    .border(
                        width = if (active) 1.dp else 0.5.dp,
                        color = if (active) accent else Neon.BORDER_SOFT,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .clickable {
                        if (isB) onSelectB(idx) else onSelectA(idx)
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("D${idx + 1}",
                        color = if (active) Color.White else Neon.TEXT_DIM,
                        fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    if (active) {
                        Spacer(Modifier.width(3.dp))
                        Text(if (isA) "A" else "B",
                            color = accent, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
