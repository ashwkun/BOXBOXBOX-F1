package com.f1tracker.data.api

import com.f1tracker.data.SeasonConfig
import com.f1tracker.data.models.ConstructorStandingsResponse
import com.f1tracker.data.models.DriverStandingsResponse
import com.f1tracker.data.models.QualifyingResponse
import com.f1tracker.data.models.RacesResponse
import com.f1tracker.data.models.SprintResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Url

interface F1ApiService {
    
    @GET("ergast/f1/{season}/races/")
    suspend fun getAllRaces(
        @Path("season") season: String = SeasonConfig.CURRENT_SEASON
    ): RacesResponse
    
    @GET("ergast/f1/{season}/{round}/races/")
    suspend fun getRaceByRound(
        @Path("season") season: String = SeasonConfig.CURRENT_SEASON,
        @Path("round") round: String
    ): RacesResponse
    
    @GET("ergast/f1/{season}/{round}/qualifying/")
    suspend fun getQualifyingResults(
        @Path("season") season: String = SeasonConfig.CURRENT_SEASON,
        @Path("round") round: String
    ): QualifyingResponse
    
    @GET("ergast/f1/{season}/{round}/sprint/")
    suspend fun getSprintResults(
        @Path("season") season: String = SeasonConfig.CURRENT_SEASON,
        @Path("round") round: String
    ): SprintResponse
    
    @GET("ergast/f1/{season}/last/results/")
    suspend fun getLastRaceResults(
        @Path("season") season: String = SeasonConfig.CURRENT_SEASON
    ): RacesResponse
    
    @GET("ergast/f1/{season}/driverstandings/")
    suspend fun getDriverStandings(
        @Path("season") season: String = SeasonConfig.CURRENT_SEASON
    ): DriverStandingsResponse
    
    @GET("ergast/f1/{season}/constructorstandings/")
    suspend fun getConstructorStandings(
        @Path("season") season: String = SeasonConfig.CURRENT_SEASON
    ): ConstructorStandingsResponse
    
    @GET("ergast/f1/{season}/circuits/{circuitId}/results/")
    suspend fun getRaceResultsByCircuit(
        @Path("season") season: String = SeasonConfig.CURRENT_SEASON,
        @Path("circuitId") circuitId: String
    ): RacesResponse
    
    @GET("ergast/f1/{season}/results/")
    suspend fun getAllRaceResultsForSeason(
        @Path("season") season: String = SeasonConfig.CURRENT_SEASON
    ): RacesResponse

    @GET("ergast/f1/{season}/circuits/{circuitId}/sprint/")
    suspend fun getSprintResultsByCircuit(
        @Path("season") season: String = SeasonConfig.CURRENT_SEASON,
        @Path("circuitId") circuitId: String
    ): SprintResponse

    @GET
    suspend fun getInstagramFeed(@Url url: String): List<com.f1tracker.data.models.InstagramPost>

    @GET
    suspend fun getHighlights(@Url url: String): List<com.f1tracker.data.models.HighlightVideo>

    @GET
    suspend fun getYouTubeVideosJson(@Url url: String): List<com.f1tracker.data.models.YouTubeVideo>
}
