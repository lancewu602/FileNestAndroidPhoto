# Realtime Sync Progress Design

## Overview

Implement real-time sync progress display in the Sync screen by connecting MediaSyncService to the UI through a shared state manager. The progress will be updated after each file upload, and the MediaStore generation number will be updated after each successful upload to enable incremental sync.

## Requirements

- Display real-time sync progress in the Sync screen UI
- Progress format: `(current/total)` where current is the number of completed uploads
- Update progress after each file upload completes
- Update MEDIA_STORE_LAST_GEN after each file upload (not just at the end)
- SyncScreen should automatically reflect progress changes without manual refresh
- After sync completes, keep the final state showing `X/X` and set isSyncing = false

## Architecture

### State Flow

```
MediaSyncService
    └─> SyncStateManager (shared state)
        └─> SyncViewModel
            └─> SyncScreen (UI)
```

### Components

1. **SyncStateManager** - Extended with progress support
   - Added `syncProgress: StateFlow<SyncProgress>`
   - Added `updateProgress(current: Int, total: Int)` function
   - Maintains thread-safe state updates

2. **MediaSyncService** - Updates progress after each upload
   - Sets `SyncStateManager.isSyncing = true` before starting
   - Gets current MediaStore generation at start
   - After each upload: updates progress and sets MEDIA_STORE_LAST_GEN
   - Sets `SyncStateManager.isSyncing = false` when done

3. **SyncViewModel** - Observes SyncStateManager
   - Collects `syncProgress` and updates local StateFlow
   - Removes hardcoded mock data

4. **SyncScreen** - Auto-updates UI
   - Collects progress from viewModel
   - Displays `(current/total)` format

## Data Structure

### SyncProgress

```kotlin
data class SyncProgress(
    val current: Int = 0,
    val total: Int = 0
)
```

## Implementation Details

### SyncStateManager Extension

```kotlin
object SyncStateManager {
    private val _syncProgress = MutableStateFlow(SyncProgress())
    val syncProgress: StateFlow<SyncProgress> = _syncProgress.asStateFlow()

    fun setSyncing(syncing: Boolean)
    fun updateProgress(current: Int, total: Int) {
        _syncProgress.value = SyncProgress(current, total)
    }
}
```

### MediaSyncService Changes

1. Inject `AppDataStore` to update generation
2. At start:
   ```kotlin
   SyncStateManager.setSyncing(true)
   val currentGen = MediaStore.getGeneration(context)
   val lastGen = AppPrefKeys.getMediaStoreLastGen()
   ```

3. During upload loop:
   ```kotlin
   medias.forEachIndexed { index, item ->
       mediaSyncUploadUseCase.uploadMedia(item)
       SyncStateManager.updateProgress(index + 1, total)
       AppPrefKeys.setMediaStoreLastGen(currentGen)
       updateNotification("正在上传 (${index + 1}/${total})", progress)
   }
   ```

4. After completion:
   ```kotlin
   SyncStateManager.setSyncing(false)
   updateNotification("同步完成")
   stopSelf()
   ```

### SyncViewModel Changes

Remove mock data collection, observe SyncStateManager:

```kotlin
init {
    viewModelScope.launch {
        SyncStateManager.isSyncing.collect { isSyncing ->
            _isSyncing.value = isSyncing
        }
        SyncStateManager.syncProgress.collect { progress ->
            _syncProgressInfo.value = SyncProgressInfo(
                totalProgress = progress.current,
                totalFiles = progress.total,
                // Keep other fields as is (file name, file progress)
            )
        }
    }
}
```

### SyncScreen Changes

Progress display already uses `syncProgressInfo.totalProgress / totalFiles`, so UI code doesn't need modification - viewModel will feed real data instead of mock.

## Error Handling

- If service crashes, SyncStateManager will maintain last known state
- Service restart will continue from where it left off (lastGen ensures incremental sync)
- All state updates are thread-safe through StateFlow

## Testing

1. Start sync and verify progress updates in UI after each file
2. Kill app during sync, reopen and verify correct progress displayed
3. Verify lastGen is updated after each upload (check through debug logs)
4. Verify sync completion final state shows "X/X" and isSyncing = false

## Notes

- Progress update frequency: once per file upload (not per-file-progress granularity)
- SyncScreen will remain responsive through Compose recomposition
- Using shared state manager avoids complex Service binding or BroadcastReceiver setup
