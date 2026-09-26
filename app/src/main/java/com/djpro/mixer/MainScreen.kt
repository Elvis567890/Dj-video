package com.djpro.mixer

import android.app.Activity
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.djpro.mixer.audio.AudioEngine
import com.djpro.mixer.audio.CrossfaderMode
import com.djpro.mixer.audio.DeckPlayer
import com.djpro.mixer.ui.BeatgridWaveform
import com.djpro.mixer.ui.KnobSmall
import com.djpro.mixer.ui.MasterVu
import com.djpro.mixer.ui.MetalJogWheel
import com.djpro.mixer.ui.VideoDeckView
import com.djpro.mixer.ui.VideoTransition
import com.djpro.mixer.ui.VolumeFader
import com.djpro.mixer.video.MixRecorder
import kotlinx.coroutines.delay

private const val SAMPLE_VIDEO_1 = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
private const val SAMPLE_VIDEO_2 = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"

private enum class BottomPanel { NONE, SAMPLER, FX, LIBRARY }

@Composable
fun MainScreen(engine: AudioEngine, recorder: MixRecorder, activity: Activity) {
    val context = LocalContext.current
    var audioCrossfader by remember { mutableFloatStateOf(0.5f) }
    var videoCrossfader by remember { mutableFloatStateOf(0.5f) }
    var crossMode by remember { mutableStateOf(CrossfaderMode.FADE) }
    var videoTransition by remember { mutableStateOf(VideoTransition.CROSSFADE) }
    var clubMode by remember { mutableStateOf(false) }
    var bottomPanel by remember { mutableStateOf(BottomPanel.NONE) }
    var anyPlaying by remember { mutableStateOf(false) }
    var bpmMaster by remember { mutableFloatStateOf(128f) }
    var activeDeckA by remember { mutableStateOf(0) }
    var activeDeckB by remember { mutableStateOf(1) }
    var pendingDeckId by remember { mutableStateOf<Int?>(null) }
    var recording by remember { mutableStateOf(false) }

    val toneGen = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 80) }
    DisposableEffect(Unit) { onDispose { try { toneGen.release() } catch (_: Throwable) {} } }

    val recordLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val ok = recorder.start(result.resultCode, result.data!!)
            recording = ok
        }
    }

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
    fun pickFile(id: Int) { pendingDeckId = id; filePicker.launch(arrayOf("video/*", "audio/*")) }

    fun toggleRecording() {
        if (recording) { recorder.stop(); recording = false }
        else {
            val intent = recorder.buildIntent()
            if (intent != null) recordLauncher.launch(intent)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            anyPlaying = engine.decks.any { it.isPlaying }
            bpmMaster = engine.deckA().bpm
            delay(400)
        }
    }

    val deckA = engine.decks[activeDeckA]
    val deckB = engine.decks[activeDeckB]
    val accentA = Neon.DECK_COLORS[activeDeckA]
    val accentB = Neon.DECK_COLORS[activeDeckB]

    Column(modifier = Modifier.fillMaxSize()
        .background(if (clubMode) Neon.CLUB_BG else Neon.BG)
        .padding(horizontal = 6.dp, vertical = 4.dp)) {
        TopBar(bpmMaster = bpmMaster, clubMode = clubMode, onClubToggle = { clubMode = !clubMode }, recording = recording, onRecordToggle = { toggleRecording() })
        Spacer(Modifier.height(3.dp))
        VideoStrip(deckA = deckA, deckB = deckB, videoCrossfader = videoCrossfader, transition = videoTransition)
        Spacer(Modifier.height(2.dp))
        VideoControlsRow(videoCrossfader = videoCrossfader, onValueChange = { videoCrossfader = it },
            accentA = accentA, accentB = accentB, transition = videoTransition, onTransitionChange = { videoTransition = it })
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DeckPanel(deck = deckA, accent = accentA, label = "DECK A",
                onPickFile = { pickFile(activeDeckA) }, onPlay = { engine.togglePlay(activeDeckA) },
                onSync = { engine.syncBpm(activeDeckB, activeDeckA) }, onCue = { engine.setCue(activeDeckA, 0) },
                onJumpCue = { slot -> engine.jumpCue(activeDeckA, slot) },
                onSetCue = { slot -> engine.setCue(activeDeckA, slot) },
                onToggleLoop = { beats -> engine.toggleLoop(activeDeckA, beats) },
                onSpinback = { engine.spinback(activeDeckA) }, onBrake = { engine.brake(activeDeckA) },
                onScratchStart = { engine.beginScratch(activeDeckA) },
                onScratchMove = { dy -> engine.scratchMove(activeDeckA, dy) },
                onScratchEnd = { engine.endScratch(activeDeckA) },
                onEqChange = { low, mid, high -> engine.setEq(activeDeckA, low, mid, high) },
                onStemsChange = { drm, bas, oth -> engine.setStemMutes(drm, bas, oth) },
                modifier = Modifier.weight(1f).fillMaxHeight())
            CenterMixer(engine = engine, audioCrossfader = audioCrossfader,
                onAudioCrossfaderChange = { audioCrossfader = it; engine.setCrossfader(it) },
                crossMode = crossMode, onCrossModeChange = { crossMode = it; engine.setCrossfaderMode(it) },
                anyPlaying = anyPlaying, modifier = Modifier.width(160.dp).fillMaxHeight())
            DeckPanel(deck = deckB, accent = accentB, label = "DECK B",
                onPickFile = { pickFile(activeDeckB) }, onPlay = { engine.togglePlay(activeDeckB) },
                onSync = { engine.syncBpm(activeDeckA, activeDeckB) }, onCue = { engine.setCue(activeDeckB, 0) },
                onJumpCue = { slot -> engine.jumpCue(activeDeckB, slot) },
                onSetCue = { slot -> engine.setCue(activeDeckB, slot) },
                onToggleLoop = { beats -> engine.toggleLoop(activeDeckB, beats) },
                onSpinback = { engine.spinback(activeDeckB) }, onBrake = { engine.brake(activeDeckB) },
                onScratchStart = { engine.beginScratch(activeDeckB) },
                onScratchMove = { dy -> engine.scratchMove(activeDeckB, dy) },
                onScratchEnd = { engine.endScratch(activeDeckB) },
                onEqChange = { low, mid, high -> engine.setEq(activeDeckB, low, mid, high) },
                onStemsChange = { drm, bas, oth -> engine.setStemMutes(drm, bas, oth) },
                modifier = Modifier.weight(1f).fillMaxHeight())
        }
        Spacer(Modifier.height(4.dp))
        if (bottomPanel == BottomPanel.SAMPLER) {
            SamplerDrawer(onPlayTone = { tone -> try { toneGen.startTone(tone, 180) } catch (_: Throwable) {} })
            Spacer(Modifier.height(4.dp))
        } else if (bottomPanel == BottomPanel.FX) {
            FxDrawer(engine = engine, deckA = activeDeckA); Spacer(Modifier.height(4.dp))
        } else if (bottomPanel == BottomPanel.LIBRARY) {
            LibraryDrawer(
                onPickForA = { pickFile(activeDeckA) }, onPickForB = { pickFile(activeDeckB) },
                onLoadSampleA = { engine.loadDeck(activeDeckA, SAMPLE_VIDEO_1, "Sample A") },
                onLoadSampleB = { engine.loadDeck(activeDeckB, SAMPLE_VIDEO_2, "Sample B") })
            Spacer(Modifier.height(4.dp))
        }
        BottomBar(activePanel = bottomPanel,
            onTogglePanel = { p -> bottomPanel = if (bottomPanel == p) BottomPanel.NONE else p },
            activeDeckA = activeDeckA, activeDeckB = activeDeckB,
            onSwapA = { id -> engine.swapToA(id); activeDeckA = id },
            onSwapB = { id -> engine.swapToB(id); activeDeckB = id },
            onLoadSample = { id -> val url = if (id % 2 == 0) SAMPLE_VIDEO_1 else SAMPLE_VIDEO_2; engine.loadDeck(id, url, "Sample ${id + 1}") })
    }
}

@Composable
private fun TopBar(bpmMaster: Float, clubMode: Boolean, onClubToggle: () -> Unit, recording: Boolean, onRecordToggle: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(26.dp).clip(CircleShape).background(Neon.PANEL)
                .border(1.dp, Neon.CYAN.copy(alpha = 0.5f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Menu, "menu", tint = Neon.CYAN, modifier = Modifier.size(15.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text("DJ PRO MIXER", color = Neon.CYAN, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
        }
        Text("BPM MASTER  ${"%.1f".format(bpmMaster)}", color = Neon.TEXT, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.clip(RoundedCornerShape(50))
                .background(if (recording) Neon.RED.copy(alpha = 0.5f) else Neon.PANEL)
                .border(1.dp, Neon.RED.copy(alpha = 0.7f), RoundedCornerShape(50))
                .clickable { onRecordToggle() }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Neon.RED))
                    Spacer(Modifier.width(4.dp))
                    Text(if (recording) "STOP" else "REC", color = Neon.RED, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(6.dp))
            Box(modifier = Modifier.clip(RoundedCornerShape(50))
                .background(if (clubMode) Neon.CYAN.copy(alpha = 0.3f) else Neon.PANEL)
                .border(1.dp, Neon.CYAN.copy(alpha = 0.5f), RoundedCornerShape(50))
                .clickable { onClubToggle() }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text(if (clubMode) "CLUB ON" else "CLUB", color = Neon.CYAN, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(6.dp))
            Box(modifier = Modifier.size(26.dp).clip(CircleShape).background(Neon.PANEL)
                .border(1.dp, Neon.CYAN.copy(alpha = 0.5f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Settings, "settings", tint = Neon.CYAN, modifier = Modifier.size(15.dp))
            }
        }
    }
}

@Composable
private fun VideoStrip(deckA: DeckPlayer, deckB: DeckPlayer, videoCrossfader: Float, transition: VideoTransition) {
    Box(modifier = Modifier.fillMaxWidth().aspectRatio(21f / 9f)
        .clip(RoundedCornerShape(6.dp)).background(Color.Black)
        .border(1.dp, Neon.CYAN.copy(alpha = 0.55f), RoundedCornerShape(6.dp))) {
        VideoDeckView(deck = deckA, alpha = 1f, transition = transition, accentColor = Neon.CYAN, modifier = Modifier.fillMaxSize())
        VideoDeckView(deck = deckB, alpha = 1f - videoCrossfader, transition = transition, accentColor = Neon.MAGENTA, modifier = Modifier.fillMaxSize())
        Box(modifier = Modifier.align(Alignment.TopStart).padding(5.dp)
            .clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.6f))
            .border(1.dp, Neon.CYAN.copy(alpha = 0.7f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp)) {
            Text("VIDEO", color = Neon.CYAN, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
        Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp)
            .clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 8.dp, vertical = 2.dp)) {
            Text("VIDEO MIX", color = Color.White.copy(alpha = 0.85f), fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
        }
        Row(modifier = Modifier.align(Alignment.TopEnd).padding(5.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(if (deckA.isPlaying) Neon.LIVE else Color(0x4000E5FF)))
            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(if (deckB.isPlaying) Neon.LIVE else Color(0x40FF2BD6)))
        }
    }
}

@Composable
private fun VideoControlsRow(videoCrossfader: Float, onValueChange: (Float) -> Unit,
                             accentA: Color, accentB: Color,
                             transition: VideoTransition, onTransitionChange: (VideoTransition) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("\u25C0", color = accentA, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Slider(value = videoCrossfader, onValueChange = onValueChange,
            colors = SliderDefaults.colors(thumbColor = Neon.CYAN,
                activeTrackColor = accentA.copy(alpha = 0.7f),
                inactiveTrackColor = accentB.copy(alpha = 0.7f)),
            modifier = Modifier.weight(1f).height(18.dp))
        Text("\u25B6", color = accentB, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        for (t in VideoTransition.values()) {
            val sel = t == transition
            Box(modifier = Modifier.clip(RoundedCornerShape(50))
                .background(if (sel) Neon.CYAN.copy(alpha = 0.3f) else Neon.BTN_BG)
                .border(1.dp, Neon.CYAN.copy(alpha = if (sel) 1f else 0.3f), RoundedCornerShape(50))
                .clickable { onTransitionChange(t) }.padding(horizontal = 6.dp, vertical = 2.dp)) {
                Text(t.name, color = Neon.CYAN, fontSize = 7.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DeckPanel(
    deck: DeckPlayer, accent: Color, label: String,
    onPickFile: () -> Unit, onPlay: () -> Unit, onSync: () -> Unit, onCue: () -> Unit,
    onJumpCue: (Int) -> Unit, onSetCue: (Int) -> Unit, onToggleLoop: (Int) -> Unit,
    onSpinback: () -> Unit, onBrake: () -> Unit,
    onScratchStart: () -> Unit, onScratchMove: (Float) -> Unit, onScratchEnd: () -> Unit,
    onEqChange: (Float, Float, Float) -> Unit,
    onStemsChange: (Boolean, Boolean, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var hiVal by remember { mutableFloatStateOf(0f) }
    var midVal by remember { mutableFloatStateOf(0f) }
    var lowVal by remember { mutableFloatStateOf(0f) }
    var killActive by remember { mutableStateOf(false) }
    var drmMute by remember { mutableStateOf(false) }
    var basMute by remember { mutableStateOf(false) }
    var othMute by remember { mutableStateOf(false) }
    val isPlaying = deck.isPlaying
    val scratching = deck.scratching
    val speed = deck.currentSpeed()

    LaunchedEffect(deck) { while (true) { positionMs = deck.positionMs(); durationMs = deck.durationMs(); delay(400) } }

    Column(modifier = modifier.clip(RoundedCornerShape(10.dp))
        .background(if (scratching) accent.copy(alpha = 0.08f) else Neon.PANEL)
        .border(if (scratching) 2.dp else 1.dp, accent.copy(alpha = if (scratching) 1f else 0.55f), RoundedCornerShape(10.dp))
        .padding(6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.width(5.dp))
                Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(if (scratching) Neon.YELLOW else accent))
            }
            Text(text = when { scratching -> "SCR \u00D7${"%.2f".format(deck.scratchRate)}"; isPlaying -> "LIVE"; else -> "READY" },
                color = when { scratching -> Neon.YELLOW; isPlaying -> Neon.LIVE; else -> Neon.TEXT_DIM },
                fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
        Text(text = if (deck.loadedName.isNotEmpty()) deck.loadedName else "Tap LOAD to pick",
            color = Neon.TEXT, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${fmt(positionMs)} / ${fmt(if (durationMs > 0) durationMs else 0L)}",
                color = accent.copy(alpha = 0.8f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text("BPM ${"%.1f".format(deck.bpm)}  |  ${"%.0f".format(speed * 100)}%",
                color = accent.copy(alpha = 0.7f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
        BeatgridWaveform(accent = accent, seed = deck.index * 31 + 7, barCount = 100)
        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            MetalJogWheel(accent = accent, size = 100.dp, isPlaying = isPlaying,
                onScratchStart = onScratchStart, onScratchMove = onScratchMove, onScratchEnd = onScratchEnd)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            PillBtn("SYNC", accent, false, onSync, Modifier.weight(1f))
            PillBtn("CUE", accent, false, onCue, Modifier.weight(1f))
            PillBtn("\u25B6 PLAY", accent, isPlaying, onPlay, Modifier.weight(1.4f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            for (b in intArrayOf(1, 2, 4, 8, 16)) {
                PillBtn("$b", accent, deck.loopActive && deck.loopBeats == b, { onToggleLoop(b) }, Modifier.weight(1f))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            for (i in 0..3) {
                val has = deck.cuePoints.getOrNull(i) ?: -1L >= 0
                CuePad(i, accent, has, { onSetCue(i) }, { onJumpCue(i) }, Modifier.weight(1f))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            for (i in 4..7) {
                val has = deck.cuePoints.getOrNull(i) ?: -1L >= 0
                CuePad(i, accent, has, { onSetCue(i) }, { onJumpCue(i) }, Modifier.weight(1f))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            StemBtn("VOX", accent, false, {}, Modifier.weight(1f))
            StemBtn("DRM", accent, drmMute, { drmMute = !drmMute; onStemsChange(drmMute, basMute, othMute) }, Modifier.weight(1f))
            StemBtn("BAS", accent, basMute, { basMute = !basMute; onStemsChange(drmMute, basMute, othMute) }, Modifier.weight(1f))
            StemBtn("OTH", accent, othMute, { othMute = !othMute; onStemsChange(drmMute, basMute, othMute) }, Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                KnobSmall("HI", accent, hiVal, onValueChange = { hiVal = it; onEqChange(lowVal, midVal, it) })
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                KnobSmall("MID", accent, midVal, onValueChange = { midVal = it; onEqChange(lowVal, it, hiVal) })
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                KnobSmall("LOW", accent, lowVal, onValueChange = { lowVal = it; onEqChange(it, midVal, hiVal) })
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(34.dp).clip(CircleShape)
                    .background(if (killActive) accent.copy(alpha = 0.4f) else Neon.BTN_BG)
                    .border(1.5.dp, accent.copy(alpha = if (killActive) 1f else 0.5f), CircleShape)
                    .clickable {
                        killActive = !killActive
                        if (killActive) onEqChange(-26f, -26f, -26f)
                        else { lowVal = 0f; midVal = 0f; hiVal = 0f; onEqChange(0f, 0f, 0f) }
                    }, contentAlignment = Alignment.Center) {
                    Text("KILL", color = accent, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            PillBtn("SPIN", accent, false, onSpinback, Modifier.weight(1f))
            PillBtn("BRAKE", accent, false, onBrake, Modifier.weight(1f))
            PillBtn("LOAD", accent, false, onPickFile, Modifier.weight(1.2f))
        }
    }
}

@Composable
private fun CuePad(index: Int, accent: Color, hasCue: Boolean, onSet: () -> Unit, onJump: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(4.dp))
        .background(if (hasCue) accent.copy(alpha = 0.35f) else Neon.BTN_BG)
        .border(1.dp, accent.copy(alpha = if (hasCue) 1f else 0.35f), RoundedCornerShape(4.dp))
        .clickable { if (hasCue) onJump() else onSet() }.padding(vertical = 5.dp),
        contentAlignment = Alignment.Center) {
        Text(if (hasCue) "C${index + 1}" else "+${index + 1}", color = accent, fontSize = 7.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StemBtn(label: String, accent: Color, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(4.dp))
        .background(if (active) accent.copy(alpha = 0.4f) else Neon.BTN_BG)
        .border(1.dp, accent.copy(alpha = if (active) 1f else 0.35f), RoundedCornerShape(4.dp))
        .clickable { onClick() }.padding(vertical = 5.dp), contentAlignment = Alignment.Center) {
        Text(label, color = accent, fontSize = 7.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CenterMixer(engine: AudioEngine, audioCrossfader: Float, onAudioCrossfaderChange: (Float) -> Unit,
                        crossMode: CrossfaderMode, onCrossModeChange: (CrossfaderMode) -> Unit,
                        anyPlaying: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier.clip(RoundedCornerShape(10.dp))
        .background(Neon.PANEL_DARK)
        .border(1.dp, Color(0xFF1E1E2E), RoundedCornerShape(10.dp))
        .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text("MIXER", color = Neon.TEXT_DIM, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("A", color = Neon.CYAN, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    VolumeFader(accent = Neon.CYAN, initialValue = 0.75f)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("B", color = Neon.MAGENTA, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    VolumeFader(accent = Neon.MAGENTA, initialValue = 0.80f)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("VU", color = Neon.TEXT, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    MasterVu(playing = anyPlaying)
                }
            }
        }
        Text("MASTER", color = Neon.TEXT_DIM, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text("CROSSFADER", color = Neon.TEXT_DIM, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Slider(value = audioCrossfader, onValueChange = onAudioCrossfaderChange,
            colors = SliderDefaults.colors(thumbColor = Neon.CYAN,
                activeTrackColor = Neon.CYAN.copy(alpha = 0.7f),
                inactiveTrackColor = Neon.MAGENTA.copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth().height(18.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("CUT", color = if (crossMode == CrossfaderMode.CUT) Neon.CYAN else Neon.TEXT_DIM,
                fontSize = 8.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onCrossModeChange(CrossfaderMode.CUT) }.padding(horizontal = 4.dp, vertical = 2.dp))
            Text("|", color = Neon.TEXT_DIM, fontSize = 8.sp)
            Text("FADE", color = if (crossMode == CrossfaderMode.FADE) Neon.CYAN else Neon.TEXT_DIM,
                fontSize = 8.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onCrossModeChange(CrossfaderMode.FADE) }.padding(horizontal = 4.dp, vertical = 2.dp))
        }
    }
}

@Composable
private fun SamplerDrawer(onPlayTone: (Int) -> Unit) {
    val pads = listOf(
        "KICK" to ToneGenerator.TONE_DTMF_1, "SNARE" to ToneGenerator.TONE_DTMF_2,
        "HIHAT" to ToneGenerator.TONE_DTMF_3, "CLAP" to ToneGenerator.TONE_DTMF_4,
        "PERC" to ToneGenerator.TONE_PROP_BEEP, "RISER" to ToneGenerator.TONE_PROP_BEEP2,
        "LASER" to ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, "SIREN" to ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK)
    Row(modifier = Modifier.fillMaxWidth().height(80.dp)
        .clip(RoundedCornerShape(10.dp)).background(Neon.PANEL_DARK)
        .border(1.dp, Neon.MAGENTA.copy(alpha = 0.5f), RoundedCornerShape(10.dp)).padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        pads.forEachIndexed { i, (name, tone) ->
            val accent = if (i % 2 == 0) Neon.MAGENTA else Neon.PURPLE
            Box(modifier = Modifier.weight(1f).fillMaxHeight()
                .clip(RoundedCornerShape(8.dp)).background(accent.copy(alpha = 0.15f))
                .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                .clickable { onPlayTone(tone) }, contentAlignment = Alignment.Center) {
                Text(name, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FxDrawer(engine: AudioEngine, deckA: Int) {
    var echoOn by remember { mutableStateOf(false) }
    var filterOn by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth().height(80.dp)
        .clip(RoundedCornerShape(10.dp)).background(Neon.PANEL_DARK)
        .border(1.dp, Neon.PURPLE.copy(alpha = 0.5f), RoundedCornerShape(10.dp)).padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        FxPad("ECHO", Neon.CYAN, echoOn, { echoOn = !echoOn; engine.setEcho(echoOn) }, Modifier.weight(1f))
        FxPad("FILTER", Neon.MAGENTA, filterOn, { filterOn = !filterOn; engine.setFilter(filterOn) }, Modifier.weight(1f))
        FxPad("ROLL", Neon.LIME, false, { engine.toggleLoop(deckA, 1) }, Modifier.weight(1f))
        FxPad("BRAKE", Neon.RED, false, { engine.brake(deckA) }, Modifier.weight(1f))
        FxPad("SPIN", Neon.ORANGE, false, { engine.spinback(deckA) }, Modifier.weight(1f))
        FxPad("VOCAL", Neon.PURPLE, false, { engine.setVocalRemoval(true) }, Modifier.weight(1f))
    }
}

@Composable
private fun FxPad(label: String, accent: Color, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxHeight()
        .clip(RoundedCornerShape(8.dp))
        .background(if (active) accent.copy(alpha = 0.35f) else accent.copy(alpha = 0.12f))
        .border(if (active) 2.dp else 1.dp, accent.copy(alpha = if (active) 1f else 0.55f), RoundedCornerShape(8.dp))
        .clickable { onClick() }, contentAlignment = Alignment.Center) {
        Text(label, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
private fun LibraryDrawer(onPickForA: () -> Unit, onPickForB: () -> Unit, onLoadSampleA: () -> Unit, onLoadSampleB: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().height(80.dp)
        .clip(RoundedCornerShape(10.dp)).background(Neon.PANEL_DARK)
        .border(1.dp, Neon.CYAN.copy(alpha = 0.5f), RoundedCornerShape(10.dp)).padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        LibBtn("FILE \u2192 A", Neon.CYAN, onPickForA, Modifier.weight(1f))
        LibBtn("FILE \u2192 B", Neon.MAGENTA, onPickForB, Modifier.weight(1f))
        LibBtn("SAMPLE A", Neon.LIME, onLoadSampleA, Modifier.weight(1f))
        LibBtn("SAMPLE B", Neon.ORANGE, onLoadSampleB, Modifier.weight(1f))
    }
}

@Composable
private fun LibBtn(label: String, accent: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxHeight()
        .clip(RoundedCornerShape(8.dp)).background(accent.copy(alpha = 0.15f))
        .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
        .clickable { onClick() }, contentAlignment = Alignment.Center) {
        Text(label, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BottomBar(activePanel: BottomPanel, onTogglePanel: (BottomPanel) -> Unit,
                      activeDeckA: Int, activeDeckB: Int,
                      onSwapA: (Int) -> Unit, onSwapB: (Int) -> Unit, onLoadSample: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        BarBtn("\u2630 LIB", Neon.CYAN, activePanel == BottomPanel.LIBRARY) { onTogglePanel(BottomPanel.LIBRARY) }
        BarBtn("SAMPLER", Neon.MAGENTA, activePanel == BottomPanel.SAMPLER) { onTogglePanel(BottomPanel.SAMPLER) }
        Spacer(Modifier.weight(1f))
        for (id in 2..5) {
            val isA = id == activeDeckA; val isB = id == activeDeckB
            val active = isA || isB
            val col = if (active) Neon.DECK_COLORS[id] else Neon.TEXT_DIM
            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp))
                .background(if (active) Neon.DECK_COLORS[id].copy(alpha = 0.25f) else Neon.BTN_BG)
                .border(1.dp, col.copy(alpha = if (active) 1f else 0.4f), RoundedCornerShape(6.dp))
                .clickable { if (isA) onSwapA(0) else if (isB) onSwapB(1) else { onSwapA(id); onLoadSample(id) } }
                .padding(horizontal = 6.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
                Text("D${id + 1}", color = col, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.weight(1f))
        BarBtn("FX", Neon.PURPLE, activePanel == BottomPanel.FX) { onTogglePanel(BottomPanel.FX) }
        BarBtn("SET", Neon.CYAN, false) { }
    }
}

@Composable
private fun BarBtn(label: String, accent: Color, active: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))
        .background(if (active) accent.copy(alpha = 0.35f) else Neon.BTN_BG)
        .border(1.dp, accent.copy(alpha = if (active) 1f else 0.5f), RoundedCornerShape(8.dp))
        .clickable { onClick() }.padding(horizontal = 8.dp, vertical = 7.dp), contentAlignment = Alignment.Center) {
        Text(label, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
private fun PillBtn(label: String, accent: Color, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(50))
        .background(if (active) accent else Neon.BTN_BG)
        .border(1.dp, accent.copy(alpha = if (active) 1f else 0.55f), RoundedCornerShape(50))
        .clickable { onClick() }.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
        Text(label, color = if (active) Color.Black else accent, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

private fun fmt(ms: Long): String {
    if (ms <= 0) return "00:00"
    val s = ms / 1000
    return "%02d:%02d".format(s / 60, s % 60)
}
