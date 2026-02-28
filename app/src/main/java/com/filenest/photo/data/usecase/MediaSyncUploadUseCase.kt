package com.filenest.photo.data.usecase

import android.content.Context
import android.util.Log
import com.filenest.photo.data.model.MediaSyncItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaSyncUploadUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    companion object {
        const val TAG = "MediaSyncUploadUseCase"
    }

    suspend fun uploadMedia(mediaSyncItem: MediaSyncItem) {
        Log.i(TAG, "uploadMedia: $mediaSyncItem")
        delay(10)
    }

}