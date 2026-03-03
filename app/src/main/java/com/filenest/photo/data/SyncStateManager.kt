package com.filenest.photo.data

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

    private val _syncCompleted = MutableStateFlow(0)
    val syncCompleted: StateFlow<Int> = _syncCompleted.asStateFlow()

    private val _syncTotal = MutableStateFlow(0)
    val syncTotal: StateFlow<Int> = _syncTotal.asStateFlow()

    private val _syncStep = MutableStateFlow("")
    val syncStep: StateFlow<String> = _syncStep.asStateFlow()

    private val _syncFileProgress = MutableStateFlow(0F)
    val syncFileProgress: StateFlow<Float> = _syncFileProgress.asStateFlow()

    private val _syncFileName = MutableStateFlow("")
    val syncFileName: StateFlow<String> = _syncFileName.asStateFlow()

    fun setSyncing(syncing: Boolean) {
        _syncResult.value = if (syncing) SyncResult.IN_PROGRESS else SyncResult.NOT_STARTED
    }

    fun setSyncResult(result: SyncResult) {
        _syncResult.value = result
    }

    fun setSyncTotal(total: Int) {
        _syncTotal.value = total
    }

    fun setSyncCompleted(completed: Int) {
        _syncCompleted.value = completed
    }

    fun setFileName(fileName: String) {
        _syncFileName.value = fileName
    }

    fun setFileProgress(progress: Float) {
        _syncFileProgress.value = progress
    }

    fun setStep(step: String) {
        _syncStep.value = step
    }
}