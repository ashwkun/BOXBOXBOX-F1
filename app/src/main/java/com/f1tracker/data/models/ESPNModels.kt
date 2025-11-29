package com.f1tracker.data.models

data class ESPNScoreboard(
    val leagues: List<ESPNLeague>,
    val events: List<ESPNRaceEvent>
)

data class ESPNLeague(
    val id: String,
    val season: ESPNSeason,
    val calendar: List<ESPNCalendarItem>
)

data class ESPNSeason(
    val year: Int
)

data class ESPNCalendarItem(
    val label: String,
    val event: ESPNEventRef
)

data class ESPNEventRef(
    val `$ref`: String
)

// Main race event data
data class ESPNRaceEvent(
    val id: String,
    val name: String,
    val competitions: List<ESPNCompetition>
)

data class ESPNCompetition(
    val id: String,
    val date: String,
    val type: ESPNSessionType,
    val status: ESPNStatus,
    val competitors: List<ESPNCompetitor>? = null
)

data class ESPNSessionType(
    val id: String,
    val abbreviation: String
)

data class ESPNStatus(
    val type: ESPNStatusType
)

data class ESPNStatusType(
    val id: String,
    val name: String,
    val completed: Boolean
)

data class ESPNCompetitor(
    val order: Int,  // Position (1-20)
    val athlete: ESPNAthlete,
    val statistics: List<ESPNStatistic>? = null
)

data class ESPNAthlete(
    val id: String?,  // ESPN athlete ID
    val fullName: String,
    val displayName: String,
    val shortName: String
)

data class ESPNStatistic(
    val name: String,
    val displayValue: String
)

// Processed session result for app use
data class SessionResult(
    val sessionType: SessionType,
    val sessionName: String,
    val results: List<DriverResult>
)

data class DriverResult(
    val position: Int,
    val driverCode: String,  // 3-letter code (VER, NOR, etc.)
    val driverName: String,
    val team: String? = null,
    val time: String? = null,  // Lap time or gap to leader
    val espnId: String  // For navigation/details
)
