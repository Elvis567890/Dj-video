package com.djpro.mixer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.djpro.mixer.expand.*

private val deckColors = listOf(
    Neon.CYAN, Neon.MAGENTA, Neon.LIME, Neon.ORANGE, Neon.PURPLE, Neon.RED
)

@Composable
fun SixDeckScreen() {
    val manager = remember { ExpandableDeckManager() }
    val layout = manager.state

    Column(
        modifier = Modifier.fillMaxSize().background(Neon.BG).padding(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("DJ PRO MIXER", color = Neon.CYAN, fontSize = 18.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
            Text(manager.crossfaderLabel(), color = Neon.TEXT_DIM, fontSize = 11.sp)
            Text("6-DECK / STEMS / VIDEO", color = Neon.TEXT_DIM, fontSize = 10.sp)
        }

        Spacer(Modifier.height(6.dp))

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                manager.bigDeckIds.forEach { id ->
                    val isExpanded = id == layout.expandedDeckId
                    val a by animateFloatAsState(
                        targetValue = if (layout.expandedDeckId == null || isExpanded) 1f else 0.6f,
                        label = "focus",
                    )
                    BigDeckPanel(
                        title = "DECK ${id + 1}",
                        accent = deckColors[id],
                        isExpanded = isExpanded,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .alpha(a)
                            .clickable { manager.tapDeck(id) },
                    )
                }
                AnimatedVisibility(
                    visible = !layout.videoFullscreen,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.weight(1.4f).fillMaxHeight(),
                ) {
                    CenterVideoPanel(manager) { manager.toggleVideoFullscreen() }
                }
            }

            AnimatedVisibility(
                visible = layout.expandedDeckId != null && (layout.expandedDeckId ?: 0) > 1,
                enter = DeckAnim.pipEnter,
                exit = DeckAnim.pipExit,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp),
            ) {
                VideoMixPiP(
                    manager = manager,
                    onTap = { manager.collapse() },
                    onDoubleTap = { manager.toggleVideoFullscreen() },
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth().height(140.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            manager.miniDeckIds.forEach { id ->
                MiniDeckPanel(
                    title = "DECK ${id + 1}",
                    accent = deckColors[id],
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { manager.tapDeck(id) },
                )
            }
            repeat(4 - manager.miniDeckIds.size) {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BigDeckPanel(title: String, accent: Color, isExpanded: Boolean, modifier: Modifier) {
    val ring by animateFloatAsState(if (isExpanded) 1f else 0.4f, label = "ring")
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Neon.PANEL)
            .border(if (isExpanded) 2.dp else 1.dp,
                accent.copy(alpha = if (isExpanded) 1f else 0.35f),
                RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, color = accent, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Box(
            modifier = Modifier
                .size(180.dp)
                .clip(RoundedCornerShape(90.dp))
                .background(Color(0xFF16161E))
                .border(3.dp, accent.copy(alpha = ring), RoundedCornerShape(90.dp))
                .pointerInput(Unit) { detectDragGestures { c, _ -> c.consume() } },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            StemButton("VOX", accent)
            StemButton("DRM", accent)
            StemButton("BAS", accent)
            StemButton("OTH", accent)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TransportBtn("LOAD", accent)
            TransportBtn("\u25B6", accent)
            TransportBtn("CUE", accent)
        }
    }
}

@Composable
private fun MiniDeckPanel(title: String, accent: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Neon.PANEL)
            .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(29.dp))
                .background(Color(0xFF16161E))
                .border(2.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(29.dp)),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            TransportBtn("L", accent)
            TransportBtn("\u25B6", accent)
        }
    }
}

@Composable
private fun CenterVideoPanel(manager: ExpandableDeckManager, onVideoTap: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .background(Neon.PANEL)
            .border(1.dp, Color(0xFF2A2A3A), RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black)
                .border(1.dp, Neon.CYAN.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                .clickable { onVideoTap() },
            contentAlignment = Alignment.Center,
        ) {
            Text("VIDEO MIX", color = Neon.CYAN.copy(alpha = 0.4f), fontSize = 12.sp)
        }
        Text(manager.crossfaderLabel(), color = Neon.TEXT_DIM, fontSize = 9.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally))
        Slider(
            value = 0.5f,
            onValueChange = {},
            colors = SliderDefaults.colors(
                thumbColor = Neon.CYAN,
                activeTrackColor = Neon.CYAN,
                inactiveTrackColor = Neon.MAGENTA.copy(alpha = 0.5f),
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(6.dp)) {
            FxChip("ECHO", Neon.CYAN, Modifier.weight(1f))
            FxChip("FILTER", Neon.LIME, Modifier.weight(1f))
            FxChip("FLANGE", Neon.PURPLE, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(6.dp)) {
            FxChip("REVERB", Neon.ORANGE, Modifier.weight(1f))
            FxChip("REC", Neon.RED, Modifier.weight(1f))
            FxChip("MASTER", Neon.MAGENTA, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StemButton(label: String, accent: Color) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A1A24))
            .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = accent.copy(alpha = 0.7f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TransportBtn(label: String, accent: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A1A24))
            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(label, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FxChip(label: String, accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
