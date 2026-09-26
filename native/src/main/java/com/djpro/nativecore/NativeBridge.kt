package com.djpro.nativecore

object NativeBridge {
    init { System.loadLibrary("djpro_native") }
    external fun startEngine(): Boolean
    external fun stopEngine()
    external fun loadDeck(id: Int, data: FloatArray, volume: Float)
    external fun setDeckVolume(id: Int, volume: Float)
    external fun scratchDeck(id: Int, rate: Float)
}

object DemucsBridge {
    external fun isModelReady(): Boolean
    external fun separate(path: String): String
}
