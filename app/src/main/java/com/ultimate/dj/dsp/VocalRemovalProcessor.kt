package com.ultimate.dj.dsp
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
class VocalRemovalProcessor : BaseAudioProcessor() {
    @Volatile var enabled: Boolean = false
    @Volatile var bassRemoveEnabled: Boolean = false
    private var hpPrevIn = FloatArray(2)
    private var hpPrevOut = FloatArray(2)
    private var hpCoeff = 0f
    private var channels = 2
    override fun onConfigure(inputFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputFormat.encoding != C.ENCODING_PCM_16BIT) throw AudioProcessor.UnhandledAudioFormatException(inputFormat)
        channels = inputFormat.channelCount
        hpPrevIn = FloatArray(channels)
        hpPrevOut = FloatArray(channels)
        val dt = 1f / inputFormat.sampleRate
        val rcHp = 1f / (2f * Math.PI.toFloat() * 350f)
        hpCoeff = rcHp / (rcHp + dt)
        return inputFormat
    }
    override fun queueInput(inputBuffer: ByteBuffer) {
        val out = replaceOutputBuffer(inputBuffer.remaining())
        if (!enabled && !bassRemoveEnabled) {
            out.put(inputBuffer)
        } else {
            var i = 0
            while (inputBuffer.remaining() >= 2) {
                val raw = inputBuffer.short.toInt()
                val s = raw.toFloat() / 32768f
                val c = i % channels
                var sample = s
                if (bassRemoveEnabled) {
                    hpPrevOut[c] = hpCoeff * (hpPrevOut[c] + sample - hpPrevIn[c])
                    hpPrevIn[c] = sample
                    sample = hpPrevOut[c]
                }
                out.putShort((sample * 32767f).coerceIn(-32768f, 32767f).toInt().toShort())
                i++
            }
        }
        out.flip()
    }
    override fun onFlush() { hpPrevIn.fill(0f); hpPrevOut.fill(0f) }
    override fun onReset() { hpPrevIn = FloatArray(2); hpPrevOut = FloatArray(2) }
}
