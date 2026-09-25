package com.example.djvideomixer

import android.content.Context
import kotlin.math.cos
import kotlin.math.sin

class AudioEngine(context: Context) {
    val deckA = Deck("A", context)
    val deckB = Deck("B", context)

    var echoEnabled = false
    var vocalRemovalEnabled = false
    var filterEnabled = false

    var crossfader: Float = 0.5f
        set(value) {
            field = value.coerceIn(0f, 1f)
            val a = cos(field * Math.PI / 2).toFloat()
            val b = sin(field * Math.PI / 2).toFloat()
            deckA.setVolume(a)
            deckB.setVolume(b)
        }

    fun release() {
        deckA.release()
        deckB.release()
    }
}
