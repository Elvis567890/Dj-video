package com.djpro.mixer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.djpro.mixer.audio.AudioEngine
import com.djpro.mixer.expand.DeckAnim
import com.djpro.mixer.expand.ExpandableDeckManager
import com.djpro.mixer.expand.VideoMixPiP

private val SAMPLE_VIDEO_1 =
    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
private val SAMPLE_VIDEO_2 =
    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"

@Composable
fun SixDeckScreen(engine: AudioEngine) {
    val manager = remember { ExpandableDeckManager() }
    val layout = manager.state
    var crossfaderValue by remember { mutableFloatStateOf(0.5f) }
    var echoOn by remember { mutableStateOf(false) }
    var filterOn by remember { mutableStateOf(false) }
    var vocalOff by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Neon.BG)
            .padding(8.dp)
    ) {
        // ---- Top bar ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DJ PRO MIXER",
                color = Neon.CYAN,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )
            Text(
                text = manager.crossfaderLabel(),
                color = Neon.TEXT_DIM,
                fontSize = 11.sp
            )
            Text(
                text = "6-DECK / STEMS / VIDEO",
                color = Neon.TEXT_DIM,
                fontSize = 10.sp
            )
        }

        Spacer(Modifier.height(6.dp))

        // ---- Main area ----
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                manager.bigDeckIds.forEach { id ->
                    val isExpanded = id == layout.expandedDeckId
                    val a by animateFloatAsState(
                        targetValue = if (layout.expandedDeckId == null || isExpanded) 1f else 0.6f,
                        label = "focus"
                    )
                    BigDeckPanel(
                        title = "DECK ${id + 1}",
                        accent = Neon.DECK_COLORS[id],
                        isExpanded = isExpanded,
                        onPlayPause = { engine.togglePlay(id) },
                        onLoad = {
                            val url = if (id == 0) SAMPLE_VIDEO_1 else SAMPLE_VIDEO_2
                            engine.loadDeck(id, url)
                        },
                        onScratch = { rate -> engine.scratch(id, rate) },
                        onScratchEnd = { engine.endScratch(id) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .alpha(a)
                            .clickable { manager.tapDeck(id) }
                    )
                }

                AnimatedVisibility(
                    visible = !layout.videoFullscreen,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.weight(1.4f).fillMaxHeight()
                ) {
                    CenterVideoPanel(
                        crossfaderValue = crossfaderValue,
                        onCrossfaderChange = {
                            crossfaderValue = it
                            engine.setCrossfader(it)
                        },
                        echoOn = echoOn,
                        filterOn = filterOn,
                        vocalOff = vocalOff,
                        onEchoToggle = { echoOn = !echoOn },
                        onFilterToggle = { filterOn = !filterOn },
                        onVocalToggle = { vocalOff = !vocalOff }
                    )
                }
            }

            AnimatedVisibility(
                visible = layout.expandedDeckId != null && (layout.expandedDeckId ?: 0) > 1,
                enter = DeckAnim.pipEnter,
                exit = DeckAnim.pipExit,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp)
            ) {
                VideoMixPiP()
            }
        }

        Spacer(Modifier.height(8.dp))

        // ---- Bottom row mini decks ----
        Row(
            modifier = Modifier.fillMaxWidth().height(140.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            manager.miniDeckIds.forEach { id ->
                MiniDeckPanel(
                    title = "DECK ${id + 1}",
                    accent = Neon.DECK_COLORS[id],
                    onPlayPause = { engine.togglePlay(id) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { manager.tapDeck(id) }
                )
            }
            repeat(4 - manager.miniDeckIds.size) {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BigDeckPanel(
    title: String,
    accent: Color,
    isExpanded: Boolean,
    onPlayPause: () -> Unit,
    onLoad: () -> Unit,
    onScratch: (Float) -> Unit,
    onScratchEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ring by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0.4f,
        label = "ring"
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Neon.PANEL)
            .border(
                width = if (isExpanded) 2.dp else 1.dp,
                color = accent.copy(alpha = if (isExpanded) 1f else 0.35f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = accent,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Box(
            modifier = Modifier
                .size(180.dp)
                .clip(RoundedCornerShape(90.dp))
                .background(Color(0xFF16161E))
                .border(3.dp, accent.copy(alpha = ring), RoundedCornerShape(90.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change: PointerInputChange, drag: Offset ->
                            change.consume()
                            val rate = 1f - (drag.y / 120f)
                            onScratch(rate)
                        },
                        onDragEnd = { onScratchEnd() },
                        onDragCancel = { onScratchEnd() }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text("JOG", color = accent.copy(alpha = 0.5f), fontSize = 12.sp)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            StemButton("VOX", accent)
            StemButton("DRM", accent)
            StemButton("BAS", accent)
            StemButton("OTH", accent)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TransportBtn("LOAD", accent, onLoad)
            TransportBtn("\u25B6", accent, onPlayPause)
            TransportBtn("CUE", accent) {}
        }
    }
}

@Composable
private fun MiniDeckPanel(
    title: String,
    accent: Color,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Neon.PANEL)
            .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            color = accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(29.dp))
                .background(Color(0xFF16161E))
                .border(2.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(29.dp))
        )
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            TransportBtn("L", accent) {}
            TransportBtn("\u25B6", accent, onPlayPause)
        }
    }
}

@Composable
private fun CenterVideoPanel(
    crossfaderValue: Float,
    onCrossfaderChange: (Float) -> Unit,
    echoOn: Boolean,
    filterOn: Boolean,
    vocalOff: Boolean,
    onEchoToggle: () -> Unit,
    onFilterToggle: () -> Unit,
    onVocalToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .background(Neon.PANEL)
            .border(1.dp, Color(0xFF2A2A3A), RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black)
                .border(1.dp, Neon.CYAN.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("VIDEO MIX", color = Neon.CYAN.copy(alpha = 0.4f), fontSize = 12.sp)
        }

        Slider(
            value = crossfaderValue,
            onValueChange = onCrossfaderChange,
            colors = SliderDefaults.colors(
                thumbColor = Neon.CYAN,
                activeTrackColor = Neon.CYAN,
                inactiveTrackColor = Neon.MAGENTA.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(6.dp)) {
            FxChip("ECHO", Neon.CYAN, echoOn, onEchoToggle, Modifier.weight(1f))
            FxChip("FILTER", Neon.LIME, filterOn, onFilterToggle, Modifier.weight(1f))
            FxChip("VOCAL OFF", Neon.PURPLE, vocalOff, onVocalToggle, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(6.dp)) {
            FxChip("REVERB", Neon.ORANGE, false, {}, Modifier.weight(1f))
            FxChip("REC", Neon.RED, false, {}, Modifier.weight(1f))
            FxChip("MASTER", Neon.MAGENTA, false, {}, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StemButton(label: String, accent: Color) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Neon.BTN_BG)
            .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = accent.copy(alpha = 0.7f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TransportBtn(label: String, accent: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Neon.BTN_BG)
            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun FxChip(
    label: String,
    accent: Color,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (active) accent.copy(alpha = 0.35f)
                else accent.copy(alpha = 0.12f)
            )
            .border(
                width = if (active) 2.dp else 1.dp,
                color = accent.copy(alpha = if (active) 1f else 0.5f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
