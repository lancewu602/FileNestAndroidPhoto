package com.filenest.photo.data.model

object MediaGrouper {
    fun groupMediaByDate(mediaList: List<MediaDetail>): List<GalleryItem> {
        val result = mutableListOf<GalleryItem>()
        var currentDate: String? = null

        for (media in mediaList) {
            if (media.sortDate != currentDate) {
                currentDate = media.sortDate
                if (currentDate.isNotEmpty()) {
                    result.add(GalleryItem.DateHeader(currentDate))
                }
            }
            result.add(GalleryItem.MediaItem(media))
        }
        return result
    }
}
