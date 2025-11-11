package com.f1tracker.data.api

import com.f1tracker.data.models.YouTubeRssFeed
import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeRssApiService {
    @GET("feeds/videos.xml")
    suspend fun getPlaylistVideos(
        @Query("playlist_id") playlistId: String
    ): YouTubeRssFeed
}

