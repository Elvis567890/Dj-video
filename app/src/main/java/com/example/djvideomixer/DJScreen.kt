package com.example.djvideomixer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DJScreen(vm: DJViewModel) {
    val crossfader by vm.crossfader
    val echo by vm.echo
    val vocal by vm.vocal
    val filter by vm.filter
    val videoAlpha by vm.videoAlpha

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(8.dp)
    ) {
        // ---- VIDEO MIX AREA (upper middle) ----
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF111111)),
            contentAlignment = Alignment.Center
        ) {
            VideoDeckView(deck = vm.deckA, alpha = 1f, modifier = Modifier.fillMaxSize())
            VideoDeckView(deck = vm.deckB, alpha = videoAlpha, modifier = Modifier.fillMaxSize())
            Text("VIDEO MIX", color = Color.White.copy(alpha = 0.3f), fontSize = 24.sp)
        }

        Spacer(Modifier.height(8.dp))

        // ---- DECKS ROW ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DeckPanel(
                title = "DECK A",
                onLoad = { vm.loadA("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4") },
                onPlay = { vm.toggleA() },
                onScratch = { vm.scratchA(it) },
                onRelease = { vm.endScratchA() }
            )

            DeckPanel(
                title = "DECK B",
                onLoad = { vm.loadB("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4") },
                onPlay = { vm.toggleB() },
                onScratch = { vm.scratchB(it) },
                onRelease = { vm.endScratchB() }
            )
        }

        Spacer(Modifier.height(8.dp))

        // ---- CROSSFADER + FX ----
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("CROSSFADER", color = Color.White, fontSize = 12.sp)
            Slider(
                value = crossfader,
                onValueChange = { vm.setCrossfader(it) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FxButton("ECHO", echo) { vm.toggleEcho() }
                FxButton("VOCAL OFF", vocal) { vm.toggleVocal() }
                FxButton("FILTER", filter) { vm.toggleFilter() }
            }
        }
    }
}

@Composable
fun DeckPanel(
    title: String,
    onLoad: () -> Unit,
    onPlay: () -> Unit,
    onScratch: (Float) -> Unit,
    onRelease: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = Color.White, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        JogWheel(label = "SCRATCH", onScratch = onScratch, onRelease = onRelease)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Button(onClick = onLoad) { Text("LOAD", fontSize = 10.sp) }
            Button(onClick = onPlay) { Text("PLAY", fontSize = 10.sp) }
        }
    }
}

@Composable
fun FxButton(label: String, active: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) Color(0xFF00BCD4) else Color(0xFF333333)
        )
    ) {
        Text(label, fontSize = 11.sp)
    }
}
