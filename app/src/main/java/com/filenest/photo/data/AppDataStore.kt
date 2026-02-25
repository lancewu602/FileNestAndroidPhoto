package com.filenest.photo.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "app_prefs_001")

object AppPrefKeys {
    // 用户名
    val USERNAME = stringPreferencesKey("username")

    // 登录Token
    val SERVER_TOKEN = stringPreferencesKey("server_token")

    // 服务器地址
    val SERVER_URL = stringPreferencesKey("server_url")

    suspend fun setUsername(context: Context, username: String) {
        context.dataStore.edit { settings ->
            settings[USERNAME] = username
        }
    }

    fun getUsername(context: Context): Flow<String> {
        return context.dataStore.data.map { settings ->
            settings[USERNAME] ?: ""
        }
    }

    suspend fun setServerToken(context: Context, serverToken: String) {
        context.dataStore.edit { settings ->
            settings[SERVER_TOKEN] = serverToken
        }
    }

    fun getServerToken(context: Context): Flow<String> {
        return context.dataStore.data.map { settings ->
            settings[SERVER_TOKEN] ?: ""
        }
    }

    suspend fun setServerUrl(context: Context, serverUrl: String) {
        context.dataStore.edit { settings ->
            settings[SERVER_URL] = serverUrl
        }
    }

    fun getServerUrl(context: Context): Flow<String> {
        return context.dataStore.data.map { settings ->
            settings[SERVER_URL] ?: ""
        }
    }

}