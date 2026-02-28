package com.filenest.photo.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filenest.photo.data.SyncStateManager
import com.filenest.photo.service.MediaSyncService
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
    val totalProgress: Int = 0,
    val totalFiles: Int = 0,
    val currentFileName: String = "",
    val fileProgress: Float = 0f
)

@HiltViewModel
class SyncViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _syncBasicInfo = MutableStateFlow(SyncBasicInfo())
    val syncBasicInfo: StateFlow<SyncBasicInfo> = _syncBasicInfo.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncProgressInfo = MutableStateFlow(SyncProgressInfo())
    val syncProgressInfo: StateFlow<SyncProgressInfo> = _syncProgressInfo.asStateFlow()

    init {
        viewModelScope.launch {
            SyncStateManager.isSyncing.collect { isSyncing ->
                _isSyncing.value = isSyncing
            }
        }
    }

    fun loadSyncInfo() {
        viewModelScope.launch {
            _syncBasicInfo.value = SyncBasicInfo(
                lastSyncTime = "2024-01-15 14:30:00",
                serverMediaCount = 1234,
                pendingSyncCount = 56
            )
            _syncProgressInfo.value = SyncProgressInfo(
                totalProgress = 53,
                totalFiles = 100,
                currentFileName = "IMG_20240115_143000.jpg",
                fileProgress = 0.65f
            )
        }
    }

    fun startSync() {
        if (SyncStateManager.isSyncing.value) {
            return
        }
        context.startForegroundService(Intent(context, MediaSyncService::class.java))
    }
}