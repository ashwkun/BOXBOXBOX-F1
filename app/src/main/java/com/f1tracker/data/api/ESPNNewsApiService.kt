package com.f1tracker.data.api

import com.f1tracker.data.models.NewsResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ESPNNewsApiService {
    
    @GET("apis/site/v2/sports/racing/f1/news")
    suspend fun getF1News(
        @Query("lang") lang: String = "en",
        @Query("region") region: String = "us"
    ): NewsResponse
}

