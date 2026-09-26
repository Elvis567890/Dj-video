package com.djpro.mixer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import com.djpro.mixer.audio.AudioEngine

class MainActivity : ComponentActivity() {

    private lateinit var engine: AudioEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        engine = AudioEngine(this)

        setContent {
            MaterialTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Neon.BG)
                ) {
                    SixDeckScreen(engine)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        engine.release()
    }
}
