package com.filenest.photo.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MediaDetail(
    val id: Int = 0,
    val type: String = "", // IMAGE | VIDEO

    val name: String = "",
    val width: Int = 0,
    val height: Int = 0,

    val sortDate: String = "",
    val sortTime: String = "",

    val duration: Int = 0,
    val durationText: String = "",

    val thumbnailPath: String = "",
    val originalPath: String = "",
    val previewPath: String = "",

    val favorite: Boolean = false,
)
