package com.filenest.photo.data.model

sealed class GalleryItem {
    data class DateHeader(val date: String) : GalleryItem()
    data class MediaItem(val media: MediaDetail) : GalleryItem()
}
