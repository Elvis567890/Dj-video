package com.ultimate.dj.video
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.Surface
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
class MixRecorder(private val context: Context) {
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var videoEncoder: MediaCodec? = null
    private var audioEncoder: MediaCodec? = null
    private var audioRecord: AudioRecord? = null
    private var muxer: MediaMuxer? = null
    private var inputSurface: Surface? = null
    private var videoTrackIndex = -1
    private var audioTrackIndex = -1
    private var muxerStarted = false
    private var outputFile: File? = null
    private val recording = AtomicBoolean(false)
    var isRecording: Boolean = false; private set
    var lastSavedUri: Uri? = null; private set
    var lastSavedName: String = ""; private set
    private val videoWidth = 1280
    private val videoHeight = 720
    private val videoBitrate = 6_000_000
    private val frameRate = 30
    private val audioSampleRate = 44100
    private val audioBitrate = 128_000
    fun buildIntent(): Intent? = try {
        context.getSystemService(MediaProjectionManager::class.java).createScreenCaptureIntent()
    } catch (_: Throwable) { null }
    fun start(resultCode: Int, data: Intent): Boolean {
        if (isRecording) return false
        return try {
            try { context.startForegroundService(Intent(context, RecordingService::class.java)) } catch (_: Throwable) {}
            val mgr = context.getSystemService(MediaProjectionManager::class.java)
            projection = mgr.getMediaProjection(resultCode, data)
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
            outputFile = File(dir, "Ultimate_${System.currentTimeMillis()}.mp4")
            muxer = MediaMuxer(outputFile!!.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val videoFormat = MediaFormat.createVideoFormat("video/avc", videoWidth, videoHeight).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, videoBitrate)
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }
            videoEncoder = MediaCodec.createEncoderByType("video/avc").apply {
                configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                inputSurface = createInputSurface()
            }
            videoEncoder!!.start()
            val audioFormat = MediaFormat.createAudioFormat("audio/mp4a-latm", audioSampleRate, 2).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, audioBitrate)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
            }
            audioEncoder = MediaCodec.createEncoderByType("audio/mp4a-latm").apply {
                configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            }
            audioEncoder!!.start()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val captureConfig = AudioPlaybackCaptureConfiguration.Builder(projection!!)
                    .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                    .addMatchingUsage(AudioAttributes.USAGE_GAME)
                    .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                    .build()
                val audioFmt = AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(audioSampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                    .build()
                audioRecord = AudioRecord.Builder()
                    .setAudioFormat(audioFmt)
                    .setBufferSizeInBytes(16384)
                    .setAudioPlaybackCaptureConfig(captureConfig)
                    .build()
            } else {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    audioSampleRate,
                    AudioFormat.CHANNEL_IN_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    16384
                )
            }
            audioRecord!!.startRecording()
            virtualDisplay = projection!!.createVirtualDisplay(
                "UltimateRec",
                videoWidth, videoHeight,
                context.resources.displayMetrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                inputSurface, null, null
            )
            recording.set(true)
            isRecording = true
            thread(name = "video-encoder") { drainVideo() }
            thread(name = "audio-encoder") { drainAudio() }
            true
        } catch (_: Throwable) {
            cleanup()
            false
        }
    }
    private fun drainVideo() {
        try {
            val bufferInfo = MediaCodec.BufferInfo()
            while (recording.get()) {
                val encoder = videoEncoder ?: break
                val index = encoder.dequeueOutputBuffer(bufferInfo, 10_000)
                if (index == MediaCodec.INFO_TRY_AGAIN_LATER) continue
                if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (!muxerStarted) {
                        videoTrackIndex = muxer!!.addTrack(encoder.outputFormat)
                        if (audioTrackIndex >= 0) {
                            muxer!!.start()
                            muxerStarted = true
                        }
                    }
                    continue
                }
                if (index >= 0) {
                    val buf = encoder.getOutputBuffer(index) ?: continue
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        bufferInfo.size = 0
                    }
                    if (bufferInfo.size > 0 && muxerStarted) {
                        buf.position(bufferInfo.offset)
                        buf.limit(bufferInfo.offset + bufferInfo.size)
                        muxer!!.writeSampleData(videoTrackIndex, buf, bufferInfo)
                    }
                    encoder.releaseOutputBuffer(index, false)
                }
            }
        } catch (_: Throwable) {}
    }
    private fun drainAudio() {
        try {
            val bufferInfo = MediaCodec.BufferInfo()
            val pcmBuf = ByteArray(16384)
            var ptsUs = 0L
            val frameSizeUs = 1000000L * 1024 / audioSampleRate
            while (recording.get()) {
                val encoder = audioEncoder ?: break
                val record = audioRecord ?: break
                val read = record.read(pcmBuf, 0, pcmBuf.size)
                if (read > 0) {
                    val inIndex = encoder.dequeueInputBuffer(10_000)
                    if (inIndex >= 0) {
                        val inBuf = encoder.getInputBuffer(inIndex)
                        if (inBuf != null) {
                            inBuf.clear()
                            inBuf.put(pcmBuf, 0, read)
                            encoder.queueInputBuffer(inIndex, 0, read, ptsUs, 0)
                            ptsUs += frameSizeUs
                        }
                    }
                }
                var outIndex = encoder.dequeueOutputBuffer(bufferInfo, 0)
                while (outIndex >= 0) {
                    val buf = encoder.getOutputBuffer(outIndex) ?: break
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        bufferInfo.size = 0
                    }
                    if (bufferInfo.size > 0 && muxerStarted) {
                        buf.position(bufferInfo.offset)
                        buf.limit(bufferInfo.offset + bufferInfo.size)
                        muxer!!.writeSampleData(audioTrackIndex, buf, bufferInfo)
                    }
                    encoder.releaseOutputBuffer(outIndex, false)
                    outIndex = encoder.dequeueOutputBuffer(bufferInfo, 0)
                }
                if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED && !muxerStarted) {
                    audioTrackIndex = muxer!!.addTrack(encoder.outputFormat)
                    if (videoTrackIndex >= 0) {
                        muxer!!.start()
                        muxerStarted = true
                    }
                }
            }
        } catch (_: Throwable) {}
    }
    fun stop(): Uri? {
        recording.set(false)
        isRecording = false
        try { Thread.sleep(300) } catch (_: Throwable) {}
        cleanup()
        val raw = outputFile ?: return null
        return saveToGallery(raw)
    }
    private fun cleanup() {
        try { virtualDisplay?.release() } catch (_: Throwable) {}
        virtualDisplay = null
        try { inputSurface?.release() } catch (_: Throwable) {}
        inputSurface = null
        try { videoEncoder?.stop() } catch (_: Throwable) {}
        try { videoEncoder?.release() } catch (_: Throwable) {}
        videoEncoder = null
        try { audioEncoder?.stop() } catch (_: Throwable) {}
        try { audioEncoder?.release() } catch (_: Throwable) {}
        audioEncoder = null
        try { audioRecord?.stop() } catch (_: Throwable) {}
        try { audioRecord?.release() } catch (_: Throwable) {}
        audioRecord = null
        try { if (muxerStarted) muxer?.stop() } catch (_: Throwable) {}
        try { muxer?.release() } catch (_: Throwable) {}
        muxer = null
        try { projection?.stop() } catch (_: Throwable) {}
        projection = null
        try { context.stopService(Intent(context, RecordingService::class.java)) } catch (_: Throwable) {}
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
