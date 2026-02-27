# Media File Upload Design

## Overview

Implement media file upload functionality in MediaSyncService, supporting both direct upload (<10MB) and chunked upload (>=10MB). Files are sourced from MediaStore, filtered by a timestamp-based partition strategy.

## Requirements

- **File Source**: Scan MediaStore for images and videos
- **Filter Strategy**: Upload all files modified after a stored timestamp
- **Upload Threshold**: Direct upload for files < 10MB, chunked upload for files >= 10MB
- **Upload Strategy**: Serial file upload (one at a time)
- **Error Handling**: Retry 3 times on failure, then skip and continue
- **Storage**: New DataStore field for timestamp partition

## Architecture

### Method Hierarchy

```
MediaSyncService
├── scanMediaFiles() - Scan MediaStore, filter by timestamp
├── doSync() - Main orchestration
├── uploadFiles() - Iterate through file list
├── uploadMediaFile() - Choose upload method by size
│   ├── if < 10MB: uploadDirect()
│   └── if >= 10MB: uploadChunked()
├── uploadDirect() - Single-file upload
└── uploadChunked() - Chunked upload with flow
    ├── checkChunks() - Check resume status
    ├── uploadEachChunk() - Upload chunks sequentially
    ├── notifyMergeChunks() - Trigger server merge
    └── pollMergeResult() - Wait for merge completion
```

### Data Flow

1. Scan MediaStore (images and videos)
2. Filter files by timestamp > syncSinceTimestamp
3. Apply serial upload with retry logic
4. Update progress notifications
5. On completion: Update syncSinceTimestamp

## Data Structures

### New DataStore Configuration

```kotlin
// In AppPrefKeys
val syncSinceTimestamp = longPreferencesKey("sync_since_timestamp")
```

### MediaFile Data Class

```kotlin
data class MediaFile(
    val uri: Uri,
    val type: MediaType, // IMAGE or VIDEO
    val name: String,
    val size: Long,
    val duration: Int, // Video duration in ms, 0 for images
    val dateAdded: Long,
    val lastModified: Long,
    val dateToken: Long, // Archive date
    val favorite: Int // 0 or 1
)

enum class MediaType {
    IMAGE, VIDEO
}
```

### Configuration Constants

```kotlin
const val THRESHOLD_SIZE = 10 * 1024 * 1024L // 10MB threshold
const val CHUNK_SIZE = 5 * 1024 * 1024L // 5MB chunk size
const val MAX_RETRY_COUNT = 3 // Max retry attempts
const val MERGE_POLL_COUNT = 30 // Max merge status polls
const val MERGE_POLL_DELAY = 2000L // 2 seconds between polls
```

## API Endpoints

### Direct Upload (<10MB)
- Endpoint: `/api/media/upload/direct`
- Method: POST (Multipart)
- Success: Ret<*>

### Chunked Upload Flow (>=10MB)
1. **Check Chunks**: `POST /api/media/upload/checkChunks`
   - Request: fileId, chunkSize, totalSize, totalChunks
   - Response: maxChunkIndex (resume support)

2. **Upload Chunk**: `POST /api/media/upload/chunk`
   - Query: fileId, chunkIndex
   - Body: Multipart chunk
   - Success: Ret<*>

3. **Notify Merge**: `POST /api/media/upload/notifyMergeChunks`
   - Request: file metadata + fileId, chunkSize, totalChunks
   - Success: Ret<*>

4. **Poll Result**: `POST /api/media/upload/pollMergeResult`
   - Request: fileId
   - Response: status, progress, error

## Error Handling

### Retry Strategy
- **Network Timeout**: Retry with exponential backoff (1s, 2s, 4s)
- **API Errors**: Check Ret.isRetOk(), retry on failure
- **File Read Errors**: Skip file, log error, continue with next
- **Merge Timeout**: Re-attempt entire chunked upload flow
- **Max Retry Exceeded**: Log failure, update notification with failure count, continue with next file

### Progress Notification
- Overall progress: (successful + failed) / total files
- Current file status: "Uploading X/Total: filename.jpg"
- Completed: "Sync complete (X succeeded, Y failed)"

## Storage & Permissions

### Required Permissions (AndroidManifest.xml)

```xml
<!-- Android 13+ -->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
```

### Timestamp Management
- **First Sync**: Use (current_time - 30 days) to upload recent files
- **Subsequent Syncs**: Use stored syncSinceTimestamp
- **Post-Sync Update**: Set syncSinceTimestamp = max(lastModified) of uploaded files

## Implementation Notes

### Retro fit Client Configuration
Current timeout settings are 30s. For large file uploads:
- Consider increasing to 300s (5 minutes) for chunked uploads
- Or use OkHttp upload progress callback for better UX

### Memory Management
- Stream file chunks to avoid loading entire file into memory
- Use contentResovler.openInputStream() for MediaStore URIs

### Edge Cases
- Empty file list notification
- Corrupted MediaStore entries
- Files moved/deleted during upload
- Server in merge state (handle via poll timeout)

## Success Criteria

- All files > timestamp uploaded successfully (or skipped with logged reason)
- Progress notifications updated throughout process
- syncSinceTimestamp properly saved after completion
- Failed files do not block remaining uploads
- Graceful handling of network errors and timeouts
