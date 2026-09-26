package com.djpro.mixer.audio

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlin.math.cos
import kotlin.math.sin

/**
 * A single DJ deck backed by ExoPlayer.
 * Supports load / play / pause / scratch / volume.
 */
class DeckPlayer(val index: Int, context: Context) {

    val player: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        repeatMode = Player.REPEAT_MODE_OFF
        playWhenReady = false
        volume = 0f
    }

    var isLoaded: Boolean = false
        private set
    var isPlaying: Boolean = false
        private set

    fun load(uri: String) {
        try {
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
            isLoaded = true
        } catch (_: Throwable) {
            isLoaded = false
        }
    }

    fun play() {
        try { player.play(); isPlaying = true } catch (_: Throwable) {}
    }

    fun pause() {
        try { player.pause(); isPlaying = false } catch (_: Throwable) {}
    }

    fun toggle() {
        if (isPlaying) pause() else play()
    }

    fun setVolume(v: Float) {
        try { player.volume = v.coerceIn(0f, 1f) } catch (_: Throwable) {}
    }

    fun scratch(rate: Float) {
        try {
            player.setPlaybackSpeed(rate.coerceIn(0.25f, 2.5f))
        } catch (_: Throwable) {}
    }

    fun endScratch() {
        try { player.setPlaybackSpeed(1f) } catch (_: Throwable) {}
    }

    fun release() {
        try { player.release() } catch (_: Throwable) {}
    }
}
