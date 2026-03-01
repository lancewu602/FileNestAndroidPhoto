package com.filenest.photo.data.usecase

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import com.filenest.photo.data.AppPrefKeys
import com.filenest.photo.data.SyncStateManager
import com.filenest.photo.data.api.CheckChunkRequest
import com.filenest.photo.data.api.MergeChunkRequest
import com.filenest.photo.data.api.MergeResultRequest
import com.filenest.photo.data.api.Ret
import com.filenest.photo.data.api.RetrofitClient
import com.filenest.photo.data.api.isRetOk
import com.filenest.photo.data.api.retMsg
import com.filenest.photo.data.model.MediaSyncItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

sealed class UploadResult {
    data object Success : UploadResult()

    data class Failure(val reason: UploadFailureReason, val message: String) : UploadResult()
}

enum class UploadFailureReason {
    NETWORK_ERROR,
    FILE_ERROR,
    SERVER_ERROR,
    UNKNOWN,
}

@Singleton
class MediaSyncUploadUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val retrofitClient: RetrofitClient,
) {

    companion object {
        const val TAG = "MediaSyncUploadUseCase"

        const val CHUNK_SIZE = 1024 * 1024 * 5

        const val CHUNK_THRESHOLD = 1024L * 1024 * 20

        val MEDIA_TYPE = "application/octet-stream".toMediaType()

        const val POLL_INTERVAL = 1000L * 3

        const val WAIT_POLL_TIMEOUT = 1000L * 60 * 30

        const val MAX_POLL_FAILURES = 5
    }

    suspend fun uploadMedia(item: MediaSyncItem): UploadResult {
        val result = if (item.size > CHUNK_THRESHOLD) {
            Log.i(TAG, "Start upload chunked: ${item.contentUri}")
            uploadMediaChunked(item)
        } else {
            Log.i(TAG, "Start upload direct: ${item.contentUri}")
            uploadMediaDirect(item)
        }

        if (result is UploadResult.Success) {
            AppPrefKeys.setMediaStoreLastGen(context, item.generationModified)
            Log.d(TAG, "Updated lastGen to ${item.generationModified}")
        }

        return result
    }

    suspend fun uploadMediaSimple(item: MediaSyncItem): Boolean {
        return uploadMedia(item) is UploadResult.Success
    }

    private fun classifyException(e: Exception): UploadFailureReason {
        val msg = e.message ?: return UploadFailureReason.UNKNOWN
        return when {
            msg.contains("Unable to resolve host") ||
            msg.contains("Connection reset") ||
            msg.contains("ConnectException") ||
            msg.contains("SocketTimeoutException") -> UploadFailureReason.NETWORK_ERROR

            msg.contains("permission") ||
            msg.contains("SecurityException") ||
            msg.contains("FileNotFoundException") -> UploadFailureReason.FILE_ERROR

            else -> UploadFailureReason.UNKNOWN
        }
    }

    private suspend fun uploadMediaDirect(item: MediaSyncItem): UploadResult {
        return withContext(Dispatchers.IO) {
            try {
                SyncStateManager.setSyncProgressStep("上传中")
                SyncStateManager.setSyncProgressFile(0F)
                val uri = item.contentUri.toUri()
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext UploadResult.Failure(UploadFailureReason.FILE_ERROR, "Cannot open input stream for URI")

                val byteArray = ByteArrayOutputStream().use { outputStream ->
                    inputStream.use { inputStream.copyTo(outputStream) }
                    outputStream.toByteArray()
                }

                val filePart = MultipartBody.Part.createFormData(
                    name = "file",
                    filename = item.name,
                    body = byteArray.toRequestBody(MEDIA_TYPE),
                )

                val ret = retrofitClient.getApiService().uploadDirect(
                    type = item.type,
                    name = item.name,
                    size = item.size,
                    dateToken = item.dateToken,
                    dateAdded = item.dateAdded,
                    dateModified = item.dateModified,
                    duration = item.duration,
                    favorite = item.favorite,
                    file = filePart,
                )

                if (isRetOk(ret)) {
                    Log.i(TAG, "Direct upload success: ${item.name}")
                    SyncStateManager.setSyncProgressFile(1F)
                    UploadResult.Success
                } else {
                    Log.e(TAG, "Direct upload failed: ${retMsg(ret)}")
                    UploadResult.Failure(UploadFailureReason.SERVER_ERROR, retMsg(ret) ?: "Server error")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Direct upload error: ${e.message}", e)
                UploadResult.Failure(classifyException(e), e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun uploadMediaChunked(item: MediaSyncItem): UploadResult {
        return withContext(Dispatchers.IO) {
            try {
                SyncStateManager.setSyncProgressStep("上传中")
                SyncStateManager.setSyncProgressFile(0F)

                val fileId = "${item.name}-${item.size}-${item.dateModified}"
                val totalChunks = (item.size + CHUNK_SIZE - 1) / CHUNK_SIZE

                Log.i(TAG, "Starting chunked upload: fileId=$fileId, totalChunks=$totalChunks")

                Log.i(TAG, "Checking chunk status: fileId=$fileId, totalChunks=$totalChunks, totalSize=${item.size}")
                val startChunkIndex = getStartChunkIndex(fileId, totalChunks, item.size)
                Log.i(TAG, "Chunk check result: startChunkIndex=$startChunkIndex")

                val uri = item.contentUri.toUri()
                var inputStream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext UploadResult.Failure(UploadFailureReason.FILE_ERROR, "Cannot open input stream for URI")

                var effectiveStartChunkIndex = startChunkIndex

                try {
                    val skipped = inputStream.skip((effectiveStartChunkIndex * CHUNK_SIZE).toLong())
                    if (skipped < effectiveStartChunkIndex * CHUNK_SIZE) {
                        Log.w(TAG, "Stream skip incomplete, restarting from 0")
                        inputStream.close()
                        inputStream = context.contentResolver.openInputStream(uri)
                            ?: return@withContext UploadResult.Failure(UploadFailureReason.FILE_ERROR, "Cannot reopen input stream")
                        effectiveStartChunkIndex = 0
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Stream skip failed: ${e.message}, restarting from 0")
                    inputStream.close()
                    inputStream = context.contentResolver.openInputStream(uri)
                        ?: return@withContext UploadResult.Failure(UploadFailureReason.FILE_ERROR, "Cannot reopen input stream")
                    effectiveStartChunkIndex = 0
                }

                Log.i(TAG, "Starting chunk upload loop: from=$effectiveStartChunkIndex, total=$totalChunks")
                inputStream.use { stream ->
                    for (chunkIndex in effectiveStartChunkIndex until totalChunks) {
                        Log.d(TAG, "Uploading chunk $chunkIndex...")
                        val chunkData = ByteArrayOutputStream().use { outputStream ->
                            val buffer = ByteArray(CHUNK_SIZE)
                            val bytesRead = stream.read(buffer)
                            if (bytesRead > 0) {
                                outputStream.write(buffer, 0, bytesRead)
                            }
                            outputStream.toByteArray()
                        }

                        if (chunkData.isEmpty()) break

                        val chunkPart = MultipartBody.Part.createFormData(
                            name = "chunk",
                            filename = "chunk_$chunkIndex",
                            body = chunkData.toRequestBody(MEDIA_TYPE),
                        )

                        val uploadRet = retrofitClient.getApiService().uploadChunk(
                            fileId = fileId,
                            chunkIndex = chunkIndex,
                            chunk = chunkPart,
                        )

                        if (!isRetOk(uploadRet)) {
                            Log.e(TAG, "Chunk $chunkIndex upload failed: ${retMsg(uploadRet)}")
                            return@withContext UploadResult.Failure(UploadFailureReason.SERVER_ERROR, retMsg(uploadRet) ?: "Chunk upload failed")
                        }

                        Log.i(TAG, "Chunk $chunkIndex uploaded successfully")
                        SyncStateManager.setSyncProgressFile(chunkIndex / totalChunks.toFloat());
                    }
                }

                Log.i(TAG, "All chunks uploaded, notifying merge: fileId=$fileId, totalChunks=$totalChunks")
                val notifyRet = notifyMergeChunks(item, fileId, totalChunks)
                if (!isRetOk(notifyRet)) {
                    Log.e(TAG, "Notify merge failed: ${retMsg(notifyRet)}")
                    return@withContext UploadResult.Failure(UploadFailureReason.SERVER_ERROR, retMsg(notifyRet) ?: "Notify merge failed")
                }
                Log.i(TAG, "Notify merge success, start polling merge result")

                Log.i(TAG, "Starting poll merge result: timeout=${WAIT_POLL_TIMEOUT}ms, interval=${POLL_INTERVAL}ms")
                SyncStateManager.setSyncProgressStep("合并分片中")
                val startTime = System.currentTimeMillis()
                var failureCount = 0
                var pollCount = 0
                while (System.currentTimeMillis() - startTime < WAIT_POLL_TIMEOUT) {
                    delay(POLL_INTERVAL)
                    ensureActive()
                    pollCount++
                    try {
                        Log.d(TAG, "Polling merge result: pollCount=$pollCount")
                        val pollRet = retrofitClient.getApiService().pollMergeResult(MergeResultRequest(fileId = fileId))
                        if (isRetOk(pollRet) && pollRet.data != null) {
                            val result = pollRet.data
                            when (result.status) {
                                "SUCCESS" -> {
                                    Log.i(TAG, "Chunked upload completed: ${item.name}")
                                    SyncStateManager.setSyncProgressFile(1F)
                                    return@withContext UploadResult.Success
                                }

                                "FAILED" -> {
                                    failureCount++
                                    Log.w(TAG, "Merge failed: ${result.error}, failure count: $failureCount")
                                    if (failureCount >= MAX_POLL_FAILURES) {
                                        Log.e(TAG, "Merge failed after $failureCount attempts")
                                        return@withContext UploadResult.Failure(UploadFailureReason.SERVER_ERROR, result.error ?: "Merge failed")
                                    }
                                }

                                else -> {
                                    Log.i(TAG, "Merging progress: ${result.progress}")
                                }
                            }
                        } else {
                            failureCount++
                            Log.w(TAG, "Poll ret failed, failure count: $failureCount")
                            if (failureCount >= MAX_POLL_FAILURES) {
                                Log.e(TAG, "Poll ret failed after $failureCount attempts")
                                return@withContext UploadResult.Failure(UploadFailureReason.SERVER_ERROR, "Poll merge result failed")
                            }
                        }
                    } catch (e: Exception) {
                        failureCount++
                        Log.w(TAG, "Poll exception: ${e.message}, failure count: $failureCount")
                        if (failureCount >= MAX_POLL_FAILURES) {
                            Log.e(TAG, "Poll exception after $failureCount attempts")
                            return@withContext UploadResult.Failure(classifyException(e), e.message ?: "Poll exception")
                        }
                    }
                }

                Log.e(TAG, "Merge poll timeout, pollCount=$pollCount, failureCount=$failureCount")
                UploadResult.Failure(UploadFailureReason.SERVER_ERROR, "Merge poll timeout")
            } catch (e: Exception) {
                Log.e(TAG, "Chunked upload error: uri=${item.contentUri}, error=${e.message}", e)
                UploadResult.Failure(classifyException(e), e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun notifyMergeChunks(
        item: MediaSyncItem,
        fileId: String,
        totalChunks: Long
    ): Ret<*> {
        val mergeRequest = MergeChunkRequest(
            type = item.type,
            name = item.name,
            size = item.size,
            duration = item.duration,
            dateToken = item.dateToken,
            dateAdded = item.dateAdded,
            dateModified = item.dateModified,
            favorite = item.favorite,
            fileId = fileId,
            chunkSize = CHUNK_SIZE,
            totalChunks = totalChunks,
        )
        return retrofitClient.getApiService().notifyMergeChunks(mergeRequest)
    }

    private suspend fun getStartChunkIndex(
        fileId: String,
        totalChunks: Long,
        totalSize: Long
    ): Int {
        return try {
            val ret = retrofitClient.getApiService().checkChunks(
                CheckChunkRequest(
                    fileId = fileId,
                    chunkSize = CHUNK_SIZE,
                    totalSize = totalSize,
                    totalChunks = totalChunks,
                )
            )
            if (isRetOk(ret)) {
                ret.data?.maxChunkIndex ?: 0
            } else {
                0
            }
        } catch (e: Exception) {
            Log.w(TAG, "Check chunks failed: ${e.message}, starting from 0")
            0
        }
    }

}