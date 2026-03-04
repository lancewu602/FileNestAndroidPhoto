package com.filenest.photo.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.filenest.photo.data.api.RetrofitClient
import com.filenest.photo.data.api.isRetOk
import com.filenest.photo.data.uistate.DetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VideoPlayerState(
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val isDragging: Boolean = false
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val retrofitClient: RetrofitClient,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val mediaId: Int = savedStateHandle.get<Int>("mediaId") ?: 0

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val _videoPlayerState = MutableStateFlow(VideoPlayerState())
    val videoPlayerState: StateFlow<VideoPlayerState> = _videoPlayerState.asStateFlow()

    val exoPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
        }
    }

    private var currentVideoUrl: String? = null

    fun setVideoUrl(url: String) {
        if (currentVideoUrl != url) {
            currentVideoUrl = url
            val mediaItem = MediaItem.fromUri(url)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        }
    }

    fun play() {
        exoPlayer.play()
    }

    fun pause() {
        exoPlayer.pause()
    }

    fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
    }

    fun setDragging(dragging: Boolean) {
        _videoPlayerState.value = _videoPlayerState.value.copy(isDragging = dragging)
    }

    private fun startVideoStateObserver() {
        viewModelScope.launch {
            while (isActive) {
                if (!_videoPlayerState.value.isDragging) {
                    _videoPlayerState.value = VideoPlayerState(
                        isPlaying = exoPlayer.isPlaying,
                        currentPosition = exoPlayer.currentPosition,
                        duration = exoPlayer.duration.coerceAtLeast(0),
                        isDragging = false
                    )
                }
                delay(500)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer.release()
    }

    init {
        loadMediaDetail()
        startVideoStateObserver()
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
}