package com.filenest.photo.data.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.filenest.photo.data.api.ApiService
import com.filenest.photo.data.api.isRetOk
import com.filenest.photo.data.model.MediaListItem

private const val TAG = "MediaPagingSource"

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

            Log.d(TAG, "page=$page, ret=$ret")
            if (isRetOk(ret) && ret.data != null) {
                val data = ret.data
                Log.d(TAG, "records size=${data.records.size}, current=${data.current}, pages=${data.pages}")
                LoadResult.Page(
                    data = data.records,
                    prevKey = if (page == 1) null else page - 1,
                    nextKey = if (page < data.pages) page + 1 else null
                )
            } else {
                LoadResult.Error(Exception("code=${ret?.code}, msg=${ret?.message}, data=${ret?.data}"))
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}