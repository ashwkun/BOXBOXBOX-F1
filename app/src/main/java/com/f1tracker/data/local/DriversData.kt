package com.f1tracker.data.local

data class DriverInfo(
    val id: String,
    val permanentNumber: String,  // Racing number (e.g., "1" for Verstappen, "44" for Hamilton)
    val code: String,
    val givenName: String,
    val familyName: String,
    val fullName: String,
    val team: String,
    val espnId: String? = null,  // ESPN athlete ID
    val headshotF1: String?,
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
    private val racingNumberMap = mutableMapOf<String, DriverInfo>()  // Racing number -> DriverInfo
    
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
    
    // ===== NEW CENTRALIZED METHODS =====
    
    /**
     * Get driver by racing number (e.g., "1" for Verstappen, "44" for Hamilton)
     * Used by SignalR live timing client
     */
    fun getDriverByRacingNumber(number: String): DriverInfo? {
        return racingNumberMap[number]
    }
    
    /**
     * Get team color as hex string (e.g., "E8002D" for Ferrari)
     * Returns null if team not found
     */
    fun getTeamColorHex(teamId: String): String? {
        return teamsMap[teamId]?.color
    }
    
    /**
     * Get team color by team name (fuzzy match)
     * Used for live timing where we get team display name
     */
    fun getTeamColorByName(teamName: String): String? {
        val team = teamsMap.values.find { 
            teamName.contains(it.displayName, ignoreCase = true) ||
            teamName.contains(it.name, ignoreCase = true) ||
            it.displayName.contains(teamName, ignoreCase = true)
        }
        return team?.color
    }
    
    /**
     * Get all loaded drivers (for iteration)
     */
    fun getAllDrivers(): Collection<DriverInfo> = driversMap.values
    
    /**
     * Get all loaded teams (for iteration)
     */
    fun getAllTeams(): Collection<TeamInfo> = teamsMap.values
    
    /**
     * Check if data is loaded
     */
    fun isDataLoaded(): Boolean = driversMap.isNotEmpty() && teamsMap.isNotEmpty()
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // URL GENERATION UTILITIES
    // Fallback URL builders for when JSON data isn't available (historical/new teams)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    private const val F1_CDN_BASE = "https://media.formula1.com/image/upload"
    
    /**
     * Team API ID → F1.com URL slug mapping
     * This needs to be updated when teams rebrand
     */
    private val teamSlugMap = mapOf(
        // 2025 Teams
        "red_bull" to "redbullracing",
        "rb" to "racingbulls",
        "ferrari" to "ferrari",
        "mclaren" to "mclaren",
        "mercedes" to "mercedes",
        "aston_martin" to "astonmartin",
        "alpine" to "alpine",
        "williams" to "williams",
        "sauber" to "kicksauber",
        "haas" to "haasf1team",
        // Historical teams (for standings history)
        "alfa" to "alfaromeo",
        "alphatauri" to "alphatauri",
        "renault" to "renault",
        "racing_point" to "racingpoint",
        "toro_rosso" to "tororosso"
    )
    
    /**
     * Get F1.com URL slug for a team
     */
    fun getTeamSlug(teamId: String): String {
        return teamSlugMap[teamId] ?: teamId.replace("_", "").lowercase()
    }
    
    /**
     * Generate driver code from name (e.g., "Max Verstappen" → "maxver01")
     */
    fun generateDriverCode(givenName: String, familyName: String): String {
        val first = givenName.take(3).lowercase()
        val last = familyName.take(3).lowercase()
        return "${first}${last}01"
    }
    
    /**
     * Generate fallback headshot URL for a driver
     * @param year Season year (e.g., "2025")
     * @param teamId Team API ID (e.g., "red_bull")
     * @param givenName Driver's first name
     * @param familyName Driver's last name
     */
    fun generateDriverHeadshotUrl(year: String, teamId: String, givenName: String, familyName: String): String {
        val teamSlug = getTeamSlug(teamId)
        val driverCode = generateDriverCode(givenName, familyName)
        return "$F1_CDN_BASE/c_lfill,w_440/q_auto/v1740000000/common/f1/$year/$teamSlug/$driverCode/$year$teamSlug${driverCode}right.webp"
    }
    
    /**
     * Generate fallback number card URL for a driver
     */
    fun generateDriverNumberUrl(year: String, teamId: String, givenName: String, familyName: String): String {
        val teamSlug = getTeamSlug(teamId)
        val driverCode = generateDriverCode(givenName, familyName)
        return "$F1_CDN_BASE/c_fit,w_876,h_742/q_auto/v1740000000/common/f1/$year/$teamSlug/$driverCode/$year$teamSlug${driverCode}numberwhitefrless.webp"
    }
    
    /**
     * Generate fallback car image URL for a team
     */
    fun generateCarImageUrl(year: String, teamId: String): String {
        val teamSlug = getTeamSlug(teamId)
        return "$F1_CDN_BASE/c_lfill,h_224/q_auto/d_common:f1:$year:fallback:car:${year}fallbackcarright.webp/v1740000000/common/f1/$year/$teamSlug/$year${teamSlug}carright.webp"
    }
    
    /**
     * Generate fallback team logo URL
     */
    fun generateTeamLogoUrl(year: String, teamId: String, size: Int = 256): String {
        val teamSlug = getTeamSlug(teamId)
        return "$F1_CDN_BASE/c_lfill,w_$size/q_auto/v1740000000/common/f1/$year/$teamSlug/$year${teamSlug}logowhite.webp"
    }
    
    /**
     * Get driver headshot - tries JSON first, falls back to generated URL
     */
    fun getDriverHeadshotWithFallback(driverId: String?, givenName: String, familyName: String, teamId: String, year: String = "2025"): String {
        // First try exact match from JSON
        val driver = driverId?.let { getDriverByApiId(it) }
        if (driver?.headshotF1 != null) return driver.headshotF1
        
        // Try by name
        val driverByName = getDriverByName("$givenName $familyName")
        if (driverByName?.headshotF1 != null) return driverByName.headshotF1
        
        // Generate fallback URL
        return generateDriverHeadshotUrl(year, teamId, givenName, familyName)
    }
    
    /**
     * Get team car image - tries JSON first, falls back to generated URL
     */
    fun getCarImageWithFallback(teamId: String, year: String = "2025"): String {
        val team = getTeamByApiId(teamId)
        if (team?.carImageUrl != null) return team.carImageUrl
        
        return generateCarImageUrl(year, teamId)
    }
    
    fun loadData(driversJson: String, teamsJson: String) {
        // Parse drivers JSON
        try {
            val driversData = parseDriversJson(driversJson)
            driversMap.putAll(driversData)
            
            // Build ESPN ID map and racing number map
            driversData.forEach { (_, driver) ->
                driver.espnId?.let { espnId ->
                    espnIdMap[espnId] = driver
                }
                racingNumberMap[driver.permanentNumber] = driver
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
        
        // Regex captures only fields we actually use (skips headshotBackgroundUrl)
        val driversMatch = Regex(""""(\w+)":\s*\{[^}]*"id":\s*"([^"]+)"[^}]*"espnId":\s*"?([^",}]+)"?[^}]*"permanentNumber":\s*"([^"]+)"[^}]*"code":\s*"([^"]+)"[^}]*"givenName":\s*"([^"]+)"[^}]*"familyName":\s*"([^"]+)"[^}]*"fullName":\s*"([^"]+)"[^}]*"team":\s*"([^"]+)"[^}]*"headshotF1":\s*"([^"]+)"[^}]*"headshotNumberUrl":\s*"([^"]+)"""")
            .findAll(json)
        
        driversMatch.forEach { match ->
            val id = match.groupValues[2]
            val espnIdStr = match.groupValues[3]
            val driver = DriverInfo(
                id = id,
                permanentNumber = match.groupValues[4],
                code = match.groupValues[5],
                givenName = match.groupValues[6],
                familyName = match.groupValues[7],
                fullName = match.groupValues[8],
                team = match.groupValues[9],
                espnId = if (espnIdStr.isNotBlank() && espnIdStr != "null") espnIdStr else null,
                headshotF1 = match.groupValues[10],
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

