package com.ultimate.dj.audio
import android.content.Context
import android.net.Uri
object SampleStore {
    private const val PREFS = "dj_sampler_v3"
    private const val PADS_PER_BANK = 8
    private const val BANKS = 4
    fun padCount() = PADS_PER_BANK * BANKS
    private fun key(bank: Int, pad: Int) = "pad_${bank}_$pad"
    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    fun getAssignedUri(context: Context, index: Int): String? {
        if (index !in 0 until padCount()) return null
        return prefs(context).getString(key(index / PADS_PER_BANK, index % PADS_PER_BANK), null)
    }
    fun assign(context: Context, index: Int, uri: Uri?) {
        if (index !in 0 until padCount()) return
        val k = key(index / PADS_PER_BANK, index % PADS_PER_BANK)
        prefs(context).edit().apply {
            if (uri == null) remove(k) else putString(k, uri.toString())
        }.apply()
    }
    fun clearAll(context: Context) { prefs(context).edit().clear().apply() }
}
