package com.filenest.photo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.filenest.photo.data.api.RetrofitClient
import com.filenest.photo.data.model.MediaListItem
import com.filenest.photo.data.paging.MediaPagingSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val retrofitClient: RetrofitClient,
) : ViewModel() {

    val mediaList: Flow<PagingData<MediaListItem>> = Pager(
        config = PagingConfig(
            pageSize = 20,
            enablePlaceholders = false,
            prefetchDistance = 5,
        ),
        pagingSourceFactory = {
            MediaPagingSource(
                apiService = retrofitClient.getApiService(),
                albumId = null
            )
        }
    ).flow.cachedIn(viewModelScope)
}