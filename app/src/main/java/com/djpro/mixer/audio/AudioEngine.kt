package com.djpro.mixer.audio
import android.content.Context
import kotlin.math.cos
import kotlin.math.sin
enum class CrossfaderMode { CUT, FADE }
class AudioEngine(context: Context) {
    val decks: List<DeckPlayer> = (0 until 6).map { DeckPlayer(it, context) }
    var crossfader: Float = 0.5f; private set
    var crossfaderMode: CrossfaderMode = CrossfaderMode.FADE; private set
    var masterVolume: Float = 1.0f; private set
    var activeDeckA: Int = 0; private set
    var activeDeckB: Int = 1; private set
    var echoEnabled = false; var filterEnabled = false; var vocalRemovalEnabled = false
    var stemDrumsMuted = false; var stemBassMuted = false; var stemOtherMuted = false
    val history = mutableListOf<String>()
    init { setCrossfader(0.5f); setMasterVolume(1.0f) }
    fun swapToA(deckId: Int) { if (deckId in decks.indices && deckId != activeDeckB) { activeDeckA = deckId; applyCrossfader() } }
    fun swapToB(deckId: Int) { if (deckId in decks.indices && deckId != activeDeckB && deckId != activeDeckA) { activeDeckB = deckId; applyCrossfader() } }
    fun deckA(): DeckPlayer = decks[activeDeckA]
    fun deckB(): DeckPlayer = decks[activeDeckB]
    fun setCrossfader(v: Float) { crossfader = v.coerceIn(0f, 1f); applyCrossfader() }
    fun setCrossfaderMode(m: CrossfaderMode) { crossfaderMode = m; applyCrossfader() }
    private fun applyCrossfader() {
        val (a, b) = when (crossfaderMode) {
            CrossfaderMode.FADE -> cos(crossfader * Math.PI / 2).toFloat() to sin(crossfader * Math.PI / 2).toFloat()
            CrossfaderMode.CUT -> {
                val zone = 0.05f
                when {
                    crossfader < 0.5f - zone -> 1f to 0f
                    crossfader > 0.5f + zone -> 0f to 1f
                    else -> { val t = (crossfader - (0.5f - zone)) / (2 * zone); 1f - t to t }
                }
            }
        }
        decks[activeDeckA].setVolume(a * masterVolume)
        decks[activeDeckB].setVolume(b * masterVolume)
    }
    fun setMasterVolume(v: Float) { masterVolume = v.coerceIn(0f, 1f); applyCrossfader() }
    fun setEcho(on: Boolean) { echoEnabled = on; decks.forEach { it.echo.enabled = on } }
    fun setFilter(on: Boolean) { filterEnabled = on; decks.forEach { it.filter.enabled = on } }
    fun setVocalRemoval(on: Boolean) { vocalRemovalEnabled = on; decks.forEach { it.vocal.enabled = on } }
    fun setStemMutes(drums: Boolean, bass: Boolean, other: Boolean) {
        stemDrumsMuted = drums; stemBassMuted = bass; stemOtherMuted = other
        val lowDb = if (bass) -26f else if (drums) -12f else 0f
        val highDb = if (other) -26f else 0f
        decks.forEach { it.eq.lowGainDb = lowDb; it.eq.highGainDb = highDb }
    }
    fun setEq(index: Int, low: Float, mid: Float, high: Float) {
        if (index !in decks.indices) return
        val d = decks[index]
        d.eq.lowGainDb = low.coerceIn(-26f, 6f)
        d.eq.midGainDb = mid.coerceIn(-26f, 6f)
        d.eq.highGainDb = high.coerceIn(-26f, 6f)
    }
    fun syncBpm(sourceIndex: Int, targetIndex: Int) {
        if (sourceIndex !in decks.indices || targetIndex !in decks.indices) return
        val src = decks[sourceIndex].bpm; val tgt = decks[targetIndex].bpm
        if (src <= 0f || tgt <= 0f) return
        try { decks[targetIndex].player.setPlaybackSpeed((src / tgt).coerceIn(0.5f, 2f)) } catch (_: Throwable) {}
    }
    fun loadDeck(index: Int, uri: String, name: String = "Loaded") {
        if (index in decks.indices) {
            decks[index].load(uri, name)
            history.add(0, name); if (history.size > 20) history.removeAt(history.size - 1)
        }
    }
    fun togglePlay(index: Int) { if (index in decks.indices) decks[index].toggle() }
    fun beginScratch(index: Int) { if (index in decks.indices) decks[index].beginScratch() }
    fun scratchMove(index: Int, dy: Float) { if (index in decks.indices) decks[index].scratchMove(dy) }
    fun endScratch(index: Int) { if (index in decks.indices) decks[index].endScratch() }
    fun spinback(index: Int) { if (index in decks.indices) decks[index].spinback() }
    fun brake(index: Int) { if (index in decks.indices) decks[index].brake() }
    fun setCue(index: Int, slot: Int) { if (index in decks.indices) decks[index].setCue(slot) }
    fun jumpCue(index: Int, slot: Int) { if (index in decks.indices) decks[index].jumpToCue(slot) }
    fun toggleLoop(index: Int, beats: Int) { if (index in decks.indices) decks[index].toggleLoop(beats) }
    fun release() { decks.forEach { it.release() } }
}
