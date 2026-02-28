package com.filenest.photo.data.model

data class MediaSyncItem(
    val contentUri: String,
    val type: String,
    val name: String,
    val size: Long,
    val dateToken: Long,
    val dateAdded: Long,
    val dateModified: Long,
    val duration: Int,
    val favorite: Int,
    val generationAdded: Long,
    val generationModified: Long,
)
