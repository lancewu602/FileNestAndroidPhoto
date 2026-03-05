package com.filenest.photo.data.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.filenest.photo.data.api.ApiService
import com.filenest.photo.data.api.isRetOk
import com.filenest.photo.data.model.MediaDetail

private const val TAG = "MediaPagingSource"

class MediaPagingSource(
    private val apiService: ApiService,
    private val albumId: Int?,
) : PagingSource<Int, MediaDetail>() {

    override fun getRefreshKey(state: PagingState<Int, MediaDetail>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaDetail> {
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
                LoadResult.Page(
                    data = data.records,
                    prevKey = if (page == 1) null else page - 1,
                    nextKey = if (page < data.pages) page + 1 else null
                )
            } else {
                LoadResult.Error(Exception("code=${ret.code}, msg=${ret.message}, data=${ret.data}"))
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}