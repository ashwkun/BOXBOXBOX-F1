package com.f1tracker.data.models

import com.google.gson.annotations.SerializedName

data class HighlightVideo(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("thumbnail") val thumbnail: String?,
    @SerializedName("publishedAt") val publishedAt: String,
    @SerializedName("sessionType") val sessionType: String,
    @SerializedName("year") val year: String,
    @SerializedName("raceName") val raceName: String,
    @SerializedName("url") val url: String
)
