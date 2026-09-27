package com.djpro.mixer.dsp
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
class KeyLockProcessor : BaseAudioProcessor() {
    @Volatile var enabled: Boolean = false
    override fun onConfigure(inputFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputFormat.encoding != C.ENCODING_PCM_16BIT) throw AudioProcessor.UnhandledAudioFormatException(inputFormat)
        return inputFormat
    }
    override fun queueInput(inputBuffer: ByteBuffer) {
        val out = replaceOutputBuffer(inputBuffer.remaining())
        out.put(inputBuffer)
        out.flip()
    }
    override fun onFlush() {}
    override fun onReset() {}
}
