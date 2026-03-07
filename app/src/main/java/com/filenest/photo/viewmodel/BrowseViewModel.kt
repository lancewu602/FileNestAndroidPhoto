package com.filenest.photo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import com.filenest.photo.data.api.RetrofitClient
import com.filenest.photo.data.model.GalleryItem
import com.filenest.photo.data.model.MediaDetail
import com.filenest.photo.data.paging.MediaPagingSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val retrofitClient: RetrofitClient,
) : ViewModel() {

    val mediaList: Flow<PagingData<MediaDetail>> = Pager(
        config = PagingConfig(
            pageSize = 50,
            enablePlaceholders = false,
            prefetchDistance = 100,
        ),
        pagingSourceFactory = {
            MediaPagingSource(
                apiService = retrofitClient.getApiService(),
                albumId = null
            )
        }
    ).flow.cachedIn(viewModelScope)

    val groupedMediaList: Flow<PagingData<GalleryItem>> = mediaList.map { pagingData ->
        pagingData.map { media -> GalleryItem.MediaItem(media) }.insertSeparators { before: GalleryItem?, after: GalleryItem? ->
            val beforeDate = (before as? GalleryItem.MediaItem)?.media?.sortDate
            val afterDate = (after as? GalleryItem.MediaItem)?.media?.sortDate
            when {
                before == null && after != null -> {
                    if (!afterDate.isNullOrEmpty()) GalleryItem.DateHeader(afterDate) else null
                }
                after != null && beforeDate != afterDate -> {
                    if (!afterDate.isNullOrEmpty()) GalleryItem.DateHeader(afterDate) else null
                }
                else -> null
            }
        }
    }.cachedIn(viewModelScope)
}