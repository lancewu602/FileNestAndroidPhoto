package com.filenest.photo.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.filenest.photo.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MediaSyncService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var syncJob: Job? = null

    companion object {
        private const val CHANNEL_ID = "media_sync_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val notification = createNotification("开始同步，请勿关闭应用")
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (syncJob?.isActive == true) {
            return START_STICKY
        }
        syncJob = serviceScope.launch {
            val totalFiles = 10
            Log.d("MediaSyncService", "开始同步，共 $totalFiles 个文件")
            
            repeat(totalFiles) { index ->
                val fileName = "photo_${index + 1}.jpg"
                val progress = index + 1
                
                Log.d("MediaSyncService", "正在上传: $fileName ($progress/$totalFiles)")
                updateNotification("正在上传: $fileName ($progress/$totalFiles)")
                kotlinx.coroutines.delay(2000)
            }
            
            Log.d("MediaSyncService", "同步完成")
            updateNotification("同步完成")
            kotlinx.coroutines.delay(3000)
            stopSelf()
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "媒体同步",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "媒体文件同步通知"
        }
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("媒体同步")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val notification = createNotification(contentText)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
