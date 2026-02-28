package com.filenest.photo.data.usecase

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import com.filenest.photo.data.AppPrefKeys
import com.filenest.photo.data.api.*
import com.filenest.photo.exception.UploadException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.apache.commons.io.IOUtils
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

data class MediaSyncItem(
    val contentUri: String,
    val type: String,
    val name: String,
    val size: Long,
    val duration: Int,
    val dateToken: Long,
    val dateAdded: Long,
    val lastModified: Long,
    val favorite: Int,
)

@Singleton
class MediaSyncUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val retrofitClient: RetrofitClient,
) {
    companion object {
        const val TAG = "MediaSyncUseCase"

        // 切片上传阈值，大于等于此值使用分片上传（单位：字节）
        const val CHUNK_THRESHOLD = 1024L * 1024 * 20

        // 单个分片的大小（单位：字节）
        const val CHUNK_SIZE = 1024 * 1024 * 5

        val MEDIA_TYPE = "application/octet-stream".toMediaType()

        // 轮询间隔（3秒一次，避免频繁请求）
        const val POLL_INTERVAL = 1000L * 3

        // 等待分片合并超时时间
        const val WAIT_POLL_TIMEOUT = 1000L * 60 * 30
    }

    private val imageQueryColumns = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.GENERATION_MODIFIED,
        MediaStore.Images.Media.SIZE,
        MediaStore.Images.Media.DATE_TAKEN,
        MediaStore.Images.Media.DATE_ADDED,
        MediaStore.Images.Media.DATE_MODIFIED,
        MediaStore.Images.Media.RELATIVE_PATH,
    )

    private val videoQueryColumns = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.GENERATION_MODIFIED,
        MediaStore.Video.Media.SIZE,
        MediaStore.Video.Media.DATE_TAKEN,
        MediaStore.Video.Media.DATE_ADDED,
        MediaStore.Video.Media.DATE_MODIFIED,
        MediaStore.Video.Media.DURATION,
        MediaStore.Video.Media.RELATIVE_PATH,
    )

    /**
     * 启动同步任务
     */
    suspend fun syncMedia() {
        launchSyncTask()
    }

    private suspend fun launchSyncTask() {
        val latestModifiedTime = AppPrefKeys.getLatestSyncTime(context).first()
        val enabledBucketIds = AppPrefKeys.getSelectedAlbums(context).first()
        val enabledSyncFavorite = false

        var total = 0

        val bucketIdsPlaceholder = enabledBucketIds.joinToString(separator = ",", prefix = "(", postfix = ")") { "?" }

        val selectionArgs = mutableListOf<String>().apply {
            add(latestModifiedTime.toString())
            enabledBucketIds.forEach { add(it.toString()) }
        }.toTypedArray()

        if (enabledBucketIds.isEmpty()) {
            return
        }

        // 查询照片
        val imageSelection = """
                ${MediaStore.Images.Media.DATE_MODIFIED} > ? AND 
                ${MediaStore.Images.Media.BUCKET_ID} IN $bucketIdsPlaceholder
            """.trimIndent()
        val imageCursor = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageQueryColumns, imageSelection, selectionArgs,
            "${MediaStore.Images.Media.DATE_MODIFIED} ASC"
        )
        total += imageCursor?.count ?: 0

        // 查询视频
        val videoSelection = """
                ${MediaStore.Video.Media.DATE_MODIFIED} > ? AND 
                ${MediaStore.Video.Media.BUCKET_ID} IN $bucketIdsPlaceholder
            """.trimIndent()
        val videoCursor = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoQueryColumns, videoSelection, selectionArgs,
            "${MediaStore.Video.Media.DATE_MODIFIED} ASC"
        )
        total += videoCursor?.count ?: 0

        if (total == 0) {
            return
        }

        // 遍历上传
        imageCursor?.use { iterateUploadImage(it, enabledSyncFavorite) }
        videoCursor?.use { iterateUploadVideo(it, enabledSyncFavorite) }
    }

    /**
     * 迭代上传图片
     */
    private suspend fun iterateUploadImage(cursor: Cursor, enabledSyncFavorite: Boolean) {
        while (cursor.moveToNext()) {
            val mediaItem = imageCursorConvertToItem(cursor, enabledSyncFavorite)
            uploadMediaItem(mediaItem)
        }
    }

    /**
     * 迭代上传图片
     */
    private suspend fun iterateUploadVideo(cursor: Cursor, enabledSyncFavorite: Boolean) {
        while (cursor.moveToNext()) {
            val mediaItem = videoCursorConvertToItem(cursor, enabledSyncFavorite)
            uploadMediaItem(mediaItem)
        }
    }

    /**
     * 图片游标转同步图片项
     */
    private fun imageCursorConvertToItem(cursor: Cursor, enabledSyncFavorite: Boolean): MediaSyncItem {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
        val favorite = if (enabledSyncFavorite) {
            cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.IS_FAVORITE))
        } else {
            0
        }
        return MediaSyncItem(
            contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id).toString(),
            type = "image",
            name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)),
            size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)),
            dateToken = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)),
            dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)),
            lastModified = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)),
            duration = 0,
            favorite = favorite,
        )
    }

    /**
     * 视频游标转同步图片项
     */
    private fun videoCursorConvertToItem(cursor: Cursor, enabledSyncFavorite: Boolean): MediaSyncItem {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
        val favorite = if (enabledSyncFavorite) {
            cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.IS_FAVORITE))
        } else {
            0
        }
        return MediaSyncItem(
            contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id).toString(),
            type = "video",
            name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)),
            size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)),
            dateToken = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN)),
            dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)),
            lastModified = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)),
            duration = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)),
            favorite = favorite,
        )
    }

    /**
     * 上传媒体文件
     */
    suspend fun uploadMediaItem(item: MediaSyncItem) {
        val fileUri = item.contentUri.toUri()

        // 根据文件大小，判断是否要进行切片上传，大于 CHUNK_THRESHOLD 使用分片上传
        if (CHUNK_THRESHOLD > item.size) {
            // 直接上传
            val fileByteArray = openFileAndSkip(fileUri, 0)?.use { stream ->
                IOUtils.toByteArray(stream)
            } ?: throw UploadException("文件打开失败")

            val filePart = MultipartBody.Part.createFormData(
                name = "file", // 对应后端接收文件的参数名
                filename = item.name, // 文件名（后端可获取）
                body = fileByteArray.toRequestBody(MEDIA_TYPE)
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
                file = filePart
            )
            if (!isRetOk(ret)) throw UploadException(retMsg(ret))

        } else {
            // 分片上传
            // 步骤1、生成 fileId、chunkSize、totalSize、totalChunks 参数
            val fileName = item.name
            val totalSize = item.size
            val totalChunks = (totalSize + CHUNK_SIZE - 1) / CHUNK_SIZE
            val fileId = "${fileName}-${totalSize}-${item.lastModified}"

            // 步骤2、检查这个文件有哪些分片已经上传到服务端了，支持断点续传
            val latestChunkIndex = getServerLatestChunkIndex(fileId, totalSize, totalChunks)

            // 步骤3：上传文件
            uploadUseChunk(fileUri, fileId, fileName, totalChunks, latestChunkIndex) {
                val progress = it.toFloat() / totalSize
            }

            // 步骤4、通知服务端合并分片
            noticeMergeChunks(fileId, totalChunks, item)

            // 步骤5、轮训合并状态
            withTimeout(WAIT_POLL_TIMEOUT) {
                pollMergeResult(fileId) {
                }
            }
        }
    }

    /**
     * 获取已上传的最大分片索引
     */
    private suspend fun getServerLatestChunkIndex(fileId: String, totalSize: Long, totalChunks: Int): Int {
        return retrofitClient.getApiService().checkChunks(
            CheckChunkRequest(
                fileId = fileId,
                chunkSize = CHUNK_SIZE.toLong(),
                totalSize = totalSize,
                totalChunks = totalChunks,
            )
        ).data?.maxChunkIndex ?: 0
    }

    /**
     * 上传媒体文件（分片上传）
     */
    private suspend fun uploadUseChunk(
        mediaUri: Uri, fileId: String, fileName: String, totalChunks: Int, latestChunkIndex: Int,
        onChunkUploadFinished: (uploadSize: Long) -> Unit
    ) {
        if (latestChunkIndex == totalChunks) return

        // 计算跳过的字节数
        val skipBytes = latestChunkIndex * CHUNK_SIZE.toLong()
        // 分块索引
        var chunkIndex = latestChunkIndex

        // 打开文件流，进行上传
        openFileAndSkip(mediaUri, skipBytes)?.use { stream ->
            val buffer = ByteArray(CHUNK_SIZE)
            var totalBytesRead = skipBytes
            var bytesRead: Int
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                // 处理当前块
                val filePart = MultipartBody.Part.createFormData(
                    name = "chunk", // 对应后端接收文件的参数名
                    filename = fileName, // 文件名（后端可获取）
                    body = buffer.toRequestBody(MEDIA_TYPE, 0, bytesRead)
                )
                val apiRet = retrofitClient.getApiService().uploadChunk(fileId, chunkIndex, filePart)
                if (isRetOk(apiRet)) {
                    chunkIndex++
                    totalBytesRead += bytesRead
                    onChunkUploadFinished(totalBytesRead)

                } else {
                    throw UploadException("上传分片异常")
                }
            }
        } ?: throw UploadException("打开文件失败")
    }

    /**
     * 通知服务端进行分片合并
     */
    private suspend fun noticeMergeChunks(
        fileId: String, totalChunks: Int, item: MediaSyncItem,
    ) {
        retrofitClient.getApiService().notifyMergeChunks(
            MergeChunkRequest(
                type = item.type,
                name = item.name,
                size = item.size,
                duration = item.duration,
                dateToken = item.dateToken,
                dateAdded = item.dateAdded,
                lastModified = item.lastModified,
                favorite = item.favorite,
                fileId = fileId,
                chunkSize = CHUNK_SIZE.toLong(),
                totalChunks = totalChunks,
            )
        )
    }

    /**
     * 轮训等待服务端合并完成
     */
    private suspend fun pollMergeResult(
        fileId: String,
        onProgress: (progress: Float) -> Unit = {},
    ) {
        var failCount = 0
        while (true) {
            if (failCount >= 3) {
                throw UploadException("合并异常次数过多，取消合并")
            }

            delay(POLL_INTERVAL)
            ensureActive()

            // 轮训合并结果
            runCatching {
                retrofitClient.getApiService().pollMergeResult(MergeResultRequest(fileId))
            }.getOrNull()?.data?.let {
                // 合并中，更新进度
                if (it.status == "MERGING") {
                    onProgress(it.progress)
                    continue
                }

                // 合并成功
                if (it.status == "SUCCESS") {
                    break
                }

                // 合并失败
                if (it.status == "FAILED") {
                    throw UploadException("合并失败：${it.error}")
                }
            }

            failCount++
        }
    }

    /**
     * 打开文件输入流，并跳过指定字节数；若跳过失败，重新打开文件（不跳过，从头开始）
     * @param fileUri 文件的 Uri
     * @param skipBytes 需要跳过的字节数（若为 0，则直接返回流）
     * @return 处理后的 InputStream（可能已跳过指定字节，或从头开始的新流；失败返回 null）
     */
    private fun openFileAndSkip(fileUri: Uri, skipBytes: Long): InputStream? {
        // 若无需跳过，直接打开流返回
        if (skipBytes <= 0) {
            return context.contentResolver.openInputStream(fileUri)
        }

        // 第一次尝试：打开流并跳过指定字节
        var inputStream: InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(fileUri)
            val inputStream = inputStream ?: return null // 流打开失败，直接返回 null
            val actualSkipped = inputStream.skip(skipBytes)
            if (actualSkipped == skipBytes) {
                // 跳过成功，返回当前流
                return inputStream
            }

            // 跳过失败，关闭当前流，准备重新打开
            Log.w(TAG, "跳过字节数不符：实际跳过 $actualSkipped，预期 $skipBytes，将重新打开文件")
            inputStream.close()
        } catch (e: Exception) {
            // 跳过过程中发生异常（如 IO 错误），关闭流
            Log.e(TAG, "跳过字节时发生错误：${e.message}")
            inputStream?.close()
        }

        // 第二次尝试：重新打开文件，不跳过（从头开始）
        return context.contentResolver.openInputStream(fileUri)
    }
}