package com.filenest.photo.data.api

import android.content.Context
import com.filenest.photo.data.AppPrefKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetrofitClient @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private var retrofit: Retrofit? = null
    private var apiService: ApiService? = null

    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        OkHttpClient.Builder()
//            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun getApiService(): ApiService {
        if (apiService == null) {
            if (retrofit == null) {
                val baseUrl = getStoredServerUrl()
                retrofit = createRetrofit(baseUrl)
            }
            apiService = retrofit!!.create(ApiService::class.java)
        }
        return apiService!!
    }

    fun setServerUrl(serverUrl: String) {
        retrofit = createRetrofit(serverUrl)
        apiService = getApiService()
    }

    private fun getStoredServerUrl(): String {
        return runBlocking {
            AppPrefKeys.getServerUrl(context).first()
        }
    }

    private fun createRetrofit(baseUrl: String): Retrofit {
        val correctedUrl = if (!baseUrl.endsWith("/")) {
            "$baseUrl/"
        } else {
            baseUrl
        }

        return Retrofit.Builder()
            .baseUrl(correctedUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun resetToStoredUrl() {
        val storedUrl = getStoredServerUrl()
        if (storedUrl.isNotBlank()) {
            retrofit = createRetrofit(storedUrl)
            apiService = getApiService()
        }
    }
}