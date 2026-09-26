package com.djpro.audio

import android.content.Context
import com.djpro.nativecore.NativeBridge
import kotlin.math.cos
import kotlin.math.sin

class SixDeckEngine(private val context: Context) {
    data class DeckState(
        var loaded: Boolean = false,
        var volume: Float = 1.0f,
        var playing: Boolean = false,
        var muteVocals: Boolean = false,
        var muteDrums: Boolean = false,
        var muteBass: Boolean = false,
        var muteOther: Boolean = false,
    )

    val decks = MutableList(6) { DeckState() }
    var crossfader: Float = 0.5f
        private set
    var masterVolume: Float = 1.0f
        private set

    init { NativeBridge.startEngine() }

    fun setCrossfader(v: Float) {
        crossfader = v.coerceIn(0f, 1f)
        val a = cos(crossfader * Math.PI / 2).toFloat()
        val b = sin(crossfader * Math.PI / 2).toFloat()
        NativeBridge.setDeckVolume(0, a * decks[0].volume)
        NativeBridge.setDeckVolume(1, b * decks[1].volume)
    }

    fun setMasterVolume(v: Float) {
        masterVolume = v.coerceIn(0f, 1f)
        for (i in 0 until 6) NativeBridge.setDeckVolume(i, masterVolume * decks[i].volume)
    }

    fun loadDeck(id: Int, samples: FloatArray, volume: Float = 1f) {
        decks[id].loaded = true
        decks[id].volume = volume
        NativeBridge.loadDeck(id, samples, volume * masterVolume)
    }

    fun toggleStem(id: Int, stem: String) {
        val d = decks[id]
        when (stem) {
            "vocals" -> d.muteVocals = !d.muteVocals
            "drums"  -> d.muteDrums = !d.muteDrums
            "bass"   -> d.muteBass = !d.muteBass
            "other"  -> d.muteOther = !d.muteOther
        }
    }

    fun release() = NativeBridge.stopEngine()
}
