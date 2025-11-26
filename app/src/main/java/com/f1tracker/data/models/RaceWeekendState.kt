package com.f1tracker.data.models

import java.time.LocalDateTime

sealed class RaceWeekendState {
    data class ComingUp(
        val race: Race,
        val nextMainEvent: SessionInfo,
        val nextMainEventType: SessionType,
        val upcomingEvents: List<UpcomingEvent>
    ) : RaceWeekendState()
    
    data class Active(
        val race: Race,
        val currentEvent: SessionEvent?,
        val completedEvents: List<CompletedEvent>,
        val upcomingEvents: List<UpcomingEvent>
    ) : RaceWeekendState()
    
    object Loading : RaceWeekendState()
    data class Error(val message: String) : RaceWeekendState()
}

data class UpcomingEvent(
    val sessionType: SessionType,
    val sessionInfo: SessionInfo,
    val isNext: Boolean = false,
    val weather: SessionWeather? = null,
    val isCompleted: Boolean = false
)

data class CompletedEvent(
    val sessionType: SessionType,
    val topThree: List<ResultEntry>
)

data class ResultEntry(
    val position: Int,
    val driverCode: String,
    val driverName: String,
    val team: String
)

data class SessionEvent(
    val sessionType: SessionType,
    val sessionInfo: SessionInfo,
    val isLive: Boolean,
    val endsAt: LocalDateTime?
)

enum class SessionType {
    FP1,
    FP2,
    FP3,
    SPRINT_QUALIFYING,
    QUALIFYING,
    SPRINT,
    RACE;
    
    fun displayName(): String = when (this) {
        FP1 -> "Practice 1"
        FP2 -> "Practice 2"
        FP3 -> "Practice 3"
        SPRINT_QUALIFYING -> "Sprint Qualifying"
        QUALIFYING -> "Qualifying"
        SPRINT -> "Sprint"
        RACE -> "Race"
    }
    
    fun priority(): Int = when (this) {
        RACE -> 1
        QUALIFYING -> 2
        SPRINT -> 3
        SPRINT_QUALIFYING -> 4
        FP3 -> 5
        FP2 -> 6
        FP1 -> 7
    }
    
    fun isMainEvent(): Boolean = when (this) {
        SPRINT_QUALIFYING, QUALIFYING, SPRINT, RACE -> true
        FP1, FP2, FP3 -> false
    }
}

