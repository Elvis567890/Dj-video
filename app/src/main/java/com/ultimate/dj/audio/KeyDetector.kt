package com.ultimate.dj.audio
import android.media.MediaExtractor
import android.media.MediaFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sin
object KeyDetector {
    private val NOTE_NAMES = listOf("C","C#","D","D#","E","F","F#","G","G#","A","A#","B")
    data class Result(val key: String, val camelot: String)
    fun detect(uri: String): Result {
        return try {
            val extractor = MediaExtractor()
            extractor.setDataSource(uri)
            var audioTrack = -1; var sampleRate = 44100
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                if (fmt.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                    audioTrack = i
                    sampleRate = fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    break
                }
            }
            if (audioTrack < 0) { extractor.release(); return Result("C", "8B") }
            extractor.selectTrack(audioTrack)
            val chroma = FloatArray(12)
            val fftSize = 2048
            val buf = ByteBuffer.allocate(fftSize * 2).order(ByteOrder.LITTLE_ENDIAN)
            var frames = 0
            while (frames < 200) {
                buf.clear()
                val n = extractor.readSampleData(buf, 0)
                if (n < 0) break
                buf.position(0); buf.limit(n)
                val samples = FloatArray(fftSize)
                var i = 0
                while (buf.remaining() >= 2 && i < fftSize) {
                    samples[i] = buf.short.toFloat() / 32768f; i++
                }
                if (i < fftSize) break
                accumulateChroma(samples, sampleRate, chroma)
                frames++
                extractor.advance()
            }
            extractor.release()
            pickKey(chroma)
        } catch (_: Throwable) { Result("C", "8B") }
    }
    private fun accumulateChroma(samples: FloatArray, sr: Int, chroma: FloatArray) {
        val n = samples.size
        for (k in 1 until n / 4) {
            val freq = k.toFloat() * sr / n
            if (freq < 80f || freq > 2000f) continue
            var re = 0f; var im = 0f
            val step = 8
            var j = 0
            while (j < n) {
                val angle = 2.0 * Math.PI * k * j / n
                re += (samples[j] * cos(angle)).toFloat()
                im -= (samples[j] * sin(angle)).toFloat()
                j += step
            }
            val mag = re * re + im * im
            val midi = 69.0 + 12.0 * ln(freq / 440.0) / ln(2.0)
            val pc = ((midi.roundToInt() % 12) + 12) % 12
            chroma[pc] += mag
        }
    }
    private fun pickKey(chroma: FloatArray): Result {
        var maxIdx = 0; var maxVal = 0f
        for (i in 0 until 12) if (chroma[i] > maxVal) { maxVal = chroma[i]; maxIdx = i }
        val major = isMajor(chroma, maxIdx)
        val keyName = NOTE_NAMES[maxIdx] + if (major) "" else "m"
        val camelot = camelotFor(maxIdx, major)
        return Result(keyName, camelot)
    }
    private fun isMajor(chroma: FloatArray, root: Int): Boolean {
        val third = chroma[(root + 4) % 12]
        val minorThird = chroma[(root + 3) % 12]
        return third >= minorThird
    }
    private fun camelotFor(root: Int, major: Boolean): String {
        val majorMap = intArrayOf(8, 3, 10, 5, 12, 7, 2, 9, 4, 11, 6, 1)
        val minorMap = intArrayOf(5, 12, 7, 2, 9, 4, 11, 6, 1, 8, 3, 10)
        return if (major) "${majorMap[root]}B" else "${minorMap[root]}A"
    }
}
