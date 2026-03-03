package com.filenest.photo.data

import com.filenest.photo.viewmodel.SyncProgressInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SyncResult {
    NOT_STARTED,
    IN_PROGRESS,
    SUCCESS,
    FAILURE
}

object SyncStateManager {
    private val _syncResult = MutableStateFlow(SyncResult.NOT_STARTED)
    val syncResult: StateFlow<SyncResult> = _syncResult.asStateFlow()

    private val _syncProgressInfo = MutableStateFlow(SyncProgressInfo())
    val syncProgressInfo: StateFlow<SyncProgressInfo> = _syncProgressInfo.asStateFlow()

    private val _syncProgressStep = MutableStateFlow("")
    val syncProgressStep: StateFlow<String> = _syncProgressStep.asStateFlow()

    private val _syncProgressFile = MutableStateFlow(0F)
    val syncProgressFile: StateFlow<Float> = _syncProgressFile.asStateFlow()

    fun setSyncing(syncing: Boolean) {
        _syncResult.value = if (syncing) SyncResult.IN_PROGRESS else SyncResult.NOT_STARTED
    }

    fun setSyncResult(result: SyncResult) {
        _syncResult.value = result
    }

    fun setSyncProgressInfo(progress: Int, total: Int, fileName: String) {
        _syncProgressInfo.value = SyncProgressInfo(
            total = progress,
            completed = total,
            fileName = fileName,
        )
    }

    fun setSyncProgressFile(progress: Float) {
        _syncProgressFile.value = progress
    }

    fun setSyncProgressStep(step: String) {
        _syncProgressStep.value = step
    }
}