package com.ultimate.dj.dsp
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
class KeyLockProcessor : BaseAudioProcessor() {
    @Volatile var enabled: Boolean = false
    @Volatile var speed: Float = 1f
    private var channelCount = 2
    private var lastSample = ShortArray(2)
    override fun onConfigure(inputFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputFormat.encoding != C.ENCODING_PCM_16BIT) throw AudioProcessor.UnhandledAudioFormatException(inputFormat)
        channelCount = inputFormat.channelCount
        lastSample = ShortArray(channelCount)
        return inputFormat
    }
    override fun queueInput(inputBuffer: ByteBuffer) {
        val out = replaceOutputBuffer(inputBuffer.remaining())
        if (!enabled || speed == 1f) {
            out.put(inputBuffer)
        } else {
            var i = 0
            while (inputBuffer.remaining() >= 2) {
                val s = inputBuffer.short
                val c = i % channelCount
                val mixed = ((lastSample[c].toInt() + s.toInt()) / 2).toShort()
                lastSample[c] = s
                out.putShort(mixed)
                i++
            }
        }
        out.flip()
    }
    override fun onFlush() { lastSample.fill(0) }
    override fun onReset() { lastSample = ShortArray(2) }
}
