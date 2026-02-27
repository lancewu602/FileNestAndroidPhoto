package com.filenest.photo.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filenest.photo.data.SyncStateManager
import com.filenest.photo.service.MediaSyncService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SyncInfo(
    val lastSyncTime: String,
    val serverMediaCount: Int,
    val pendingSyncCount: Int,
    val isSyncing: Boolean = false
)

@HiltViewModel
class SyncViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _syncInfo = MutableStateFlow(
        SyncInfo(
            lastSyncTime = "",
            serverMediaCount = 0,
            pendingSyncCount = 0
        )
    )
    val syncInfo: StateFlow<SyncInfo> = _syncInfo.asStateFlow()

    init {
        viewModelScope.launch {
            SyncStateManager.isSyncing.collect { isSyncing ->
                _syncInfo.value = _syncInfo.value.copy(isSyncing = isSyncing)
            }
        }
    }

    fun loadSyncInfo() {
        viewModelScope.launch {
            _syncInfo.value = SyncInfo(
                lastSyncTime = "2024-01-15 14:30:00",
                serverMediaCount = 1234,
                pendingSyncCount = 56,
                isSyncing = SyncStateManager.isSyncing.value
            )
        }
    }

    fun startSync() {
        if (SyncStateManager.isSyncing.value) {
            return
        }
        MediaSyncService.startSync(context)
    }
}