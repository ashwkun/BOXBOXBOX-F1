package com.f1tracker.data.models

import com.google.gson.annotations.SerializedName

data class InstagramPost(
    @SerializedName("id") val id: String,
    @SerializedName("caption") val caption: String?,
    @SerializedName("media_url") val media_url: String?, // Can be null for copyright videos
    @SerializedName("thumbnail_url") val thumbnail_url: String?, // The fallback for videos
    @SerializedName("permalink") val permalink: String,
    @SerializedName("media_type") val media_type: String, // "IMAGE", "VIDEO", "CAROUSEL_ALBUM"
    @SerializedName("author") val author: String? = "f1", // Instagram username (default for backward compatibility)
    @SerializedName("language") val language: String? = "en", // Language code (default for backward compatibility)
    @SerializedName("like_count") val like_count: Int = 0,
    @SerializedName("comments_count") val comments_count: Int = 0,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("children") val children: InstagramChildrenWrapper? = null,
    @SerializedName("audio_url") val audio_url: String? = null // URL for background audio (from merged video post)
)

data class InstagramChildrenWrapper(
    @SerializedName("data") val data: List<InstagramMedia>
)

data class InstagramMedia(
    @SerializedName("id") val id: String,
    @SerializedName("media_type") val media_type: String, // "IMAGE" or "VIDEO"
    @SerializedName("media_url") val media_url: String?,
    @SerializedName("thumbnail_url") val thumbnail_url: String?,
    @SerializedName("timestamp") val timestamp: String
)
