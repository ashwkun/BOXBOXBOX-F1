package com.f1tracker.ui.util

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger

/**
 * Optimized ImageLoader configuration for F1 content.
 * 
 * Features:
 * - 25% of app memory for memory cache
 * - 100MB disk cache for images
 * - Crossfade animations for smooth loading
 * - Aggressive caching to reduce network usage
 */
object F1ImageLoader {
    
    private var imageLoader: ImageLoader? = null
    
    /**
     * Get or create the singleton ImageLoader instance.
     * 
     * @param context Application context
     * @return Configured ImageLoader
     */
    fun get(context: Context): ImageLoader {
        return imageLoader ?: synchronized(this) {
            imageLoader ?: createImageLoader(context.applicationContext).also {
                imageLoader = it
            }
        }
    }
    
    /**
     * Create a new ImageLoader with optimized settings.
     */
    private fun createImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            // Memory cache: 25% of available memory
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25)
                    .build()
            }
            // Disk cache: 100MB
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024) // 100MB
                    .build()
            }
            // Cache policies
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            // Smooth transitions
            .crossfade(true)
            .crossfade(200) // 200ms crossfade duration
            // Respect cache headers from server
            .respectCacheHeaders(true)
            .build()
    }
    
    /**
     * Clear memory cache (useful for low memory situations).
     */
    fun clearMemoryCache() {
        imageLoader?.memoryCache?.clear()
    }
    
    /**
     * Remove a specific image from cache.
     */
    fun evict(key: String) {
        imageLoader?.memoryCache?.remove(MemoryCache.Key(key))
    }
}
