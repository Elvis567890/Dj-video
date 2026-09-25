package com.example.djvideomixer

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class Deck(val id: String, context: Context) {
    val player: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        repeatMode = Player.REPEAT_MODE_OFF
        playWhenReady = false
    }

    private var baseVolume: Float = 1f

    fun load(uri: String) {
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
    }

    fun play() = player.play()
    fun pause() = player.pause()
    fun toggle() { if (player.isPlaying) pause() else play() }

    fun setVolume(v: Float) {
        baseVolume = v.coerceIn(0f, 1f)
        player.volume = baseVolume
    }

    fun scratchRate(rate: Float) {
        val clamped = rate.coerceIn(0.25f, 2.5f)
        player.setPlaybackSpeed(clamped)
    }

    fun resetRate() {
        player.setPlaybackSpeed(1f)
    }

    fun release() = player.release()
}
