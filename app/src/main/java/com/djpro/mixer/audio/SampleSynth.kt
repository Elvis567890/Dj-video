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
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

object SampleSynth {

    private const val SR = 44100
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    private fun toShort(v: Double): Short {
        val clamped = v.coerceIn(-1.0, 1.0)
        return (clamped * 32000.0).toInt().coerceIn(-32768, 32767).toShort()
    }

    private fun makeKick(): ShortArray {
        val n = (SR.toDouble() * 0.45).toInt()
        val buf = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / SR.toDouble()
            val freq = 55.0 + 90.0 * exp(-t * 30.0)
            phase += 2.0 * PI * freq / SR.toDouble()
            val env = exp(-t * 7.0)
            val click = if (t < 0.004) (1.0 - t / 0.004) * 0.6 else 0.0
            val s = sin(phase) * env + click * (Random.nextDouble() * 2.0 - 1.0)
            buf[i] = toShort(s)
        }
        return buf
    }

    private fun makeSnare(): ShortArray {
        val n = (SR.toDouble() * 0.30).toInt()
        val buf = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / SR.toDouble()
            phase += 2.0 * PI * 200.0 / SR.toDouble()
            val noise = Random.nextDouble() * 2.0 - 1.0
            val noiseEnv = exp(-t * 18.0)
            val toneEnv = exp(-t * 14.0)
            val s = noise * noiseEnv * 0.7 + sin(phase) * toneEnv * 0.5
            buf[i] = toShort(s)
        }
        return buf
    }

    private fun makeHiHat(open: Boolean = false): ShortArray {
        val dur = if (open) 0.28 else 0.06
        val n = (SR.toDouble() * dur).toInt()
        val buf = ShortArray(n)
        var hp1 = 0.0
        var hp2 = 0.0
        val decay = if (open) 12.0 else 45.0
        for (i in 0 until n) {
            val t = i.toDouble() / SR.toDouble()
            val noise = Random.nextDouble() * 2.0 - 1.0
            hp1 += 0.90 * (noise - hp1)
            val hp1Band = noise - hp1
            hp2 += 0.90 * (hp1Band - hp2)
            val filtered = hp1Band - hp2
            val env = exp(-t * decay)
            val s = filtered * env * 0.55
            buf[i] = toShort(s)
        }
        return buf
    }

    private fun makeClap(): ShortArray {
        val n = (SR.toDouble() * 0.30).toInt()
        val buf = ShortArray(n)
        val starts = doubleArrayOf(0.0, 0.010, 0.021, 0.032)
        for (i in 0 until n) {
            val t = i.toDouble() / SR.toDouble()
            var s = 0.0
            for (st in starts) {
                if (t >= st) {
                    val dt = t - st
                    s += (Random.nextDouble() * 2.0 - 1.0) * exp(-dt * 40.0)
                }
            }
            s *= 0.35
            buf[i] = toShort(s)
        }
        return buf
    }

    private fun makePerc(): ShortArray {
        val n = (SR.toDouble() * 0.18).toInt()
        val buf = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / SR.toDouble()
            phase += 2.0 * PI * 400.0 / SR.toDouble()
            val env = exp(-t * 25.0)
            val s = sin(phase) * env * 0.8
            buf[i] = toShort(s)
        }
        return buf
    }

    private fun makeRiser(): ShortArray {
        val dur = 1.0
        val n = (SR.toDouble() * dur).toInt()
        val buf = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / SR.toDouble()
            val progress = t / dur
            val freq = 200.0 * 10.0.pow(progress * 1.0)
            phase += 2.0 * PI * freq / SR.toDouble()
            val env = progress * 0.75
            val s = sin(phase) * env
            buf[i] = toShort(s)
        }
        return buf
    }

    private fun makeLaser(): ShortArray {
        val dur = 0.20
        val n = (SR.toDouble() * dur).toInt()
        val buf = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / SR.toDouble()
            val progress = t / dur
            val freq = 2000.0 * 10.0.pow(-progress * 1.3)
            phase += 2.0 * PI * freq / SR.toDouble()
            val env = exp(-t * 6.0)
            val s = sin(phase) * env * 0.85
            buf[i] = toShort(s)
        }
        return buf
    }

    private fun makeSiren(): ShortArray {
        val dur = 0.9
        val n = (SR.toDouble() * dur).toInt()
        val buf = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / SR.toDouble()
            val vibrato = 1.0 + 0.20 * sin(2.0 * PI * 6.0 * t)
            val freq = 800.0 * vibrato
            phase += 2.0 * PI * freq / SR.toDouble()
            val env = when {
                t < 0.05 -> t / 0.05
                t > dur - 0.1 -> (dur - t) / 0.1
                else -> 1.0
            }
            val s = sin(phase) * env * 0.7
            buf[i] = toShort(s)
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
