package com.djpro.mixer.video

import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.util.DisplayMetrics
import android.view.WindowManager
import java.io.File

class MixRecorder(private val context: Context) {
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun buildIntent(): Intent? = try {
        context.getSystemService(MediaProjectionManager::class.java).createScreenCaptureIntent()
    } catch (_: Throwable) { null }

    fun start(resultCode: Int, data: Intent): Boolean {
        return try {
            val mgr = context.getSystemService(MediaProjectionManager::class.java)
            projection = mgr.getMediaProjection(resultCode, data)
            val metrics = DisplayMetrics()
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            @Suppress("DEPRECATION") wm.defaultDisplay.getRealMetrics(metrics)
            val w = metrics.widthPixels; val h = metrics.heightPixels; val dpi = metrics.densityDpi
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
            outputFile = File(dir, "DJProMix_${System.currentTimeMillis()}.mp4")
            val rec: MediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context)
            else @Suppress("DEPRECATION") MediaRecorder()
            rec.apply {
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setVideoSize(w, h); setVideoFrameRate(30); setVideoEncodingBitRate(8_000_000)
                setOutputFile(outputFile!!.absolutePath); prepare()
            }
            recorder = rec
            virtualDisplay = projection?.createVirtualDisplay(
                "DJProRec", w, h, dpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                rec.surface, null, null
            )
            rec.start(); true
        } catch (_: Throwable) { false }
    }

    fun stop(): File? {
        try { recorder?.stop() } catch (_: Throwable) {}
        try { recorder?.release() } catch (_: Throwable) {}
        recorder = null
        try { virtualDisplay?.release() } catch (_: Throwable) {}
        virtualDisplay = null
        try { projection?.stop() } catch (_: Throwable) {}
        projection = null
        return outputFile
    }
}
