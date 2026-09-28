package com.ultimate.dj.audio
import android.media.MediaExtractor
import android.media.MediaFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt
object BeatAnalyzer {
    fun analyze(uri: String): Float {
        return try {
            val extractor = MediaExtractor()
            extractor.setDataSource(uri)
            var audioTrack = -1
            var sampleRate = 44100
            var channels = 2
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                if (fmt.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                    audioTrack = i
                    sampleRate = fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    channels = fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    break
                }
            }
            if (audioTrack < 0) { extractor.release(); return 120f }
            extractor.selectTrack(audioTrack)
            val hopSize = 512
            val buf = ByteBuffer.allocate(hopSize * channels * 2).order(ByteOrder.LITTLE_ENDIAN)
            val energies = mutableListOf<Float>()
            var reads = 0
            while (energies.size < 2000 && reads < 4000) {
                buf.clear()
                val n = extractor.readSampleData(buf, 0)
                if (n < 0) break
                buf.position(0); buf.limit(n)
                var sum = 0L
                while (buf.remaining() >= 2) {
                    val s = buf.short.toInt()
                    sum += s.toLong() * s.toLong()
                }
                energies.add(sqrt(sum.toDouble() / hopSize).toFloat())
                reads++
                extractor.advance()
            }
            extractor.release()
            estimateBpm(energies, sampleRate, hopSize)
        } catch (_: Throwable) { 120f }
    }
    private fun estimateBpm(energies: List<Float>, sampleRate: Int, hopSize: Int): Float {
        if (energies.size < 100) return 120f
        val onsets = FloatArray(energies.size)
        for (i in 1 until energies.size) {
            val diff = energies[i] - energies[i - 1]
            onsets[i] = if (diff > 0) diff else 0f
        }
        val minLag = (sampleRate * 0.3f / hopSize).toInt()
        val maxLag = (sampleRate * 1.0f / hopSize).toInt()
        var bestLag = 0; var bestScore = 0f
        for (lag in minLag..maxLag) {
            var score = 0f
            for (i in 0 until onsets.size - lag) score += onsets[i] * onsets[i + lag]
            if (score > bestScore) { bestScore = score; bestLag = lag }
        }
        if (bestLag == 0) return 120f
        val secondsPerBeat = bestLag.toFloat() * hopSize / sampleRate
        return (60f / secondsPerBeat).coerceIn(60f, 200f)
    }
}
