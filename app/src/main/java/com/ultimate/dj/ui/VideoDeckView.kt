package com.ultimate.dj.ui
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.ultimate.dj.R
import com.ultimate.dj.audio.DeckPlayer
enum class VideoTransition { CROSSFADE, ZOOM, WIPE, SLIDE, FLIP, GLITCH, STROBE, SPIN, BURN }
@Composable
fun VideoDeckView(
    deck: DeckPlayer, progress: Float, isIncoming: Boolean,
    transition: VideoTransition = VideoTransition.CROSSFADE,
    accentColor: Color = Color.Cyan,
    showScratchOverlay: Boolean = true,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.clipToBounds()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { c ->
                val view = android.view.LayoutInflater.from(c)
                    .inflate(R.layout.player_view, null) as PlayerView
                view.player = deck.player
                view
            },
            update = { view -> view.player = deck.player },
            onRelease = { view -> view.player = null }
        )
    }
}
