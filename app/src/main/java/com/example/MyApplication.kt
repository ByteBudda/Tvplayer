package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.OkHttpClient
import java.io.File

class MyApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024L * 1024L) // 50MB disk cache for images
                    .build()
            }
            .okHttpClient {
                OkHttpClient.Builder()
                    .cache(
                        okhttp3.Cache(
                            directory = File(this.cacheDir, "http_cache"),
                            maxSize = 50L * 1024L * 1024L // 50MB HTTP cache
                        )
                    )
                    .addNetworkInterceptor { chain ->
                        val request = chain.request()
                        val response = chain.proceed(request)
                        val urlStr = request.url.toString()
                        
                        // Detect image files or names indicating icon/logo
                        val isImage = urlStr.contains(".png", ignoreCase = true) ||
                                      urlStr.contains(".jpg", ignoreCase = true) ||
                                      urlStr.contains(".jpeg", ignoreCase = true) ||
                                      urlStr.contains(".webp", ignoreCase = true) ||
                                      urlStr.contains("logo", ignoreCase = true) ||
                                      urlStr.contains("icon", ignoreCase = true)
                        
                        if (isImage) {
                            // Override cache-control headers on the fly to force-cache logos
                            response.newBuilder()
                                .removeHeader("Pragma")
                                .removeHeader("Cache-Control")
                                .header("Cache-Control", "public, max-age=864000") // 10 days cache
                                .build()
                        } else {
                            response
                        }
                    }
                    .build()
            }
            .respectCacheHeaders(false) // Cache images even if cache-control is set to no-store/no-cache on some hosts
            .build()
    }
}
