package com.filenest.photo.data.uistate

import com.filenest.photo.data.model.MediaDetail

data class DetailUiState(
    val isLoading: Boolean = true,
    val mediaDetail: MediaDetail? = null,
    val error: String? = null,
)