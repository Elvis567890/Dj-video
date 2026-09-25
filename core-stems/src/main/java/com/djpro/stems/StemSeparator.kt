package com.djpro.stems

import com.djpro.native.DemucsBridge
import java.io.File

class StemSeparator {
    data class Stems(val vocals: File, val drums: File, val bass: File, val other: File)

    fun isReady(): Boolean = DemucsBridge.isModelReady()

    fun quickVocalRemoval(input: FloatArray): FloatArray {
        val out = FloatArray(input.size)
        var i = 0
        while (i + 1 < input.size) {
            val l = input[i]; val r = input[i + 1]
            val side = (l - r) * 0.5f
            out[i] = side * 2f
            out[i + 1] = -side * 2f
            i += 2
        }
        return out
    }
}
