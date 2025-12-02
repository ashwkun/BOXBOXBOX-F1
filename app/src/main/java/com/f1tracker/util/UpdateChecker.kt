package com.f1tracker.util

import android.content.Context
import android.content.pm.PackageManager
import com.f1tracker.data.api.GitHubApiService
import com.f1tracker.data.models.GitHubRelease
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateChecker @Inject constructor(
    private val gitHubApiService: GitHubApiService,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "update_checker"
        private const val KEY_LAST_CHECK = "last_check_time"
        private const val KEY_CACHED_RELEASE_TAG = "cached_release_tag"
        private const val KEY_CACHED_RELEASE_BODY = "cached_release_body"
        private const val KEY_CACHED_DOWNLOAD_URL = "cached_download_url"
        private const val CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    suspend fun checkForUpdate(): UpdateStatus = withContext(Dispatchers.IO) {
        try {
            val currentVersion = getCurrentVersion()
            val lastCheck = prefs.getLong(KEY_LAST_CHECK, 0)
            val now = System.currentTimeMillis()
            
            // 1. Check if we have a cached update that is still valid (newer than current)
            val cachedTag = prefs.getString(KEY_CACHED_RELEASE_TAG, null)
            if (cachedTag != null) {
                val cachedVersion = parseVersion(cachedTag)
                if (cachedVersion > currentVersion) {
                    // We have a known update. Return it immediately.
                    // We still might want to refresh if cache is old, but for UI purposes, we know an update exists.
                    // If cache is expired, we'll try to refresh in background, but for now return available.
                    
                    if (now - lastCheck < CHECK_INTERVAL_MS) {
                         return@withContext createCachedUpdateStatus()
                    }
                } else {
                    // Cached version is same or older (user updated). Clear cache.
                    clearCachedRelease()
                }
            }

            // 2. If we are here, either no cache, cache is old, or cache is invalid.
            // Check rate limit for API call
            val isDebug = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
            android.util.Log.d("UpdateChecker", "Checking for updates... Last check: $lastCheck, Now: $now, Interval: $CHECK_INTERVAL_MS, Debug: $isDebug")
            
            if (!isDebug && now - lastCheck < CHECK_INTERVAL_MS) {
                android.util.Log.d("UpdateChecker", "Skipping update check due to rate limit")
                return@withContext UpdateStatus.NoUpdateAvailable
            }

            // 3. Fetch from API
            val latestRelease = gitHubApiService.getLatestRelease()
            prefs.edit().putLong(KEY_LAST_CHECK, now).apply()

            val latestVersion = parseVersion(latestRelease.tagName)
            android.util.Log.d("UpdateChecker", "Current version: $currentVersion, Latest version: $latestVersion (tag: ${latestRelease.tagName})")

            if (latestVersion > currentVersion) {
                val apkAsset = latestRelease.assets.find { 
                    it.name.endsWith(".apk", ignoreCase = true) 
                }
                
                if (apkAsset != null) {
                    // Cache the new release
                    cacheRelease(latestRelease, apkAsset.browserDownloadUrl)
                    
                    UpdateStatus.UpdateAvailable(
                        release = latestRelease,
                        downloadUrl = apkAsset.browserDownloadUrl
                    )
                } else {
                    android.util.Log.w("UpdateChecker", "Update available but no APK asset found")
                    UpdateStatus.NoUpdateAvailable
                }
            } else {
                UpdateStatus.NoUpdateAvailable
            }
        } catch (e: Exception) {
            // If API fails but we have a cached update, return that
            val cachedTag = prefs.getString(KEY_CACHED_RELEASE_TAG, null)
            if (cachedTag != null) {
                 val cachedVersion = parseVersion(cachedTag)
                 if (cachedVersion > getCurrentVersion()) {
                     return@withContext createCachedUpdateStatus()
                 }
            }
            UpdateStatus.Error(e.message ?: "Failed to check for updates")
        }
    }

    private fun cacheRelease(release: GitHubRelease, downloadUrl: String) {
        prefs.edit()
            .putString(KEY_CACHED_RELEASE_TAG, release.tagName)
            .putString(KEY_CACHED_RELEASE_BODY, release.body)
            .putString(KEY_CACHED_DOWNLOAD_URL, downloadUrl)
            .apply()
    }

    private fun clearCachedRelease() {
        prefs.edit()
            .remove(KEY_CACHED_RELEASE_TAG)
            .remove(KEY_CACHED_RELEASE_BODY)
            .remove(KEY_CACHED_DOWNLOAD_URL)
            .apply()
    }

    private fun createCachedUpdateStatus(): UpdateStatus {
        val tag = prefs.getString(KEY_CACHED_RELEASE_TAG, "") ?: ""
        val body = prefs.getString(KEY_CACHED_RELEASE_BODY, "") ?: ""
        val url = prefs.getString(KEY_CACHED_DOWNLOAD_URL, "") ?: ""
        
        return UpdateStatus.UpdateAvailable(
            release = GitHubRelease(tag, tag, body, "", emptyList()), // Reconstruct minimal object
            downloadUrl = url,
            shouldShowDialog = false // Don't show dialog for cached updates (header only)
        )
    }

    fun resetLastCheck() {
        prefs.edit().remove(KEY_LAST_CHECK).apply()
    }

    private fun getCurrentVersion(): Int {
        return try {
            val versionName = context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionName
            parseVersion(versionName)
        } catch (e: PackageManager.NameNotFoundException) {
            0
        }
    }

    private fun parseVersion(versionString: String): Int {
        val cleanVersion = versionString.removePrefix("v").trim()
        val parts = cleanVersion.split(".")
        return try {
            val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
            // Use larger multipliers to support versions like 1.99.99
            major * 10000 + minor * 100 + patch
        } catch (e: Exception) {
            0
        }
    }
}

sealed class UpdateStatus {
    object NoUpdateAvailable : UpdateStatus()
    data class UpdateAvailable(
        val release: GitHubRelease,
        val downloadUrl: String,
        val shouldShowDialog: Boolean = true
    ) : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
}
