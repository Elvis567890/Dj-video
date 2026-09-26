package com.djpro.mixer.dsp
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import kotlin.math.pow
class EqProcessor : BaseAudioProcessor() {
    @Volatile var lowGainDb = 0f
    @Volatile var midGainDb = 0f
    @Volatile var highGainDb = 0f
    private var channels = 2
    private var lowPrev = FloatArray(2)
    private var hpPrevIn = FloatArray(2)
    private var hpPrevOut = FloatArray(2)
    private var lpCoeff = 0f
    private var hpCoeff = 0f
    override fun onConfigure(inputFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputFormat.encoding != C.ENCODING_PCM_16BIT) throw AudioProcessor.UnhandledAudioFormatException(inputFormat)
        channels = inputFormat.channelCount
        lowPrev = FloatArray(channels); hpPrevIn = FloatArray(channels); hpPrevOut = FloatArray(channels)
        val dt = 1f / inputFormat.sampleRate
        val rcLp = 1f / (2f * Math.PI.toFloat() * 200f); lpCoeff = dt / (rcLp + dt)
        val rcHp = 1f / (2f * Math.PI.toFloat() * 4000f); hpCoeff = rcHp / (rcHp + dt)
        return inputFormat
    }
    private fun dbToLin(db: Float) = if (db == 0f) 1f else 10f.pow(db / 20f)
    override fun queueInput(inputBuffer: ByteBuffer) {
        val out = replaceOutputBuffer(inputBuffer.remaining())
        val ch = channels; var i = 0
        val lg = dbToLin(lowGainDb); val mg = dbToLin(midGainDb); val hg = dbToLin(highGainDb)
        while (inputBuffer.remaining() >= 2) {
            val s = inputBuffer.short.toFloat() / 32768f; val c = i % ch
            lowPrev[c] += lpCoeff * (s - lowPrev[c]); val low = lowPrev[c]
            hpPrevOut[c] = hpCoeff * (hpPrevOut[c] + s - hpPrevIn[c]); hpPrevIn[c] = s; val high = hpPrevOut[c]
            val mid = s - low - high
            val y = (low * lg + mid * mg + high * hg).coerceIn(-1f, 1f)
            out.putShort((y * 32767f).toInt().toShort()); i++
        }
        out.flip()
    }
    override fun onFlush() { lowPrev.fill(0f); hpPrevIn.fill(0f); hpPrevOut.fill(0f) }
    override fun onReset() { lowPrev = FloatArray(2); hpPrevIn = FloatArray(2); hpPrevOut = FloatArray(2) }
}
