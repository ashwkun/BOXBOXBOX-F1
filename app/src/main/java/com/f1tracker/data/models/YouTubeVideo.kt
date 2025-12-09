package com.f1tracker.data.models

import com.google.gson.annotations.SerializedName

/**
 * Model for videos from data/f1_youtube.json and specialized feeds
 * (data/f1_official.json, data/f1_trending.json, data/f1_analysis.json)
 */
data class YouTubeVideo(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("thumbnail") val thumbnail: String,
    @SerializedName("publishedAt") val publishedAt: String,
    @SerializedName("channelTitle") val channelTitle: String,
    @SerializedName("channelScore") val channelScore: Double = 0.5,
    @SerializedName("viewCount") val viewCount: String,
    @SerializedName("likeCount") val likeCount: String?,
    @SerializedName("durationSec") val durationSec: Int,
    @SerializedName("tags") val tags: List<String>? = null, // Nullable for backward compat
    @SerializedName("url") val url: String
)


