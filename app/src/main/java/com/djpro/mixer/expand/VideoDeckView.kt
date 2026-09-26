package com.djpro.mixer.expand

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.djpro.mixer.audio.DeckPlayer

@Composable
fun VideoDeckView(
    deck: DeckPlayer,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.graphicsLayer { this.alpha = alpha },
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = deck.player
                useController = false
                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
            }
        }
    )
}
