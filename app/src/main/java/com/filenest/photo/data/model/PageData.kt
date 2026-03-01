package com.filenest.photo.data.model

data class PageData<T>(
    val total: Int = 0,
    val size: Int = 0,
    val current: Int = 0,
    val pages: Int = 0,
    val records: List<T> = emptyList(),
)
