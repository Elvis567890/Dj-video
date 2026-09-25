package com.example.djvideomixer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView

@Composable
fun VideoDeckView(
    deck: Deck,
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
