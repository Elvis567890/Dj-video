package com.djpro.mixer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.djpro.mixer.audio.AudioEngine
import com.djpro.mixer.audio.DeckPlayer
import com.djpro.mixer.expand.ExpandableDeckManager
import com.djpro.mixer.expand.VideoDeckView
import com.djpro.mixer.expand.VideoMixPiP
import com.djpro.mixer.expand.VideoTransition
import com.djpro.mixer.ui.EqKnob
import com.djpro.mixer.ui.JogWheel
import com.djpro.mixer.ui.MasterVuMeter
import com.djpro.mixer.ui.VerticalFader
import com.djpro.mixer.ui.WaveformView
import kotlinx.coroutines.delay

private const val SAMPLE_VIDEO_1 =
    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
private const val SAMPLE_VIDEO_2 =
    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"

@Composable
fun SixDeckScreen(engine: AudioEngine, activity: Activity) {
    val context = LocalContext.current
    val manager = remember { ExpandableDeckManager() }
    val layout = manager.state
    var crossfaderValue by remember { mutableFloatStateOf(0.5f) }
    var echoOn by remember { mutableStateOf(false) }
    var filterOn by remember { mutableStateOf(false) }
    var flangeOn by remember { mutableStateOf(false) }
    var reverbOn by remember { mutableStateOf(false) }
    var recOn by remember { mutableStateOf(false) }
    var vocalOff by remember { mutableStateOf(false) }
    var anyPlaying by remember { mutableStateOf(false) }
    var clubMode by remember { mutableStateOf(false) }
    var transition by remember { mutableStateOf(VideoTransition.CROSSFADE) }
    var pendingDeckId by remember { mutableStateOf<Int?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        val id = pendingDeckId; pendingDeckId = null
        if (uri != null && id != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Throwable) {}
            val name = uri.lastPathSegment?.substringAfterLast('/') ?: "Picked"
            engine.loadDeck(id, uri.toString(), name)
        }
    }

    fun pickFileForDeck(deckId: Int) {
        pendingDeckId = deckId
        filePicker.launch(arrayOf("video/*", "audio/*"))
    }

    LaunchedEffect(Unit) {
        while (true) { anyPlaying = engine.decks.any { it.isPlaying }; delay(400) }
    }

    Column(
        modifier = Modifier.fillMaxSize()
            .background(if (clubMode) Neon.CLUB_BG else Neon.BG)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        TopBar(playing = anyPlaying, clubMode = clubMode, onClubToggle = { clubMode = !clubMode })
        Spacer(Modifier.height(4.dp))
        MasterVuMeter(playing = anyPlaying)
        Spacer(Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                manager.bigDeckIds.forEach { id ->
                    val isExpanded = id == layout.expandedDeckId
                    val a by animateFloatAsState(
                        targetValue = if (layout.expandedDeckId == null || isExpanded) 1f else 0.6f,
                        label = "focus"
                    )
                    BigDeckPanel(
                        deck = engine.decks[id], engine = engine,
                        title = "DECK ${id + 1}", accent = Neon.DECK_COLORS[id],
                        isExpanded = isExpanded,
                        onPlayPause = { engine.togglePlay(id) },
                        onPickFile = { pickFileForDeck(id) },
                        onLoadSample = {
                            val url = if (id == 0) SAMPLE_VIDEO_1 else SAMPLE_VIDEO_2
                            engine.loadDeck(id, url, "Sample ${id + 1}")
                        },
                        onScratch = { rate -> engine.scratch(id, rate) },
                        onScratchEnd = { engine.endScratch(id) },
                        onSpinback = { engine.spinback(id) },
                        onBrake = { engine.brake(id) },
                        onVocalToggle = { vocalOff = !vocalOff; engine.setVocalRemoval(vocalOff) },
                        onSetCue = { slot -> engine.setCue(id, slot) },
                        onJumpCue = { slot -> engine.jumpCue(id, slot) },
                        onToggleLoop = { beats -> engine.toggleLoop(id, beats) },
                        onSync = { val other = if (id == 0) 1 else 0; engine.syncBpm(other, id) },
                        modifier = Modifier.weight(1f).fillMaxHeight().alpha(a)
                            .clickable { manager.tapDeck(id) }
                    )
                }

                if (!layout.videoFullscreen) {
                    Box(modifier = Modifier.weight(1.4f).fillMaxHeight()) {
                        CenterVideoPanel(
                            engine = engine, transition = transition,
                            onTransitionChange = { transition = it },
                            crossfaderValue = crossfaderValue,
                            onCrossfaderChange = { crossfaderValue = it; engine.setCrossfader(it) },
                            echoOn = echoOn, filterOn = filterOn,
                            flangeOn = flangeOn, reverbOn = reverbOn,
                            recOn = recOn, vocalOff = vocalOff,
                            onEchoToggle = { echoOn = !echoOn; engine.setEcho(echoOn) },
                            onFilterToggle = { filterOn = !filterOn; engine.setFilter(filterOn) },
                            onFlangeToggle = { flangeOn = !flangeOn },
                            onReverbToggle = { reverbOn = !reverbOn },
                            onRecToggle = { recOn = !recOn },
                            onVocalToggle = { vocalOff = !vocalOff; engine.setVocalRemoval(vocalOff) }
                        )
                    }
                } else { Spacer(Modifier.weight(1.4f)) }
            }

            if (layout.expandedDeckId != null && (layout.expandedDeckId ?: 0) > 1) {
                VideoMixPiP(modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp))
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth().height(130.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            manager.miniDeckIds.forEach { id ->
                MiniDeckPanel(
                    deck = engine.decks[id], title = "DECK ${id + 1}",
                    accent = Neon.DECK_COLORS[id],
                    onPlayPause = { engine.togglePlay(id) },
                    onPickFile = { pickFileForDeck(id) },
                    modifier = Modifier.weight(1f).fillMaxHeight().clickable { manager.tapDeck(id) }
                )
            }
            repeat(4 - manager.miniDeckIds.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun TopBar(playing: Boolean, clubMode: Boolean, onClubToggle: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Neon.PANEL)
            .border(1.dp, Neon.CYAN.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center) { Icon(Icons.Filled.Menu, "menu", tint = Neon.CYAN) }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("DJ PRO MIXER", color = Neon.CYAN, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 8.sp)
            Spacer(Modifier.width(12.dp))
            Box(modifier = Modifier.size(8.dp).clip(CircleShape)
                .background(if (playing) Neon.LIVE else Neon.RED.copy(alpha = 0.5f)))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.clip(RoundedCornerShape(50))
                .background(if (clubMode) Neon.CYAN.copy(alpha = 0.3f) else Neon.PANEL)
                .border(1.dp, Neon.CYAN.copy(alpha = 0.5f), RoundedCornerShape(50))
                .clickable { onClubToggle() }.padding(horizontal = 12.dp, vertical = 6.dp)
            ) { Text(if (clubMode) "CLUB ON" else "CLUB", color = Neon.CYAN, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(8.dp))
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Neon.PANEL)
                .border(1.dp, Neon.CYAN.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center) { Icon(Icons.Filled.Settings, "settings", tint = Neon.CYAN) }
        }
    }
}

@Composable
private fun BigDeckPanel(
    deck: DeckPlayer, engine: AudioEngine, title: String, accent: Color,
    isExpanded: Boolean,
    onPlayPause: () -> Unit, onPickFile: () -> Unit, onLoadSample: () -> Unit,
    onScratch: (Float) -> Unit, onScratchEnd: () -> Unit,
    onSpinback: () -> Unit, onBrake: () -> Unit, onVocalToggle: () -> Unit,
    onSetCue: (Int) -> Unit, onJumpCue: (Int) -> Unit, onToggleLoop: (Int) -> Unit,
    onSync: () -> Unit, modifier: Modifier = Modifier
) {
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    val isPlaying = deck.isPlaying
    val scratching = deck.scratching
    val scratchRate = deck.scratchRate

    LaunchedEffect(deck) {
        while (true) {
            positionMs = deck.positionMs(); durationMs = deck.durationMs(); delay(500)
        }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (scratching) accent.copy(alpha = 0.10f) else Neon.PANEL)
            .border(
                width = if (isExpanded || scratching) 2.dp else 1.5.dp,
                color = accent.copy(alpha = if (isExpanded || scratching) 1f else 0.7f),
                shape = RoundedCornerShape(16.dp)
            ).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = accent, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(7.dp).clip(CircleShape)
                    .background(
                        when {
                            scratching -> Neon.YELLOW
                            isPlaying -> Neon.LIVE
                            else -> accent.copy(alpha = 0.4f)
                        }
                    ))
                Spacer(Modifier.width(5.dp))
                Text(
                    text = when {
                        scratching -> "SCRATCH \u00D7${"%.2f".format(scratchRate)}"
                        isPlaying -> "LIVE"
                        else -> "READY"
                    },
                    color = when {
                        scratching -> Neon.YELLOW
                        isPlaying -> Neon.LIVE
                        else -> Neon.TEXT_DIM
                    },
                    fontSize = 10.sp, fontWeight = FontWeight.Bold
                )
            }
        }

        if (deck.isLoaded && deck.loadedName.isNotEmpty()) {
            Text(deck.loadedName, color = accent.copy(alpha = 0.7f), fontSize = 9.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        WaveformView(accent = accent, seed = deck.index * 17 + 3, barCount = 70)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${fmt(positionMs)} / ${fmt(if (durationMs > 0) durationMs else 195000L)}",
                color = accent.copy(alpha = 0.9f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text("BPM ${if (deck.bpm > 0) "%.1f".format(deck.bpm) else "126.0"}",
                color = accent.copy(alpha = 0.7f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(if (deck.loopActive) "LOOP ${deck.loopBeats}" else "NO LOOP",
                color = if (deck.loopActive) Neon.LIVE else accent.copy(alpha = 0.5f),
                fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }

        Row(modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            VerticalFader(accent = accent, modifier = Modifier.fillMaxHeight())
            Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                JogWheel(accent = accent, size = 130.dp, isPlaying = isPlaying,
                    onScratch = onScratch, onScratchEnd = onScratchEnd)
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                EqKnob("HI", accent, onValueChange = { engine.setEq(deck.index, deck.eq.lowGainDb, deck.eq.midGainDb, it) })
                EqKnob("MID", accent, onValueChange = { engine.setEq(deck.index, deck.eq.lowGainDb, it, deck.eq.highGainDb) })
                EqKnob("LOW", accent, onValueChange = { engine.setEq(deck.index, it, deck.eq.midGainDb, deck.eq.highGainDb) })
            }
        }

        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(4.dp)) {
            TransportBtn("SPIN", accent, onSpinback, Modifier.weight(1f))
            TransportBtn("BRAKE", accent, onBrake, Modifier.weight(1f))
            TransportBtn("SYNC", accent, onSync, Modifier.weight(1f))
        }

        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(4.dp)) {
            for (b in intArrayOf(1,2,4,8,16)) {
                TransportBtn("$b", accent, { onToggleLoop(b) }, Modifier.weight(1f))
            }
        }

        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(4.dp)) {
            for (i in 0..3) {
                val hasCue = deck.cuePoints.getOrNull(i) ?: -1L >= 0
                CuePad(i, accent, hasCue, { onSetCue(i) }, { onJumpCue(i) }, Modifier.weight(1f))
            }
        }
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(4.dp)) {
            for (i in 4..7) {
                val hasCue = deck.cuePoints.getOrNull(i) ?: -1L >= 0
                CuePad(i, accent, hasCue, { onSetCue(i) }, { onJumpCue(i) }, Modifier.weight(1f))
            }
        }

        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(4.dp)) {
            StemButton("VOX", accent, onVocalToggle, Modifier.weight(1f))
            StemButton("DRM", accent, {}, Modifier.weight(1f))
            StemButton("BAS", accent, {}, Modifier.weight(1f))
            StemButton("OTH", accent, {}, Modifier.weight(1f))
        }

        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(4.dp)) {
            TransportBtn("PICK", accent, onPickFile, Modifier.weight(1.4f))
            TransportBtn("SAMPLE", accent, onLoadSample, Modifier.weight(1f))
            TransportBtn("\u25B6", accent, onPlayPause, Modifier.weight(1.2f))
        }
    }
}

@Composable
private fun CuePad(index: Int, accent: Color, hasCue: Boolean, onSet: () -> Unit, onJump: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(6.dp))
        .background(if (hasCue) accent.copy(alpha = 0.35f) else Neon.BTN_BG)
        .border(1.dp, accent.copy(alpha = if (hasCue) 1f else 0.35f), RoundedCornerShape(6.dp))
        .clickable { if (hasCue) onJump() else onSet() }.padding(vertical = 8.dp),
        contentAlignment = Alignment.Center) {
        Text(if (hasCue) "C${index + 1}" else "+${index + 1}", color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MiniDeckPanel(
    deck: DeckPlayer, title: String, accent: Color,
    onPlayPause: () -> Unit, onPickFile: () -> Unit, modifier: Modifier = Modifier
) {
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    val isPlaying = deck.isPlaying

    LaunchedEffect(deck) {
        while (true) { positionMs = deck.positionMs(); durationMs = deck.durationMs(); delay(700) }
    }

    Column(modifier = modifier.clip(RoundedCornerShape(14.dp)).background(Neon.PANEL)
        .border(1.5.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(14.dp)).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape)
                    .background(if (isPlaying) Neon.LIVE else accent.copy(alpha = 0.5f)))
                Spacer(Modifier.width(4.dp))
                Text(if (isPlaying) "LIVE" else "READY",
                    color = if (isPlaying) Neon.LIVE else accent.copy(alpha = 0.7f),
                    fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            JogWheel(accent = accent, size = 52.dp, isPlaying = isPlaying, onScratch = {}, onScratchEnd = {})
        }
        WaveformView(accent = accent, barCount = 50, seed = deck.index * 13 + 5)
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("${fmt(positionMs)} / ${fmt(if (durationMs > 0) durationMs else 175000L)}",
                color = accent.copy(alpha = 0.85f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(4.dp)) {
            TransportBtn("PICK", accent, onPickFile, Modifier.weight(1f))
            TransportBtn("PLAY", accent, onPlayPause, Modifier.weight(1f))
        }
    }
}

@Composable
private fun CenterVideoPanel(
    engine: AudioEngine, transition: VideoTransition,
    onTransitionChange: (VideoTransition) -> Unit,
    crossfaderValue: Float, onCrossfaderChange: (Float) -> Unit,
    echoOn: Boolean, filterOn: Boolean, flangeOn: Boolean, reverbOn: Boolean,
    recOn: Boolean, vocalOff: Boolean,
    onEchoToggle: () -> Unit, onFilterToggle: () -> Unit,
    onFlangeToggle: () -> Unit, onReverbToggle: () -> Unit,
    onRecToggle: () -> Unit, onVocalToggle: () -> Unit
) {
    val deck0 = engine.decks[0]
    val deck1 = engine.decks[1]
    val scratch0 = deck0.scratching
    val scratch1 = deck1.scratching

    Column(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
        .background(Neon.PANEL)
        .border(
            width = if (scratch0 || scratch1) 3.dp else 1.5.dp,
            color = when {
                scratch0 && scratch1 -> Neon.YELLOW
                scratch0 -> Neon.CYAN
                scratch1 -> Neon.MAGENTA
                else -> Color(0xFF2A2A3A)
            },
            shape = RoundedCornerShape(16.dp)
        ).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(Neon.DEEP)
                .border(
                    width = if (scratch0 || scratch1) 2.dp else 1.5.dp,
                    color = if (scratch0 || scratch1) Neon.YELLOW else Neon.CYAN.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(50)
                ).padding(horizontal = 14.dp, vertical = 3.dp)
            ) {
                Text(
                    text = when {
                        scratch0 && scratch1 -> "\u26A1 BOTH SCRATCHING"
                        scratch0 -> "\u26A1 DECK 1 SCRATCH"
                        scratch1 -> "\u26A1 DECK 2 SCRATCH"
                        else -> "VIDEO MIX"
                    },
                    color = if (scratch0 || scratch1) Neon.YELLOW else Neon.CYAN,
                    fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (t in VideoTransition.values()) {
                    val selected = t == transition
                    Box(modifier = Modifier.clip(RoundedCornerShape(50))
                        .background(if (selected) Neon.CYAN.copy(alpha = 0.3f) else Neon.BTN_BG)
                        .border(1.dp, Neon.CYAN.copy(alpha = if (selected) 1f else 0.3f), RoundedCornerShape(50))
                        .clickable { onTransitionChange(t) }.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) { Text(t.name, color = Neon.CYAN, fontSize = 8.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f)
            .clip(RoundedCornerShape(12.dp)).background(Color.Black)
            .border(2.dp, Neon.CYAN.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
        ) {
            VideoDeckView(
                deck = deck0, alpha = 1f, transition = transition,
                accentColor = Neon.CYAN, showScratchOverlay = true,
                modifier = Modifier.fillMaxSize()
            )
            VideoDeckView(
                deck = deck1, alpha = 1f - crossfaderValue, transition = transition,
                accentColor = Neon.MAGENTA, showScratchOverlay = true,
                modifier = Modifier.fillMaxSize()
            )

            Row(modifier = Modifier.align(Alignment.BottomStart).padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                DeckLiveBadge("D1", deck0.isPlaying, deck0.scratching, Neon.CYAN)
                DeckLiveBadge("D2", deck1.isPlaying, deck1.scratching, Neon.MAGENTA)
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Slider(value = crossfaderValue, onValueChange = onCrossfaderChange,
                colors = SliderDefaults.colors(
                    thumbColor = Neon.CYAN, activeTrackColor = Neon.CYAN,
                    inactiveTrackColor = Neon.MAGENTA.copy(alpha = 0.6f)
                ), modifier = Modifier.fillMaxWidth())
            Text("CROSSFADER", color = Neon.TEXT_DIM, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
        }

        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(4.dp)) {
            FxChip("ECHO", Neon.CYAN, echoOn, onEchoToggle, Modifier.weight(1f))
            FxChip("FILTER", Neon.MAGENTA, filterOn, onFilterToggle, Modifier.weight(1f))
            FxChip("FLANGE", Neon.PURPLE, flangeOn, onFlangeToggle, Modifier.weight(1f))
            FxChip("REVERB", Neon.PURPLE, reverbOn, onReverbToggle, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(4.dp)) {
            FxChip("\u25CF REC", Neon.RED, recOn, onRecToggle, Modifier.weight(1f))
            FxChip("VOCAL OFF", Neon.PURPLE, vocalOff, onVocalToggle, Modifier.weight(1f))
            FxChip("MASTER", Neon.YELLOW, true, {}, Modifier.weight(1f))
        }
    }
}

@Composable
private fun DeckLiveBadge(label: String, playing: Boolean, scratching: Boolean, accent: Color) {
    val bg = when {
        scratching -> Neon.YELLOW
        playing -> accent
        else -> Color(0xFF1A1A24)
    }
    val fg = when {
        scratching || playing -> Color.Black
        else -> accent.copy(alpha = 0.5f)
    }
    Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(bg.copy(alpha = 0.85f))
        .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = when {
                scratching -> "$label SCRATCH"
                playing -> "$label LIVE"
                else -> "$label IDLE"
            },
            color = fg, fontSize = 8.sp, fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StemButton(label: String, accent: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(8.dp)).background(Neon.BTN_BG)
        .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
        .clickable { onClick() }.padding(vertical = 6.dp), contentAlignment = Alignment.Center
    ) { Text(label, color = accent.copy(alpha = 0.85f), fontSize = 9.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun TransportBtn(label: String, accent: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(8.dp)).background(Neon.BTN_BG)
        .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
        .clickable { onClick() }.padding(vertical = 6.dp), contentAlignment = Alignment.Center
    ) { Text(label, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun FxChip(label: String, accent: Color, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(50))
        .background(if (active) accent.copy(alpha = 0.35f) else accent.copy(alpha = 0.10f))
        .border(
            width = if (active) 2.dp else 1.dp,
            color = accent.copy(alpha = if (active) 1f else 0.55f),
            shape = RoundedCornerShape(50)
        )
        .clickable { onClick() }.padding(vertical = 8.dp), contentAlignment = Alignment.Center
    ) { Text(label, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
}

private fun fmt(ms: Long): String {
    if (ms <= 0) return "00:00"
    val s = ms / 1000
    return "%02d:%02d".format(s / 60, s % 60)
}
