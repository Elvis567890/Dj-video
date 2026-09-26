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
import com.djpro.mixer.video.MixRecorder
class MainActivity : ComponentActivity() {
    private lateinit var engine: AudioEngine
    private lateinit var recorder: MixRecorder
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        engine = AudioEngine(this)
        recorder = MixRecorder(this)
        val activityRef = this
        val engineRef = engine
        val recorderRef = recorder
        setContent {
            MaterialTheme {
                Box(modifier = Modifier.fillMaxSize().background(Neon.BG)) {
                    MainScreen(engineRef, recorderRef, activityRef)
                }
            }
        }
    }
    override fun onDestroy() { super.onDestroy(); engine.release(); try { recorder.stop() } catch (_: Throwable) {} }
}
