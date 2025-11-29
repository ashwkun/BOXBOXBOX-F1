package com.f1tracker.data.api

import com.f1tracker.data.models.JsonFeedResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface F1NewsApiService {
    
    @GET("bridge01/")
    suspend fun getF1News(
        @Query("action") action: String = "display",
        @Query("bridge") bridge: String = "Formula1Bridge",
        @Query("limit") limit: Int = 100,
        @Query("format") format: String = "Json"
    ): JsonFeedResponse
}
