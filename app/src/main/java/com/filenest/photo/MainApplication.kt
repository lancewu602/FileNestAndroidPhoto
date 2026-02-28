package com.filenest.photo

import android.app.Application
import com.filenest.photo.data.usecase.ApplicationUseCase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MainApplication : Application() {

    @Inject
    lateinit var applicationUseCase: ApplicationUseCase

    override fun onCreate() {
        super.onCreate()

        CoroutineScope(Dispatchers.IO).launch {
            applicationUseCase.initMediaStoreVersion();
        }
    }
}