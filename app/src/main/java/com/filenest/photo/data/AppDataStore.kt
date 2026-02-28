package com.filenest.photo.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.dataStore by preferencesDataStore(name = "app_prefs_001")

object AppPrefKeys {
    // 用户名
    val USERNAME = stringPreferencesKey("username")

    // 登录Token
    val SERVER_TOKEN = stringPreferencesKey("server_token")

    // 服务器地址
    val SERVER_URL = stringPreferencesKey("server_url")

    // 上次同步时间
    val LATEST_SYNC_TIME = longPreferencesKey("latest_sync_time")

    // 已选择的相册 bucketId 列表（JSON 格式）
    val SELECTED_ALBUMS = stringPreferencesKey("selected_albums")

    // MediaStore 版本，后续可以存储到云端，避免App卸载后丢失
    val MEDIA_STORE_VERSION = stringPreferencesKey("media_store_version")

    // 媒体文件在数据库中的“修改代数”，后续可以存储到云端，避免App卸载后丢失
    val MEDIA_STORE_LAST_GEN = longPreferencesKey("media_store_last_gen")

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

    suspend fun setLatestSyncTime(context: Context, time: Long) {
        context.dataStore.edit { settings ->
            settings[LATEST_SYNC_TIME] = time
        }
    }

    fun getLatestSyncTime(context: Context): Flow<Long> {
        return context.dataStore.data.map { settings ->
            settings[LATEST_SYNC_TIME] ?: 0L
        }
    }

    fun getSelectedAlbums(context: Context): Flow<Set<Long>> {
        return context.dataStore.data.map { settings ->
            val jsonString = settings[SELECTED_ALBUMS] ?: return@map emptySet()
            try {
                Json.decodeFromString<List<Long>>(jsonString).toSet()
            } catch (e: Exception) {
                emptySet()
            }
        }
    }

    suspend fun setSelectedAlbums(context: Context, albums: Set<Long>) {
        context.dataStore.edit { settings ->
            settings[SELECTED_ALBUMS] = Json.encodeToString(albums.toList())
        }
    }

    fun getMediaStoreVersion(context: Context): Flow<String> {
        return context.dataStore.data.map { settings ->
            settings[MEDIA_STORE_VERSION] ?: ""
        }
    }

    suspend fun setMediaStoreVersion(context: Context, version: String) {
        context.dataStore.edit { settings ->
            settings[MEDIA_STORE_VERSION] = version
        }
    }

    fun getMediaStoreLastGen(context: Context): Flow<Long> {
        return context.dataStore.data.map { settings ->
            settings[MEDIA_STORE_LAST_GEN] ?: 0L
        }
    }

    suspend fun setMediaStoreLastGen(context: Context, gen: Long) {
        context.dataStore.edit { settings ->
            settings[MEDIA_STORE_LAST_GEN] = gen
        }
    }

}