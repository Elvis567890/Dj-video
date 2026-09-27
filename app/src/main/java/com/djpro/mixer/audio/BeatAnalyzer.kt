package com.djpro.mixer.audio
import android.media.MediaExtractor
import android.media.MediaFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
object BeatAnalyzer {
    fun analyze(uri: String): Float {
        return try {
            val extractor = MediaExtractor()
            extractor.setDataSource(uri)
            var audioTrack = -1
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                if (fmt.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                    audioTrack = i; break
                }
            }
            if (audioTrack < 0) { extractor.release(); return 120f }
            extractor.selectTrack(audioTrack)
            val buf = ByteBuffer.allocate(2048).order(ByteOrder.LITTLE_ENDIAN)
            val energies = mutableListOf<Float>()
            var reads = 0
            while (energies.size < 400 && reads < 800) {
                buf.clear()
                val n = extractor.readSampleData(buf, 0)
                if (n < 0) break
                buf.position(0); buf.limit(n)
                var sum = 0L
                while (buf.remaining() >= 2) sum += abs(buf.short.toInt()).toLong()
                energies.add(sum.toFloat())
                reads++
                extractor.advance()
            }
            extractor.release()
            estimateBpm(energies)
        } catch (_: Throwable) { 120f }
    }
    private fun estimateBpm(energies: List<Float>): Float {
        if (energies.size < 20) return 120f
        val mean = energies.average().toFloat()
        val c = energies.map { it - mean }
        var bestLag = 0; var bestScore = 0f
        for (lag in 5..60) {
            var score = 0f
            for (i in 0 until c.size - lag) score += c[i] * c[i + lag]
            if (score > bestScore) { bestScore = score; bestLag = lag }
        }
        if (bestLag == 0) return 120f
        return ((60f * 200f) / bestLag).coerceIn(60f, 200f)
    }
}
