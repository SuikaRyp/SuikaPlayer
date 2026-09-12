package com.demonlab.suikaplayer

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.demonlab.suikaplayer.tools.AudioThumbnailFetcher
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class SuikaPlayerApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        // A dedicated, tuned OkHttp client for image loading: a larger
        // connection pool means repeated cover-art requests (e.g. scrolling
        // through the online catalog) reuse warm connections to the CDN
        // instead of renegotiating TLS every time, which is a big chunk of
        // the perceived "delay" when loading many small images in a row.
        val imageHttpClient = OkHttpClient.Builder()
            .connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        return ImageLoader.Builder(this)
            .components {
                add(AudioThumbnailFetcher.Factory(this@SuikaPlayerApplication))
            }
            .okHttpClient(imageHttpClient)
            .crossfade(150)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil_cache"))
                    .maxSizeBytes(300L * 1024 * 1024)
                    .build()
            }
            .build()
    }
}
