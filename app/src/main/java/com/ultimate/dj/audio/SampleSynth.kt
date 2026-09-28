package com.ultimate.dj.audio
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random
object SampleSynth {
    private const val SR = 44100
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private fun toShort(v: Double): Short = (v.coerceIn(-1.0, 1.0) * 32000.0).toInt().coerceIn(-32768, 32767).toShort()
    private fun makeKick(): ShortArray {
        val n = (SR.toDouble() * 0.45).toInt(); val buf = ShortArray(n); var p = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / SR.toDouble()
            val f = 55.0 + 90.0 * exp(-t * 30.0); p += 2.0 * PI * f / SR.toDouble()
            val click = if (t < 0.004) (1.0 - t / 0.004) * 0.6 else 0.0
            buf[i] = toShort(sin(p) * exp(-t * 7.0) + click * (Random.nextDouble() * 2.0 - 1.0))
        }
        return buf
    }
    private fun makeSnare(): ShortArray {
        val n = (SR.toDouble() * 0.30).toInt(); val buf = ShortArray(n); var p = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / SR.toDouble(); p += 2.0 * PI * 200.0 / SR.toDouble()
            buf[i] = toShort((Random.nextDouble() * 2.0 - 1.0) * exp(-t * 18.0) * 0.7 + sin(p) * exp(-t * 14.0) * 0.5)
        }
        return buf
    }
    private fun makeHiHat(open: Boolean = false): ShortArray {
        val dur = if (open) 0.28 else 0.06; val n = (SR.toDouble() * dur).toInt()
        val buf = ShortArray(n); var h1 = 0.0; var h2 = 0.0
        val dec = if (open) 12.0 else 45.0
        for (i in 0 until n) {
            val t = i.toDouble() / SR.toDouble()
            val noise = Random.nextDouble() * 2.0 - 1.0
            h1 += 0.90 * (noise - h1); val band = noise - h1
            h2 += 0.90 * (band - h2)
            buf[i] = toShort((band - h2) * exp(-t * dec) * 0.55)
        }
        return buf
    }
    private fun makeClap(): ShortArray {
        val n = (SR.toDouble() * 0.30).toInt(); val buf = ShortArray(n)
        val starts = doubleArrayOf(0.0, 0.010, 0.021, 0.032)
        for (i in 0 until n) {
            val t = i.toDouble() / SR.toDouble(); var s = 0.0
            for (st in starts) if (t >= st) { val dt = t - st; s += (Random.nextDouble() * 2.0 - 1.0) * exp(-dt * 40.0) }
            buf[i] = toShort(s * 0.35)
        }
        return buf
    }
    private fun makePerc(): ShortArray {
        val n = (SR.toDouble() * 0.18).toInt(); val buf = ShortArray(n); var p = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / SR.toDouble(); p += 2.0 * PI * 400.0 / SR.toDouble()
            buf[i] = toShort(sin(p) * exp(-t * 25.0) * 0.8)
        }
        return buf
    }
    private fun makeRiser(): ShortArray {
        val dur = 1.0; val n = (SR.toDouble() * dur).toInt(); val buf = ShortArray(n); var p = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / SR.toDouble(); val pr = t / dur
            p += 2.0 * PI * 200.0 * 10.0.pow(pr) / SR.toDouble()
            buf[i] = toShort(sin(p) * pr * 0.75)
        }
        return buf
    }
    private fun makeLaser(): ShortArray {
        val dur = 0.20; val n = (SR.toDouble() * dur).toInt(); val buf = ShortArray(n); var p = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / SR.toDouble(); val pr = t / dur
            p += 2.0 * PI * 2000.0 * 10.0.pow(-pr * 1.3) / SR.toDouble()
            buf[i] = toShort(sin(p) * exp(-t * 6.0) * 0.85)
        }
        return buf
    }
    private fun makeSiren(): ShortArray {
        val dur = 0.9; val n = (SR.toDouble() * dur).toInt(); val buf = ShortArray(n); var p = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / SR.toDouble()
            val vib = 1.0 + 0.20 * sin(2.0 * PI * 6.0 * t)
            p += 2.0 * PI * 800.0 * vib / SR.toDouble()
            val env = when { t < 0.05 -> t / 0.05; t > dur - 0.1 -> (dur - t) / 0.1; else -> 1.0 }
            buf[i] = toShort(sin(p) * env * 0.7)
        }
        return buf
    }
    private val cache = mutableMapOf<String, ShortArray>()
    private fun getSample(n: String): ShortArray = cache.getOrPut(n) {
        when (n) {
            "KICK" -> makeKick(); "SNARE" -> makeSnare()
            "HIHAT" -> makeHiHat(); "HATOPEN" -> makeHiHat(true)
            "CLAP" -> makeClap(); "PERC" -> makePerc()
            "RISER" -> makeRiser(); "LASER" -> makeLaser()
            "SIREN" -> makeSiren(); else -> makeKick()
        }
    }
    fun play(name: String) {
        val s = getSample(name)
        scope.launch {
            try {
                val tr = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    .setAudioFormat(AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SR)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(s.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC).build()
                tr.write(s, 0, s.size); tr.play()
                kotlinx.coroutines.delay((s.size.toLong() * 1000L / SR) + 120)
                try { tr.stop() } catch (_: Throwable) {}
                try { tr.release() } catch (_: Throwable) {}
            } catch (_: Throwable) {}
        }
    }
}
