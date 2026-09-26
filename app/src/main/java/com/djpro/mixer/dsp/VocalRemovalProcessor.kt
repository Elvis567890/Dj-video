package com.djpro.mixer.dsp
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
class VocalRemovalProcessor : BaseAudioProcessor() {
    @Volatile var enabled: Boolean = false
    override fun onConfigure(inputFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputFormat.encoding != C.ENCODING_PCM_16BIT) throw AudioProcessor.UnhandledAudioFormatException(inputFormat)
        return inputFormat
    }
    override fun queueInput(inputBuffer: ByteBuffer) {
        val out = replaceOutputBuffer(inputBuffer.remaining())
        if (!enabled) out.put(inputBuffer)
        else {
            while (inputBuffer.remaining() >= 4) {
                val l = inputBuffer.short.toInt(); val r = inputBuffer.short.toInt()
                val side = (l - r) / 2
                out.putShort((side * 2).coerceIn(-32768, 32767).toShort())
                out.putShort((-side * 2).coerceIn(-32768, 32767).toShort())
            }
            while (inputBuffer.remaining() > 0) out.put(inputBuffer.get())
        }
        out.flip()
    }
    override fun onFlush() {}
    override fun onReset() {}
}
