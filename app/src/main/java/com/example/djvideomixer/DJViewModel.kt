package com.example.djvideomixer

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel

class DJViewModel(app: Application) : AndroidViewModel(app) {
    private val engine = AudioEngine(app)

    val deckA: Deck get() = engine.deckA
    val deckB: Deck get() = engine.deckB

    private val _crossfader = mutableStateOf(0.5f)
    val crossfader: State<Float> = _crossfader

    private val _echo = mutableStateOf(false)
    val echo: State<Boolean> = _echo

    private val _vocal = mutableStateOf(false)
    val vocal: State<Boolean> = _vocal

    private val _filter = mutableStateOf(false)
    val filter: State<Boolean> = _filter

    private val _videoAlpha = mutableStateOf(0.5f)
    val videoAlpha: State<Float> = _videoAlpha

    fun setCrossfader(v: Float) {
        _crossfader.value = v
        engine.crossfader = v
        _videoAlpha.value = 1f - v
    }

    fun toggleEcho() {
        _echo.value = !_echo.value
        engine.echoEnabled = _echo.value
    }

    fun toggleVocal() {
        _vocal.value = !_vocal.value
        engine.vocalRemovalEnabled = _vocal.value
    }

    fun toggleFilter() {
        _filter.value = !_filter.value
        engine.filterEnabled = _filter.value
    }

    fun loadA(uri: String) = engine.deckA.load(uri)
    fun loadB(uri: String) = engine.deckB.load(uri)
    fun toggleA() = engine.deckA.toggle()
    fun toggleB() = engine.deckB.toggle()

    fun scratchA(rate: Float) = engine.deckA.scratchRate(rate)
    fun scratchB(rate: Float) = engine.deckB.scratchRate(rate)
    fun endScratchA() = engine.deckA.resetRate()
    fun endScratchB() = engine.deckB.resetRate()

    fun release() = engine.release()
}
