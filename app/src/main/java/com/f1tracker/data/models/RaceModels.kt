package com.f1tracker.data.models

import com.google.gson.annotations.SerializedName

// Root response for races endpoint
data class RacesResponse(
    @SerializedName("MRData") val mrData: MRData
)

data class MRData(
    @SerializedName("RaceTable") val raceTable: RaceTable
)

data class RaceTable(
    @SerializedName("season") val season: String,
    @SerializedName("Races") val races: List<Race>
)

data class Race(
    @SerializedName("season") val season: String,
    @SerializedName("round") val round: String,
    @SerializedName("url") val url: String,
    @SerializedName("raceName") val raceName: String,
    @SerializedName("Circuit") val circuit: Circuit,
    @SerializedName("date") val date: String,
    @SerializedName("time") val time: String,
    @SerializedName("FirstPractice") val firstPractice: SessionInfo?,
    @SerializedName("SecondPractice") val secondPractice: SessionInfo?,
    @SerializedName("ThirdPractice") val thirdPractice: SessionInfo?,
    @SerializedName("Qualifying") val qualifying: SessionInfo?,
    @SerializedName("Sprint") val sprint: SessionInfo?,
    @SerializedName("SprintQualifying") val sprintQualifying: SessionInfo?,
    @SerializedName("Results") val results: List<RaceResult>? // For race results
)

data class Circuit(
    @SerializedName("circuitId") val circuitId: String,
    @SerializedName("url") val url: String,
    @SerializedName("circuitName") val circuitName: String,
    @SerializedName("Location") val location: Location
)

data class Location(
    @SerializedName("lat") val lat: String,
    @SerializedName("long") val long: String,
    @SerializedName("locality") val locality: String,
    @SerializedName("country") val country: String
)

data class SessionInfo(
    @SerializedName("date") val date: String,
    @SerializedName("time") val time: String
)

// Qualifying results
data class QualifyingResponse(
    @SerializedName("MRData") val mrData: QualifyingMRData
)

data class QualifyingMRData(
    @SerializedName("RaceTable") val raceTable: QualifyingRaceTable
)

data class QualifyingRaceTable(
    @SerializedName("season") val season: String,
    @SerializedName("Races") val races: List<RaceWithQualifying>
)

data class RaceWithQualifying(
    @SerializedName("season") val season: String,
    @SerializedName("round") val round: String,
    @SerializedName("raceName") val raceName: String,
    @SerializedName("QualifyingResults") val qualifyingResults: List<QualifyingResult>
)

data class QualifyingResult(
    @SerializedName("number") val number: String,
    @SerializedName("position") val position: String,
    @SerializedName("Driver") val driver: Driver,
    @SerializedName("Constructor") val constructor: Constructor,
    @SerializedName("Q1") val q1: String?,
    @SerializedName("Q2") val q2: String?,
    @SerializedName("Q3") val q3: String?
)

data class Driver(
    @SerializedName("driverId") val driverId: String,
    @SerializedName("permanentNumber") val permanentNumber: String?,
    @SerializedName("code") val code: String,
    @SerializedName("givenName") val givenName: String,
    @SerializedName("familyName") val familyName: String
)

data class Constructor(
    @SerializedName("constructorId") val constructorId: String,
    @SerializedName("name") val name: String,
    @SerializedName("nationality") val nationality: String
)

// Sprint results (similar structure)
data class SprintResponse(
    @SerializedName("MRData") val mrData: SprintMRData
)

data class SprintMRData(
    @SerializedName("RaceTable") val raceTable: SprintRaceTable
)

data class SprintRaceTable(
    @SerializedName("season") val season: String,
    @SerializedName("Races") val races: List<RaceWithSprint>
)

data class RaceWithSprint(
    @SerializedName("season") val season: String,
    @SerializedName("round") val round: String,
    @SerializedName("raceName") val raceName: String,
    @SerializedName("SprintResults") val sprintResults: List<SprintResult>
)

data class SprintResult(
    @SerializedName("number") val number: String,
    @SerializedName("position") val position: String,
    @SerializedName("Driver") val driver: Driver,
    @SerializedName("Constructor") val constructor: Constructor,
    @SerializedName("Time") val time: Time?,
    @SerializedName("status") val status: String
)

data class Time(
    @SerializedName("time") val time: String
)

// Race results
data class RaceResult(
    @SerializedName("number") val number: String,
    @SerializedName("position") val position: String,
    @SerializedName("positionText") val positionText: String,
    @SerializedName("points") val points: String,
    @SerializedName("Driver") val driver: Driver,
    @SerializedName("Constructor") val constructor: Constructor,
    @SerializedName("grid") val grid: String,
    @SerializedName("laps") val laps: String,
    @SerializedName("status") val status: String,
    @SerializedName("Time") val time: Time?,
    @SerializedName("FastestLap") val fastestLap: FastestLap?
)

data class FastestLap(
    @SerializedName("rank") val rank: String,
    @SerializedName("lap") val lap: String,
    @SerializedName("Time") val time: Time
)

// Driver Standings
data class DriverStandingsResponse(
    @SerializedName("MRData") val mrData: StandingsMRData
)

data class StandingsMRData(
    @SerializedName("StandingsTable") val standingsTable: StandingsTable
)

data class StandingsTable(
    @SerializedName("season") val season: String,
    @SerializedName("StandingsLists") val standingsLists: List<StandingsList>
)

data class StandingsList(
    @SerializedName("season") val season: String,
    @SerializedName("round") val round: String,
    @SerializedName("DriverStandings") val driverStandings: List<DriverStanding>
)

data class DriverStanding(
    @SerializedName("position") val position: String,
    @SerializedName("positionText") val positionText: String,
    @SerializedName("points") val points: String,
    @SerializedName("wins") val wins: String,
    @SerializedName("Driver") val driver: Driver,
    @SerializedName("Constructors") val constructors: List<Constructor>
)

// Constructor Standings
data class ConstructorStandingsResponse(
    @SerializedName("MRData") val mrData: ConstructorStandingsMRData
)

data class ConstructorStandingsMRData(
    @SerializedName("StandingsTable") val standingsTable: ConstructorStandingsTable
)

data class ConstructorStandingsTable(
    @SerializedName("season") val season: String,
    @SerializedName("StandingsLists") val standingsLists: List<ConstructorStandingsList>
)

data class ConstructorStandingsList(
    @SerializedName("season") val season: String,
    @SerializedName("round") val round: String,
    @SerializedName("ConstructorStandings") val constructorStandings: List<ConstructorStanding>
)

data class ConstructorStanding(
    @SerializedName("position") val position: String,
    @SerializedName("positionText") val positionText: String,
    @SerializedName("points") val points: String,
    @SerializedName("wins") val wins: String,
    @SerializedName("Constructor") val constructor: Constructor
)

// ESPN F1 News API Models
data class NewsResponse(
    @SerializedName("header") val header: String,
    @SerializedName("articles") val articles: List<NewsArticle>
)

data class NewsArticle(
    @SerializedName("id") val id: Long,
    @SerializedName("headline") val headline: String,
    @SerializedName("description") val description: String,
    @SerializedName("published") val published: String,
    @SerializedName("images") val images: List<NewsImage>?,
    @SerializedName("links") val links: NewsLinks?
)

data class NewsImage(
    @SerializedName("name") val name: String?,
    @SerializedName("url") val url: String,
    @SerializedName("height") val height: Int,
    @SerializedName("width") val width: Int,
    @SerializedName("type") val type: String?
)

data class NewsLinks(
    @SerializedName("web") val web: NewsWebLink?
)

data class NewsWebLink(
    @SerializedName("href") val href: String
)

