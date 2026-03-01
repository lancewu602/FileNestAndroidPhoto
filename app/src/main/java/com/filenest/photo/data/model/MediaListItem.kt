package com.filenest.photo.data.model

data class MediaListItem(
    val id: Int,
    val type: String,
    val duration: Int = 0,
    val durationText: String = "",
    val sortDate: String = "",
    val sortTime: String = "",
    val thumbnail: String = "",
)
