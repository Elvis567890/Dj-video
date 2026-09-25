package com.djpro.dsp

class EffectChain {
    var echoMix: Float = 0f
    var filterCutoff: Float = 1f
    var flangerMix: Float = 0f
    var reverbMix: Float = 0f
    var delayTimeMs: Int = 280

    fun apply(samples: FloatArray): FloatArray {
        var s = samples
        if (filterCutoff < 1f) s = lowPass(s, filterCutoff)
        if (echoMix > 0f) s = echo(s, echoMix)
        if (flangerMix > 0f) s = flanger(s, flangerMix)
        if (reverbMix > 0f) s = reverb(s, reverbMix)
        return s
    }

    private fun lowPass(input: FloatArray, cutoff: Float): FloatArray {
        val out = FloatArray(input.size)
        val alpha = cutoff.coerceIn(0.01f, 1f)
        var prev = 0f
        for (i in input.indices) { prev = alpha * input[i] + (1 - alpha) * prev; out[i] = prev }
        return out
    }
    private fun echo(input: FloatArray, mix: Float): FloatArray {
        val d = (48000 * delayTimeMs / 1000f).toInt()
        val out = input.copyOf()
        for (i in d until input.size) out[i] += input[i - d] * mix
        return out
    }
    private fun flanger(input: FloatArray, mix: Float): FloatArray {
        val out = FloatArray(input.size)
        val p = 48000 / 2
        for (i in input.indices) {
            val mod = (Math.sin(2 * Math.PI * i / p) * 5).toInt()
            val idx = (i - mod).coerceAtLeast(0)
            out[i] = input[i] * (1 - mix) + input[idx] * mix
        }
        return out
    }
    private fun reverb(input: FloatArray, mix: Float): FloatArray {
        val combs = intArrayOf(1557, 1617, 1491)
        val out = input.copyOf()
        for (c in combs) for (i in c until input.size) out[i] += out[i - c] * mix * 0.3f
        return out
    }
}
