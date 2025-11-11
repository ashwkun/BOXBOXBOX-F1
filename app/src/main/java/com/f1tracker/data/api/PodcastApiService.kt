package com.f1tracker.data.api

import com.f1tracker.data.models.PodcastRssFeed
import retrofit2.http.GET
import retrofit2.http.Url

interface PodcastApiService {
    @GET
    suspend fun getPodcastFeed(@Url url: String): PodcastRssFeed
}

