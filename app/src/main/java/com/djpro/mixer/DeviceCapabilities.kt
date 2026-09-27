package com.djpro.mixer
import android.app.ActivityManager
import android.content.Context
object DeviceCapabilities {
    fun isLowEnd(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return am.isLowRamDevice
    }
    fun ramGb(context: Context): Int {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return (info.totalMem / (1024L * 1024L * 1024L)).toInt()
    }
}
