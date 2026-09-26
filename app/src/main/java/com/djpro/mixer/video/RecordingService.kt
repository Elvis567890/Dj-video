package com.djpro.mixer.video
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
class RecordingService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val cid = "dj_recording"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            nm.createNotificationChannel(NotificationChannel(cid, "DJ Recording", NotificationManager.IMPORTANCE_LOW))
        val n: Notification = NotificationCompat.Builder(this, cid)
            .setContentTitle("DJ Pro Mixer").setContentText("Recording")
            .setSmallIcon(android.R.drawable.presence_video_online).setOngoing(true).build()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0
        try { ServiceCompat.startForeground(this, 4242, n, type) }
        catch (_: Throwable) { try { startForeground(4242, n) } catch (_: Throwable) {} }
        return START_NOT_STICKY
    }
}
