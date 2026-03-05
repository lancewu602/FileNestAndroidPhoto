package com.filenest.photo.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MediaListItem(
    val id: Int,
    val type: String,
    val name: String = "",
    val duration: Int = 0,
    val durationText: String = "",
    val sortDate: String = "",
    val sortTime: String = "",

    val thumbnailPath: String = "",
    val previewPath: String = "",
    val originalPath: String = "",
)
