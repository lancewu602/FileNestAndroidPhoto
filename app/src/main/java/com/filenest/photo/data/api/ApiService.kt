package com.filenest.photo.data.api

import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("api/user/login")
    suspend fun login(@Body request: LoginRequest): Ret<String>
}

data class LoginRequest(
    val username: String,
    val password: String,
)