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
            CrossfaderMode.FADE -> cos(crossfader * Math.PI.toFloat() / 2f) to sin(crossfader * Math.PI.toFloat() / 2f)
            CrossfaderMode.CUT -> {
                val zone = 0.05f
                when {
                    crossfader < 0.5f - zone -> 1f to 0f
                    crossfader > 0.5f + zone -> 0f to 1f
                    else -> { val t = (crossfader - (0.5f - zone)) / (2 * zone); (1f - t) to t }
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
    fun setStemMutes(d: Boolean, b: Boolean, o: Boolean) {
        stemDrumsMuted = d; stemBassMuted = b; stemOtherMuted = o
        val ld = if (b) -26f else if (d) -12f else 0f
        val hd = if (o) -26f else 0f
        decks.forEach { it.eq.lowGainDb = ld; it.eq.highGainDb = hd }
    }
    fun setEq(index: Int, low: Float, mid: Float, high: Float) {
        if (index !in decks.indices) return
        val d = decks[index]
        d.eq.lowGainDb = low.coerceIn(-26f, 6f)
        d.eq.midGainDb = mid.coerceIn(-26f, 6f)
        d.eq.highGainDb = high.coerceIn(-26f, 6f)
    }
    fun syncBpm(s: Int, t: Int) {
        if (s !in decks.indices || t !in decks.indices) return
        val a = decks[s].bpm; val b = decks[t].bpm
        if (a <= 0f || b <= 0f) return
        try { decks[t].player.setPlaybackSpeed((a / b).coerceIn(0.5f, 2f)) } catch (_: Throwable) {}
    }
    fun loadDeck(index: Int, uri: String, name: String = "Loaded") {
        if (index in decks.indices) {
            decks[index].load(uri, name)
            history.add(0, name); if (history.size > 20) history.removeAt(history.size - 1)
        }
    }
    fun togglePlay(i: Int) { if (i in decks.indices) decks[i].toggle() }
    fun beginScratch(i: Int) { if (i in decks.indices) decks[i].beginScratch() }
    fun scratchMove(i: Int, dy: Float) { if (i in decks.indices) decks[i].scratchMove(dy) }
    fun endScratch(i: Int) { if (i in decks.indices) decks[i].endScratch() }
    fun spinback(i: Int) { if (i in decks.indices) decks[i].spinback() }
    fun brake(i: Int) { if (i in decks.indices) decks[i].brake() }
    fun setCue(i: Int, s: Int) { if (i in decks.indices) decks[i].setCue(s) }
    fun jumpCue(i: Int, s: Int) { if (i in decks.indices) decks[i].jumpToCue(s) }
    fun toggleLoop(i: Int, b: Int) { if (i in decks.indices) decks[i].toggleLoop(b) }
    fun release() { decks.forEach { it.release() } }
}
