package com.f1tracker.data.api

import com.f1tracker.data.models.MotorsportRssFeed
import retrofit2.http.GET

interface MotorsportApiService {
    @GET("rss/f1/news/")
    suspend fun getNews(): MotorsportRssFeed
}
