package com.filenest.photo.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.filenest.photo.R
import com.filenest.photo.data.AppPrefKeys
import com.filenest.photo.data.SyncStateManager
import com.filenest.photo.data.usecase.MediaSyncFetchUseCase
import com.filenest.photo.data.usecase.MediaSyncUploadUseCase
import com.filenest.photo.data.usecase.UploadResult
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MediaSyncService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var syncJob: Job? = null

    companion object {
        private const val TAG = "MediaSyncService"
        private const val CHANNEL_ID = "media_sync_channel"
        private const val NOTIFICATION_ID = 1001
    }

    @Inject
    lateinit var mediaSyncFetchUseCase: MediaSyncFetchUseCase

    @Inject
    lateinit var mediaSyncUploadUseCase: MediaSyncUploadUseCase

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
            try {
                SyncStateManager.setSyncing(true)

                val medias = mediaSyncFetchUseCase.fetchMedias()
                val total = medias.size
                Log.d(TAG, "开始同步: $total 个文件")

                var syncedCount = 0
                var failed = false

                for (item in medias) {
                    val result = mediaSyncUploadUseCase.uploadMedia(item)
                    if (result is UploadResult.Failure) {
                        Log.w(TAG, "上传失败: ${result.message}, 已同步 $syncedCount 个文件")
                        updateNotification("同步失败: ${result.message}, 已同步 $syncedCount/$total 个文件")
                        failed = true
                        break
                    }
                    syncedCount++
                    val progress = syncedCount * 100 / total
                    updateNotification("已上传 ($syncedCount/$total)", progress)
                    SyncStateManager.setSyncProgressInfo(syncedCount, total, item.name)
                }

                if (!failed) {
                    Log.d(TAG, "同步完成")
                    updateNotification("同步完成")
                }
            } catch (e: Exception) {
                Log.e(TAG, "同步失败", e)
                updateNotification("同步失败: ${e.message}")
            } finally {
                AppPrefKeys.setLatestSyncTime(applicationContext, System.currentTimeMillis())
                SyncStateManager.setSyncing(false)
                stopSelf()
            }
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

    private fun createNotification(contentText: String, progress: Int? = null): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("媒体同步")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)

        if (progress != null) {
            builder.setProgress(100, progress, false)
        }

        return builder.build()
    }

    private fun updateNotification(contentText: String, progress: Int? = null) {
        val notification = createNotification(contentText, progress)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}