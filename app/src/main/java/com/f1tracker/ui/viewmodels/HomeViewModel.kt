package com.f1tracker.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.f1tracker.data.api.RetrofitClient
import com.f1tracker.data.models.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class HomeViewModel : ViewModel() {
    
    init {
        Log.d("HomeViewModel", "HomeViewModel initialized")
    }
    
    private val _raceWeekendState = MutableStateFlow<RaceWeekendState>(RaceWeekendState.Loading)
    val raceWeekendState: StateFlow<RaceWeekendState> = _raceWeekendState.asStateFlow()
    
    private val _lastRaceResult = MutableStateFlow<Race?>(null)
    val lastRaceResult: StateFlow<Race?> = _lastRaceResult.asStateFlow()
    
    private val _driverStandings = MutableStateFlow<List<DriverStanding>?>(null)
    val driverStandings: StateFlow<List<DriverStanding>?> = _driverStandings.asStateFlow()
    
    private val _constructorStandings = MutableStateFlow<List<ConstructorStanding>?>(null)
    val constructorStandings: StateFlow<List<ConstructorStanding>?> = _constructorStandings.asStateFlow()
    
    private val _newsArticles = MutableStateFlow<List<NewsArticle>>(emptyList())
    val newsArticles: StateFlow<List<NewsArticle>> = _newsArticles.asStateFlow()
    
    private val _allRaces = MutableStateFlow<List<Race>>(emptyList())
    val allRaces: StateFlow<List<Race>> = _allRaces.asStateFlow()
    
    private val _youtubeVideos = MutableStateFlow<List<F1Video>>(emptyList())
    val youtubeVideos: StateFlow<List<F1Video>> = _youtubeVideos.asStateFlow()
    
    private val _podcasts = MutableStateFlow<List<Podcast>>(emptyList())
    val podcasts: StateFlow<List<Podcast>> = _podcasts.asStateFlow()
    
    private val f1ApiService = RetrofitClient.f1ApiService
    private val weatherApiService = RetrofitClient.weatherApiService
    private val espnNewsApiService = RetrofitClient.espnNewsApiService
    private val youtubeRssApiService = RetrofitClient.youtubeRssApiService
    private val podcastApiService = RetrofitClient.podcastApiService
    private var allRacesCache: List<Race> = emptyList()
    
    // Add flag to track if already loading
    private var isLoading = false
    
    // Cache data to prevent loss on sleep/resume
    private var cachedRaceState: RaceWeekendState? = null
    private var lastUpdateTime: Long = 0
    private val weatherCache = mutableMapOf<String, SessionWeather>()
    
    init {
        // Start periodic update (for countdown refresh)
        startPeriodicUpdate()
    }
    
    companion object {
        @Volatile
        private var instance: HomeViewModel? = null
        
        fun getInstance(): HomeViewModel {
            return instance ?: synchronized(this) {
                instance ?: HomeViewModel().also { 
                    instance = it
                    Log.d("HomeViewModel", "getInstance() called - triggering all loads")
                    // Trigger initial load
                    it.loadRaceWeekendState()
                    it.loadLastRaceResult()
                    it.loadDriverStandings()
                    it.loadConstructorStandings()
                    it.loadNews()
                    it.loadYouTubeVideos()
                    Log.d("HomeViewModel", "About to call loadPodcasts()")
                    it.loadPodcasts()
                    Log.d("HomeViewModel", "loadPodcasts() called")
                }
            }
        }
    }
    
    private fun startPeriodicUpdate() {
        viewModelScope.launch {
            while (true) {
                delay(60000) // Update every minute
                if (allRacesCache.isNotEmpty()) {
                    updateRaceWeekendState()
                } else if (cachedRaceState != null) {
                    // If races list is empty but we have cached state, restore it
                    _raceWeekendState.value = cachedRaceState!!
                }
            }
        }
    }
    
    fun refreshIfStale() {
        val now = System.currentTimeMillis()
        
        // Always restore cached state first if available
        if (cachedRaceState != null && _raceWeekendState.value is RaceWeekendState.Loading) {
            Log.d("HomeViewModel", "Restoring cached race state")
            _raceWeekendState.value = cachedRaceState!!
        }
        
        // Only refresh if data is older than 5 minutes
        if (now - lastUpdateTime > 300000) {
            Log.d("HomeViewModel", "Data is stale, refreshing...")
            if (allRacesCache.isEmpty()) {
                loadRaceWeekendState()
            } else {
                viewModelScope.launch {
                    updateRaceWeekendState()
                }
            }
        }
    }
    
    fun loadRaceWeekendState() {
        viewModelScope.launch {
            try {
                val response = f1ApiService.getAllRaces("2025")
                allRacesCache = response.mrData.raceTable.races
                _allRaces.value = allRacesCache
                updateRaceWeekendState()
            } catch (e: Exception) {
                _raceWeekendState.value = RaceWeekendState.Error(e.message ?: "Failed to load races")
            }
        }
    }
    
    fun loadLastRaceResult() {
        viewModelScope.launch {
            try {
                val response = f1ApiService.getLastRaceResults("2025")
                if (response.mrData.raceTable.races.isNotEmpty()) {
                    _lastRaceResult.value = response.mrData.raceTable.races.first()
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Failed to load last race result: ${e.message}")
            }
        }
    }
    
    fun loadDriverStandings() {
        viewModelScope.launch {
            try {
                val response = f1ApiService.getDriverStandings("2025")
                if (response.mrData.standingsTable.standingsLists.isNotEmpty()) {
                    _driverStandings.value = response.mrData.standingsTable.standingsLists.first().driverStandings
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Failed to load driver standings: ${e.message}")
            }
        }
    }
    
    fun loadConstructorStandings() {
        viewModelScope.launch {
            try {
                val response = f1ApiService.getConstructorStandings("2025")
                if (response.mrData.standingsTable.standingsLists.isNotEmpty()) {
                    _constructorStandings.value = response.mrData.standingsTable.standingsLists.first().constructorStandings
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Failed to load constructor standings: ${e.message}")
            }
        }
    }
    
    fun loadNews() {
        viewModelScope.launch {
            try {
                val response = espnNewsApiService.getF1News()
                _newsArticles.value = response.articles
                Log.d("HomeViewModel", "Loaded ${response.articles.size} news articles")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Failed to load news: ${e.message}", e)
            }
        }
    }
    
    private suspend fun updateRaceWeekendState() {
        val now = LocalDateTime.now()
        val currentOrNextRace = findCurrentOrNextRace(now)
        
        if (currentOrNextRace == null) {
            _raceWeekendState.value = RaceWeekendState.Error("No upcoming races found")
            cachedRaceState = _raceWeekendState.value
            return
        }
        
        val isWeekendActive = isRaceWeekendActive(currentOrNextRace, now)
        
        if (isWeekendActive) {
            _raceWeekendState.value = buildActiveState(currentOrNextRace, now)
        } else {
            _raceWeekendState.value = buildComingUpState(currentOrNextRace)
        }
        
        // Cache the state and update timestamp
        cachedRaceState = _raceWeekendState.value
        lastUpdateTime = System.currentTimeMillis()
    }
    
    private fun findCurrentOrNextRace(now: LocalDateTime): Race? {
        return allRacesCache.firstOrNull { race ->
            val raceDateTime = parseDateTime(race.date, race.time)
            raceDateTime.isAfter(now) || isRaceWeekendActive(race, now)
        }
    }
    
    private fun isRaceWeekendActive(race: Race, now: LocalDateTime): Boolean {
        val firstEvent = getFirstMainEvent(race) ?: return false
        val lastEvent = getRaceSession(race) ?: return false
        
        val firstEventStart = parseDateTime(firstEvent.date, firstEvent.time)
        val lastEventEnd = parseDateTime(lastEvent.date, lastEvent.time).plusHours(3) // Assume 3 hour buffer
        
        return now.isAfter(firstEventStart) && now.isBefore(lastEventEnd)
    }
    
    private fun getFirstMainEvent(race: Race): SessionInfo? {
        return race.sprintQualifying ?: race.qualifying
    }
    
    private fun getRaceSession(race: Race): SessionInfo {
        return SessionInfo(race.date, race.time)
    }
    
    private suspend fun buildActiveState(race: Race, now: LocalDateTime): RaceWeekendState.Active {
        val mainEvents = getMainEvents(race)
        val currentEvent = findCurrentSession(mainEvents, now)
        val completedEvents = mutableListOf<CompletedEvent>()
        val upcomingEventsList = mutableListOf<Pair<SessionType, SessionInfo>>()
        
        val latitude = race.circuit.location.lat.toDouble()
        val longitude = race.circuit.location.long.toDouble()
        
        mainEvents.forEach { (type, session) ->
            val sessionStart = parseDateTime(session.date, session.time)
            val sessionEnd = sessionStart.plusHours(2) // Approximate session duration
            
            when {
                now.isBefore(sessionStart) -> {
                    upcomingEventsList.add(type to session)
                }
                now.isAfter(sessionEnd) -> {
                    // Fetch results only for qualifying and sprint (not FP sessions)
                    if (type == SessionType.QUALIFYING || type == SessionType.SPRINT) {
                        val results = fetchSessionResults(race.round, type)
                        if (results.isNotEmpty()) {
                            completedEvents.add(CompletedEvent(type, results))
                        }
                    } else if (type == SessionType.FP1 || type == SessionType.FP2 || type == SessionType.FP3) {
                        // Add FP sessions as completed without results
                        completedEvents.add(CompletedEvent(type, emptyList()))
                    }
                }
            }
        }
        
        // Fetch weather for upcoming events
        val upcomingEvents = upcomingEventsList.mapIndexed { index, (type, session) ->
            val sessionDateTime = parseDateTime(session.date, session.time)
            val weather = fetchWeatherForSession(latitude, longitude, sessionDateTime)
            
            UpcomingEvent(
                sessionType = type,
                sessionInfo = session,
                isNext = index == 0,
                weather = weather
            )
        }
        
        return RaceWeekendState.Active(
            race = race,
            currentEvent = currentEvent,
            completedEvents = completedEvents,
            upcomingEvents = upcomingEvents
        )
    }
    
    private fun findCurrentSession(events: List<Pair<SessionType, SessionInfo>>, now: LocalDateTime): SessionEvent? {
        events.forEach { (type, session) ->
            val sessionStart = parseDateTime(session.date, session.time)
            val sessionEnd = sessionStart.plusHours(2)
            
            if (now.isAfter(sessionStart) && now.isBefore(sessionEnd)) {
                return SessionEvent(
                    sessionType = type,
                    sessionInfo = session,
                    isLive = true,
                    endsAt = sessionEnd
                )
            }
        }
        return null
    }
    
    private suspend fun fetchSessionResults(round: String, sessionType: SessionType): List<ResultEntry> {
        return try {
            when (sessionType) {
                SessionType.QUALIFYING -> {
                    val response = f1ApiService.getQualifyingResults(round = round)
                    response.mrData.raceTable.races.firstOrNull()?.qualifyingResults
                        ?.take(3)
                        ?.map {
                            ResultEntry(
                                position = it.position.toInt(),
                                driverCode = it.driver.code,
                                driverName = "${it.driver.givenName} ${it.driver.familyName}",
                                team = it.constructor.name
                            )
                        } ?: emptyList()
                }
                SessionType.SPRINT -> {
                    val response = f1ApiService.getSprintResults(round = round)
                    response.mrData.raceTable.races.firstOrNull()?.sprintResults
                        ?.take(3)
                        ?.map {
                            ResultEntry(
                                position = it.position.toInt(),
                                driverCode = it.driver.code,
                                driverName = "${it.driver.givenName} ${it.driver.familyName}",
                                team = it.constructor.name
                            )
                        } ?: emptyList()
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private suspend fun fetchWeatherForSession(
        latitude: Double,
        longitude: Double,
        sessionDateTime: LocalDateTime
    ): SessionWeather? {
        // Create cache key
        val cacheKey = "${latitude}_${longitude}_${sessionDateTime}"
        
        // Return cached weather if available
        weatherCache[cacheKey]?.let {
            Log.d("HomeViewModel", "Using cached weather for $sessionDateTime")
            return it
        }
        
        return try {
            Log.d("HomeViewModel", "Fetching weather for lat=$latitude, lon=$longitude, time=$sessionDateTime")
            
            val response = weatherApiService.getWeatherForecast(
                latitude = latitude,
                longitude = longitude,
                forecastDays = 16  // Maximum supported by free API
            )
            
            Log.d("HomeViewModel", "Weather API response received with ${response.hourly.time.size} hourly entries")
            
            // Convert IST session time back to UTC for comparison with weather API (which returns UTC)
            val sessionDateTimeUTC = sessionDateTime
                .atZone(ZoneId.of("Asia/Kolkata"))
                .withZoneSameInstant(ZoneId.of("UTC"))
                .toLocalDateTime()
            
            Log.d("HomeViewModel", "Session time in UTC: $sessionDateTimeUTC")
            
            // Find the closest hour to the session time
            var closestIndex = -1
            var minDiff = Long.MAX_VALUE
            
            response.hourly.time.forEachIndexed { index, timeString ->
                try {
                    // Parse weather time (format: "2025-03-15T14:00")
                    val weatherTime = LocalDateTime.parse(timeString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    val diff = Duration.between(weatherTime, sessionDateTimeUTC).abs().toMinutes()
                    
                    if (diff < minDiff) {
                        minDiff = diff
                        closestIndex = index
                    }
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Error parsing weather time: $timeString", e)
                }
            }
            
            Log.d("HomeViewModel", "Closest weather index: $closestIndex, diff: $minDiff minutes")
            
            if (closestIndex != -1 && minDiff < 180) { // Within 3 hours
                val temp = response.hourly.temperature[closestIndex].toInt()
                val rainChance = response.hourly.precipitationProbability.getOrNull(closestIndex) ?: 0
                val weatherCode = response.hourly.weatherCode.getOrNull(closestIndex) ?: 0
                
                Log.d("HomeViewModel", "Weather found: temp=$temp, rain=$rainChance%, code=$weatherCode")
                
                val weather = SessionWeather(
                    temperature = temp,
                    rainChance = rainChance,
                    weatherCode = weatherCode,
                    weatherIcon = WeatherIcon.fromWMOCode(weatherCode)
                )
                
                // Cache the weather data
                weatherCache[cacheKey] = weather
                Log.d("HomeViewModel", "Cached weather for session at $sessionDateTime")
                
                weather
            } else {
                Log.w("HomeViewModel", "No matching weather data found (closest was $minDiff minutes away)")
                null
            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error fetching weather", e)
            null
        }
    }
    
    private suspend fun fetchWeatherForEvents(
        race: Race,
        events: List<Pair<SessionType, SessionInfo>>
    ): List<UpcomingEvent> {
        val latitude = race.circuit.location.lat.toDouble()
        val longitude = race.circuit.location.long.toDouble()
        val now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"))
        
        Log.d("HomeViewModel", "Fetching weather for ${events.size} events at circuit: ${race.circuit.circuitName}")
        
        return events.map { (type, session) ->
            val sessionDateTime = parseDateTime(session.date, session.time)
            val sessionEndTime = sessionDateTime.plusHours(2) // Assume 2 hour session duration
            val isCompleted = now.isAfter(sessionEndTime)
            val weather = if (!isCompleted) fetchWeatherForSession(latitude, longitude, sessionDateTime) else null
            
            UpcomingEvent(
                sessionType = type,
                sessionInfo = session,
                isNext = false,
                weather = weather,
                isCompleted = isCompleted
            )
        }
    }
    
    private suspend fun buildComingUpState(race: Race): RaceWeekendState.ComingUp {
        val allEvents = getMainEvents(race)
        // Find first main event (not FP session)
        val nextMainEvent = allEvents.first { it.first.isMainEvent() }
        
        val upcomingEvents = fetchWeatherForEvents(race, allEvents)
        
        return RaceWeekendState.ComingUp(
            race = race,
            nextMainEvent = nextMainEvent.second,
            nextMainEventType = nextMainEvent.first,
            upcomingEvents = upcomingEvents
        )
    }
    
    private fun getMainEvents(race: Race): List<Pair<SessionType, SessionInfo>> {
        val events = mutableListOf<Pair<SessionType, SessionInfo>>()
        
        // Add practice sessions
        race.firstPractice?.let { events.add(SessionType.FP1 to it) }
        race.secondPractice?.let { events.add(SessionType.FP2 to it) }
        race.thirdPractice?.let { events.add(SessionType.FP3 to it) }
        
        // Add sprint weekend or regular weekend sessions
        race.sprintQualifying?.let { events.add(SessionType.SPRINT_QUALIFYING to it) }
        race.qualifying?.let { events.add(SessionType.QUALIFYING to it) }
        race.sprint?.let { events.add(SessionType.SPRINT to it) }
        
        // Add race
        events.add(SessionType.RACE to SessionInfo(race.date, race.time))
        
        return events.sortedBy { parseDateTime(it.second.date, it.second.time) }
    }
    
    private fun parseDateTime(date: String, time: String): LocalDateTime {
        val dateTimeString = "${date}T${time}"
        val utcDateTime = LocalDateTime.parse(dateTimeString, DateTimeFormatter.ISO_DATE_TIME)
        // Convert UTC to IST
        return utcDateTime.atZone(ZoneId.of("UTC")).withZoneSameInstant(ZoneId.of("Asia/Kolkata")).toLocalDateTime()
    }
    
    fun getCountdownTo(targetDateTime: LocalDateTime): String {
        val now = LocalDateTime.now()
        val duration = Duration.between(now, targetDateTime)
        
        if (duration.isNegative) return "00d 00h 00m 00s"
        
        val days = duration.toDays()
        val hours = duration.toHours() % 24
        val minutes = duration.toMinutes() % 60
        val seconds = duration.seconds % 60
        
        return when {
            days > 0 -> "${days}d ${hours}h ${minutes}m ${seconds}s"
            hours > 0 -> "00d ${hours}h ${minutes}m ${seconds}s"
            minutes > 0 -> "00d 00h ${minutes}m ${seconds}s"
            else -> "00d 00h 00m ${seconds}s"
        }
    }
    
    fun loadYouTubeVideos() {
        viewModelScope.launch {
            try {
                Log.d("HomeViewModel", "Loading YouTube videos from RSS feed...")
                val feed = youtubeRssApiService.getPlaylistVideos("UULFB_qr75-ydFVKSF9Dmo6izg")
                
                // Show all videos from the feed (no filtering)
                val videos = feed.entries
                    ?.mapNotNull { entry ->
                        val videoId = entry.videoId
                        val title = entry.title
                        val thumbnailUrl = entry.mediaGroup?.thumbnail?.url
                        val views = entry.mediaGroup?.community?.statistics?.views
                        val published = entry.published
                        val durationSeconds = entry.mediaGroup?.content?.duration
                        
                        if (videoId != null && title != null && thumbnailUrl != null) {
                            // Enhance thumbnail quality - use maxresdefault for highest quality
                            // YouTube thumbnail quality options: maxresdefault > sddefault > hqdefault > mqdefault > default
                            val highQualityThumbnail = "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg"
                            
                            F1Video(
                                videoId = videoId,
                                title = title,
                                thumbnailUrl = highQualityThumbnail,
                                views = formatViews(views ?: "0"),
                                publishedDate = published ?: "",
                                duration = formatDuration(durationSeconds ?: 0)
                            )
                        } else null
                    } ?: emptyList()
                
                _youtubeVideos.value = videos
                Log.d("HomeViewModel", "Loaded ${videos.size} YouTube videos")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading YouTube videos", e)
                _youtubeVideos.value = emptyList()
            }
        }
    }
    
    private fun formatViews(views: String): String {
        return try {
            val viewCount = views.toLongOrNull() ?: 0L
            when {
                viewCount >= 1_000_000 -> "${viewCount / 1_000_000}M views"
                viewCount >= 1_000 -> "${viewCount / 1_000}K views"
                else -> "$viewCount views"
            }
        } catch (e: Exception) {
            views
        }
    }
    
    private fun formatDuration(seconds: Int): String {
        if (seconds == 0) return ""
        
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        
        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, secs)
            minutes > 0 -> String.format("%d:%02d", minutes, secs)
            else -> String.format("0:%02d", secs)
        }
    }
    
    fun loadPodcasts() {
        viewModelScope.launch {
            try {
                Log.d("HomeViewModel", "Starting to load podcasts from RSS feeds...")
                val podcastFeeds = listOf(
                    "https://audioboom.com/channels/4964339.rss",
                    "https://feeds.megaphone.fm/NYOOM4196406795",
                    "https://feeds.acast.com/public/shows/67a4d8a83ef0b176ea9b64e1"
                )
                
                val podcasts = mutableListOf<Podcast>()
                
                podcastFeeds.forEachIndexed { index, feedUrl ->
                    try {
                        Log.d("HomeViewModel", "Fetching podcast ${index + 1}/${podcastFeeds.size} from: $feedUrl")
                        val feed = podcastApiService.getPodcastFeed(feedUrl)
                        Log.d("HomeViewModel", "Received feed data for: $feedUrl")
                        
                        val channel = feed.channel
                        
                        if (channel == null) {
                            Log.e("HomeViewModel", "Channel is null for $feedUrl")
                            return@forEachIndexed
                        }
                        
                        val podcastName = channel.title ?: "F1 Podcast"
                        val podcastImage = channel.getFinalImageUrl() ?: ""
                        val itemCount = channel.items?.size ?: 0
                        
                        Log.d("HomeViewModel", "Podcast: $podcastName, Image: $podcastImage, Items: $itemCount")
                        
                        if (channel.items.isNullOrEmpty()) {
                            Log.w("HomeViewModel", "No items found for podcast: $podcastName")
                            return@forEachIndexed
                        }
                        
                        val episodes = channel.items?.mapNotNull { item ->
                            val title = item.title ?: return@mapNotNull null
                            val audioUrl = item.enclosure?.url ?: return@mapNotNull null
                            val description = stripHtml(item.description ?: "")
                            val duration = formatPodcastDuration(item.duration ?: "")
                            val publishedDate = item.pubDate ?: ""
                            val episodeImage = item.image?.href ?: podcastImage
                            
                            PodcastEpisode(
                                title = title,
                                description = description,
                                audioUrl = audioUrl,
                                duration = duration,
                                publishedDate = publishedDate,
                                imageUrl = episodeImage
                            )
                        } ?: emptyList()
                        
                        Log.d("HomeViewModel", "Parsed ${episodes.size} episodes for $podcastName")
                        
                        if (episodes.isNotEmpty()) {
                            podcasts.add(
                                Podcast(
                                    name = podcastName,
                                    imageUrl = podcastImage,
                                    episodes = episodes
                                )
                            )
                            Log.d("HomeViewModel", "Added podcast: $podcastName with ${episodes.size} episodes")
                        }
                    } catch (e: Exception) {
                        Log.e("HomeViewModel", "Error loading podcast from $feedUrl: ${e.message}", e)
                        e.printStackTrace()
                    }
                }
                
                _podcasts.value = podcasts
                Log.d("HomeViewModel", "Successfully loaded ${podcasts.size} podcasts total")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error in loadPodcasts: ${e.message}", e)
                e.printStackTrace()
                _podcasts.value = emptyList()
            }
        }
    }
    
    private fun formatPodcastDuration(duration: String): String {
        return try {
            // Duration can be in seconds (3533) or HH:MM:SS format
            val seconds = duration.toLongOrNull()
            if (seconds != null) {
                val hours = seconds / 3600
                val minutes = (seconds % 3600) / 60
                val secs = seconds % 60
                
                when {
                    hours > 0 -> String.format("%dh %dm", hours, minutes)
                    minutes > 0 -> String.format("%dm", minutes)
                    else -> String.format("%ds", secs)
                }
            } else {
                // Already formatted or parse time string
                duration
            }
        } catch (e: Exception) {
            duration
        }
    }
    
    private fun stripHtml(html: String): String {
        return html
            .replace(Regex("<[^>]*>"), "")
            .replace(Regex("\\[CDATA\\[|\\]\\]"), "")
            .replace(Regex("&nbsp;"), " ")
            .replace(Regex("&amp;"), "&")
            .replace(Regex("&lt;"), "<")
            .replace(Regex("&gt;"), ">")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}

