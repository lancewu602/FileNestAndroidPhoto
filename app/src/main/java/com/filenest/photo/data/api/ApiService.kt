package com.filenest.photo.data.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface ApiService {

    // 用户登录
    @POST("user/login")
    suspend fun login(
        @Body request: LoginRequest,
    ): Ret<String>

}

// 创建 Retrofit 实例
val retrofit: Retrofit = Retrofit.Builder()
    .baseUrl("https://api.example.com")
    .client(OkHttpClient.Builder().build())
    .addConverterFactory(GsonConverterFactory.create())
    .build()

var apiService: ApiService = retrofit.create(ApiService::class.java)
const val urlPrefix = "/api/photos/backend/"
var serverDomain = ""
var serverToken = ""

fun resetApiService(newServerDomain: String, newServerToken: String) {
    if (newServerDomain.isEmpty()) throw IllegalArgumentException("ServerDomain is empty")

    serverDomain = newServerDomain
    serverToken = newServerToken

    apiService = Retrofit.Builder()
        .baseUrl(serverDomain + urlPrefix)
        .client(
            OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("file-nest-token", serverToken)
                        .build()
                    return@addInterceptor chain.proceed(request)
                }
//                .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .callTimeout(30, TimeUnit.SECONDS)
                .build()
        )
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)
}