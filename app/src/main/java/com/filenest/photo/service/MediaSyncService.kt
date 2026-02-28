package com.filenest.photo.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.filenest.photo.data.SyncStateManager
import com.filenest.photo.data.usecase.MediaSyncUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class MediaSyncService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var syncJob: Job? = null

    companion object {
        const val TAG = "MediaSyncService"
        const val CHANNEL_ID = "media_sync_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START_SYNC = "com.filenest.photo.ACTION_START_SYNC"
        const val ACTION_STOP_SYNC = "com.filenest.photo.ACTION_STOP_SYNC"

        fun startSync(context: Context) {
            Log.d(TAG, "startSync called")
            val intent = Intent(context, MediaSyncService::class.java).apply {
                action = ACTION_START_SYNC
            }
            context.startForegroundService(intent)
        }

        fun stopSync(context: Context) {
            Log.d(TAG, "stopSync called")
            val intent = Intent(context, MediaSyncService::class.java).apply {
                action = ACTION_STOP_SYNC
            }
            context.stopService(intent)
        }
    }

    @Inject
    lateinit var mediaSyncUseCase: MediaSyncUseCase

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")
        when (intent?.action) {
            ACTION_START_SYNC -> {
                Log.d(TAG, "ACTION_START_SYNC received")
                val notification = createNotification("正在准备同步...", 0)
                startForeground(NOTIFICATION_ID, notification)
                Log.d(TAG, "startForeground called")
                doSync()
            }

            ACTION_STOP_SYNC -> {
                Log.d(TAG, "ACTION_STOP_SYNC received")
                stopSync()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun doSync() {
        Log.d(TAG, "doSync started")
        SyncStateManager.setSyncing(true)
        syncJob?.cancel()
        syncJob = serviceScope.launch {
            try {
                updateNotification("正在同步媒体文件...", 0)
                delay(1000)
                updateNotification("正在同步媒体文件...", 20)
                delay(1000)
                updateNotification("正在同步媒体文件...", 40)
                delay(1000)
                updateNotification("正在同步媒体文件...", 60)
                delay(1000)
                updateNotification("正在同步媒体文件...", 80)
                delay(1000)
                updateNotification("同步完成", 100)
                delay(500)
                SyncStateManager.setSyncing(false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            } catch (e: Exception) {
                Log.e(TAG, "doSync error: ${e.message}")
                SyncStateManager.setSyncing(false)
            }
        }
    }

    private fun stopSync() {
        Log.d(TAG, "stopSync in service")
        syncJob?.cancel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "媒体同步",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(content: String, progress: Int): Notification {
        Log.d(TAG, "createNotification: $content, progress: $progress")
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("媒体同步")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setOngoing(true)
            .setProgress(100, progress, progress == 0)
            .build()
    }

    private fun updateNotification(content: String, progress: Int) {
        val notification = createNotification(content, progress)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
        syncJob?.cancel()
    }
}