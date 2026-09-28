package com.ultimate.dj
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.ultimate.dj.audio.AudioEngine
import com.ultimate.dj.video.MixRecorder
class MainActivity : ComponentActivity() {
    private lateinit var engine: AudioEngine
    private lateinit var recorder: MixRecorder
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
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
