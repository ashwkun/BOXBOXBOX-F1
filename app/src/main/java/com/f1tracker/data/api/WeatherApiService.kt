package com.f1tracker.data.api

import com.f1tracker.data.models.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {
    
    @GET("v1/forecast")
    suspend fun getWeatherForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("hourly") hourly: String = "temperature_2m,precipitation_probability,weather_code",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 7
    ): WeatherResponse
}

