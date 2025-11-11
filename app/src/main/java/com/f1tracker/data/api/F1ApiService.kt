package com.f1tracker.data.api

import com.f1tracker.data.models.ConstructorStandingsResponse
import com.f1tracker.data.models.DriverStandingsResponse
import com.f1tracker.data.models.QualifyingResponse
import com.f1tracker.data.models.RacesResponse
import com.f1tracker.data.models.SprintResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface F1ApiService {
    
    @GET("ergast/f1/{season}/races/")
    suspend fun getAllRaces(
        @Path("season") season: String = "2025"
    ): RacesResponse
    
    @GET("ergast/f1/{season}/{round}/races/")
    suspend fun getRaceByRound(
        @Path("season") season: String = "2025",
        @Path("round") round: String
    ): RacesResponse
    
    @GET("ergast/f1/{season}/{round}/qualifying/")
    suspend fun getQualifyingResults(
        @Path("season") season: String = "2025",
        @Path("round") round: String
    ): QualifyingResponse
    
    @GET("ergast/f1/{season}/{round}/sprint/")
    suspend fun getSprintResults(
        @Path("season") season: String = "2025",
        @Path("round") round: String
    ): SprintResponse
    
    @GET("ergast/f1/{season}/last/results/")
    suspend fun getLastRaceResults(
        @Path("season") season: String = "2025"
    ): RacesResponse
    
    @GET("ergast/f1/{season}/driverstandings/")
    suspend fun getDriverStandings(
        @Path("season") season: String = "2025"
    ): DriverStandingsResponse
    
    @GET("ergast/f1/{season}/constructorstandings/")
    suspend fun getConstructorStandings(
        @Path("season") season: String = "2025"
    ): ConstructorStandingsResponse
}

