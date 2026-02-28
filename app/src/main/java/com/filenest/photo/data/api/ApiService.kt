package com.filenest.photo.data.api

import okhttp3.MultipartBody
import retrofit2.http.*

interface ApiService {

    @POST("api/user/login")
    suspend fun login(@Body request: LoginRequest): Ret<String>

    // 上传图片
    @Multipart
    @POST("/api/media/upload/direct")
    suspend fun uploadDirect(
        @Query("type") type: String,
        @Query("name") name: String,
        @Query("size") size: Long,
        @Query("dateToken") dateToken: Long,
        @Query("dateAdded") dateAdded: Long,
        @Query("lastModified") lastModified: Long,
        @Query("duration") duration: Int,
        @Query("favorite") favorite: Int,
        @Part file: MultipartBody.Part,
    ): Ret<*>

    // 检查分片上传进度，支持断点续传
    @POST("/api/media/upload/checkChunks")
    suspend fun checkChunks(@Body request: CheckChunkRequest): Ret<CheckChunkResp>

    // 上传文件
    @Multipart
    @POST("/api/media/upload/chunk")
    suspend fun uploadChunk(
        @Query("fileId") fileId: String,
        @Query("chunkIndex") chunkIndex: Int,
        @Part chunk: MultipartBody.Part,
    ): Ret<*>

    // 合并分片
    @POST("/api/media/upload/notifyMergeChunks")
    suspend fun notifyMergeChunks(@Body request: MergeChunkRequest): Ret<*>

    // 轮询合并结果
    @POST("/api/media/upload/pollMergeResult")
    suspend fun pollMergeResult(@Body request: MergeResultRequest): Ret<MergeResultResp?>
}

data class LoginRequest(
    val username: String,
    val password: String,
)

data class CheckChunkRequest(
    val fileId: String,
    val chunkSize: Int,
    val totalSize: Long,
    val totalChunks: Long,
)

data class MergeChunkRequest(
    val type: String,
    val name: String,
    val size: Long,
    val duration: Int,
    val dateToken: Long,
    val dateAdded: Long,
    val lastModified: Long,
    val favorite: Int,
    val fileId: String,
    val chunkSize: Int,
    val totalChunks: Long,
)

data class MergeResultRequest(
    val fileId: String,
)

data class CheckChunkResp(
    val maxChunkIndex: Int,
)

data class MergeResultResp(
    val status: String,
    val progress: Float,
    val error: String,
)