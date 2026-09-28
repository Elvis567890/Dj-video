package com.ultimate.dj
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultimate.dj.audio.*
import com.ultimate.dj.ui.*
import com.ultimate.dj.video.MixRecorder
import kotlinx.coroutines.delay
private const val SAMPLE_A = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
private const val SAMPLE_B = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
private enum class BottomPanel { NONE, SAMPLER, FX, LIBRARY }
@Composable
fun MainScreen(engine: AudioEngine, recorder: MixRecorder, activity: Activity) {
    val context = LocalContext.current
    val isLowEnd = remember { DeviceCapabilities.isLowEnd(context) }
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
    var pendingPadIndex by remember { mutableStateOf<Int?>(null) }
    var recording by remember { mutableStateOf(false) }
    var recordingTime by remember { mutableLongStateOf(0L) }
    var padRev by remember { mutableLongStateOf(0L) }
    var samplerBank by remember { mutableStateOf(0) }
    var keyLockOn by remember { mutableStateOf(false) }
    var fxChainFilter by remember { mutableStateOf(false) }
    var fxChainEcho by remember { mutableStateOf(false) }
    val recordLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            recording = recorder.start(result.resultCode, result.data!!)
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
    val samplePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        val idx = pendingPadIndex; pendingPadIndex = null
        if (uri != null && idx != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Throwable) {}
            SampleStore.assign(context, idx, uri); padRev = System.currentTimeMillis()
        }
    }
    fun pickSampleForPad(idx: Int) { pendingPadIndex = idx; samplePicker.launch(arrayOf("audio/*")) }
    fun toggleRecording() {
        if (recording) {
            val uri = recorder.stop()
            recording = false; recordingTime = 0
            if (uri != null) Toast.makeText(context, "Saved: Movies/ULTIMATE/${recorder.lastSavedName}", Toast.LENGTH_LONG).show()
            else Toast.makeText(context, "Recording failed", Toast.LENGTH_SHORT).show()
        } else {
            recorder.buildIntent()?.let { recordLauncher.launch(it) }
        }
    }
    fun shareLastRecording() {
        val uri = recorder.lastSavedUri
        if (uri == null) { Toast.makeText(context, "No recording yet", Toast.LENGTH_SHORT).show(); return }
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"; putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(share, "Share mix"))
    }
    fun openLastRecording() {
        val uri = recorder.lastSavedUri
        if (uri == null) { Toast.makeText(context, "No recording yet", Toast.LENGTH_SHORT).show(); return }
        val view = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/mp4"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try { context.startActivity(view) } catch (_: Throwable) { Toast.makeText(context, "No video player", Toast.LENGTH_SHORT).show() }
    }
    LaunchedEffect(Unit) {
        while (true) {
            anyPlaying = engine.decks.any { it.isPlaying }
            bpmMaster = engine.deckA().bpm
            if (recording) recordingTime += 1000
            delay(1000)
        }
    }
    val deckA = engine.decks[activeDeckA]
    val deckB = engine.decks[activeDeckB]
    val accentA = Neon.DECK_COLORS[activeDeckA % Neon.DECK_COLORS.size]
    val accentB = Neon.DECK_COLORS[activeDeckB % Neon.DECK_COLORS.size]
    Column(modifier = Modifier.fillMaxSize()
        .background(if (clubMode) Neon.CLUB_BG else Neon.BG)
        .padding(horizontal = 8.dp, vertical = 6.dp)) {
        TopBar(recording, recordingTime, clubMode, isLowEnd, { toggleRecording() }, { clubMode = !clubMode }, { shareLastRecording() }, { openLastRecording() })
        Spacer(Modifier.height(6.dp))
        DeckStrip(decks = engine.decks, activeA = activeDeckA, activeB = activeDeckB,
            onSelect = { idx -> if (idx != activeDeckA && idx != activeDeckB) activeDeckA = idx },
            onLoadInto = { idx -> pickFile(idx) })
        Spacer(Modifier.height(6.dp))
        VideoStrip(deckA, deckB, videoCrossfader, videoTransition)
        Spacer(Modifier.height(4.dp))
        VideoControlsRow(
            vcf = videoCrossfader,
            onChange = { v: Float -> videoCrossfader = v },
            a = accentA,
            b = accentB,
            transition = videoTransition,
            onTransition = { t: VideoTransition -> videoTransition = t },
            keyLock = keyLockOn,
            onKeyLock = { v: Boolean -> keyLockOn = v; engine.setKeyLock(v) },
            fxFilter = fxChainFilter,
            onFxFilter = { v: Boolean -> fxChainFilter = v; engine.setFilter(v) },
            fxEcho = fxChainEcho,
            onFxEcho = { v: Boolean -> fxChainEcho = v; engine.setEcho(v) }
        )
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DeckPanel(deck = deckA, accent = accentA, label = "DECK A", engine = engine, deckIndex = activeDeckA,
                onPickFile = { pickFile(activeDeckA) }, onPlay = { engine.togglePlay(activeDeckA) },
                onSync = { engine.syncBpm(activeDeckB, activeDeckA) },
                onEqChange = { l, m, h -> engine.setEq(activeDeckA, l, m, h) },
                onStemsChange = { d, b, o -> engine.setStemMutes(d, b, o) },
                onSpeedChange = { s -> engine.setSpeed(activeDeckA, s) },
                modifier = Modifier.weight(1f).fillMaxHeight())
            CenterMixer(engine, audioCrossfader, { audioCrossfader = it; engine.setCrossfader(it) },
                crossMode, { crossMode = it; engine.setCrossfaderMode(it) }, anyPlaying, bpmMaster,
                Modifier.width(180.dp).fillMaxHeight())
            DeckPanel(deck = deckB, accent = accentB, label = "DECK B", engine = engine, deckIndex = activeDeckB,
                onPickFile = { pickFile(activeDeckB) }, onPlay = { engine.togglePlay(activeDeckB) },
                onSync = { engine.syncBpm(activeDeckA, activeDeckB) },
                onEqChange = { l, m, h -> engine.setEq(activeDeckB, l, m, h) },
                onStemsChange = { d, b, o -> engine.setStemMutes(d, b, o) },
                onSpeedChange = { s -> engine.setSpeed(activeDeckB, s) },
                modifier = Modifier.weight(1f).fillMaxHeight())
        }
        Spacer(Modifier.height(6.dp))
        if (bottomPanel == BottomPanel.SAMPLER) {
            SamplerDrawer(context, samplerBank, padRev,
                onRequestImport = { padIndex -> pickSampleForPad(padIndex) },
                onClearPad = { padIndex ->
                    SampleStore.assign(context, padIndex, null)
                    padRev = System.currentTimeMillis()
                },
                onBankChange = { samplerBank = it })
            Spacer(Modifier.height(6.dp))
        } else if (bottomPanel == BottomPanel.FX) {
            FxDrawer(engine, activeDeckA); Spacer(Modifier.height(6.dp))
        } else if (bottomPanel == BottomPanel.LIBRARY) {
            LibraryBrowser(context, onPickTrack = { track ->
                engine.loadDeck(activeDeckA, track.uri, track.title)
            })
            Spacer(Modifier.height(6.dp))
        }
        BottomBar(bottomPanel, { p -> bottomPanel = if (bottomPanel == p) BottomPanel.NONE else p },
            activeDeckA, activeDeckB,
            { id -> engine.swapToA(id); activeDeckA = id },
            { id -> engine.swapToB(id); activeDeckB = id },
            { id -> val url = if (id % 2 == 0) SAMPLE_A else SAMPLE_B; engine.loadDeck(id, url, "Sample ${id + 1}") })
    }
}
@Composable
private fun TopBar(recording: Boolean, recordingTime: Long, clubMode: Boolean, isLowEnd: Boolean,
                   onRecord: () -> Unit, onClub: () -> Unit, onShare: () -> Unit, onOpen: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("ULTIMATE", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 10.sp)
            Text("DJ STUDIO", color = Neon.CYAN.copy(alpha = 0.7f), fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 5.sp)
        }
        Spacer(Modifier.weight(1f))
        if (isLowEnd) {
            Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(Neon.ORANGE.copy(alpha = 0.2f))
                .padding(horizontal = 8.dp, vertical = 3.dp)) {
                Text("LITE", color = Neon.ORANGE, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            Spacer(Modifier.width(6.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.clip(RoundedCornerShape(50))
                .background(Neon.BTN_BG)
                .border(1.dp, Neon.CYAN.copy(alpha = 0.6f), RoundedCornerShape(50))
                .clickable { onOpen() }.padding(horizontal = 10.dp, vertical = 5.dp)) {
                Text("LAST REC", color = Neon.CYAN, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            Spacer(Modifier.width(6.dp))
            Box(modifier = Modifier.clip(RoundedCornerShape(50))
                .background(Neon.BTN_BG)
                .border(1.dp, Neon.LIME.copy(alpha = 0.6f), RoundedCornerShape(50))
                .clickable { onShare() }.padding(horizontal = 10.dp, vertical = 5.dp)) {
                Text("SHARE", color = Neon.LIME, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            Spacer(Modifier.width(6.dp))
            Box(modifier = Modifier.clip(RoundedCornerShape(50))
                .background(if (recording) Neon.RED.copy(alpha = 0.35f) else Neon.PANEL)
                .border(1.dp, Neon.RED.copy(alpha = 0.75f), RoundedCornerShape(50))
                .clickable { onRecord() }.padding(horizontal = 12.dp, vertical = 5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Neon.RED))
                    Spacer(Modifier.width(6.dp))
                    Text(if (recording) "STOP ${fmt(recordingTime)}" else "REC",
                        color = Neon.RED, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
            Spacer(Modifier.width(6.dp))
            Box(modifier = Modifier.clip(RoundedCornerShape(50))
                .background(if (clubMode) Neon.CYAN.copy(alpha = 0.25f) else Neon.PANEL)
                .border(1.dp, Neon.CYAN.copy(alpha = 0.55f), RoundedCornerShape(50))
                .clickable { onClub() }.padding(horizontal = 12.dp, vertical = 5.dp)) {
                Text(if (clubMode) "CLUB ON" else "CLUB", color = Neon.CYAN, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}
@Composable
private fun VideoStrip(deckA: DeckPlayer, deckB: DeckPlayer, videoCrossfader: Float, transition: VideoTransition) {
    Box(modifier = Modifier.fillMaxWidth().aspectRatio(21f / 9f)
        .clip(RoundedCornerShape(10.dp)).background(Color.Black)
        .border(1.dp, Neon.BORDER_SOFT, RoundedCornerShape(10.dp))) {
        if (deckA.hasVideo) {
            VideoDeckView(deckA, videoCrossfader, false, transition, Neon.CYAN, true, Modifier.fillMaxSize())
        } else if (deckA.isLoaded) {
            AudioVisualizer(Neon.CYAN, deckA.isPlaying, deckA.loadedName, deckA.waveform, Modifier.fillMaxSize())
        }
        if (deckB.hasVideo) {
            VideoDeckView(deckB, videoCrossfader, true, transition, Neon.MAGENTA, true, Modifier.fillMaxSize())
        } else if (deckB.isLoaded) {
            Box(modifier = Modifier.fillMaxSize().graphicsLayer {
                alpha = (1f - videoCrossfader).coerceIn(0f, 1f)
            }) {
                AudioVisualizer(Neon.MAGENTA, deckB.isPlaying, deckB.loadedName, deckB.waveform, Modifier.fillMaxSize())
            }
        }
        if (!deckA.isLoaded && !deckB.isLoaded) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LOAD A VIDEO OR AUDIO", color = Neon.CYAN.copy(alpha = 0.6f), fontSize = 14.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 6.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("Tap LOAD on a deck  •  Use LIB to browse your library",
                        color = Neon.TEXT_FAINT, fontSize = 10.sp, letterSpacing = 2.sp)
                }
            }
        }
        Box(modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
            .clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.55f))
            .border(1.dp, Neon.CYAN.copy(alpha = 0.7f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp)) {
            Text("MIX", color = Neon.CYAN, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
        }
        val modeText = when {
            deckA.hasVideo && deckB.hasVideo -> transition.name
            !deckA.hasVideo && !deckB.hasVideo && deckA.isLoaded && deckB.isLoaded -> "AUDIO MIX"
            deckA.hasVideo || deckB.hasVideo -> "VIDEO • AUDIO"
            else -> "READY"
        }
        Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp)
            .clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 12.dp, vertical = 3.dp)) {
            Text(modeText, color = Color.White.copy(alpha = 0.95f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
        }
        Row(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (deckA.isPlaying) Neon.LIVE else Color(0x4000E5FF)))
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (deckB.isPlaying) Neon.LIVE else Color(0x40FF2BD6)))
        }
    }
}
@Composable
private fun VideoControlsRow(
    vcf: Float, onChange: (Float) -> Unit,
    a: Color, b: Color,
    transition: VideoTransition, onTransition: (VideoTransition) -> Unit,
    keyLock: Boolean, onKeyLock: (Boolean) -> Unit,
    fxFilter: Boolean, onFxFilter: (Boolean) -> Unit,
    fxEcho: Boolean, onFxEcho: (Boolean) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("◀", color = a, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Slider(value = vcf, onValueChange = onChange,
            colors = SliderDefaults.colors(thumbColor = Neon.CYAN,
                activeTrackColor = a.copy(alpha = 0.8f),
                inactiveTrackColor = b.copy(alpha = 0.8f)),
            modifier = Modifier.width(100.dp).height(18.dp))
        Text("▶", color = b, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Box(modifier = Modifier.clip(RoundedCornerShape(50))
            .background(if (keyLock) Neon.LIME.copy(alpha = 0.3f) else Neon.BTN_BG)
            .border(1.dp, Neon.LIME.copy(alpha = if (keyLock) 1f else 0.4f), RoundedCornerShape(50))
            .clickable { onKeyLock(!keyLock) }.padding(horizontal = 8.dp, vertical = 3.dp)) {
            Text("KEYLOCK", color = Neon.LIME, fontSize = 7.sp, fontWeight = FontWeight.Bold)
        }
        Box(modifier = Modifier.clip(RoundedCornerShape(50))
            .background(if (fxFilter) Neon.ORANGE.copy(alpha = 0.3f) else Neon.BTN_BG)
            .border(1.dp, Neon.ORANGE.copy(alpha = if (fxFilter) 1f else 0.4f), RoundedCornerShape(50))
            .clickable { onFxFilter(!fxFilter) }.padding(horizontal = 8.dp, vertical = 3.dp)) {
            Text("FILTER", color = Neon.ORANGE, fontSize = 7.sp, fontWeight = FontWeight.Bold)
        }
        Box(modifier = Modifier.clip(RoundedCornerShape(50))
            .background(if (fxEcho) Neon.CYAN.copy(alpha = 0.3f) else Neon.BTN_BG)
            .border(1.dp, Neon.CYAN.copy(alpha = if (fxEcho) 1f else 0.4f), RoundedCornerShape(50))
            .clickable { onFxEcho(!fxEcho) }.padding(horizontal = 8.dp, vertical = 3.dp)) {
            Text("ECHO", color = Neon.CYAN, fontSize = 7.sp, fontWeight = FontWeight.Bold)
        }
        Row(modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            for (t in VideoTransition.values()) {
                val sel = t == transition
                Box(modifier = Modifier.clip(RoundedCornerShape(50))
                    .background(if (sel) Neon.CYAN.copy(alpha = 0.3f) else Neon.BTN_BG)
                    .border(if (sel) 1.5.dp else 1.dp, Neon.CYAN.copy(alpha = if (sel) 1f else 0.3f), RoundedCornerShape(50))
                    .clickable { onTransition(t) }.padding(horizontal = 9.dp, vertical = 3.dp)) {
                    Text(t.name, color = if (sel) Neon.CYAN else Neon.TEXT_DIM, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
@Composable
private fun DeckPanel(
    deck: DeckPlayer, accent: Color, label: String,
    engine: AudioEngine, deckIndex: Int,
    onPickFile: () -> Unit, onPlay: () -> Unit, onSync: () -> Unit,
    onEqChange: (Float, Float, Float) -> Unit,
    onStemsChange: (Boolean, Boolean, Boolean) -> Unit,
    onSpeedChange: (Float) -> Unit,
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
    var pitchVal by remember { mutableFloatStateOf(1f) }
    val isPlaying = deck.isPlaying
    val scratching = deck.scratching
    val speed = deck.currentSpeed()
    val kindTag = if (deck.hasVideo) "VIDEO" else if (deck.isLoaded) "AUDIO" else ""
    val progress = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
    LaunchedEffect(deck) { while (true) { positionMs = deck.positionMs(); durationMs = deck.durationMs(); delay(400) } }
    Column(modifier = modifier.clip(RoundedCornerShape(14.dp))
        .background(if (scratching) accent.copy(alpha = 0.08f) else Neon.PANEL)
        .border(if (scratching) 2.dp else 1.dp, accent.copy(alpha = if (scratching) 1f else 0.5f), RoundedCornerShape(14.dp))
        .padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (scratching) Neon.YELLOW else accent))
                Spacer(Modifier.width(6.dp))
                Text(label, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                if (kindTag.isNotEmpty()) {
                    Spacer(Modifier.width(6.dp))
                    Box(modifier = Modifier.clip(RoundedCornerShape(50))
                        .background(accent.copy(alpha = 0.15f))
                        .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(50))
                        .padding(horizontal = 6.dp, vertical = 1.dp)) {
                        Text(kindTag, color = accent, fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
                if (deck.slipMode) {
                    Spacer(Modifier.width(4.dp))
                    Box(modifier = Modifier.clip(RoundedCornerShape(50))
                        .background(Neon.YELLOW.copy(alpha = 0.25f))
                        .padding(horizontal = 5.dp, vertical = 1.dp)) {
                        Text("SLIP", color = Neon.YELLOW, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (deck.censorActive) {
                    Spacer(Modifier.width(4.dp))
                    Box(modifier = Modifier.clip(RoundedCornerShape(50))
                        .background(Neon.RED.copy(alpha = 0.25f))
                        .padding(horizontal = 5.dp, vertical = 1.dp)) {
                        Text("CENSOR", color = Neon.RED, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text(when { scratching -> "SCR ×${"%.2f".format(deck.scratchRate)}"; isPlaying -> "LIVE"; else -> "READY" },
                color = when { scratching -> Neon.YELLOW; isPlaying -> Neon.LIVE; else -> Neon.TEXT_FAINT },
                fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        Text(if (deck.loadedName.isNotEmpty()) deck.loadedName else "Tap LOAD to pick a track",
            color = Neon.TEXT, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${fmt(positionMs)} / ${fmt(if (durationMs > 0) durationMs else 0L)}",
                color = accent.copy(alpha = 0.85f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Row {
                if (deck.musicalKey.isNotEmpty()) {
                    Text("${deck.musicalKey} (${deck.camelot})", color = accent.copy(alpha = 0.9f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                }
                Text("BPM ${"%.1f".format(deck.bpm)} • ${"%.0f".format(speed * 100)}%",
                    color = accent.copy(alpha = 0.75f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        SplitWaveform(accent = accent, waveform = deck.waveform, bpm = deck.bpm, progress = progress)
        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            MetalJogWheel(accent = accent, size = 100.dp, isPlaying = isPlaying,
                onScratchStart = { engine.beginScratch(deckIndex) },
                onScratchMove = { dy -> engine.scratchMove(deckIndex, dy) },
                onScratchEnd = { engine.endScratch(deckIndex) })
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            PillBtn("SYNC", accent, false, onSync, Modifier.weight(1f))
            PillBtn("CUE", accent, false, { engine.setCue(deckIndex, 0) }, Modifier.weight(1f))
            PillBtn("▶", accent, isPlaying, onPlay, Modifier.weight(1.4f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (b in intArrayOf(1, 2, 4, 8, 16)) {
                PillBtn("$b", accent, deck.loopActive && deck.loopBeats == b, { engine.toggleLoop(deckIndex, b) }, Modifier.weight(1f))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            PillBtn("ROLL", accent, deck.loopRollActive, {
                if (deck.loopRollActive) engine.stopLoopRoll(deckIndex) else engine.loopRoll(deckIndex, 1)
            }, Modifier.weight(1f))
            PillBtn("SLIP", accent, deck.slipMode, { engine.toggleSlip(deckIndex) }, Modifier.weight(1f))
            PillBtn("CENS", accent, deck.censorActive, { engine.censor(deckIndex, 4) }, Modifier.weight(1f))
            PillBtn("SPIN", accent, false, { engine.spinback(deckIndex) }, Modifier.weight(1f))
            PillBtn("BRK", accent, false, { engine.brake(deckIndex) }, Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (bj in intArrayOf(-8, -4, -1, 1, 4, 8)) {
                PillBtn(if (bj > 0) "+$bj" else "$bj", accent, false, { engine.beatJump(deckIndex, bj) }, Modifier.weight(1f))
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
                Box(modifier = Modifier.size(36.dp).clip(CircleShape)
                    .background(if (killActive) accent.copy(alpha = 0.4f) else Neon.BTN_BG)
                    .border(1.5.dp, accent.copy(alpha = if (killActive) 1f else 0.5f), CircleShape)
                    .clickable {
                        killActive = !killActive
                        if (killActive) onEqChange(-26f, -26f, -26f)
                        else { lowVal = 0f; midVal = 0f; hiVal = 0f; onEqChange(0f, 0f, 0f) }
                    }, contentAlignment = Alignment.Center) {
                    Text("KILL", color = accent, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("PITCH", color = accent.copy(alpha = 0.8f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Slider(value = pitchVal, onValueChange = {
                pitchVal = it; onSpeedChange(it * 2f)
            }, valueRange = 0.25f..2f,
                colors = SliderDefaults.colors(thumbColor = accent,
                    activeTrackColor = accent.copy(alpha = 0.7f),
                    inactiveTrackColor = Neon.BORDER_SOFT),
                modifier = Modifier.weight(1f).height(20.dp))
            Text("${"%.2f".format(pitchVal)}×", color = accent, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            PillBtn("LOAD", accent, false, onPickFile, Modifier.weight(1f))
        }
    }
}
@Composable
private fun CuePad(index: Int, accent: Color, hasCue: Boolean, onSet: () -> Unit, onJump: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(6.dp))
        .background(if (hasCue) accent.copy(alpha = 0.35f) else Neon.BTN_BG)
        .border(1.dp, accent.copy(alpha = if (hasCue) 1f else 0.35f), RoundedCornerShape(6.dp))
        .clickable { if (hasCue) onJump() else onSet() }.padding(vertical = 5.dp),
        contentAlignment = Alignment.Center) {
        Text(if (hasCue) "C${index + 1}" else "+${index + 1}", color = accent, fontSize = 7.sp, fontWeight = FontWeight.Bold)
    }
}
@Composable
private fun StemBtn(label: String, accent: Color, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(6.dp))
        .background(if (active) accent.copy(alpha = 0.4f) else Neon.BTN_BG)
        .border(1.dp, accent.copy(alpha = if (active) 1f else 0.35f), RoundedCornerShape(6.dp))
        .clickable { onClick() }.padding(vertical = 5.dp), contentAlignment = Alignment.Center) {
        Text(label, color = accent, fontSize = 7.sp, fontWeight = FontWeight.Bold)
    }
}
@Composable
private fun CenterMixer(engine: AudioEngine, acf: Float, onAcf: (Float) -> Unit,
                        cm: CrossfaderMode, onCm: (CrossfaderMode) -> Unit,
                        playing: Boolean, bpm: Float, modifier: Modifier = Modifier) {
    Column(modifier = modifier.clip(RoundedCornerShape(14.dp))
        .background(Neon.PANEL_GLASS)
        .border(1.dp, Neon.BORDER_SOFT, RoundedCornerShape(14.dp))
        .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("MIXER", color = Neon.TEXT_DIM, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
        Text("BPM ${"%.1f".format(bpm)}", color = Neon.CYAN, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("A", color = Neon.CYAN, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(4.dp))
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    VolumeFader(Neon.CYAN, 0.75f)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("B", color = Neon.MAGENTA, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(4.dp))
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    VolumeFader(Neon.MAGENTA, 0.80f)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("VU", color = Neon.TEXT, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(4.dp))
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    MasterVu(playing = playing)
                }
            }
        }
        Text("CROSSFADER", color = Neon.TEXT_DIM, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
        Slider(value = acf, onValueChange = onAcf,
            colors = SliderDefaults.colors(thumbColor = Neon.CYAN,
                activeTrackColor = Neon.CYAN.copy(alpha = 0.8f),
                inactiveTrackColor = Neon.MAGENTA.copy(alpha = 0.8f)),
            modifier = Modifier.fillMaxWidth().height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("CUT", color = if (cm == CrossfaderMode.CUT) Neon.CYAN else Neon.TEXT_FAINT,
                fontSize = 9.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onCm(CrossfaderMode.CUT) }.padding(horizontal = 6.dp, vertical = 3.dp))
            Text("│", color = Neon.TEXT_FAINT, fontSize = 9.sp)
            Text("FADE", color = if (cm == CrossfaderMode.FADE) Neon.CYAN else Neon.TEXT_FAINT,
                fontSize = 9.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onCm(CrossfaderMode.FADE) }.padding(horizontal = 6.dp, vertical = 3.dp))
        }
    }
}
@Composable
private fun FxDrawer(engine: AudioEngine, deckA: Int) {
    var echoOn by remember { mutableStateOf(false) }
    var filterOn by remember { mutableStateOf(false) }
    var filterHP by remember { mutableStateOf(false) }
    var vocalOn by remember { mutableStateOf(false) }
    var bassRemoveOn by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth().height(70.dp)
        .clip(RoundedCornerShape(14.dp)).background(Neon.PANEL_GLASS)
        .border(1.dp, Neon.PURPLE.copy(alpha = 0.5f), RoundedCornerShape(14.dp)).padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FxPad("ECHO", Neon.CYAN, echoOn, { echoOn = !echoOn; engine.setEcho(echoOn) }, Modifier.weight(1f))
        FxPad("FLT LP", Neon.ORANGE, filterOn && !filterHP, { filterOn = true; filterHP = false; engine.setFilter(true); engine.setFilterHighPass(false) }, Modifier.weight(1f))
        FxPad("FLT HP", Neon.ORANGE, filterOn && filterHP, { filterOn = true; filterHP = true; engine.setFilter(true); engine.setFilterHighPass(true) }, Modifier.weight(1f))
        FxPad("VOCAL", Neon.PURPLE, vocalOn, { vocalOn = !vocalOn; engine.setVocalRemoval(vocalOn) }, Modifier.weight(1f))
        FxPad("BASS", Neon.LIME, bassRemoveOn, { bassRemoveOn = !bassRemoveOn; engine.setBassRemove(bassRemoveOn) }, Modifier.weight(1f))
        FxPad("CENS", Neon.RED, false, { engine.censor(deckA, 4) }, Modifier.weight(1f))
    }
}
@Composable
private fun FxPad(label: String, accent: Color, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxHeight()
        .clip(RoundedCornerShape(10.dp))
        .background(if (active) accent.copy(alpha = 0.35f) else accent.copy(alpha = 0.12f))
        .border(if (active) 2.dp else 1.dp, accent.copy(alpha = if (active) 1f else 0.55f), RoundedCornerShape(10.dp))
        .clickable { onClick() }, contentAlignment = Alignment.Center) {
        Text(label, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}
@Composable
private fun BottomBar(activePanel: BottomPanel, onTogglePanel: (BottomPanel) -> Unit,
                      activeDeckA: Int, activeDeckB: Int,
                      onSwapA: (Int) -> Unit, onSwapB: (Int) -> Unit, onLoadSample: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        BarBtn("☰ LIB", Neon.CYAN, activePanel == BottomPanel.LIBRARY) { onTogglePanel(BottomPanel.LIBRARY) }
        BarBtn("SAMPLER", Neon.MAGENTA, activePanel == BottomPanel.SAMPLER) { onTogglePanel(BottomPanel.SAMPLER) }
        Spacer(Modifier.weight(1f))
        for (id in 2..5) {
            val isA = id == activeDeckA; val isB = id == activeDeckB
            val active = isA || isB
            val col = if (active) Neon.DECK_COLORS[id] else Neon.TEXT_DIM
            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))
                .background(if (active) Neon.DECK_COLORS[id].copy(alpha = 0.25f) else Neon.BTN_BG)
                .border(1.dp, col.copy(alpha = if (active) 1f else 0.4f), RoundedCornerShape(8.dp))
                .clickable { if (isA) onSwapA(0) else if (isB) onSwapB(1) else { onSwapA(id); onLoadSample(id) } }
                .padding(horizontal = 8.dp, vertical = 7.dp), contentAlignment = Alignment.Center) {
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
    Box(modifier = Modifier.clip(RoundedCornerShape(10.dp))
        .background(if (active) accent.copy(alpha = 0.35f) else Neon.BTN_BG)
        .border(1.dp, accent.copy(alpha = if (active) 1f else 0.5f), RoundedCornerShape(10.dp))
        .clickable { onClick() }.padding(horizontal = 10.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
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
