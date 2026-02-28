package com.filenest.photo.data.usecase

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import com.filenest.photo.data.api.ApiService
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
    private val apiService: ApiService,
) {

    companion object {
        const val TAG = "MediaSyncUploadUseCase"

        // 单个分片的大小，MB
        const val CHUNK_SIZE = 1024 * 1024 * 5

        val MEDIA_TYPE = "application/octet-stream".toMediaType()

        // 轮询间隔（3秒一次，避免频繁请求）
        const val POLL_INTERVAL = 1000L * 3

        // 等待分片合并超时时间
        const val WAIT_POLL_TIMEOUT = 1000L * 60 * 30
    }

    suspend fun uploadMedia(item: MediaSyncItem): Boolean {
        Log.i(TAG, "uploadMedia: $item")
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

                val ret = apiService.uploadDirect(
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
                    Log.i(TAG, "Upload success: ${item.name}")
                    true
                } else {
                    Log.e(TAG, "Upload failed: ${retMsg(ret)}")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Upload error: ${e.message}", e)
                false
            }
        }
    }

}