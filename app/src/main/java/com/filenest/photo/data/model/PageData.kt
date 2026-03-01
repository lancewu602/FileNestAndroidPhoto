package com.filenest.photo.data.model

data class PageData<T>(
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val list: List<T> = emptyList(),
)
