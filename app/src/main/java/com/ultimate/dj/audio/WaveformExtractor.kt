package com.ultimate.dj.audio
import android.media.MediaExtractor
import android.media.MediaFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
object WaveformExtractor {
    private val cache = mutableMapOf<String, FloatArray>()
    fun extract(uri: String, bars: Int = 400): FloatArray {
        cache[uri]?.let { if (it.size == bars) return it }
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
            if (audioTrack < 0) { extractor.release(); return FloatArray(bars) { 0.3f } }
            extractor.selectTrack(audioTrack)
            val buf = ByteBuffer.allocate(4096).order(ByteOrder.LITTLE_ENDIAN)
            val peaks = mutableListOf<Float>()
            var reads = 0
            while (peaks.size < bars && reads < bars * 20) {
                buf.clear()
                val n = extractor.readSampleData(buf, 0)
                if (n < 0) break
                buf.position(0); buf.limit(n)
                var maxAbs = 0
                while (buf.remaining() >= 2) {
                    val v = abs(buf.short.toInt())
                    if (v > maxAbs) maxAbs = v
                }
                peaks.add(maxAbs / 32768f)
                reads++
                repeat(3) { extractor.advance() }
            }
            extractor.release()
            val result = FloatArray(bars) { i ->
                if (i < peaks.size) peaks[i]
                else if (peaks.isNotEmpty()) peaks[i % peaks.size]
                else 0f
            }
            cache[uri] = result
            result
        } catch (_: Throwable) { FloatArray(bars) { 0.3f } }
    }
    fun clearCache() { cache.clear() }
}
