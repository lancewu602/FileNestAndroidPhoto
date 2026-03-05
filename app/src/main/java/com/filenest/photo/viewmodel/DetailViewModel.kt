package com.filenest.photo.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.filenest.photo.data.api.RetrofitClient
import com.filenest.photo.data.api.isRetOk
import com.filenest.photo.data.model.MediaDetailItem
import com.filenest.photo.data.model.MediaListItem
import com.filenest.photo.data.uistate.DetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val retrofitClient: RetrofitClient,
) : ViewModel() {

    private var mediaId: Int = 0

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    val exoPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
        }
    }

    private var currentVideoUrl: String? = null

    init {
        viewModelScope.launch {
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    _currentPosition.value = exoPlayer.currentPosition
                    _duration.value = exoPlayer.duration.coerceAtLeast(0)
                }
                delay(500)
            }
        }
    }

    fun setInitialData(id: Int, jsonData: String) {
        this.mediaId = id
        if (jsonData.isNotEmpty()) {
            try {
                val mediaListItem = Json.decodeFromString<MediaListItem>(jsonData)
                val mediaDetail = mediaListItem.toMediaDetailItem()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    mediaDetail = mediaDetail
                )
            } catch (e: Exception) {
                loadMediaDetail()
            }
        } else {
            loadMediaDetail()
        }
    }

    private fun MediaListItem.toMediaDetailItem(): MediaDetailItem {
        return MediaDetailItem(
            id = id,
            type = type,
            name = name,
            width = 0,
            height = 0,
            duration = duration,
            durationText = durationText,
            originalPath = originalPath,
            previewPath = previewPath,
            favorite = false,
            inAlbumIds = emptyList()
        )
    }

    private fun loadMediaDetail() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = retrofitClient.getApiService().fetchMedia(mediaId)
                if (isRetOk(response)) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        mediaDetail = response.data
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = response.message
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "加载失败"
                )
            }
        }
    }

    fun setVideoUrl(url: String) {
        if (currentVideoUrl != url) {
            currentVideoUrl = url
            val mediaItem = MediaItem.fromUri(url)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        }
    }

    fun play() {
        _isPlaying.value = true
        exoPlayer.play()
    }

    fun pause() {
        _isPlaying.value = false
        exoPlayer.pause()
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer.release()
    }
}