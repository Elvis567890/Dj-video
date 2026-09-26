package com.djpro.mixer.dsp

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer

class EchoAudioProcessor : BaseAudioProcessor() {
    @Volatile var enabled: Boolean = false
    @Volatile var mix: Float = 0.35f
    private var delayBuffer = ShortArray(0)
    private var writePos = 0

    override fun onConfigure(inputFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputFormat)
        }
        val samples = (inputFormat.sampleRate * 0.28f).toInt().coerceAtLeast(1)
        delayBuffer = ShortArray(samples * inputFormat.channelCount)
        writePos = 0
        return inputFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val out = replaceOutputBuffer(inputBuffer.remaining())
        if (!enabled || delayBuffer.isEmpty()) {
            out.put(inputBuffer)
        } else {
            while (inputBuffer.remaining() >= 2) {
                val s = inputBuffer.short
                val d = delayBuffer[writePos]
                val mixed = (s + (d * mix).toInt()).coerceIn(-32768, 32767).toShort()
                delayBuffer[writePos] = s
                out.putShort(mixed)
                writePos = (writePos + 1) % delayBuffer.size
            }
        }
        out.flip()
    }

    override fun onFlush() { delayBuffer.fill(0); writePos = 0 }
    override fun onReset() { delayBuffer = ShortArray(0) }
}
