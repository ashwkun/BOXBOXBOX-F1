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
    fun provideESPNNewsApiService(): ESPNNewsApiService = RetrofitClient.espnNewsApiService

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
    fun provideF1Repository(
        f1ApiService: F1ApiService,
        weatherApiService: WeatherApiService,
        espnNewsApiService: ESPNNewsApiService,
        youtubeRssApiService: YouTubeRssApiService,
        podcastApiService: PodcastApiService
    ): F1Repository {
        return F1RepositoryImpl(
            f1ApiService,
            weatherApiService,
            espnNewsApiService,
            youtubeRssApiService,
            podcastApiService
        )
    }
}
