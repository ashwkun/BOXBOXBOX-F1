package com.f1tracker.data.models

import com.google.gson.annotations.SerializedName

/**
 * Model for videos from f1_youtube.json - includes videos from multiple channels
 * (F1, ESPN F1, The Race, etc.)
 */
data class YouTubeVideo(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("thumbnail") val thumbnail: String,
    @SerializedName("publishedAt") val publishedAt: String,
    @SerializedName("channelTitle") val channelTitle: String,
    @SerializedName("viewCount") val viewCount: String,
    @SerializedName("likeCount") val likeCount: String?,
    @SerializedName("duration") val duration: String?, // ISO 8601 format like PT7M31S
    @SerializedName("durationSec") val durationSec: Int,
    @SerializedName("url") val url: String
)
