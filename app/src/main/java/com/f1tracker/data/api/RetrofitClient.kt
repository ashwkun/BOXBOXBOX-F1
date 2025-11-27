package com.f1tracker.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import org.simpleframework.xml.core.Persister
import org.simpleframework.xml.strategy.CycleStrategy
import org.simpleframework.xml.strategy.Strategy
import org.simpleframework.xml.strategy.VisitorStrategy
import org.simpleframework.xml.transform.RegistryMatcher
import org.simpleframework.xml.stream.Format
import org.simpleframework.xml.stream.HyphenStyle
import java.util.concurrent.TimeUnit

object RetrofitClient {
    
    private const val F1_BASE_URL = "https://api.jolpi.ca/"
    private const val WEATHER_BASE_URL = "https://api.open-meteo.com/"
    private const val ESPN_BASE_URL = "https://site.api.espn.com/"
    private const val YOUTUBE_RSS_BASE_URL = "https://www.youtube.com/"
    
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
    
    private val youtubeRssRetrofit = Retrofit.Builder()
        .baseUrl(YOUTUBE_RSS_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(SimpleXmlConverterFactory.createNonStrict(Persister()))
        .build()
    
    // Generic RSS/XML retrofit for podcast feeds with lenient parsing
    private val podcastRetrofit = Retrofit.Builder()
        .baseUrl("https://audioboom.com/") // Base URL (will be overridden with @Url)
        .client(okHttpClient)
        .addConverterFactory(
            SimpleXmlConverterFactory.createNonStrict(
                Persister(RegistryMatcher())
            )
        )
        .build()
    
    val f1ApiService: F1ApiService = f1Retrofit.create(F1ApiService::class.java)
    val weatherApiService: WeatherApiService = weatherRetrofit.create(WeatherApiService::class.java)
    val espnNewsApiService: ESPNNewsApiService = espnRetrofit.create(ESPNNewsApiService::class.java)
    val youtubeRssApiService: YouTubeRssApiService = youtubeRssRetrofit.create(YouTubeRssApiService::class.java)
    val podcastApiService: PodcastApiService by lazy {
        podcastRetrofit.create(PodcastApiService::class.java)
    }

    private val githubRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val gitHubApiService: GitHubApiService by lazy {
        githubRetrofit.create(GitHubApiService::class.java)
    }
}
