package com.djpro.mixer.audio
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import com.djpro.mixer.dsp.EchoAudioProcessor
import com.djpro.mixer.dsp.EqProcessor
import com.djpro.mixer.dsp.FilterAudioProcessor
import com.djpro.mixer.dsp.MasterLimiterProcessor
import com.djpro.mixer.dsp.VocalRemovalProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DeckPlayer(val index: Int, context: Context) {
    val echo = EchoAudioProcessor()
    val filter = FilterAudioProcessor()
    val vocal = VocalRemovalProcessor()
    val eq = EqProcessor()
    val limiter = MasterLimiterProcessor()
    private val audioSink: AudioSink = DefaultAudioSink.Builder()
        .setAudioProcessors(arrayOf<AudioProcessor>(eq, echo, filter, vocal, limiter)).build()
    private val renderersFactory = object : DefaultRenderersFactory(context) {
        override fun buildAudioSink(context: Context, enableFloatOutput: Boolean,
                                    enableAudioTrackPlaybackParams: Boolean): AudioSink = audioSink
    }
    val player: ExoPlayer = ExoPlayer.Builder(context, renderersFactory).build().apply {
        repeatMode = Player.REPEAT_MODE_OFF; playWhenReady = false; volume = 1f
    }
    var isLoaded by mutableStateOf(false); private set
    var isPlaying by mutableStateOf(false); private set
    var scratching by mutableStateOf(false); private set
    var scratchRate by mutableFloatStateOf(1f); private set
    var scratchVelocity by mutableFloatStateOf(0f); private set
    var bpm: Float = 128f; private set
    var loadedName: String = ""; private set
    val cuePoints = LongArray(8) { -1L }
    var loopActive by mutableStateOf(false); private set
    var loopBeats = 4; private set
    var loopStartMs = 0L; private set
    var loopEndMs = 0L; private set
    private var scratchStartMs = 0L
    private var scratchAccumMs = 0L
    private var wasPlayingBeforeScratch = false
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var effectJob: Job? = null
    private var loopJob: Job? = null

    fun load(uri: String, displayName: String = "Loaded") {
        try {
            player.setMediaItem(MediaItem.fromUri(uri)); player.prepare()
            isLoaded = true; bpm = 120f + (0..16).random(); loadedName = displayName
        } catch (_: Throwable) { isLoaded = false }
    }
    fun play() { try { player.play(); isPlaying = true } catch (_: Throwable) {} }
    fun pause() { try { player.pause(); isPlaying = false } catch (_: Throwable) {} }
    fun toggle() { if (isPlaying) pause() else play() }
    fun setVolume(v: Float) { try { player.volume = v.coerceIn(0f, 1f) } catch (_: Throwable) {} }
    fun currentSpeed(): Float = try { player.playbackParameters.speed } catch (_: Throwable) { 1f }

    fun beginScratch() {
        try {
            wasPlayingBeforeScratch = player.isPlaying
            scratching = true
            scratchStartMs = player.currentPosition; scratchAccumMs = 0L
            scratchRate = 0f; scratchVelocity = 0f
            player.pause()
        } catch (_: Throwable) {}
    }
    fun scratchMove(deltaPixels: Float) {
        try {
            val msPerPixel = 5f
            scratchAccumMs += (-deltaPixels * msPerPixel).toLong()
            val dur = player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
            val target = (scratchStartMs + scratchAccumMs).coerceIn(0L, dur)
            player.seekTo(target)
            val instantRate = (-deltaPixels / 15f).coerceIn(-3f, 3f)
            scratchRate = instantRate; scratchVelocity = instantRate
        } catch (_: Throwable) {}
    }
    fun endScratch() {
        try {
            val inertiaMs = (scratchAccumMs * 0.20f).toLong()
            val dur = player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
            val finalPos = (scratchStartMs + scratchAccumMs + inertiaMs).coerceIn(0L, dur)
            player.seekTo(finalPos)
            if (wasPlayingBeforeScratch) player.play()
            scratching = false; scratchRate = 1f; scratchVelocity = 0f
        } catch (_: Throwable) { scratching = false }
    }
    fun spinback() {
        effectJob?.cancel()
        effectJob = scope.launch {
            try {
                val wasPlaying = player.isPlaying
                scratching = true
                val startMs = player.currentPosition
                val rewindMs = 900L; val steps = 18; val stepBack = rewindMs / steps
                for (i in 0 until steps) {
                    player.seekTo((startMs - i * stepBack).coerceAtLeast(0L))
                    scratchRate = -3f; scratchVelocity = -3f; delay(18)
                }
                scratchRate = 1f; scratchVelocity = 0f; scratching = false
                if (wasPlaying) player.play()
            } catch (_: Throwable) { scratching = false }
        }
    }
    fun brake() {
        effectJob?.cancel()
        effectJob = scope.launch {
            try {
                scratching = true
                var speed = player.playbackParameters.speed
                if (speed <= 0.05f) speed = 1f
                val step = (speed - 0.05f) / 14f
                for (i in 0 until 14) {
                    speed -= step; player.setPlaybackSpeed(speed.coerceAtLeast(0.05f))
                    scratchRate = speed; scratchVelocity = speed - 1f; delay(35)
                }
                player.pause(); player.setPlaybackSpeed(1f)
                scratchRate = 1f; scratchVelocity = 0f
            } catch (_: Throwable) {}
            scratching = false
        }
    }
    fun setCue(slot: Int) { if (slot in 0..7) cuePoints[slot] = positionMs() }
    fun jumpToCue(slot: Int) {
        if (slot !in 0..7) return
        val t = cuePoints[slot]; if (t >= 0) try { player.seekTo(t) } catch (_: Throwable) {}
    }
    fun toggleLoop(beats: Int = loopBeats) { loopBeats = beats; if (loopActive) stopLoop() else startLoop(beats) }
    private fun startLoop(beats: Int) {
        try {
            val beatMs = if (bpm > 0) (60000f / bpm).toLong() else 500L
            val loopLen = beatMs * beats; val start = positionMs()
            loopStartMs = start; loopEndMs = start + loopLen; loopActive = true
            loopJob?.cancel(); loopJob = scope.launch {
                while (loopActive) {
                    try { if (player.currentPosition >= loopEndMs) player.seekTo(loopStartMs) } catch (_: Throwable) {}
                    delay(30)
                }
            }
        } catch (_: Throwable) {}
    }
    fun stopLoop() { loopActive = false; loopJob?.cancel(); loopJob = null }
    fun positionMs(): Long = try { player.currentPosition } catch (_: Throwable) { 0L }
    fun durationMs(): Long = try { val d = player.duration; if (d > 0) d else 0L } catch (_: Throwable) { 0L }
    fun release() {
        effectJob?.cancel(); loopJob?.cancel()
        try { player.release() } catch (_: Throwable) {}
        try { audioSink.release() } catch (_: Throwable) {}
    }
}
