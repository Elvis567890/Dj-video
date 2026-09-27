package com.djpro.mixer.audio

import android.content.Context
import android.net.Uri

object SampleStore {
    private const val PREFS = "dj_sampler_v1"
    private const val PAD_COUNT = 8
    private const val KEY_PREFIX = "pad_uri_"

    fun getAssignedUri(context: Context, padIndex: Int): String? {
        if (padIndex !in 0 until PAD_COUNT) return null
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PREFIX + padIndex, null)
    }

    fun assign(context: Context, padIndex: Int, uri: Uri?) {
        if (padIndex !in 0 until PAD_COUNT) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().apply {
            if (uri == null) remove(KEY_PREFIX + padIndex)
            else putString(KEY_PREFIX + padIndex, uri.toString())
        }.apply()
    }

    fun clearAll(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
