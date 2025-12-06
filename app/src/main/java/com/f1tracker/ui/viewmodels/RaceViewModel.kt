package com.f1tracker.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.f1tracker.data.models.*
import com.f1tracker.data.repository.F1Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class RaceViewModel @Inject constructor(
    private val repository: F1Repository
) : ViewModel() {

    private val _raceWeekendState = MutableStateFlow<RaceWeekendState>(RaceWeekendState.Loading)
    val raceWeekendState: StateFlow<RaceWeekendState> = _raceWeekendState.asStateFlow()

    private val _allRaces = MutableStateFlow<List<Race>>(emptyList())
    val allRaces: StateFlow<List<Race>> = _allRaces.asStateFlow()

    private val _upcomingRaces = MutableStateFlow<List<Race>>(emptyList())
    val upcomingRaces: StateFlow<List<Race>> = _upcomingRaces.asStateFlow()

    private val _completedRaces = MutableStateFlow<List<Race>>(emptyList())
    val completedRaces: StateFlow<List<Race>> = _completedRaces.asStateFlow()

    private val _lastRaceResult = MutableStateFlow<Race?>(null)
    val lastRaceResult: StateFlow<Race?> = _lastRaceResult.asStateFlow()

    private val _selectedRace = MutableStateFlow<Race?>(null)
    val selectedRace: StateFlow<Race?> = _selectedRace.asStateFlow()

    private val _lastYearRaceResults = MutableStateFlow<Race?>(null)
    val lastYearRaceResults: StateFlow<Race?> = _lastYearRaceResults.asStateFlow()

    private var allRacesCache: List<Race> = emptyList()
    private var cachedRaceState: RaceWeekendState? = null
    private var lastUpdateTime: Long = 0
    private val weatherCache = mutableMapOf<String, SessionWeather>()

    init {
        loadRaceWeekendState()
        loadLastRaceResult()
        startPeriodicUpdate()
    }

    private fun startPeriodicUpdate() {
        viewModelScope.launch {
            while (true) {
                delay(60000) // Update every minute
                if (allRacesCache.isNotEmpty()) {
                    updateRaceWeekendState()
                } else if (cachedRaceState != null) {
                    _raceWeekendState.value = cachedRaceState!!
                }
            }
        }
    }

    fun refreshIfStale() {
        val now = System.currentTimeMillis()
        if (cachedRaceState != null && _raceWeekendState.value is RaceWeekendState.Loading) {
            _raceWeekendState.value = cachedRaceState!!
        }
        if (now - lastUpdateTime > 300000) { // 5 minutes
            if (allRacesCache.isEmpty()) {
                loadRaceWeekendState()
            } else {
                viewModelScope.launch { updateRaceWeekendState() }
            }
        }
    }

    fun loadRaceWeekendState() {
        viewModelScope.launch {
            val result = repository.getAllRaces("2025") // TODO: Dynamic year
            result.onSuccess { races ->
                allRacesCache = races
                _allRaces.value = races
                
                // Fetch results for completed races
                loadCompletedRaceResults(races)
                
                updateFilteredRaces(races)
                updateRaceWeekendState()
            }.onFailure { e ->
                _raceWeekendState.value = RaceWeekendState.Error(e.message ?: "Failed to load races")
            }
        }
    }
    
    private fun loadCompletedRaceResults(races: List<Race>) {
        viewModelScope.launch {
            try {
                Log.d("RaceViewModel", "Fetching results for completed races...")
                
                // Check which races are completed
                val now = LocalDateTime.now(ZoneId.of("UTC"))
                val completedRacesList = races.filter { race ->
                    try {
                        val raceDateTime = LocalDateTime.parse("${race.date}T${race.time}", DateTimeFormatter.ISO_DATE_TIME)
                        raceDateTime.isBefore(now)
                    } catch (e: Exception) {
                        false
                    }
                }
                
                Log.d("RaceViewModel", "Found ${completedRacesList.size} completed races")
                
                // Fetch results for each completed race
                val updatedRaces = races.map { race ->
                    val isCompleted = completedRacesList.any { it.round == race.round }
                    if (isCompleted) {
                        try {
                            val result = repository.getRaceResultsByCircuit("2025", race.circuit.circuitId)
                            val raceWithResults = result.getOrNull()
                            if (raceWithResults?.results != null) {
                                Log.d("RaceViewModel", "Round ${race.round} (${race.raceName}): ${raceWithResults.results.size} results")
                                race.copy(results = raceWithResults.results)
                            } else {
                                Log.d("RaceViewModel", "Round ${race.round} (${race.raceName}): No results found")
                                race
                            }
                        } catch (e: Exception) {
                            Log.e("RaceViewModel", "Failed to fetch results for ${race.raceName}", e)
                            race
                        }
                    } else {
                        race
                    }
                }
                
                allRacesCache = updatedRaces
                _allRaces.value = allRacesCache
                updateFilteredRaces(allRacesCache)
                
                Log.d("RaceViewModel", "Updated completed races: ${_completedRaces.value.size} races")
                _completedRaces.value.forEach { race ->
                    Log.d("RaceViewModel", "Completed race ${race.round}: results=${race.results?.size ?: 0}")
                }
            } catch (e: Exception) {
                Log.e("RaceViewModel", "Failed to load race results", e)
            }
        }
    }
    
    private fun Race.copy(results: List<com.f1tracker.data.models.RaceResult>?): Race {
        return Race(
            season = this.season,
            round = this.round,
            url = this.url,
            raceName = this.raceName,
            circuit = this.circuit,
            date = this.date,
            time = this.time,
            firstPractice = this.firstPractice,
            secondPractice = this.secondPractice,
            thirdPractice = this.thirdPractice,
            qualifying = this.qualifying,
            sprint = this.sprint,
            sprintQualifying = this.sprintQualifying,
            results = results
        )
    }

    fun loadLastRaceResult() {
        viewModelScope.launch {
            val result = repository.getLastRaceResults("2025")
            result.onSuccess { race ->
                _lastRaceResult.value = race
            }.onFailure { e ->
                Log.e("RaceViewModel", "Failed to load last race result", e)
            }
        }
    }

    fun selectRace(race: Race) {
        _selectedRace.value = race
        
        // Check if race is completed
        val isCompleted = try {
            val now = LocalDateTime.now(ZoneId.of("UTC"))
            val raceDateTime = LocalDateTime.parse("${race.date}T${race.time}", DateTimeFormatter.ISO_DATE_TIME)
            raceDateTime.isBefore(now)
        } catch (e: Exception) {
            false
        }
        
        if (isCompleted) {
            // Fetch current year results for completed races
            loadCurrentYearRaceResults(race.circuit.circuitId)
        } else {
            // Fetch last year results for upcoming races
            loadLastYearRaceResults(race.circuit.circuitId)
        }
    }
    
    private fun loadCurrentYearRaceResults(circuitId: String) {
        viewModelScope.launch {
            val currentSeason = java.time.Year.now().value.toString()
            val result = repository.getRaceResultsByCircuit(currentSeason, circuitId)
            result.onSuccess { race ->
                _lastYearRaceResults.value = race
            }.onFailure { e ->
                Log.e("RaceViewModel", "Failed to load current year race results", e)
                _lastYearRaceResults.value = null
            }
            
            // Also load current year's sprint results
            loadSprintResults(currentSeason, circuitId)
        }
    }

    private fun loadLastYearRaceResults(circuitId: String) {
        viewModelScope.launch {
            val lastSeason = (java.time.Year.now().value - 1).toString()
            val result = repository.getRaceResultsByCircuit(lastSeason, circuitId)
            result.onSuccess { race ->
                _lastYearRaceResults.value = race
            }.onFailure { e ->
                Log.e("RaceViewModel", "Failed to load last year race results", e)
                _lastYearRaceResults.value = null
            }
            
            // Also load last year's sprint results
            loadSprintResults(lastSeason, circuitId)
        }
    }
    
    private val _lastYearSprintResults = MutableStateFlow<List<RaceResult>?>(null)
    val lastYearSprintResults: StateFlow<List<RaceResult>?> = _lastYearSprintResults.asStateFlow()
    
    private fun loadSprintResults(season: String, circuitId: String) {
        viewModelScope.launch {
            val result = repository.getSprintResultsByCircuit(season, circuitId)
            result.onSuccess { results ->
                _lastYearSprintResults.value = results
            }.onFailure { e ->
                Log.e("RaceViewModel", "Failed to load sprint results for $season", e)
                _lastYearSprintResults.value = null
            }
        }
    }

    private fun updateFilteredRaces(races: List<Race>) {
        val now = LocalDateTime.now(ZoneId.of("UTC"))
        
        _upcomingRaces.value = races.filter { race ->
            try {
                val raceDateTime = LocalDateTime.parse("${race.date}T${race.time}", DateTimeFormatter.ISO_DATE_TIME)
                raceDateTime.isAfter(now)
            } catch (e: Exception) {
                false
            }
        }

        _completedRaces.value = races.filter { race ->
            try {
                val raceDateTime = LocalDateTime.parse("${race.date}T${race.time}", DateTimeFormatter.ISO_DATE_TIME)
                raceDateTime.isBefore(now)
            } catch (e: Exception) {
                false
            }
        }.reversed()
    }

    fun forceStateRefresh() {
        viewModelScope.launch {
            updateRaceWeekendState()
        }
    }

    private suspend fun updateRaceWeekendState() {
        val now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"))
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
        // Use the very first session of the weekend (e.g., FP1), not just the first "main" event
        val allEvents = getMainEvents(race)
        val firstEvent = allEvents.firstOrNull()?.second ?: return false
        val lastEvent = getRaceSession(race)
        
        val firstEventStart = parseDateTime(firstEvent.date, firstEvent.time)
        val lastEventEnd = parseDateTime(lastEvent.date, lastEvent.time).plusHours(4) // Extended buffer

        return !now.isBefore(firstEventStart) && now.isBefore(lastEventEnd)
    }

    private fun getFirstMainEvent(race: Race): SessionInfo? {
        return race.sprintQualifying ?: race.qualifying
    }

    private fun getRaceSession(race: Race): SessionInfo {
        return SessionInfo(race.date, race.time)
    }

    private fun getSessionDuration(sessionType: SessionType): Duration {
        return when (sessionType) {
            SessionType.FP1, SessionType.FP2, SessionType.FP3 -> Duration.ofMinutes(75) // 1hr 15mins
            SessionType.QUALIFYING -> Duration.ofMinutes(75) // 1hr 15mins
            SessionType.SPRINT_QUALIFYING -> Duration.ofHours(1) // 1hr
            SessionType.SPRINT -> Duration.ofMinutes(40) // 40mins
            SessionType.RACE -> Duration.ofHours(2) // 2hrs
        }
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
            val sessionEnd = sessionStart.plus(getSessionDuration(type))

            when {
                now.isBefore(sessionStart) -> {
                    upcomingEventsList.add(type to session)
                }
                !now.isBefore(sessionEnd) -> { // Completed (now >= end)
                    completedEvents.add(CompletedEvent(type, emptyList()))
                }
            }
        }

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

        // Fetch ESPN session results for completed sessions
        // Fetch ESPN session results for completed sessions
        val sessionResults = try {
            val round = race.round.toIntOrNull() ?: 0
            if (round > 0) {
                Log.d("RaceViewModel", "Fetching ESPN results for round $round")
                val results = repository.getESPNSessionResults(round).getOrNull() ?: emptyList()
                
                if (results.isEmpty()) {
                    // If fetch failed/empty, try to recover from cache
                    val cachedState = cachedRaceState
                    if (cachedState is RaceWeekendState.Active && cachedState.race.round == race.round && cachedState.sessionResults.isNotEmpty()) {
                        Log.d("RaceViewModel", "Fetch failed/empty, preserving ${cachedState.sessionResults.size} cached session results")
                        cachedState.sessionResults
                    } else {
                        emptyList()
                    }
                } else {
                    Log.d("RaceViewModel", "Fetched ${results.size} session results: ${results.map { it.sessionType }}")
                    results
                }
            } else {
                Log.w("RaceViewModel", "Invalid round number: ${race.round}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("RaceViewModel", "Error fetching ESPN session results", e)
             // Try to recover from cache
            val cachedState = cachedRaceState
            if (cachedState is RaceWeekendState.Active && cachedState.race.round == race.round && cachedState.sessionResults.isNotEmpty()) {
                Log.d("RaceViewModel", "Exception occurred, preserving ${cachedState.sessionResults.size} cached session results")
                cachedState.sessionResults
            } else {
                emptyList()
            }
        }

        return RaceWeekendState.Active(
            race = race,
            currentEvent = currentEvent,
            completedEvents = completedEvents,
            upcomingEvents = upcomingEvents,
            sessionResults = sessionResults
        )
    }

    private fun findCurrentSession(events: List<Pair<SessionType, SessionInfo>>, now: LocalDateTime): SessionEvent? {
        events.forEach { (type, session) ->
            val sessionStartIST = parseDateTime(session.date, session.time)
            val sessionEndIST = sessionStartIST.plus(getSessionDuration(type))
            
            // Check if current time is within the session window
            if (!now.isBefore(sessionStartIST) && now.isBefore(sessionEndIST)) {
                return SessionEvent(
                    sessionType = type,
                    sessionInfo = session,
                    isLive = true,
                    endsAt = sessionEndIST
                )
            }
        }
        return null
    }

    private suspend fun fetchWeatherForSession(
        latitude: Double,
        longitude: Double,
        sessionDateTime: LocalDateTime
    ): SessionWeather? {
        val cacheKey = "${latitude}_${longitude}_${sessionDateTime}"
        weatherCache[cacheKey]?.let { return it }

        val result = repository.getWeather(latitude, longitude)
        val response = result.getOrNull() ?: return null

        // Convert IST session time back to UTC for comparison
        val sessionDateTimeUTC = sessionDateTime
            .atZone(ZoneId.of("Asia/Kolkata"))
            .withZoneSameInstant(ZoneId.of("UTC"))
            .toLocalDateTime()

        var closestIndex = -1
        var minDiff = Long.MAX_VALUE

        response.hourly.time.forEachIndexed { index, timeString ->
            try {
                val weatherTime = LocalDateTime.parse(timeString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                val diff = Duration.between(weatherTime, sessionDateTimeUTC).abs().toMinutes()
                
                if (diff < minDiff) {
                    minDiff = diff
                    closestIndex = index
                }
            } catch (e: Exception) {
                Log.e("RaceViewModel", "Error parsing weather time", e)
            }
        }

        if (closestIndex != -1 && minDiff < 180) {
            val temp = response.hourly.temperature[closestIndex].toInt()
            val rainChance = response.hourly.precipitationProbability.getOrNull(closestIndex) ?: 0
            val weatherCode = response.hourly.weatherCode.getOrNull(closestIndex) ?: 0
            
            val weather = SessionWeather(
                temperature = temp,
                rainChance = rainChance,
                weatherCode = weatherCode,
                weatherIcon = WeatherIcon.fromWMOCode(weatherCode)
            )
            weatherCache[cacheKey] = weather
            return weather
        }
        return null
    }

    private suspend fun buildComingUpState(race: Race): RaceWeekendState.ComingUp {
        val allEvents = getMainEvents(race)
        val nextMainEvent = allEvents.first { it.first.isMainEvent() }
        val upcomingEvents = fetchWeatherForEvents(race, allEvents)
        
        return RaceWeekendState.ComingUp(
            race = race,
            nextMainEvent = nextMainEvent.second,
            nextMainEventType = nextMainEvent.first,
            upcomingEvents = upcomingEvents
        )
    }

    private suspend fun fetchWeatherForEvents(
        race: Race,
        events: List<Pair<SessionType, SessionInfo>>
    ): List<UpcomingEvent> {
        val latitude = race.circuit.location.lat.toDouble()
        val longitude = race.circuit.location.long.toDouble()
        val now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"))
        
        return events.map { (type, session) ->
            val sessionDateTime = parseDateTime(session.date, session.time)
            val sessionEndTime = sessionDateTime.plus(getSessionDuration(type))
            val isCompleted = !now.isBefore(sessionEndTime)
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

    private fun getMainEvents(race: Race): List<Pair<SessionType, SessionInfo>> {
        val events = mutableListOf<Pair<SessionType, SessionInfo>>()
        race.firstPractice?.let { events.add(SessionType.FP1 to it) }
        race.secondPractice?.let { events.add(SessionType.FP2 to it) }
        race.thirdPractice?.let { events.add(SessionType.FP3 to it) }
        race.sprintQualifying?.let { events.add(SessionType.SPRINT_QUALIFYING to it) }
        race.qualifying?.let { events.add(SessionType.QUALIFYING to it) }
        race.sprint?.let { events.add(SessionType.SPRINT to it) }
        events.add(SessionType.RACE to SessionInfo(race.date, race.time))
        return events.sortedBy { parseDateTime(it.second.date, it.second.time) }
    }

    private fun parseDateTime(date: String, time: String): LocalDateTime {
        // Ergast API returns UTC (Zulu) time: "2025-11-28T13:30:00Z"
        // Rule: Always treat as UTC, convert to IST
        val dateTimeString = "${date}T${time}"
        return try {
            val instant = if (time.endsWith("Z")) {
                java.time.Instant.parse(dateTimeString)
            } else {
                // Add Z suffix if missing (Ergast sometimes omits it)
                java.time.Instant.parse("${dateTimeString}Z")
            }
            // Convert UTC to IST (Asia/Kolkata = UTC+5:30)
            instant.atZone(ZoneId.of("Asia/Kolkata")).toLocalDateTime()
        } catch (e: Exception) {
            Log.e("RaceViewModel", "Error parsing Zulu time: $dateTimeString", e)
            LocalDateTime.now(ZoneId.of("Asia/Kolkata"))
        }
    }
    
    fun getCountdownTo(targetDateTime: LocalDateTime): String {
        val now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"))
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
}
