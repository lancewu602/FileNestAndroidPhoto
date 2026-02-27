package com.filenest.photo.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SyncInfo(
    val lastSyncTime: String,
    val serverMediaCount: Int,
    val pendingSyncCount: Int
)

class SyncViewModel : ViewModel() {

    private val _syncInfo = MutableStateFlow(
        SyncInfo(
            lastSyncTime = "",
            serverMediaCount = 0,
            pendingSyncCount = 0
        )
    )
    val syncInfo: StateFlow<SyncInfo> = _syncInfo.asStateFlow()

    fun loadSyncInfo() {
        _syncInfo.value = SyncInfo(
            lastSyncTime = "2024-01-15 14:30:00",
            serverMediaCount = 1234,
            pendingSyncCount = 56
        )
    }
}