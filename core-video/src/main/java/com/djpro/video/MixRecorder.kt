package com.djpro.video

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

    fun buildIntent(): Intent {
        val mgr = context.getSystemService(MediaProjectionManager::class.java)
        return mgr.createScreenCaptureIntent()
    }

    fun start(resultCode: Int, data: Intent) {
        val mgr = context.getSystemService(MediaProjectionManager::class.java)
        projection = mgr.getMediaProjection(resultCode, data)
        val metrics = DisplayMetrics()
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION") wm.defaultDisplay.getRealMetrics(metrics)
        val w = metrics.widthPixels; val h = metrics.heightPixels; val dpi = metrics.densityDpi
        outputFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "DJProMix_${System.currentTimeMillis()}.mp4"
        )
        recorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()).apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setVideoSize(w, h); setVideoFrameRate(30); setVideoEncodingBitRate(8_000_000)
            setOutputFile(outputFile!!.absolutePath)
            prepare()
        }
        virtualDisplay = projection?.createVirtualDisplay("DJProRec", w, h, dpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, recorder!!.surface, null, null)
        recorder?.start()
    }

    fun stop(): File? {
        try { recorder?.stop() } catch (_: Exception) {}
        recorder?.release(); recorder = null
        virtualDisplay?.release(); virtualDisplay = null
        projection?.stop(); projection = null
        return outputFile
    }
}
