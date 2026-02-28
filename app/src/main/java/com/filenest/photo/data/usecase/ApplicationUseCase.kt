package com.filenest.photo.data.usecase

import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.filenest.photo.data.AppPrefKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApplicationUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    suspend fun initMediaStoreVersion() {
        if (AppPrefKeys.getMediaStoreVersion(context).first().isNotEmpty()) {
            return
        }
        val version = MediaStore.getVersion(context)
        AppPrefKeys.setMediaStoreVersion(context, version)

        val generation = MediaStore.getGeneration(context, MediaStore.VOLUME_EXTERNAL_PRIMARY)
        AppPrefKeys.setMediaStoreLastGen(context, generation)

        Log.i("MediaStore", "Init mediaStore version: $version, generation: $generation")
    }

}