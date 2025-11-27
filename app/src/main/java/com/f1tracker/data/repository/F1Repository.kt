package com.f1tracker.data.repository

import com.f1tracker.data.api.*
import com.f1tracker.data.models.*
import javax.inject.Inject

interface F1Repository {
    suspend fun getAllRaces(season: String): Result<List<Race>>
    suspend fun getLastRaceResults(season: String): Result<Race?>
    suspend fun getDriverStandings(season: String): Result<List<DriverStanding>>
    suspend fun getConstructorStandings(season: String): Result<List<ConstructorStanding>>
    suspend fun getF1News(): Result<List<NewsArticle>>
    suspend fun getWeather(latitude: Double, longitude: Double): Result<WeatherResponse>
    suspend fun getYouTubeVideos(playlistId: String): Result<List<F1Video>>
    suspend fun getPodcasts(feedUrls: List<String>): Result<List<Podcast>>
    suspend fun getRaceResultsByCircuit(season: String, circuitId: String): Result<Race?>
    suspend fun getAllRaceResultsForSeason(season: String): Result<List<Race>>
}

class F1RepositoryImpl @Inject constructor(
    private val f1ApiService: F1ApiService,
    private val weatherApiService: WeatherApiService,
    private val espnNewsApiService: ESPNNewsApiService,
    private val youtubeRssApiService: YouTubeRssApiService,
    private val podcastApiService: PodcastApiService
) : F1Repository {

    private fun getCurrentSeason(): String {
        return java.time.Year.now().toString()
    }

    override suspend fun getAllRaces(season: String): Result<List<Race>> {
        return try {
            val s = if (season.isEmpty()) getCurrentSeason() else season
            val response = f1ApiService.getAllRaces(s)
            Result.success(response.mrData.raceTable.races)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLastRaceResults(season: String): Result<Race?> {
        return try {
            val s = if (season.isEmpty()) getCurrentSeason() else season
            val response = f1ApiService.getLastRaceResults(s)
            val race = response.mrData.raceTable.races.firstOrNull()
            Result.success(race)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDriverStandings(season: String): Result<List<DriverStanding>> {
        return try {
            val s = if (season.isEmpty()) getCurrentSeason() else season
            val response = f1ApiService.getDriverStandings(s)
            val standings = response.mrData.standingsTable.standingsLists.firstOrNull()?.driverStandings ?: emptyList()
            Result.success(standings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getConstructorStandings(season: String): Result<List<ConstructorStanding>> {
        return try {
            val s = if (season.isEmpty()) getCurrentSeason() else season
            val response = f1ApiService.getConstructorStandings(s)
            val standings = response.mrData.standingsTable.standingsLists.firstOrNull()?.constructorStandings ?: emptyList()
            Result.success(standings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getF1News(): Result<List<NewsArticle>> {
        return try {
            val response = espnNewsApiService.getF1News()
            Result.success(response.articles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getWeather(latitude: Double, longitude: Double): Result<WeatherResponse> {
        return try {
            val response = weatherApiService.getWeatherForecast(
                latitude = latitude,
                longitude = longitude,
                forecastDays = 16
            )
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getYouTubeVideos(playlistId: String): Result<List<F1Video>> {
        return try {
            val feed = youtubeRssApiService.getPlaylistVideos(playlistId)
            val videos = feed.entries?.mapNotNull { entry ->
                val videoId = entry.videoId
                val title = entry.title
                val thumbnailUrl = "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg"
                val views = entry.mediaGroup?.community?.statistics?.views
                val published = entry.published
                val durationSeconds = entry.mediaGroup?.content?.duration

                if (videoId != null && title != null) {
                    F1Video(
                        videoId = videoId,
                        title = title,
                        thumbnailUrl = thumbnailUrl,
                        views = views ?: "0", // Format in ViewModel
                        publishedDate = published ?: "",
                        duration = durationSeconds?.toString() ?: "0" // Format in ViewModel
                    )
                } else null
            } ?: emptyList()
            Result.success(videos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPodcasts(feedUrls: List<String>): Result<List<Podcast>> {
        // Implementation for podcasts (simplified for brevity, logic moved from ViewModel)
        // Since this involves multiple calls, we might want to do it in ViewModel or here.
        // For now, I'll return empty list or implement basic loop.
        // Given the complexity of the original code (stripping HTML, formatting), 
        // I'll leave the heavy lifting to the ViewModel for now and just expose the service calls if possible,
        // OR just move the logic here. Moving logic here is better.
        
        val podcasts = mutableListOf<Podcast>()
        for (url in feedUrls) {
            try {
                val feed = podcastApiService.getPodcastFeed(url)
                val channel = feed.channel ?: continue
                val episodes = channel.items?.mapNotNull { item ->
                    PodcastEpisode(
                        title = item.title ?: "",
                        description = item.description ?: "",
                        audioUrl = item.enclosure?.url ?: "",
                        duration = item.duration ?: "",
                        publishedDate = item.pubDate ?: "",
                        imageUrl = item.image?.href ?: channel.getFinalImageUrl() ?: ""
                    )
                } ?: emptyList()
                
                if (episodes.isNotEmpty()) {
                    podcasts.add(Podcast(channel.title ?: "Podcast", channel.getFinalImageUrl() ?: "", episodes))
                }
            } catch (e: Exception) {
                // Ignore errors for individual feeds
            }
        }
        return Result.success(podcasts)
    }

    override suspend fun getRaceResultsByCircuit(season: String, circuitId: String): Result<Race?> {
        return try {
            val response = f1ApiService.getRaceResultsByCircuit(season, circuitId)
            val race = response.mrData.raceTable.races.firstOrNull()
            Result.success(race)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getAllRaceResultsForSeason(season: String): Result<List<Race>> {
        return try {
            val response = f1ApiService.getAllRaceResultsForSeason(season)
            Result.success(response.mrData.raceTable.races)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
