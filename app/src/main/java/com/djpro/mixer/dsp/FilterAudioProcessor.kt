package com.djpro.mixer.dsp

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer

class FilterAudioProcessor : BaseAudioProcessor() {
    @Volatile var enabled: Boolean = false
    private var prev = FloatArray(2)

    override fun onConfigure(inputFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputFormat)
        }
        prev = FloatArray(inputFormat.channelCount)
        return inputFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val out = replaceOutputBuffer(inputBuffer.remaining())
        val ch = prev.size
        var i = 0
        while (inputBuffer.remaining() >= 2) {
            val s = inputBuffer.short.toFloat() / 32768f
            val c = i % ch
            val filtered = if (enabled) {
                val y = 0.15f * s + 0.85f * prev[c]
                prev[c] = y
                y
            } else s
            out.putShort((filtered * 32767f).coerceIn(-32768f, 32767f).toInt().toShort())
            i++
        }
        out.flip()
    }

    override fun onFlush() { prev.fill(0f) }
    override fun onReset() { prev = FloatArray(2) }
}
