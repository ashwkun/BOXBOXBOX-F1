package com.f1tracker.di

import com.f1tracker.data.api.*
import com.f1tracker.data.repository.F1Repository
import com.f1tracker.data.repository.F1RepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideF1ApiService(): F1ApiService = RetrofitClient.f1ApiService

    @Provides
    @Singleton
    fun provideWeatherApiService(): WeatherApiService = RetrofitClient.weatherApiService

    @Provides
    @Singleton
    fun provideMotorsportApiService(): MotorsportApiService = RetrofitClient.motorsportApiService

    @Provides
    @Singleton
    fun provideYouTubeRssApiService(): YouTubeRssApiService = RetrofitClient.youtubeRssApiService

    @Provides
    @Singleton
    fun providePodcastApiService(): PodcastApiService = RetrofitClient.podcastApiService

    @Provides
    @Singleton
    fun provideGitHubApiService(): GitHubApiService = RetrofitClient.gitHubApiService

    @Provides
    @Singleton
    fun provideESPNApiService(): ESPNApiService = RetrofitClient.espnApiService

    @Provides
    @Singleton
    fun provideF1NewsApiService(): F1NewsApiService = RetrofitClient.f1NewsApiService

    @Provides
    @Singleton
    fun provideF1Repository(
        f1ApiService: F1ApiService,
        weatherApiService: WeatherApiService,
        motorsportApiService: MotorsportApiService,
        f1NewsApiService: F1NewsApiService,
        espnApiService: ESPNApiService,
        youtubeRssApiService: YouTubeRssApiService,
        podcastApiService: PodcastApiService
    ): F1Repository {
        return F1RepositoryImpl(
            f1ApiService,
            weatherApiService,
            motorsportApiService,
            f1NewsApiService,
            espnApiService,
            youtubeRssApiService,
            podcastApiService
        )
    }
}
