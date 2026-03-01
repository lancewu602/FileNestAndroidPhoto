package com.filenest.photo.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.filenest.photo.data.api.ApiService
import com.filenest.photo.data.api.isRetOk
import com.filenest.photo.data.model.MediaListItem

class MediaPagingSource(
    private val apiService: ApiService,
    private val albumId: Int?,
) : PagingSource<Int, MediaListItem>() {

    override fun getRefreshKey(state: PagingState<Int, MediaListItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaListItem> {
        return try {
            val page = params.key ?: 1
            val ret = apiService.listMedia(
                albumId = albumId,
                pageNum = page,
                pageSize = params.loadSize
            )

            if (isRetOk(ret) && ret.data != null) {
                val data = ret.data
                LoadResult.Page(
                    data = data.list,
                    prevKey = if (page == 1) null else page - 1,
                    nextKey = if (data.hasNext) page + 1 else null
                )
            } else {
                LoadResult.Error(Exception(ret.message ?: "Unknown error"))
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}