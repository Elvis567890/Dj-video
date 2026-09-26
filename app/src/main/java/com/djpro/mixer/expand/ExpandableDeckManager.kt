package com.djpro.mixer.expand

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class DeckSize { MINI, BIG, EXPANDED }

data class DeckLayout(
    val expandedDeckId: Int? = null,
    val crossfaderA: Int = 0,
    val crossfaderB: Int = 1,
    val videoFullscreen: Boolean = false,
    val lastExpandedDeckId: Int? = null,
)

@Stable
class ExpandableDeckManager {
    var state by mutableStateOf(DeckLayout())
        private set

    val bigDeckIds: List<Int>
        get() = buildList {
            add(0)
            state.expandedDeckId?.takeIf { it > 1 }?.let { add(it) }
            add(1)
        }

    val miniDeckIds: List<Int>
        get() = (2..5).filter { it != state.expandedDeckId }

    fun tapDeck(id: Int) {
        val current = state.expandedDeckId
        if (current == id) collapse() else if (id > 1) expand(id)
    }

    fun expand(id: Int) {
        state = state.copy(
            expandedDeckId = id,
            lastExpandedDeckId = id,
            crossfaderA = 0,
            crossfaderB = id
        )
    }

    fun collapse() {
        state = state.copy(
            expandedDeckId = null,
            crossfaderA = 0,
            crossfaderB = 1
        )
    }

    fun toggleVideoFullscreen() {
        state = state.copy(videoFullscreen = !state.videoFullscreen)
    }

    fun crossfaderLabel(): String =
        "DECK ${state.crossfaderA + 1}  \u2194  DECK ${state.crossfaderB + 1}"
}
