package com.f1tracker.data.models

data class InstagramPost(
    val id: String,
    val caption: String?,
    val media_url: String?, // Can be null for copyright videos
    val thumbnail_url: String?, // The fallback for videos
    val permalink: String,
    val media_type: String, // "IMAGE", "VIDEO", "CAROUSEL_ALBUM"
    val author: String? = "f1", // Instagram username (default for backward compatibility)
    val language: String? = "en", // Language code (default for backward compatibility)
    val like_count: Int = 0,
    val comments_count: Int = 0,
    val timestamp: String
)
