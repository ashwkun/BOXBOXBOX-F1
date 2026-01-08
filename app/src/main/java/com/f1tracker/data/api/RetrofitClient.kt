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
    
    private var okHttpClient: OkHttpClient? = null
    
    fun initialize(context: android.content.Context) {
        if (okHttpClient != null) return
        
        val cacheSize = 50L * 1024 * 1024 // 50 MB
        val cache = okhttp3.Cache(context.cacheDir.resolve("http_cache"), cacheSize)
        
        okHttpClient = OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor(loggingInterceptor)
            .addNetworkInterceptor { chain ->
                val response = chain.proceed(chain.request())
                val cacheControl = response.header("Cache-Control")
                
                // For GitHub Pages JSON (Instagram feeds), use shorter cache to get fresh CDN URLs
                val isInstagramFeed = chain.request().url.toString().contains("ashwkun.github.io")
                val maxAge = if (isInstagramFeed) 60 else 300 // 1 min for Instagram, 5 min for others
                
                if (cacheControl == null || cacheControl.contains("no-store") || cacheControl.contains("no-cache") ||
                    cacheControl.contains("must-revalidate") || cacheControl.contains("max-age=0")
                ) {
                    // Instagram CDN URLs expire in 4-6 hours, so we use short cache for feed JSON
                    response.newBuilder()
                        .header("Cache-Control", "public, max-age=$maxAge, stale-while-revalidate=30")
                        .build()
                } else {
                    response
                }
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    private val client: OkHttpClient
        get() = okHttpClient ?: OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .build().also { 
                // Fallback if not initialized (should not happen if App calls init)
                android.util.Log.w("RetrofitClient", "OkHttpClient used before initialization! No disk cache available.")
                okHttpClient = it
            }
    
    private val f1Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(F1_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    private val weatherRetrofit by lazy {
        Retrofit.Builder()
            .baseUrl(WEATHER_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    private val espnRetrofit by lazy {
        Retrofit.Builder()
            .baseUrl(ESPN_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    private val youtubeRssRetrofit by lazy {
        Retrofit.Builder()
            .baseUrl(YOUTUBE_RSS_BASE_URL)
            .client(client)
            .addConverterFactory(SimpleXmlConverterFactory.createNonStrict(Persister()))
            .build()
    }
    
    // Generic RSS/XML retrofit for podcast feeds with lenient parsing
    private val podcastRetrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://audioboom.com/") // Base URL (will be overridden with @Url)
            .client(client)
            .addConverterFactory(
                SimpleXmlConverterFactory.createNonStrict(
                    Persister(RegistryMatcher())
                )
            )
            .build()
    }
    
    val f1ApiService: F1ApiService by lazy { f1Retrofit.create(F1ApiService::class.java) }
    val weatherApiService: WeatherApiService by lazy { weatherRetrofit.create(WeatherApiService::class.java) }
    val espnNewsApiService: ESPNNewsApiService by lazy { espnRetrofit.create(ESPNNewsApiService::class.java) }
    val espnApiService: ESPNApiService by lazy { espnRetrofit.create(ESPNApiService::class.java) }
    val youtubeRssApiService: YouTubeRssApiService by lazy { youtubeRssRetrofit.create(YouTubeRssApiService::class.java) }
    val podcastApiService: PodcastApiService by lazy {
        podcastRetrofit.create(PodcastApiService::class.java)
    }

    private val githubRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val f1NewsRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://rss-bridge.org/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val gitHubApiService: GitHubApiService by lazy {
        githubRetrofit.create(GitHubApiService::class.java)
    }

    val f1NewsApiService: F1NewsApiService by lazy {
        f1NewsRetrofit.create(F1NewsApiService::class.java)
    }

    private const val MOTORSPORT_RSS_BASE_URL = "https://www.motorsport.com/"

    private val motorsportRetrofit by lazy {
        Retrofit.Builder()
            .baseUrl(MOTORSPORT_RSS_BASE_URL)
            .client(client)
            .addConverterFactory(SimpleXmlConverterFactory.createNonStrict(Persister()))
            .build()
    }

    val motorsportApiService: MotorsportApiService by lazy {
        motorsportRetrofit.create(MotorsportApiService::class.java)
    }
}
