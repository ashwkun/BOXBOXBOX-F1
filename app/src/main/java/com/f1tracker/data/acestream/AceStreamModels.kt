package com.f1tracker.data.acestream

import com.google.gson.annotations.SerializedName

/**
 * Ace Stream Engine Search API response models.
 * API endpoint: http://127.0.0.1:6878/search?query=...
 */

data class AceStreamSearchResponse(
    @SerializedName("result") val result: AceStreamSearchResult?,
    @SerializedName("error") val error: String?
)

data class AceStreamSearchResult(
    @SerializedName("total") val total: Int,
    @SerializedName("results") val results: List<AceStreamChannelGroup>,
    @SerializedName("time") val time: Double
)

data class AceStreamChannelGroup(
    @SerializedName("name") val name: String,
    @SerializedName("items") val items: List<AceStreamChannel>,
    @SerializedName("icons") val icons: List<AceStreamIcon>?
)

data class AceStreamChannel(
    @SerializedName("infohash") val infohash: String,
    @SerializedName("name") val name: String,
    @SerializedName("status") val status: Int, // 1 = no guarantee, 2 = available
    @SerializedName("availability") val availability: Double, // 0.0 to 1.0
    @SerializedName("availability_updated_at") val availabilityUpdatedAt: Long,
    @SerializedName("categories") val categories: List<String>,
    @SerializedName("languages") val languages: List<String>?,
    @SerializedName("countries") val countries: List<String>?,
    @SerializedName("channel_id") val channelId: Int?
) {
    val isAvailable: Boolean get() = status == 2
    val availabilityPercent: Int get() = (availability * 100).toInt()
}

data class AceStreamIcon(
    @SerializedName("url") val url: String,
    @SerializedName("type") val type: Int
)

data class AceStreamVersionResponse(
    @SerializedName("result") val result: AceStreamVersion?,
    @SerializedName("error") val error: String?
)

data class AceStreamVersion(
    @SerializedName("version") val version: String,
    @SerializedName("code") val code: Int,
    @SerializedName("platform") val platform: String
)

data class AceStreamPlaybackResponse(
    @SerializedName("response") val response: AceStreamPlaybackResult?,
    @SerializedName("error") val error: String?
)

data class AceStreamPlaybackResult(
    @SerializedName("stat_url") val statUrl: String?,
    @SerializedName("playback_url") val playbackUrl: String?
)
