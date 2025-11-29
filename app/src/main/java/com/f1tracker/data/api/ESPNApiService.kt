package com.f1tracker.data.api

import com.f1tracker.data.models.ESPNRaceEvent
import com.f1tracker.data.models.ESPNScoreboard
import retrofit2.http.GET
import retrofit2.http.Path

interface ESPNApiService {
    @GET("apis/site/v2/sports/racing/f1/scoreboard")
    suspend fun getScoreboard(): ESPNScoreboard
    
    @GET("apis/site/v2/sports/racing/f1/events/{eventId}")
    suspend fun getRaceEvent(@Path("eventId") eventId: String): ESPNRaceEvent
}
