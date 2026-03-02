package com.filenest.photo.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filenest.photo.data.AppPrefKeys
import com.filenest.photo.data.api.RetrofitClient
import com.filenest.photo.data.api.isRetOk
import com.filenest.photo.data.SyncStateManager
import com.filenest.photo.service.MediaSyncService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    }

    fun startSync() {
        if (SyncStateManager.isSyncing.value) {
            return
        }
        context.startForegroundService(Intent(context, MediaSyncService::class.java))
    }
}