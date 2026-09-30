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
    var anyPlaying by remember { mutableStateOf(false) }
    var bpmMaster by remember { mutableFloatStateOf(128f) }
    var activeDeckA by remember { mutableStateOf(0) }
    var activeDeckB by remember { mutableStateOf(1) }
    var recording by remember { mutableStateOf(false) }
    var recordingTime by remember { mutableLongStateOf(0L) }
    var theaterMode by remember { mutableStateOf(false) }
    var libraryOpen by remember { mutableStateOf(false) }
    var libraryTarget by remember { mutableStateOf(0) }
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
            recording = false
            recordingTime = 0
            theaterMode = false
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
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { theaterMode = false })
                    }
            ) {
                VideoFullscreen(deckA, deckB, videoCrossfader)
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 4.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("ULTIMATE", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 5.sp)
                        Text("DJ STUDIO", color = Neon.BLUE, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    if (isLowEnd) {
                        TopBtn("LITE", Neon.AMBER, false) {}
                        Spacer(Modifier.width(4.dp))
                    }
                    TopBtn("LAST REC", Neon.BLUE, false) {}
                    Spacer(Modifier.width(4.dp))
                    TopBtn("SHARE", Neon.GREEN, false) {}
                    Spacer(Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (recording) Neon.RED.copy(alpha = 0.30f) else Neon.RED.copy(alpha = 0.12f))
                            .border(1.dp, Neon.RED.copy(alpha = if (recording) 1f else 0.6f), RoundedCornerShape(20.dp))
                            .clickable { toggleRecording() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Neon.RED))
                            Spacer(Modifier.width(5.dp))
                            Text(
                                if (recording) fmt(recordingTime) else "REC",
                                color = Neon.RED, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                            )
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                    TopBtn("CLUB", Neon.BLUE, false) {}
                }

                Spacer(Modifier.height(6.dp))

                VideoBox(deckA, deckB, videoCrossfader)

                Spacer(Modifier.height(6.dp))

                DeckStrip(
                    decks = engine.decks,
                    activeA = activeDeckA,
                    activeB = activeDeckB,
                    onSelectA = { idx -> activeDeckA = idx },
                    onSelectB = { idx -> activeDeckB = idx }
                )

                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DeckPanel(
                        deck = deckA, accent = accentA, label = "DECK A",
                        engine = engine, deckIndex = activeDeckA,
                        onOpenLibrary = { openLibrary(activeDeckA) },
                        onPlay = { engine.togglePlay(activeDeckA) },
                        onSync = { engine.syncBpm(activeDeckB, activeDeckA) },
                        onEqChange = { l, m, h -> engine.setEq(activeDeckA, l, m, h) },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    DeckPanel(
                        deck = deckB, accent = accentB, label = "DECK B",
                        engine = engine, deckIndex = activeDeckB,
                        onOpenLibrary = { openLibrary(activeDeckB) },
                        onPlay = { engine.togglePlay(activeDeckB) },
                        onSync = { engine.syncBpm(activeDeckA, activeDeckB) },
                        onEqChange = { l, m, h -> engine.setEq(activeDeckB, l, m, h) },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }

                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().height(34.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Neon.BTN_BG)
                            .border(0.5.dp, Neon.GREEN.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .clickable { openLibrary(activeDeckA) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("LIB", color = Neon.GREEN, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("A", color = accentA, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.width(4.dp))
                    Slider(
                        value = audioCrossfader,
                        onValueChange = { v: Float -> audioCrossfader = v; engine.setCrossfader(v) },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = accentA.copy(alpha = 0.8f),
                            inactiveTrackColor = accentB.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier.weight(1f).height(22.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("B", color = accentB, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Neon.BTN_BG)
                            .border(0.5.dp, Neon.TEXT_FAINT.copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("MIX", color = Neon.TEXT_DIM, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
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
                        onPickForOtherDeck = { track ->
                            val other = if (libraryTarget == activeDeckA) activeDeckB else activeDeckA
                            engine.loadDeck(other, track.uri, track.title)
                            libraryOpen = false
                        },
                        onClose = { libraryOpen = false },
                        modifier = Modifier.fillMaxSize().padding(20.dp)
                    )
                }
            }

            if (countdown > 0) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
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
private fun TopBtn(label: String, accent: Color, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (active) accent.copy(alpha = 0.25f) else accent.copy(alpha = 0.10f))
            .border(1.dp, accent.copy(alpha = if (active) 1f else 0.6f), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(label, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
private fun SmallBtn(
    label: String,
    accent: Color,
    active: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (active) accent.copy(alpha = 0.30f) else Neon.BTN_BG)
            .border(0.5.dp, accent.copy(alpha = if (active) 1f else 0.35f), RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = accent, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun VideoBox(deckA: DeckPlayer, deckB: DeckPlayer, videoCrossfader: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black)
            .border(1.dp, Neon.BORDER_SOFT, RoundedCornerShape(8.dp))
    ) {
        VideoContents(deckA, deckB, videoCrossfader)
    }
}

@Composable
private fun VideoFullscreen(deckA: DeckPlayer, deckB: DeckPlayer, videoCrossfader: Float) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        VideoContents(deckA, deckB, videoCrossfader)
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.VideoContents(
    deckA: DeckPlayer, deckB: DeckPlayer, videoCrossfader: Float
) {
    if (deckA.hasVideo) {
        VideoDeckView(deckA, videoCrossfader, false, VideoTransition.CROSSFADE, Neon.BLUE, false, Modifier.fillMaxSize())
    } else if (deckA.isLoaded) {
        AudioVisualizer(Neon.BLUE, deckA.isPlaying, deckA.loadedName, deckA.waveform, Modifier.fillMaxSize())
    }
    if (deckB.hasVideo) {
        VideoDeckView(deckB, videoCrossfader, true, VideoTransition.CROSSFADE, Neon.AMBER, false, Modifier.fillMaxSize())
    } else if (deckB.isLoaded) {
        Box(
            modifier = Modifier.fillMaxSize().graphicsLayer {
                alpha = (1f - videoCrossfader).coerceIn(0f, 1f)
            }
        ) {
            AudioVisualizer(Neon.AMBER, deckB.isPlaying, deckB.loadedName, deckB.waveform, Modifier.fillMaxSize())
        }
    }
    if (!deckA.isLoaded && !deckB.isLoaded) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "LOAD A VIDEO OR AUDIO", color = Neon.BLUE, fontSize = 14.sp,
                    fontWeight = FontWeight.Black, letterSpacing = 4.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Drag & drop or select from media library to start mixing",
                    color = Neon.TEXT_FAINT, fontSize = 9.sp, letterSpacing = 1.sp
                )
            }
        }
    }
    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Neon.BLUE.copy(alpha = 0.15f))
            .border(1.dp, Neon.BLUE.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text("MIX", color = Neon.BLUE, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
    }
    Row(
        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (deckA.isPlaying) Neon.BLUE else Color(0x33FFFFFF)))
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (deckB.isPlaying) Neon.AMBER else Color(0x33FFFFFF)))
    }
}

@Composable
private fun DeckPanel(
    deck: DeckPlayer, accent: Color, label: String,
    engine: AudioEngine, deckIndex: Int,
    onOpenLibrary: () -> Unit, onPlay: () -> Unit, onSync: () -> Unit,
    onEqChange: (Float, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var hiVal by remember { mutableFloatStateOf(0f) }
    var midVal by remember { mutableFloatStateOf(0f) }
    var lowVal by remember { mutableFloatStateOf(0f) }
    var pitchVal by remember { mutableFloatStateOf(1f) }
    val isPlaying = deck.isPlaying

    LaunchedEffect(deck) {
        while (true) {
            positionMs = deck.positionMs()
            durationMs = deck.durationMs()
            delay(400)
        }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Neon.PANEL)
            .border(
                0.5.dp,
                if (isPlaying) accent.copy(alpha = 0.6f) else Neon.BORDER_SOFT,
                RoundedCornerShape(8.dp)
            )
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(accent.copy(alpha = 0.15f))
                    .border(1.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .clickable { onOpenLibrary() }
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text("LOAD", color = accent, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().height(70.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                MetalJogWheel(
                    accent = accent, size = 62.dp, isPlaying = isPlaying,
                    onScratchStart = { engine.beginScratch(deckIndex) },
                    onScratchMove = { dy: Float -> engine.scratchMove(deckIndex, dy) },
                    onScratchEnd = { engine.endScratch(deckIndex) }
                )
            }
            Column(
                modifier = Modifier.width(38.dp).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("VOL", color = Neon.TEXT_FAINT, fontSize = 6.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(2.dp))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    VolumeFader(
                        accent = accent,
                        initialValue = 0.75f,
                        onValueChange = { v: Float -> deck.setVolume(v) }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            KnobSmall(
                "LOW", accent, lowVal, size = 30.dp,
                onValueChange = { v: Float -> lowVal = v; onEqChange(v, midVal, hiVal) }
            )
            KnobSmall(
                "MID", accent, midVal, size = 30.dp,
                onValueChange = { v: Float -> midVal = v; onEqChange(lowVal, v, hiVal) }
            )
            KnobSmall(
                "HIGH", accent, hiVal, size = 30.dp,
                onValueChange = { v: Float -> hiVal = v; onEqChange(lowVal, midVal, v) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            SmallBtn(
                label = if (isPlaying) "PAUSE" else "PLAY",
                accent = accent, active = isPlaying,
                onClick = { onPlay() },
                modifier = Modifier.weight(1.4f)
            )
            SmallBtn("SYNC", accent, false, { onSync() }, Modifier.weight(1f))
            SmallBtn("CUE", accent, false, { engine.setCue(deckIndex, 0) }, Modifier.weight(1f))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            for (i in 0..3) {
                val has = deck.hasCue(i)
                SmallBtn(
                    label = if (has) "C${i + 1}" else "+${i + 1}",
                    accent = accent, active = has,
                    onClick = {
                        if (has) engine.jumpCue(deckIndex, i)
                        else engine.setCue(deckIndex, i)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            for (i in 4..7) {
                val has = deck.hasCue(i)
                SmallBtn(
                    label = if (has) "C${i + 1}" else "+${i + 1}",
                    accent = accent, active = has,
                    onClick = {
                        if (has) engine.jumpCue(deckIndex, i)
                        else engine.setCue(deckIndex, i)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            for (b in intArrayOf(1, 2, 4, 8, 16)) {
                SmallBtn(
                    label = "$b",
                    accent = accent,
                    active = deck.loopActive && deck.loopBeats == b,
                    onClick = { engine.toggleLoop(deckIndex, b) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            SmallBtn("-8", accent, false, { engine.beatJump(deckIndex, -8) }, Modifier.weight(1f))
            SmallBtn("-4", accent, false, { engine.beatJump(deckIndex, -4) }, Modifier.weight(1f))
            SmallBtn("+4", accent, false, { engine.beatJump(deckIndex, 4) }, Modifier.weight(1f))
            SmallBtn("+8", accent, false, { engine.beatJump(deckIndex, 8) }, Modifier.weight(1f))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            SmallBtn(
                "ROLL", accent, deck.loopRollActive,
                {
                    if (deck.loopRollActive) engine.stopLoopRoll(deckIndex)
                    else engine.loopRoll(deckIndex, 1)
                },
                Modifier.weight(1f)
            )
            SmallBtn(
                "CENS", accent, deck.censorActive,
                { engine.censor(deckIndex, 4) },
                Modifier.weight(1f)
            )
            SmallBtn("SPIN", accent, false, { engine.spinback(deckIndex) }, Modifier.weight(1f))
            SmallBtn("BRK", accent, false, { engine.brake(deckIndex) }, Modifier.weight(1f))
            SmallBtn("SLIP", accent, deck.slipMode, { engine.toggleSlip(deckIndex) }, Modifier.weight(1f))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("PITCH", color = Neon.TEXT_FAINT, fontSize = 6.sp, fontWeight = FontWeight.Bold)
            Slider(
                value = pitchVal,
                onValueChange = { v: Float -> pitchVal = v; engine.setSpeed(deckIndex, v) },
                valueRange = 0.5f..1.5f,
                colors = SliderDefaults.colors(
                    thumbColor = accent,
                    activeTrackColor = accent.copy(alpha = 0.6f),
                    inactiveTrackColor = Neon.BORDER_SOFT
                ),
                modifier = Modifier.weight(1f).height(14.dp)
            )
            Text(
                "${"%.2f".format(pitchVal)}x",
                color = accent, fontSize = 7.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.width(30.dp)
            )
        }

        Text(
            if (deck.loadedName.isNotEmpty()) deck.loadedName else "No track loaded",
            color = if (deck.isLoaded) Neon.TEXT else Neon.TEXT_FAINT,
            fontSize = 9.sp, fontWeight = FontWeight.Medium,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "${fmt(positionMs)} / ${fmt(durationMs)}",
                color = Neon.TEXT_DIM, fontSize = 8.sp, fontWeight = FontWeight.Medium
            )
            Text(
                "${"%.0f".format(deck.bpm)} BPM",
                color = accent, fontSize = 8.sp, fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun fmt(ms: Long): String {
    if (ms <= 0) return "00:00"
    val s = ms / 1000
    return "%02d:%02d".format(s / 60, s % 60)
}
