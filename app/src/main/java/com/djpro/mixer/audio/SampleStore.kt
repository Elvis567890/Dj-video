package com.djpro.mixer.audio
import android.content.Context
import android.net.Uri
object SampleStore {
    private const val PREFS = "dj_sampler_v1"
    private const val N = 8
    private const val P = "pad_uri_"
    fun getAssignedUri(context: Context, i: Int): String? {
        if (i !in 0 until N) return null
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(P + i, null)
    }
    fun assign(context: Context, i: Int, uri: Uri?) {
        if (i !in 0 until N) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
            if (uri == null) remove(P + i) else putString(P + i, uri.toString())
        }.apply()
    }
    fun clearAll(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
