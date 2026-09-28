package com.ultimate.dj.video
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.WindowManager
import java.io.File
class MixRecorder(private val context: Context) {
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    var isRecording: Boolean = false; private set
    var lastSavedUri: Uri? = null; private set
    var lastSavedName: String = ""; private set
    fun buildIntent(): Intent? = try {
        context.getSystemService(MediaProjectionManager::class.java).createScreenCaptureIntent()
    } catch (_: Throwable) { null }
    fun start(resultCode: Int, data: Intent): Boolean {
        if (isRecording) return false
        return try {
            try { context.startForegroundService(Intent(context, RecordingService::class.java)) } catch (_: Throwable) {}
            val mgr = context.getSystemService(MediaProjectionManager::class.java)
            projection = mgr.getMediaProjection(resultCode, data)
            val metrics = DisplayMetrics()
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            @Suppress("DEPRECATION") wm.defaultDisplay.getRealMetrics(metrics)
            val w = metrics.widthPixels; val h = metrics.heightPixels; val dpi = metrics.densityDpi
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
            outputFile = File(dir, "Ultimate_${System.currentTimeMillis()}.mp4")
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
                "UltimateRec", w, h, dpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                rec.surface, null, null)
            rec.start(); isRecording = true; true
        } catch (_: Throwable) { false }
    }
    fun stop(): Uri? {
        try { recorder?.stop() } catch (_: Throwable) {}
        try { recorder?.release() } catch (_: Throwable) {}
        recorder = null; isRecording = false
        try { virtualDisplay?.release() } catch (_: Throwable) {}
        virtualDisplay = null
        try { projection?.stop() } catch (_: Throwable) {}
        projection = null
        try { context.stopService(Intent(context, RecordingService::class.java)) } catch (_: Throwable) {}
        val raw = outputFile ?: return null
        return saveToGallery(raw)
    }
    private fun saveToGallery(raw: File): Uri? {
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, raw.name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/ULTIMATE")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }
            val uri = context.contentResolver.insert(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return Uri.fromFile(raw)
            context.contentResolver.openOutputStream(uri)?.use { out ->
                raw.inputStream().use { it.copyTo(out) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
            }
            raw.delete()
            lastSavedUri = uri
            lastSavedName = raw.name
            uri
        } catch (_: Throwable) { null }
    }
}
