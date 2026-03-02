package com.filenest.photo.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filenest.photo.data.api.RetrofitClient
import com.filenest.photo.data.api.isRetOk
import com.filenest.photo.data.uistate.DetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val retrofitClient: RetrofitClient,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val mediaId: Int = savedStateHandle.get<Int>("mediaId") ?: 0

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        loadMediaDetail()
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