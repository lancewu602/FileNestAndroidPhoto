package com.filenest.photo.viewmodel

import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filenest.photo.data.AppPrefKeys
import com.filenest.photo.data.api.RetrofitClient
import com.filenest.photo.data.api.isRetOk
import com.filenest.photo.data.SyncStateManager
import com.filenest.photo.service.MediaSyncService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class SyncProgressInfo(
    val total: Int = 0,
    val completed: Int = 0,
    val fileName: String = "",
    val step: String = "",
)

@HiltViewModel
class SyncViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val retrofitClient: RetrofitClient
) : ViewModel() {

    private val _lastSyncTime = MutableStateFlow("")
    val lastSyncTime: StateFlow<String> = _lastSyncTime.asStateFlow()

    private val _serverMediaCount = MutableStateFlow(0)
    val serverMediaCount: StateFlow<Int> = _serverMediaCount.asStateFlow()

    private val _pendingSyncCount = MutableStateFlow(0)
    val pendingSyncCount: StateFlow<Int> = _pendingSyncCount.asStateFlow()

    val isSyncing: StateFlow<Boolean> = SyncStateManager.isSyncing
    val syncProgressInfo: StateFlow<SyncProgressInfo> = SyncStateManager.syncProgressInfo
    val syncProgressFile: StateFlow<Float> = SyncStateManager.syncProgressFile
    val syncProgressStep: StateFlow<String> = SyncStateManager.syncProgressStep

    fun loadSyncInfo() {
        viewModelScope.launch {
            AppPrefKeys.getLatestSyncTime(context).collect { timestamp ->
                _lastSyncTime.value = if (timestamp > 0) {
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    sdf.format(Date(timestamp))
                } else {
                    "从未同步"
                }
            }
        }
        viewModelScope.launch {
            try {
                val ret = retrofitClient.getApiService().countMedia()
                _serverMediaCount.value = if (isRetOk(ret)) ret.data?.toInt() ?: 0 else 0
            } catch (e: Exception) {
                _serverMediaCount.value = 0
            }
        }
        viewModelScope.launch {
            _pendingSyncCount.value = countPendingSyncMedia()
        }
    }

    private suspend fun countPendingSyncMedia(): Int = withContext(Dispatchers.IO) {
        try {
            val albumBucketIds = AppPrefKeys.getSelectedAlbums(context).first()
            val lastGen = AppPrefKeys.getMediaStoreLastGen(context).first()
            if (albumBucketIds.isEmpty()) return@withContext 0

            val bucketIdsPlaceholder = albumBucketIds.joinToString(separator = ",", prefix = "(", postfix = ")") { "?" }
            val selectionArgs = albumBucketIds.map { it.toString() }.toTypedArray()

            val imageSelection = """
                ${MediaStore.Images.Media.BUCKET_ID} IN $bucketIdsPlaceholder
                AND (${MediaStore.Images.Media.GENERATION_ADDED} > ? OR ${MediaStore.Images.Media.GENERATION_MODIFIED} > ?)
            """.trimIndent()
            val imageSelectionArgs = selectionArgs + lastGen.toString() + lastGen.toString()

            var count = 0
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media._ID),
                imageSelection,
                imageSelectionArgs,
                null
            )?.use { cursor ->
                count += cursor.count
            }

            val videoSelection = """
                ${MediaStore.Video.Media.BUCKET_ID} IN $bucketIdsPlaceholder
                AND (${MediaStore.Video.Media.GENERATION_ADDED} > ? OR ${MediaStore.Video.Media.GENERATION_MODIFIED} > ?)
            """.trimIndent()
            val videoSelectionArgs = selectionArgs + lastGen.toString() + lastGen.toString()

            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Video.Media._ID),
                videoSelection,
                videoSelectionArgs,
                null
            )?.use { cursor ->
                count += cursor.count
            }

            count
        } catch (e: Exception) {
            0
        }
    }

    fun startSync() {
        if (SyncStateManager.isSyncing.value) {
            return
        }
        context.startForegroundService(Intent(context, MediaSyncService::class.java))
    }
}