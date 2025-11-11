package com.f1tracker.data.local

data class DriverInfo(
    val id: String,
    val code: String,
    val givenName: String,
    val familyName: String,
    val fullName: String,
    val team: String,
    val headshotF1: String?,
    val headshotBackgroundUrl: String?,
    val headshotNumberUrl: String?
)

data class TeamInfo(
    val id: String,
    val name: String,
    val displayName: String,
    val abbreviation: String,
    val color: String,
    val symbolUrl: String,
    val fullLogoUrl: String,
    val carImageUrl: String? = null
)

object F1DataProvider {
    private val driversMap = mutableMapOf<String, DriverInfo>()
    private val teamsMap = mutableMapOf<String, TeamInfo>()
    
    fun getDriverByApiId(apiDriverId: String): DriverInfo? {
        return driversMap[apiDriverId]
    }
    
    fun getTeamByApiId(apiTeamId: String): TeamInfo? {
        return teamsMap[apiTeamId]
    }
    
    fun loadData(driversJson: String, teamsJson: String) {
        // Parse drivers JSON
        try {
            val driversData = parseDriversJson(driversJson)
            driversMap.putAll(driversData)
        } catch (e: Exception) {
            android.util.Log.e("F1DataProvider", "Failed to parse drivers JSON: ${e.message}")
        }
        
        // Parse teams JSON
        try {
            val teamsData = parseTeamsJson(teamsJson)
            teamsMap.putAll(teamsData)
        } catch (e: Exception) {
            android.util.Log.e("F1DataProvider", "Failed to parse teams JSON: ${e.message}")
        }
    }
    
    private fun parseDriversJson(json: String): Map<String, DriverInfo> {
        val map = mutableMapOf<String, DriverInfo>()
        
        // Simple manual JSON parsing for the specific structure
        val driversMatch = Regex(""""(\w+)":\s*\{[^}]*"id":\s*"([^"]+)"[^}]*"code":\s*"([^"]+)"[^}]*"givenName":\s*"([^"]+)"[^}]*"familyName":\s*"([^"]+)"[^}]*"fullName":\s*"([^"]+)"[^}]*"team":\s*"([^"]+)"[^}]*"headshotF1":\s*"([^"]+)"[^}]*"headshotBackgroundUrl":\s*"([^"]+)"[^}]*"headshotNumberUrl":\s*"([^"]+)"""")
            .findAll(json)
        
        driversMatch.forEach { match ->
            val id = match.groupValues[2]
            val driver = DriverInfo(
                id = id,
                code = match.groupValues[3],
                givenName = match.groupValues[4],
                familyName = match.groupValues[5],
                fullName = match.groupValues[6],
                team = match.groupValues[7],
                headshotF1 = match.groupValues[8],
                headshotBackgroundUrl = match.groupValues[9],
                headshotNumberUrl = match.groupValues[10]
            )
            map[id] = driver
        }
        
        return map
    }
    
    private fun parseTeamsJson(json: String): Map<String, TeamInfo> {
        val map = mutableMapOf<String, TeamInfo>()
        
        val teamsMatch = Regex(""""(\w+)":\s*\{[^}]*"id":\s*"([^"]+)"[^}]*"name":\s*"([^"]+)"[^}]*"displayName":\s*"([^"]+)"[^}]*"abbreviation":\s*"([^"]+)"[^}]*"color":\s*"([^"]+)"[^}]*"carImageUrl":\s*"([^"]+)"[^}]*"symbolUrl":\s*"([^"]+)"[^}]*"fullLogoUrl":\s*"([^"]+)"""")
            .findAll(json)
        
        teamsMatch.forEach { match ->
            val id = match.groupValues[2]
            val team = TeamInfo(
                id = id,
                name = match.groupValues[3],
                displayName = match.groupValues[4],
                abbreviation = match.groupValues[5],
                color = match.groupValues[6],
                symbolUrl = match.groupValues[8],
                fullLogoUrl = match.groupValues[9],
                carImageUrl = match.groupValues[7]
            )
            map[id] = team
        }
        
        return map
    }
}

