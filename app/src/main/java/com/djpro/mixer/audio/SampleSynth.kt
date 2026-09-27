package com.djpro.mixer.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

object SampleSynth {

    private const val SR = 44100
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    private fun makeKick(): ShortArray {
        val n = (SR * 0.45f).toInt(); val buf = ShortArray(n); var phase = 0.0
        for (i in 0 until n) {
            val t = i.toFloat() / SR
            val freq = 55f + 90f * exp(-t * 30f)
            phase += 2.0 * PI * freq / SR
            val env = exp(-t * 7f)
            val click = if (t < 0.004f) (1f - t / 0.004f) * 0.6f else 0f
            val s = (sin(phase) * env + click * (Random.nextFloat() * 2 - 1)).coerceIn(-1f, 1f)
            buf[i] = (s * 30000f).toInt().coerceIn(-32768, 32767).toShort()
        }
        return buf
    }

    private fun makeSnare(): ShortArray {
        val n = (SR * 0.30f).toInt(); val buf = ShortArray(n); var phase = 0.0
        for (i in 0 until n) {
            val t = i.toFloat() / SR
            phase += 2.0 * PI * 200f / SR
            val noise = (Random.nextFloat() * 2 - 1)
            val noiseEnv = exp(-t * 18f); val toneEnv = exp(-t * 14f)
            val s = (noise * noiseEnv * 0.7f + sin(phase) * toneEnv * 0.5f).coerceIn(-1f, 1f)
            buf[i] = (s * 28000f).toInt().coerceIn(-32768, 32767).toShort()
        }
        return buf
    }

    private fun makeHiHat(open: Boolean = false): ShortArray {
        val dur = if (open) 0.28f else 0.06f
        val n = (SR * dur).toInt(); val buf = ShortArray(n)
        var hp1 = 0f; var hp2 = 0f
        for (i in 0 until n) {
            val t = i.toFloat() / SR
            val noise = (Random.nextFloat() * 2 - 1)
            hp1 = hp1 + 0.90f * (noise - hp1); val hp1Band = noise - hp1
            hp2 = hp2 + 0.90f * (hp1Band - hp2); val filtered = hp1Band - hp2
            val env = exp(-t * (if (open) 12f else 45f))
            val s = (filtered * env * 0.55f).coerceIn(-1f, 1f)
            buf[i] = (s * 26000f).toInt().coerceIn(-32768, 32767).toShort()
        }
        return buf
    }

    private fun makeClap(): ShortArray {
        val n = (SR * 0.30f).toInt(); val buf = ShortArray(n)
        val starts = floatArrayOf(0f, 0.010f, 0.021f, 0.032f)
        for (i in 0 until n) {
            val t = i.toFloat() / SR; var s = 0f
            for (st in starts) if (t >= st) { val dt = t - st; s += (Random.nextFloat() * 2 - 1) * exp(-dt * 40f) }
            s = (s * 0.35f).coerceIn(-1f, 1f)
            buf[i] = (s * 27000f).toInt().coerceIn(-32768, 32767).toShort()
        }
        return buf
    }

    private fun makePerc(): ShortArray {
        val n = (SR * 0.18f).toInt(); val buf = ShortArray(n); var phase = 0.0
        for (i in 0 until n) {
            val t = i.toFloat() / SR
            phase += 2.0 * PI * 400f / SR
            val env = exp(-t * 25f)
            val s = (sin(phase) * env * 0.8f).coerceIn(-1f, 1f)
            buf[i] = (s * 26000f).toInt().coerceIn(-32768, 32767).toShort()
        }
        return buf
    }

    private fun makeRiser(): ShortArray {
        val dur = 1.0f; val n = (SR * dur).toInt(); val buf = ShortArray(n); var phase = 0.0
        for (i in 0 until n) {
            val t = i.toFloat() / SR
            val progress = t / dur
            val freq = 200f * Math.pow(10.0, progress * 1.0).toFloat()
            phase += 2.0 * PI * freq / SR
            val env = progress * 0.75f
            val s = (sin(phase) * env).coerceIn(-1f, 1f)
            buf[i] = (s * 24000f).toInt().coerceIn(-32768, 32767).toShort()
        }
        return buf
    }

    private fun makeLaser(): ShortArray {
        val dur = 0.20f; val n = (SR * dur).toInt(); val buf = ShortArray(n); var phase = 0.0
        for (i in 0 until n) {
            val t = i.toFloat() / SR
            val progress = t / dur
            val freq = 2000f * Math.pow(10.0, -progress * 1.3).toFloat()
            phase += 2.0 * PI * freq / SR
            val env = exp(-t * 6f)
            val s = (sin(phase) * env * 0.85f).coerceIn(-1f, 1f)
            buf[i] = (s * 26000f).toInt().coerceIn(-32768, 32767).toShort()
        }
        return buf
    }

    private fun makeSiren(): ShortArray {
        val dur = 0.9f; val n = (SR * dur).toInt(); val buf = ShortArray(n); var phase = 0.0
        for (i in 0 until n) {
            val t = i.toFloat() / SR
            val vibrato = 1.0 + 0.20 * sin(2.0 * PI * 6.0 * t)
            val freq = 800.0 * vibrato
            phase += 2.0 * PI * freq / SR
            val env = if (t < 0.05f) t / 0.05f else if (t > dur - 0.1f) (dur - t) / 0.1f else 1f
            val s = (sin(phase) * env * 0.7f).coerceIn(-1f, 1f)
            buf[i] = (s * 26000f).toInt().coerceIn(-32768, 32767).toShort()
        }
        return buf
    }

    private val cache = mutableMapOf<String, ShortArray>()
    private fun getSample(name: String): ShortArray = cache.getOrPut(name) {
        when (name) {
            "KICK" -> makeKick()
            "SNARE" -> makeSnare()
            "HIHAT" -> makeHiHat(false)
            "HATOPEN" -> makeHiHat(true)
            "CLAP" -> makeClap()
            "PERC" -> makePerc()
            "RISER" -> makeRiser()
            "LASER" -> makeLaser()
            "SIREN" -> makeSiren()
            else -> makeKick()
        }
    }

    fun play(name: String) {
        val samples = getSample(name)
        scope.launch {
            try {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    .setAudioFormat(AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SR)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(samples.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC).build()
                track.write(samples, 0, samples.size)
                track.play()
                kotlinx.coroutines.delay((samples.size.toLong() * 1000L / SR) + 120)
                try { track.stop() } catch (_: Throwable) {}
                try { track.release() } catch (_: Throwable) {}
            } catch (_: Throwable) {}
        }
    }
}
