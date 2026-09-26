package com.djpro.mixer.dsp
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import kotlin.math.tanh
class MasterLimiterProcessor : BaseAudioProcessor() {
    @Volatile var ceiling: Float = 0.92f
    override fun onConfigure(inputFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputFormat.encoding != C.ENCODING_PCM_16BIT) throw AudioProcessor.UnhandledAudioFormatException(inputFormat)
        return inputFormat
    }
    override fun queueInput(inputBuffer: ByteBuffer) {
        val out = replaceOutputBuffer(inputBuffer.remaining())
        while (inputBuffer.remaining() >= 2) {
            val s = inputBuffer.short.toFloat() / 32768f
            val limited = tanh(s).toFloat() * ceiling
            out.putShort((limited * 32767f).toInt().toShort())
        }
        out.flip()
    }
    override fun onFlush() {}
    override fun onReset() {}
}
