package com.djpro.mixer.audio
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
object SampleFilePlayer {
    private val active = mutableListOf<MediaPlayer>()
    fun play(context: Context, uri: Uri, onError: (() -> Unit)? = null) {
        try {
            val mp = MediaPlayer()
            mp.setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            mp.setDataSource(context, uri)
            mp.setOnCompletionListener { p -> try { p.release() } catch (_: Throwable) {}; active.remove(p) }
            mp.setOnErrorListener { p, _, _ -> try { p.release() } catch (_: Throwable) {}; active.remove(p); onError?.invoke(); true }
            mp.prepare(); mp.start(); active.add(mp)
            if (active.size > 8) { val o = active.removeAt(0); try { o.release() } catch (_: Throwable) {} }
        } catch (_: Throwable) { onError?.invoke() }
    }
    fun stopAll() {
        for (mp in active.toList()) { try { mp.stop() } catch (_: Throwable) {}; try { mp.release() } catch (_: Throwable) {} }
        active.clear()
    }
}
