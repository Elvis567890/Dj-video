package com.djpro.mixer.audio

import android.content.Context
import kotlin.math.cos
import kotlin.math.sin

class AudioEngine(context: Context) {

    val decks: List<DeckPlayer> = (0 until 6).map { DeckPlayer(it, context) }

    var crossfader: Float = 0.5f
        private set
    var masterVolume: Float = 1.0f
        private set

    var echoEnabled = false
    var filterEnabled = false
    var vocalRemovalEnabled = false

    init {
        setCrossfader(0.5f)
        setMasterVolume(1.0f)
    }

    fun setCrossfader(v: Float) {
        crossfader = v.coerceIn(0f, 1f)
        val a = cos(crossfader * Math.PI / 2).toFloat()
        val b = sin(crossfader * Math.PI / 2).toFloat()
        decks[0].setVolume(a * masterVolume)
        decks[1].setVolume(b * masterVolume)
    }

    fun setMasterVolume(v: Float) {
        masterVolume = v.coerceIn(0f, 1f)
        setCrossfader(crossfader)
    }

    fun setEcho(on: Boolean) {
        echoEnabled = on
        decks.forEach { it.echo.enabled = on }
    }

    fun setFilter(on: Boolean) {
        filterEnabled = on
        decks.forEach { it.filter.enabled = on }
    }

    fun setVocalRemoval(on: Boolean) {
        vocalRemovalEnabled = on
        decks.forEach { it.vocal.enabled = on }
    }

    fun loadDeck(index: Int, uri: String) {
        if (index in decks.indices) decks[index].load(uri)
    }

    fun togglePlay(index: Int) {
        if (index in decks.indices) decks[index].toggle()
    }

    fun scratch(index: Int, rate: Float) {
        if (index in decks.indices) decks[index].scratch(rate)
    }

    fun endScratch(index: Int) {
        if (index in decks.indices) decks[index].endScratch()
    }

    fun release() {
        decks.forEach { it.release() }
    }
}
