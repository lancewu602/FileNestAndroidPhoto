# Realtime Sync Progress Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement real-time sync progress display by connecting MediaSyncService to UI through shared state manager, with per-file generation updates.

**Architecture:** Extend SyncStateManager with progress state, have MediaSyncService update progress after each upload completion, SyncViewModel observes the state and updates UI, lastGen updated after each successful upload.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt DI, StateFlow, DataStore, MediaStore API

---

## Task 1: Extend SyncStateManager with Progress Support

**Files:**
- Modify: app/src/main/java/com/filenest/photo/data/SyncStateManager.kt

**Step 1: Add SyncProgress data class and extend SyncStateManager**

```kotlin
package com.filenest.photo.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SyncProgress(
    val current: Int = 0,
    val total: Int = 0
)

object SyncStateManager {
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncProgress = MutableStateFlow(SyncProgress())
    val syncProgress: StateFlow<SyncProgress> = _syncProgress.asStateFlow()

    fun setSyncing(syncing: Boolean) {
        _isSyncing.value = syncing
    }

    fun updateProgress(current: Int, total: Int) {
        _syncProgress.value = SyncProgress(current, total)
    }
}
```

**Step 2: Run build to verify no compilation errors**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/com/filenest/photo/data/SyncStateManager.kt
git commit -m "feat: add progress state to SyncStateManager"
```

---

## Task 2: Modify MediaSyncService to Update Progress and LastGen Per File

**Files:**
- Modify: app/src/main/java/com/filenest/photo/service/MediaSyncService.kt

**Step 1: Add required imports**

Add these imports at the top of the file:
```kotlin
import android.provider.MediaStore
import com.filenest.photo.data.AppDataStore
import com.filenest.photo.data.AppPrefKeys
import com.filenest.photo.data.SyncStateManager

// Keep existing imports...
```

**Step 2: Inject AppDataStore at class level**

Add after existing @Inject fields:
```kotlin
    @Inject
    lateinit var mediaSyncUploadUseCase: MediaSyncUploadUseCase

    @Inject
    lateinit var appDataStore: AppDataStore
```

**Step 3: Modify onStartCommand to use SyncStateManager and update lastGen per file**

Replace the entire `onStartCommand` method with:
```kotlin
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (syncJob?.isActive == true) {
            return START_STICKY
        }

        SyncStateManager.setSyncing(true)

        syncJob = serviceScope.launch {
            val currentGen = MediaStore.getGeneration(applicationContext.contentResolver)
            val medias = mediaSyncFetchUseCase.fetchMedias()
            val total = medias.size
            Log.d(TAG, "开始同步: $total 个文件, currentGen: $currentGen")

            medias.forEachIndexed { index, item ->
                mediaSyncUploadUseCase.uploadMedia(item)

                SyncStateManager.updateProgress(index + 1, total)
                AppPrefKeys.setMediaStoreLastGen(applicationContext, currentGen)

                val progress = (index + 1) * 100 / total
                updateNotification("正在上传 (${index + 1}/${total})", progress)

                Log.d(TAG, "上传完成 ${index + 1}/$total, 更新 lastGen: $currentGen")
            }

            Log.d(TAG, "同步完成")
            SyncStateManager.setSyncing(false)
            updateNotification("同步完成")
            stopSelf()
        }
        return START_STICKY
    }
```

**Step 4: Run build to verify no compilation errors**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add app/src/main/java/com/filenest/photo/service/MediaSyncService.kt
git commit -m "feat: update sync progress and lastGen after each file upload"
```

---

## Task 3: Modify SyncViewModel to Observe SyncStateManager

**Files:**
- Modify: app/src/main/java/com/filenest/photo/viewmodel/SyncViewModel.kt

**Step 1: Remove loadSyncInfo() mock data and observe SyncStateManager**

Replace the entire `init` block and `loadSyncInfo()` method with:
```kotlin
    init {
        viewModelScope.launch {
            SyncStateManager.isSyncing.collect { isSyncing ->
                _isSyncing.value = isSyncing
            }
        }
        viewModelScope.launch {
            SyncStateManager.syncProgress.collect { progress ->
                _syncProgressInfo.value = SyncProgressInfo(
                    totalProgress = progress.current,
                    totalFiles = progress.total,
                    currentFileName = "",  // Not provided by SyncStateManager
                    fileProgress = 0f      // Not provided by SyncStateManager
                )
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
        }
    }
```

**Step 2: Run build to verify no compilation errors**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/com/filenest/photo/viewmodel/SyncViewModel.kt
git commit -m "refactor: observe SyncStateManager for real-time progress"
```

---

## Task 4: Manual Testing

**Files:**
- No files modified

**Step 1: Build and install debug APK**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

Install to device/emulator:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**Step 2: Test sync progress display**

1. Open app and navigate to Sync screen
2. Tap "开始同步" button
3. Observe UI shows "同步中..."
4. Verify progress counter updates after each file upload displayed as `(current/total)`
5. Verify button remains disabled during sync
6. Wait for sync to complete
7. Verify final state shows `X/X` where X = total files
8. Verify button re-enables with "开始同步" text

**Step 3: Test lastGen persistence**

1. Stop sync after a few files
2. Close and reopen app
3. Tap "开始同步" again
4. Verify sync continues (fetchMedia should only get new files based on lastGen)

**Step 4: Test app restart during sync**

1. Start sync
2. Force close app while sync is in progress
3. Reopen app and navigate to Sync screen
4. Verify correct progress state is displayed (from SyncStateManager)
5. Tap sync again and verify it continues correctly

**Step 5: Document test results**

Create test documentation if any issues found

---

## Task 5: Verify Code Quality

**Files:**
- All modified files

**Step 1: Run linter if available**

Run: `./gradlew ktlintCheck` (if configured)
Expected: No lint errors

**Step 2: Run all tests (if any exist)**

Run: `./gradlew test`
Expected: All tests pass

**Step 3: Review all changes**

Review each modified file to ensure:
- No commented-out code
- No unused imports
- Proper error handling
- Thread-safe state updates (StateFlow is thread-safe)

---

## Task 6: Final Verification

**Files:**
- All modified files

**Step 1: Full clean build**

Run: `./gradlew clean build`
Expected: BUILD SUCCESSFUL

**Step 2: Summary of changes**

Verify all requirements met:
- [ ] Real-time progress display in Sync screen
- [ ] Progress format `(current/total)`
- [ ] Progress updates after each file upload
- [ ] MEDIA_STORE_LAST_GEN updated after each file upload
- [ ] UI auto-updates without manual refresh
- [ ] Final state shows `X/X` after completion
- [ ] isSyncing set to false after completion

**Step 3: Prepare for deployment**

If all tests pass and requirements met, the feature is ready for deployment.
