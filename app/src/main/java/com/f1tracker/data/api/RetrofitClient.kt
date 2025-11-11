package com.f1tracker.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    
    private const val F1_BASE_URL = "https://api.jolpi.ca/"
    private const val WEATHER_BASE_URL = "https://api.open-meteo.com/"
    private const val ESPN_BASE_URL = "https://site.api.espn.com/"
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val f1Retrofit = Retrofit.Builder()
        .baseUrl(F1_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    private val weatherRetrofit = Retrofit.Builder()
        .baseUrl(WEATHER_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    private val espnRetrofit = Retrofit.Builder()
        .baseUrl(ESPN_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val f1ApiService: F1ApiService = f1Retrofit.create(F1ApiService::class.java)
    val weatherApiService: WeatherApiService = weatherRetrofit.create(WeatherApiService::class.java)
    val espnNewsApiService: ESPNNewsApiService = espnRetrofit.create(ESPNNewsApiService::class.java)
}

