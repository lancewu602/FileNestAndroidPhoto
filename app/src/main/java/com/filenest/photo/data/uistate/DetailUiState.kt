package com.filenest.photo.data.uistate

import com.filenest.photo.data.model.MediaDetailItem

data class DetailUiState(
    val isLoading: Boolean = true,
    val mediaDetail: MediaDetailItem? = null,
    val error: String? = null,
)