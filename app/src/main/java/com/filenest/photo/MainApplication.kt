package com.filenest.photo

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.allowConversionToBitmap
import coil3.request.allowHardware
import coil3.request.allowRgb565
import coil3.request.crossfade
import com.filenest.photo.data.usecase.ApplicationUseCase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MainApplication : Application(), SingletonImageLoader.Factory {

    @Inject
    lateinit var applicationUseCase: ApplicationUseCase

    override fun onCreate() {
        super.onCreate()

        CoroutineScope(Dispatchers.IO).launch {
            applicationUseCase.initMediaStoreVersion()
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            // 缓存策略优化
            .networkCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            // 内存缓存优化
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizeBytes(200 * 1024 * 1024)
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            // 磁盘缓存优化
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("media_cache_002"))
                    .maxSizeBytes(200 * 1024 * 1024)
                    .maxSizePercent(0.02)
                    .build()
            }
            .allowConversionToBitmap(false)
            .allowRgb565(false)
            .allowHardware(true)
            .crossfade(true)
            .build()
    }
}