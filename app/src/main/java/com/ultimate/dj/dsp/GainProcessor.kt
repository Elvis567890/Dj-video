package com.ultimate.dj.dsp
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
class GainProcessor : BaseAudioProcessor() {
    @Volatile var gainDb: Float = 0f
    override fun onConfigure(inputFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputFormat.encoding != C.ENCODING_PCM_16BIT) throw AudioProcessor.UnhandledAudioFormatException(inputFormat)
        return inputFormat
    }
    override fun queueInput(inputBuffer: ByteBuffer) {
        val out = replaceOutputBuffer(inputBuffer.remaining())
        val g = if (gainDb == 0f) 1f else Math.pow(10.0, gainDb / 20.0).toFloat()
        while (inputBuffer.remaining() >= 2) {
            val s = inputBuffer.short.toFloat()
            out.putShort((s * g).coerceIn(-32768f, 32767f).toInt().toShort())
        }
        out.flip()
    }
    override fun onFlush() {}
    override fun onReset() {}
}
