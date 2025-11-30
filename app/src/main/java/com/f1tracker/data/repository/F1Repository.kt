package com.f1tracker.data.repository

import com.f1tracker.data.api.*
import com.f1tracker.data.models.*
import kotlinx.coroutines.*
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
    suspend fun getSprintResultsByCircuit(season: String, circuitId: String): Result<List<RaceResult>>
    suspend fun getAllRaceResultsForSeason(season: String): Result<List<Race>>
    suspend fun getESPNSessionResults(round: Int): Result<List<SessionResult>>
}

class F1RepositoryImpl @Inject constructor(
    private val f1ApiService: F1ApiService,
    private val weatherApiService: WeatherApiService,
    private val motorsportApiService: MotorsportApiService,
    private val f1NewsApiService: F1NewsApiService,
    private val espnApiService: ESPNApiService,
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
        return kotlinx.coroutines.coroutineScope {
            val f1NewsDeferred = async {
                try {
                    val response = f1NewsApiService.getF1News()
                    response.items.map { item ->
                        NewsArticle(
                            id = item.id.hashCode().toLong(),
                            headline = item.title,
                            description = item.contentHtml.replace(Regex("<.*?>"), ""),
                            published = item.dateModified,
                            images = item.attachments?.map { attachment ->
                                NewsImage(
                                    name = null,
                                    url = attachment.url,
                                    height = 0,
                                    width = 0,
                                    type = attachment.mimeType
                                )
                            },
                            links = NewsLinks(
                                web = NewsWebLink(href = item.url)
                            )
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("F1Repository", "Error fetching F1 news", e)
                    emptyList()
                }
            }

            val motorsportNewsDeferred = async {
                try {
                    val response = motorsportApiService.getNews()
                    response.channel?.items?.map { item ->
                        NewsArticle(
                            id = item.link.hashCode().toLong(),
                            headline = item.title ?: "",
                            description = item.description
                                ?.replace(Regex("(<br\\s*/?>\\s*)+", RegexOption.IGNORE_CASE), " ") // Replace one or more <br> with single space
                                ?.replace(Regex("<.*?>"), "") // Remove other tags
                                ?.replace(Regex("&nbsp;"), " ")
                                ?.trim() 
                                ?: "",
                            published = try {
                                val zdt = java.time.ZonedDateTime.parse(item.pubDate, java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME)
                                zdt.format(java.time.format.DateTimeFormatter.ISO_DATE_TIME)
                            } catch (e: Exception) {
                                item.pubDate ?: ""
                            },
                            images = listOfNotNull(
                                item.enclosure?.url?.let { url ->
                                    NewsImage(
                                        name = null,
                                        url = url,
                                        height = 0,
                                        width = 0,
                                        type = item.enclosure?.type
                                    )
                                }
                            ),
                            links = NewsLinks(
                                web = NewsWebLink(href = item.link ?: "")
                            )
                        )
                    } ?: emptyList()
                } catch (e: Exception) {
                    android.util.Log.e("F1Repository", "Error fetching Motorsport news", e)
                    emptyList()
                }
            }

            try {
                val f1Articles = f1NewsDeferred.await()
                val motorsportArticles = motorsportNewsDeferred.await()
                
                // Combine and sort
                val allArticles = (f1Articles + motorsportArticles).sortedByDescending { 
                    // Simple date parsing for sorting if needed, or just rely on string comparison if ISO
                    // Motorsport uses RFC 1123 usually, F1 uses ISO. 
                    // For now, let's just sort. The UI handles parsing for display.
                    it.published 
                }
                
                if (allArticles.isEmpty()) {
                    Result.failure(Exception("Failed to fetch news from all sources"))
                } else {
                    Result.success(allArticles)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
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

    override suspend fun getSprintResultsByCircuit(season: String, circuitId: String): Result<List<RaceResult>> {
        return try {
            val response = f1ApiService.getSprintResultsByCircuit(season, circuitId)
            val raceWithSprint = response.mrData.raceTable.races.firstOrNull()
            
            val mappedResults = raceWithSprint?.sprintResults?.map { sprintResult ->
                RaceResult(
                    number = sprintResult.number,
                    position = sprintResult.position,
                    positionText = sprintResult.position,
                    points = "0", // Sprint points not in model
                    driver = sprintResult.driver,
                    constructor = sprintResult.constructor,
                    grid = "0",
                    laps = "0",
                    status = sprintResult.status,
                    time = sprintResult.time,
                    fastestLap = null
                )
            } ?: emptyList()
            
            Result.success(mappedResults)
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

    override suspend fun getESPNSessionResults(round: Int): Result<List<SessionResult>> {
        return try {
            // Get ESPN race ID for this round
            val espnId = com.f1tracker.data.local.ESPNRaceIdProvider.getESPNIdForRound(round)
                ?: return Result.failure(Exception("ESPN ID not found for round $round"))
            
            // Fetch scoreboard which contains the active event and its results
            val scoreboard = espnApiService.getScoreboard()
            
            // Find the event matching our ID, or fallback to the first event if it looks like the right one
            val raceEvent = scoreboard.events.find { it.id == espnId } 
                ?: scoreboard.events.firstOrNull()
                ?: return Result.failure(Exception("Event not found in scoreboard"))
            
            android.util.Log.d("F1Repository", "Found event: ${raceEvent.name} (${raceEvent.id})")
            
            // Process completed sessions
            val sessionResults = raceEvent.competitions
                .filter { it.status.type.completed }
                .mapNotNull { competition ->
                    val sessionType = mapESPNSessionType(competition.type.abbreviation) ?: return@mapNotNull null
                    val competitors = competition.competitors ?: return@mapNotNull null
                    
                    SessionResult(
                        sessionType = sessionType,
                        sessionName = sessionType.displayName(),
                        results = competitors.map { competitor ->
                            val athleteId = competitor.athlete.id ?: ""
                            DriverResult(
                                position = competitor.order,
                                driverCode = mapESPNAthleteToDriverCode(competitor.athlete) ?: "???",
                                driverName = competitor.athlete.shortName,
                                team = null,  // Can be added later if needed
                                time = null,  // Can be extracted from statistics if available
                                espnId = athleteId
                            )
                        }
                    )
                }
            
            Result.success(sessionResults)
        } catch (e: Exception) {
            android.util.Log.e("F1Repository", "Error fetching ESPN session results", e)
            Result.failure(e)
        }
    }
    
    private fun mapESPNSessionType(abbreviation: String): SessionType? {
        android.util.Log.d("F1Repository", "Mapping ESPN session type: $abbreviation")
        return when (abbreviation.uppercase()) {
            "FP1", "P1", "PRA1" -> SessionType.FP1
            "FP2", "P2", "PRA2" -> SessionType.FP2
            "FP3", "P3", "PRA3" -> SessionType.FP3
            "SS", "SQ", "SPRINT SHOOTOUT", "SPRINT QUALIFYING" -> SessionType.SPRINT_QUALIFYING
            "SR", "SPRINT", "SPR" -> SessionType.SPRINT
            "QUAL", "Q", "QUALI", "QUALIFYING" -> SessionType.QUALIFYING
            "RACE", "R", "GP" -> SessionType.RACE
            else -> {
                android.util.Log.w("F1Repository", "Unknown ESPN session type: $abbreviation")
                null
            }
        }
    }
    
    private fun mapESPNAthleteToDriverCode(athlete: ESPNAthlete): String? {
        // Try ID first
        val byId = athlete.id?.let { com.f1tracker.data.local.F1DataProvider.getDriverByESPNId(it)?.code }
        if (byId != null) return byId
        
        // Fallback to name
        return com.f1tracker.data.local.F1DataProvider.getDriverByName(athlete.fullName)?.code
            ?: com.f1tracker.data.local.F1DataProvider.getDriverByName(athlete.displayName)?.code
            ?: com.f1tracker.data.local.F1DataProvider.getDriverByName(athlete.shortName)?.code
    }
}
