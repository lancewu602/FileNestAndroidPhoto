package com.filenest.photo.data.usecase

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import com.filenest.photo.data.api.CheckChunkRequest
import com.filenest.photo.data.api.MergeChunkRequest
import com.filenest.photo.data.api.MergeResultRequest
import com.filenest.photo.data.api.RetrofitClient
import com.filenest.photo.data.api.isRetOk
import com.filenest.photo.data.api.retMsg
import com.filenest.photo.data.model.MediaSyncItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

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
    }

    suspend fun uploadMedia(item: MediaSyncItem): Boolean {
        Log.i(TAG, "uploadMedia: $item")
        return if (item.size > CHUNK_THRESHOLD) {
            uploadMediaChunked(item)
        } else {
            uploadMediaDirect(item)
        }
    }

    private suspend fun uploadMediaDirect(item: MediaSyncItem): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val uri = item.contentUri.toUri()
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("Cannot open input stream for URI")

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
                    lastModified = item.lastModified,
                    duration = item.duration,
                    favorite = item.favorite,
                    file = filePart,
                )

                if (isRetOk(ret)) {
                    Log.i(TAG, "Direct upload success: ${item.name}")
                    true
                } else {
                    Log.e(TAG, "Direct upload failed: ${retMsg(ret)}")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Direct upload error: ${e.message}", e)
                false
            }
        }
    }

    private suspend fun uploadMediaChunked(item: MediaSyncItem): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val fileId = "${item.name}-${item.size}-${item.lastModified}"
                val totalChunks = (item.size + CHUNK_SIZE - 1) / CHUNK_SIZE

                Log.i(TAG, "Starting chunked upload: fileId=$fileId, totalChunks=$totalChunks")

                val startChunkIndex = getStartChunkIndex(fileId, totalChunks, item.size)

                val uri = item.contentUri.toUri()
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("Cannot open input stream for URI")

                inputStream.use { stream ->
                    stream.skip((startChunkIndex * CHUNK_SIZE).toLong())

                    for (chunkIndex in startChunkIndex until totalChunks) {
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
                            return@withContext false
                        }

                        Log.i(TAG, "Chunk $chunkIndex uploaded successfully")
                    }
                }

                val mergeRequest = MergeChunkRequest(
                    type = item.type,
                    name = item.name,
                    size = item.size,
                    duration = item.duration,
                    dateToken = item.dateToken,
                    dateAdded = item.dateAdded,
                    lastModified = item.lastModified,
                    favorite = item.favorite,
                    fileId = fileId,
                    chunkSize = CHUNK_SIZE,
                    totalChunks = totalChunks,
                )

                val mergeRet = retrofitClient.getApiService().notifyMergeChunks(mergeRequest)
                if (!isRetOk(mergeRet)) {
                    Log.e(TAG, "Notify merge failed: ${retMsg(mergeRet)}")
                    return@withContext false
                }

                val startTime = System.currentTimeMillis()
                while (System.currentTimeMillis() - startTime < WAIT_POLL_TIMEOUT) {
                    val pollRet = retrofitClient.getApiService().pollMergeResult(MergeResultRequest(fileId = fileId))
                    if (isRetOk(pollRet) && pollRet.data != null) {
                        val result = pollRet.data
                        when (result.status) {
                            "completed" -> {
                                Log.i(TAG, "Chunked upload completed: ${item.name}")
                                return@withContext true
                            }

                            "failed" -> {
                                Log.e(TAG, "Merge failed: ${result.error}")
                                return@withContext false
                            }

                            else -> {
                                Log.i(TAG, "Merging progress: ${result.progress}")
                            }
                        }
                    }
                    kotlinx.coroutines.delay(POLL_INTERVAL)
                }

                Log.e(TAG, "Merge poll timeout")
                false
            } catch (e: Exception) {
                Log.e(TAG, "Chunked upload error: ${e.message}", e)
                false
            }
        }
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