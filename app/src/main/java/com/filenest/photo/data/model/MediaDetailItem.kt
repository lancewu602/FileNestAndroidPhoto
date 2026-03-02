package com.filenest.photo.data.model

data class MediaDetailItem(
    val id: Int = 0,
    val type: String = "", // IMAGE | VIDEO

    val name: String = "",
    val width: Int = 0,
    val height: Int = 0,

    val duration: Int = 0,
    val durationText: String = "",

    val originalPath: String = "",
    val previewPath: String = "",

    val favorite: Boolean = false,

    val inAlbumIds: List<Int> = emptyList(),
)
