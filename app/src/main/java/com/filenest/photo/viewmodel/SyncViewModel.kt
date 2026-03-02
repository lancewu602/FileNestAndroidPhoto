package com.filenest.photo.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filenest.photo.data.AppPrefKeys
import com.filenest.photo.data.SyncStateManager
import com.filenest.photo.service.MediaSyncService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SyncBasicInfo(
    val lastSyncTime: String = "",
    val serverMediaCount: Int = 0,
    val pendingSyncCount: Int = 0
)

data class SyncProgressInfo(
    val total: Int = 0,
    val completed: Int = 0,
    val fileName: String = "",
    val step: String = "",
)

@HiltViewModel
class SyncViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _syncBasicInfo = MutableStateFlow(SyncBasicInfo())
    val syncBasicInfo: StateFlow<SyncBasicInfo> = _syncBasicInfo.asStateFlow()

    val isSyncing: StateFlow<Boolean> = SyncStateManager.isSyncing
    val syncProgressInfo: StateFlow<SyncProgressInfo> = SyncStateManager.syncProgressInfo
    val syncProgressFile: StateFlow<Float> = SyncStateManager.syncProgressFile
    val syncProgressStep: StateFlow<String> = SyncStateManager.syncProgressStep

    fun loadSyncInfo() {
        viewModelScope.launch {
            AppPrefKeys.getLatestSyncTime(context).collect { timestamp ->
                val lastSyncTimeStr = if (timestamp > 0) {
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    sdf.format(Date(timestamp))
                } else {
                    "从未同步"
                }
                _syncBasicInfo.value = SyncBasicInfo(
                    lastSyncTime = lastSyncTimeStr,
                    serverMediaCount = 1234,
                    pendingSyncCount = 56
                )
            }
        }
    }

    fun startSync() {
        if (SyncStateManager.isSyncing.value) {
            return
        }
        context.startForegroundService(Intent(context, MediaSyncService::class.java))
    }
}