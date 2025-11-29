package com.f1tracker.data.local

data class DriverInfo(
    val id: String,
    val code: String,
    val givenName: String,
    val familyName: String,
    val fullName: String,
    val team: String,
    val espnId: String? = null,  // ESPN athlete ID
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
    private val espnIdMap = mutableMapOf<String, DriverInfo>()  // ESPN ID -> DriverInfo
    
    fun getDriverByApiId(apiDriverId: String): DriverInfo? {
        return driversMap[apiDriverId]
    }
    
    fun getDriverByESPNId(espnId: String): DriverInfo? {
        return espnIdMap[espnId]
    }
    
    fun getDriverByName(name: String): DriverInfo? {
        // Try exact match on full name, or contains match
        return driversMap.values.find { 
            it.fullName.equals(name, ignoreCase = true) || 
            it.familyName.equals(name, ignoreCase = true) ||
            it.givenName.equals(name, ignoreCase = true) ||
            // Handle "Max Verstappen" vs "Verstappen"
            name.contains(it.familyName, ignoreCase = true)
        }
    }
    
    fun getTeamByApiId(apiTeamId: String): TeamInfo? {
        return teamsMap[apiTeamId]
    }

    fun getTeamColorByDriverCode(driverCode: String): String? {
        val driver = driversMap.values.find { it.code == driverCode }
        if (driver == null) {
            android.util.Log.e("F1DataProvider", "Driver not found for code: $driverCode")
            return null
        }
        
        // driver.team is the team ID (e.g. "red_bull"), so we can look it up directly
        val team = teamsMap[driver.team]
        return team?.color
    }
    
    fun loadData(driversJson: String, teamsJson: String) {
        // Parse drivers JSON
        try {
            val driversData = parseDriversJson(driversJson)
            driversMap.putAll(driversData)
            
            // Build ESPN ID map
            driversData.forEach { (_, driver) ->
                driver.espnId?.let { espnId ->
                    espnIdMap[espnId] = driver
                }
            }
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
        
        // Updated regex to capture espnId
        val driversMatch = Regex(""""(\w+)":\s*\{[^}]*"id":\s*"([^"]+)"[^}]*"espnId":\s*"?([^",}]+)"?[^}]*"code":\s*"([^"]+)"[^}]*"givenName":\s*"([^"]+)"[^}]*"familyName":\s*"([^"]+)"[^}]*"fullName":\s*"([^"]+)"[^}]*"team":\s*"([^"]+)"[^}]*"headshotF1":\s*"([^"]+)"[^}]*"headshotBackgroundUrl":\s*"([^"]+)"[^}]*"headshotNumberUrl":\s*"([^"]+)"""")
            .findAll(json)
        
        driversMatch.forEach { match ->
            val id = match.groupValues[2]
            val espnIdStr = match.groupValues[3]
            val driver = DriverInfo(
                id = id,
                code = match.groupValues[4],
                givenName = match.groupValues[5],
                familyName = match.groupValues[6],
                fullName = match.groupValues[7],
                team = match.groupValues[8],
                espnId = if (espnIdStr.isNotBlank() && espnIdStr != "null") espnIdStr else null,
                headshotF1 = match.groupValues[9],
                headshotBackgroundUrl = match.groupValues[10],
                headshotNumberUrl = match.groupValues[11]
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

