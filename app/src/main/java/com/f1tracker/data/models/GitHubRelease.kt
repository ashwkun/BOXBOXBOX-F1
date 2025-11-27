package com.f1tracker.data.models

import com.google.gson.annotations.SerializedName

data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String,           // "v1.3"
    @SerializedName("name") val name: String,                  // "Release v1.3"
    @SerializedName("body") val body: String,                  // Changelog markdown
    @SerializedName("published_at") val publishedAt: String,
    @SerializedName("assets") val assets: List<ReleaseAsset>
)

data class ReleaseAsset(
    @SerializedName("name") val name: String,                  // "app-debug.apk"
    @SerializedName("browser_download_url") val browserDownloadUrl: String
)
