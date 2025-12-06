package com.f1tracker

import android.app.Application
import coil.ImageLoaderFactory
import coil.ImageLoader
import com.f1tracker.data.api.RetrofitClient // Added import
import com.f1tracker.ui.util.ExoPlayerPool
import com.f1tracker.ui.util.F1ImageLoader
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class F1Application : Application(), ImageLoaderFactory {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Core Services
        RetrofitClient.initialize(this) // Initialize HTTP Cache
        ExoPlayerPool.initialize(this)
    }
    
    override fun newImageLoader(): ImageLoader {
        // Return optimized Coil ImageLoader
        return F1ImageLoader.get(this)
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        // Clear image memory cache on low memory
        F1ImageLoader.clearMemoryCache()
    }
    
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_MODERATE) {
            F1ImageLoader.clearMemoryCache()
        }
    }
}
