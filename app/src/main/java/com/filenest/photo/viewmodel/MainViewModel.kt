package com.filenest.photo.viewmodel

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filenest.photo.data.AlbumData
import com.filenest.photo.data.AppPrefKeys
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _selectedAlbums = MutableStateFlow<Set<Long>>(emptySet())
    val selectedAlbums: StateFlow<Set<Long>> = _selectedAlbums.asStateFlow()

    private val _albums = MutableStateFlow<List<AlbumData>>(emptyList())
    val albums: StateFlow<List<AlbumData>> = _albums.asStateFlow()

    init {
        viewModelScope.launch {
            AppPrefKeys.getServerToken(context).collect { token ->
                _isLoggedIn.value = token.isNotBlank()
            }
        }
        viewModelScope.launch {
            AppPrefKeys.getSelectedAlbums(context).collect { _selectedAlbums.value = it }
        }
    }

    fun login(serverUrl: String, username: String, password: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                val mockToken = "mock_token_${java.util.UUID.randomUUID()}"
                AppPrefKeys.setServerUrl(context, serverUrl)
                AppPrefKeys.setUsername(context, username)
                AppPrefKeys.setServerToken(context, mockToken)
                _isLoggedIn.value = true
                onComplete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                AppPrefKeys.setServerUrl(context, "")
                AppPrefKeys.setUsername(context, "")
                AppPrefKeys.setServerToken(context, "")
                _isLoggedIn.value = false
                onComplete()
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete()
            }
        }
    }

    fun loadAlbums() {
        viewModelScope.launch {
            val albums = try {
                val albumMap = mutableMapOf<Long, Pair<String, Int>>()

                fun queryMedia(storeUri: android.net.Uri, bucketIdCol: String, bucketNameCol: String) {
                    val projection = arrayOf(bucketIdCol, bucketNameCol)
                    context.contentResolver.query(
                        storeUri,
                        projection,
                        null,
                        null,
                        null
                    )?.use { cursor ->
                        val bucketIdColumn = cursor.getColumnIndexOrThrow(bucketIdCol)
                        val bucketNameColumn = cursor.getColumnIndexOrThrow(bucketNameCol)

                        while (cursor.moveToNext()) {
                            val bucketId = cursor.getLong(bucketIdColumn)
                            val bucketName = cursor.getString(bucketNameColumn)
                            val current = albumMap[bucketId]
                            if (current != null) {
                                albumMap[bucketId] = current.copy(second = current.second + 1)
                            } else {
                                albumMap[bucketId] = bucketName to 1
                            }
                        }
                    }
                }

                queryMedia(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    MediaStore.Images.Media.BUCKET_ID,
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME
                )
                queryMedia(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    MediaStore.Video.Media.BUCKET_ID,
                    MediaStore.Video.Media.BUCKET_DISPLAY_NAME
                )

                albumMap.map { (bucketId, bucketNameAndCount) ->
                    AlbumData(bucketId, bucketNameAndCount.first, bucketNameAndCount.second)
                }.sortedBy { it.bucketName }
            } catch (e: Exception) {
                emptyList()
            }
            _albums.value = albums
        }
    }

    fun toggleAlbum(bucketId: Long) {
        viewModelScope.launch {
            val current = _selectedAlbums.value.toMutableSet()
            if (current.contains(bucketId)) {
                current.remove(bucketId)
            } else {
                current.add(bucketId)
            }
            _selectedAlbums.value = current
            AppPrefKeys.setSelectedAlbums(context, current)
        }
    }

    fun getServerUrl() = AppPrefKeys.getServerUrl(context)

    fun getUsername() = AppPrefKeys.getUsername(context)
}