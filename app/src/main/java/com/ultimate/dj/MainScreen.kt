package com.ultimate.dj
import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultimate.dj.audio.*
import com.ultimate.dj.ui.*
import com.ultimate.dj.video.MixRecorder
import kotlinx.coroutines.delay
@Composable
fun MainScreen(engine: AudioEngine, recorder: MixRecorder, activity: Activity) {
    val context = LocalContext.current
    val isLowEnd = remember { DeviceCapabilities.isLowEnd(context) }
    var audioCrossfader by remember { mutableFloatStateOf(0.5f) }
    var videoCrossfader by remember { mutableFloatStateOf(0.5f) }
    var crossMode by remember { mutableStateOf(CrossfaderMode.FADE) }
    var videoTransition by remember { mutableStateOf(VideoTransition.CROSSFADE) }
    var anyPlaying by remember { mutableStateOf(false) }
    var bpmMaster by remember { mutableFloatStateOf(128f) }
    var activeDeckA by remember { mutableStateOf(0) }
    var activeDeckB by remember { mutableStateOf(1) }
    var recording by remember { mutableStateOf(false) }
    var recordingTime by remember { mutableLongStateOf(0L) }
    var theaterMode by remember { mutableStateOf(false) }
    var libraryOpen by remember { mutableStateOf(false) }
    var libraryTarget by remember { mutableStateOf(0) }
    var keyLockOn by remember { mutableStateOf(false) }
    var fxChainFilter by remember { mutableStateOf(false) }
    var fxChainEcho by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf(0) }
    val recordLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val started = recorder.start(result.resultCode, result.data!!)
            if (started) {
                recording = true
                recordingTime = 0
                countdown = 3
            }
        }
    }
    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown -= 1
            if (countdown == 0) theaterMode = true
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            anyPlaying = engine.decks.any { it.isPlaying }
            bpmMaster = engine.deckA().bpm
            if (recording) recordingTime += 1000
            delay(1000)
        }
    }
    fun toggleRecording() {
        if (recording) {
            val uri = recorder.stop()
            recording = false; recordingTime = 0; theaterMode = false
            if (uri != null) Toast.makeText(context, "Saved to Movies/ULTIMATE/${recorder.lastSavedName}", Toast.LENGTH_LONG).show()
            else Toast.makeText(context, "Recording failed", Toast.LENGTH_SHORT).show()
        } else {
            recorder.buildIntent()?.let { recordLauncher.launch(it) }
        }
    }
    fun openLibrary(targetDeck: Int) {
        libraryTarget = targetDeck
        libraryOpen = true
    }
    val deckA = engine.decks[activeDeckA]
    val deckB = engine.decks[activeDeckB]
    val accentA = Neon.BLUE
    val accentB = Neon.AMBER

    Box(modifier = Modifier.fillMaxSize().background(Neon.BG)) {

        if (theaterMode) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { theaterMode = false })
                }) {
                Box(modifier = Modifier.fillMaxSize()) {
                    VideoStrip(deckA, deckB, videoCrossfader, videoTransition, fullscreen = true)
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp)) {

                Row(modifier = Modifier.fillMaxWidth().height(36.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("ULTIMATE", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black, letterSpacing = 5.sp)
                        Text("DJ STUDIO", color = Neon.TEXT_DIM, fontSize = 7.sp, fontWeight = FontWeight.Medium, letterSpacing = 3.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    if (isLowEnd) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
                            .background(Neon.AMBER.copy(alpha = 0.15f))
                            .border(0.5.dp, Neon.AMBER.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)) {
                            Text("LITE", color = Neon.AMBER, fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
                        .background(Neon.BTN_BG)
                        .border(0.5.dp, Neon.BLUE.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .clickable { openLibrary(activeDeckA) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text("LIBRARY", color = Neon.BLUE, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                    Spacer(Modifier.width(6.dp))
                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
                        .background(if (recording) Neon.RED.copy(alpha = 0.25f) else Neon.BTN_BG)
                        .border(0.5.dp, Neon.RED.copy(alpha = if (recording) 1f else 0.5f), RoundedCornerShape(4.dp))
                        .clickable { toggleRecording() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Neon.RED))
                            Spacer(Modifier.width(6.dp))
                            Text(if (recording) "STOP ${fmt(recordingTime)}" else "REC",
                                color = Neon.RED, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                VideoStrip(deckA, deckB, videoCrossfader, videoTransition, fullscreen = false)

                Spacer(Modifier.height(6.dp))

                Column(modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(4.dp))
                    .background(Neon.PANEL_DARK).border(0.5.dp, Neon.BORDER_SOFT, RoundedCornerShape(4.dp))
                    .padding(4.dp)) {
                    SplitWaveform(accent = accentA, waveform = deckA.waveform, bpm = deckA.bpm,
                        progress = if (deckA.durationMs() > 0) deckA.positionMs().toFloat() / deckA.durationMs() else 0f,
                        modifier = Modifier.weight(1f))
                    SplitWaveform(accent = accentB, waveform = deckB.waveform, bpm = deckB.bpm,
                        progress = if (deckB.durationMs() > 0) deckB.positionMs().toFloat() / deckB.durationMs() else 0f,
                        modifier = Modifier.weight(1f))
                }

                Spacer(Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DeckPanel(
                        deck = deckA, accent = accentA, label = "DECK A",
                        engine = engine, deckIndex = activeDeckA,
                        onOpenLibrary = { openLibrary(activeDeckA) },
                        onPlay = { engine.togglePlay(activeDeckA) },
                        onSync = { engine.syncBpm(activeDeckB, activeDeckA) },
                        onEqChange = { l, m, h -> engine.setEq(activeDeckA, l, m, h) },
                        onSpeedChange = { s -> engine.setSpeed(activeDeckA, s) },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    CenterMixer(
                        audioCrossfader,
                        { v: Float -> audioCrossfader = v; engine.setCrossfader(v) },
                        crossMode,
                        { m: CrossfaderMode -> crossMode = m; engine.setCrossfaderMode(m) },
                        anyPlaying, bpmMaster,
                        modifier = Modifier.width(160.dp).fillMaxHeight()
                    )
                    DeckPanel(
                        deck = deckB, accent = accentB, label = "DECK B",
                        engine = engine, deckIndex = activeDeckB,
                        onOpenLibrary = { openLibrary(activeDeckB) },
                        onPlay = { engine.togglePlay(activeDeckB) },
                        onSync = { engine.syncBpm(activeDeckA, activeDeckB) },
                        onEqChange = { l, m, h -> engine.setEq(activeDeckB, l, m, h) },
                        onSpeedChange = { s -> engine.setSpeed(activeDeckB, s) },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }

                Spacer(Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth().height(34.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    BarBtn("LIB", Neon.BLUE, libraryOpen) { openLibrary(activeDeckA) }
                    BarBtn("FX", Neon.PURPLE, fxChainFilter || fxChainEcho) {
                        fxChainFilter = !fxChainFilter
                        engine.setFilter(fxChainFilter)
                    }
                    BarBtn("KEY", Neon.GREEN, keyLockOn) {
                        keyLockOn = !keyLockOn
                        engine.setKeyLock(keyLockOn)
                    }
                    Spacer(Modifier.weight(1f))
                    Text("Deck", color = Neon.TEXT_FAINT, fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.width(6.dp))
                    for (id in 0 until 6) {
                        val isA = id == activeDeckA
                        val isB = id == activeDeckB
                        val active = isA || isB
                        val col = if (active) Neon.DECK_COLORS[id] else Neon.TEXT_FAINT
                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
                            .background(if (active) Neon.DECK_COLORS[id].copy(alpha = 0.18f) else Neon.BTN_BG)
                            .border(0.5.dp, col.copy(alpha = if (active) 0.85f else 0.2f), RoundedCornerShape(4.dp))
                            .clickable {
                                if (!(isA || isB)) activeDeckA = id
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp), contentAlignment = Alignment.Center) {
                            Text("${id + 1}", color = col, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            AnimatedVisibility(visible = libraryOpen, enter = fadeIn(), exit = fadeOut()) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f))) {
                    LibraryBrowser(
                        context = context,
                        targetDeckName = if (libraryTarget == activeDeckA) "DECK A" else "DECK B",
                        onPickTrack = { track ->
                            engine.loadDeck(libraryTarget, track.uri, track.title)
                            libraryOpen = false
                        },
                        onClose = { libraryOpen = false },
                        modifier = Modifier.fillMaxSize().padding(24.dp)
                    )
                }
            }

            if (countdown > 0) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Recording starts in", color = Neon.TEXT_DIM, fontSize = 12.sp, letterSpacing = 3.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("$countdown", color = Neon.RED, fontSize = 72.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoStrip(deckA: DeckPlayer, deckB: DeckPlayer, videoCrossfader: Float,
                       transition: VideoTransition, fullscreen: Boolean) {
    val mod = if (fullscreen) Modifier.fillMaxSize()
              else Modifier.fillMaxWidth().aspectRatio(21f / 9f)
    Box(modifier = mod
        .clip(RoundedCornerShape(if (fullscreen) 0.dp else 6.dp))
        .background(Color.Black)
        .then(if (fullscreen) Modifier else Modifier.border(0.5.dp, Neon.BORDER_MED, RoundedCornerShape(6.dp)))) {
        if (deckA.hasVideo) {
            VideoDeckView(deckA, videoCrossfader, false, transition, Neon.BLUE, false, Modifier.fillMaxSize())
        } else if (deckA.isLoaded) {
            AudioVisualizer(Neon.BLUE, deckA.isPlaying, deckA.loadedName, deckA.waveform, Modifier.fillMaxSize())
        }
        if (deckB.hasVideo) {
            VideoDeckView(deckB, videoCrossfader, true, transition, Neon.AMBER, false, Modifier.fillMaxSize())
        } else if (deckB.isLoaded) {
            Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = (1f - videoCrossfader).coerceIn(0f, 1f) }) {
                AudioVisualizer(Neon.AMBER, deckB.isPlaying, deckB.loadedName, deckB.waveform, Modifier.fillMaxSize())
            }
        }
        if (!deckA.isLoaded && !deckB.isLoaded) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LOAD A TRACK", color = Neon.TEXT_FAINT, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 5.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Tap LIBRARY to browse your music and videos",
                        color = Neon.TEXT_FAINT.copy(alpha = 0.6f), fontSize = 8.sp, letterSpacing = 1.sp)
                }
            }
        }
    }
}

@Composable
private fun DeckPanel(
    deck: DeckPlayer, accent: Color, label: String,
    engine: AudioEngine, deckIndex: Int,
    onOpenLibrary: () -> Unit, onPlay: () -> Unit, onSync: () -> Unit,
    onEqChange: (Float, Float, Float) -> Unit,
    onSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var hiVal by remember { mutableFloatStateOf(0f) }
    var midVal by remember { mutableFloatStateOf(0f) }
    var lowVal by remember { mutableFloatStateOf(0f) }
    var pitchVal by remember { mutableFloatStateOf(1f) }
    val isPlaying = deck.isPlaying
    val speed = deck.currentSpeed()
    val kindTag = if (deck.hasVideo) "VIDEO" else if (deck.isLoaded) "AUDIO" else ""
    LaunchedEffect(deck) { while (true) { positionMs = deck.positionMs(); durationMs = deck.durationMs(); delay(400) } }

    Column(modifier = modifier
        .clip(RoundedCornerShape(6.dp))
        .background(Neon.PANEL)
        .border(0.5.dp, if (isPlaying) accent.copy(alpha = 0.55f) else Neon.BORDER_SOFT, RoundedCornerShape(6.dp))
        .padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(accent))
                Spacer(Modifier.width(6.dp))
                Text(label, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                if (kindTag.isNotEmpty()) {
                    Spacer(Modifier.width(6.dp))
                    Text(kindTag, color = accent.copy(alpha = 0.5f), fontSize = 7.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
                }
            }
            Text(if (isPlaying) "LIVE" else "READY",
                color = if (isPlaying) Neon.LIVE else Neon.TEXT_FAINT,
                fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }

        Text(if (deck.loadedName.isNotEmpty()) deck.loadedName else "No track loaded",
            color = if (deck.isLoaded) Neon.TEXT else Neon.TEXT_FAINT,
            fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${fmt(positionMs)} / ${fmt(durationMs)}",
                color = accent.copy(alpha = 0.75f), fontSize = 8.sp, fontWeight = FontWeight.Medium)
            Row {
                if (deck.musicalKey.isNotEmpty()) {
                    Text("${deck.musicalKey}  ${deck.camelot}", color = accent.copy(alpha = 0.85f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                }
                Text("${"%.1f".format(deck.bpm)} BPM  ·  ${"%.0f".format(speed * 100)}%",
                    color = accent.copy(alpha = 0.65f), fontSize = 8.sp, fontWeight = FontWeight.Medium)
            }
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            MetalJogWheel(accent = accent, size = 100.dp, isPlaying = isPlaying,
                onScratchStart = { engine.beginScratch(deckIndex) },
                onScratchMove = { dy -> engine.scratchMove(deckIndex, dy) },
                onScratchEnd = { engine.endScratch(deckIndex) })
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            PillBtn("SYNC", accent, false, onSync, Modifier.weight(1f))
            PillBtn("CUE", accent, false, { engine.setCue(deckIndex, 0) }, Modifier.weight(1f))
            PillBtn(if (isPlaying) "PAUSE" else "PLAY", accent, isPlaying, onPlay, Modifier.weight(1.6f))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (b in intArrayOf(1, 2, 4, 8)) {
                PillBtn("${b}B", accent, deck.loopActive && deck.loopBeats == b, { engine.toggleLoop(deckIndex, b) }, Modifier.weight(1f))
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (i in 0..3) {
                val has = deck.hasCue(i)
                CuePad(i, accent, has, { engine.setCue(deckIndex, i) }, { engine.jumpCue(deckIndex, i) }, Modifier.weight(1f))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (i in 4..7) {
                val has = deck.hasCue(i)
                CuePad(i, accent, has, { engine.setCue(deckIndex, i) }, { engine.jumpCue(deckIndex, i) }, Modifier.weight(1f))
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            KnobSmall("HI", accent, hiVal, onValueChange = { hiVal = it; onEqChange(lowVal, midVal, it) })
            KnobSmall("MID", accent, midVal, onValueChange = { midVal = it; onEqChange(lowVal, it, hiVal) })
            KnobSmall("LOW", accent, lowVal, onValueChange = { lowVal = it; onEqChange(it, midVal, hiVal) })
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("PITCH", color = accent.copy(alpha = 0.6f), fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Slider(value = pitchVal, onValueChange = { pitchVal = it; onSpeedChange(it * 2f) },
                valueRange = 0.25f..2f,
                colors = SliderDefaults.colors(thumbColor = accent,
                    activeTrackColor = accent.copy(alpha = 0.6f),
                    inactiveTrackColor = Neon.BORDER_SOFT),
                modifier = Modifier.weight(1f).height(16.dp))
            Text("${"%.2f".format(pitchVal)}×", color = accent, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }

        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
            .background(accent.copy(alpha = 0.15f))
            .border(0.5.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
            .clickable { onOpenLibrary() }
            .padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            Text("LOAD TRACK", color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
    }
}

@Composable
private fun CuePad(index: Int, accent: Color, hasCue: Boolean, onSet: () -> Unit, onJump: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(4.dp))
        .background(if (hasCue) accent.copy(alpha = 0.22f) else Neon.BTN_BG)
        .border(0.5.dp, accent.copy(alpha = if (hasCue) 0.85f else 0.20f), RoundedCornerShape(4.dp))
        .clickable { if (hasCue) onJump() else onSet() }
        .padding(vertical = 5.dp), contentAlignment = Alignment.Center) {
        Text("${index + 1}", color = if (hasCue) accent else accent.copy(alpha = 0.45f),
            fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CenterMixer(
    acf: Float, onAcf: (Float) -> Unit,
    cm: CrossfaderMode, onCm: (CrossfaderMode) -> Unit,
    playing: Boolean, bpm: Float,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier
        .clip(RoundedCornerShape(6.dp))
        .background(Neon.PANEL_GLASS)
        .border(0.5.dp, Neon.BORDER_SOFT, RoundedCornerShape(6.dp))
        .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("MIXER", color = Neon.TEXT_FAINT, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
        Text("${"%.1f".format(bpm)}", color = Neon.TEXT, fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        Text("BPM", color = Neon.TEXT_FAINT, fontSize = 7.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.sp)
        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("A", color = Neon.BLUE, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(4.dp))
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    VolumeFader(Neon.BLUE, 0.75f)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("B", color = Neon.AMBER, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(4.dp))
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    VolumeFader(Neon.AMBER, 0.80f)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("VU", color = Neon.TEXT_FAINT, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(4.dp))
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    MasterVu(playing = playing)
                }
            }
        }
        Text("CROSSFADER", color = Neon.TEXT_FAINT, fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
        Slider(value = acf, onValueChange = onAcf,
            colors = SliderDefaults.colors(thumbColor = Neon.BLUE,
                activeTrackColor = Neon.BLUE.copy(alpha = 0.6f),
                inactiveTrackColor = Neon.AMBER.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth().height(18.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("CUT", color = if (cm == CrossfaderMode.CUT) Neon.BLUE else Neon.TEXT_FAINT,
                fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
                modifier = Modifier.clickable { onCm(CrossfaderMode.CUT) }.padding(horizontal = 6.dp, vertical = 3.dp))
            Text("·", color = Neon.TEXT_FAINT, fontSize = 8.sp)
            Text("FADE", color = if (cm == CrossfaderMode.FADE) Neon.BLUE else Neon.TEXT_FAINT,
                fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
                modifier = Modifier.clickable { onCm(CrossfaderMode.FADE) }.padding(horizontal = 6.dp, vertical = 3.dp))
        }
    }
}

@Composable
private fun BarBtn(label: String, accent: Color, active: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
        .background(if (active) accent.copy(alpha = 0.18f) else Neon.BTN_BG)
        .border(0.5.dp, accent.copy(alpha = if (active) 0.85f else 0.3f), RoundedCornerShape(4.dp))
        .clickable { onClick() }.padding(horizontal = 12.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center) {
        Text(label, color = accent, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
private fun PillBtn(label: String, accent: Color, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(4.dp))
        .background(if (active) accent.copy(alpha = 0.25f) else Neon.BTN_BG)
        .border(0.5.dp, accent.copy(alpha = if (active) 0.9f else 0.25f), RoundedCornerShape(4.dp))
        .clickable { onClick() }.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
        Text(label, color = accent, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
    }
}

private fun fmt(ms: Long): String {
    if (ms <= 0) return "00:00"
    val s = ms / 1000
    return "%02d:%02d".format(s / 60, s % 60)
}
