package com.f1tracker.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f1tracker.R
import com.f1tracker.data.live.SignalRLiveTimingClient
import com.f1tracker.data.local.F1DataProvider
import com.f1tracker.data.live.LiveDriver

@Composable
fun LiveScreen(
    raceViewModel: com.f1tracker.ui.viewmodels.RaceViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val brigendsFont = FontFamily(Font(R.font.brigends_expanded, FontWeight.Normal))
    val michromaFont = FontFamily(Font(R.font.michroma, FontWeight.Normal))
    
    val liveClient = remember { SignalRLiveTimingClient.getInstance() }
    val liveDrivers by liveClient.liveDrivers.collectAsState()
    val sessionName by liveClient.sessionName.collectAsState()
    val isConnected by liveClient.isConnected.collectAsState()
    val connectionError by liveClient.connectionError.collectAsState()
    
    // Race weekend state for session detection
    val raceWeekendState by raceViewModel.raceWeekendState.collectAsState()
    
    var showRawView by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf(30) }
    var countdownFinished by remember { mutableStateOf(false) }
    
    // Countdown timer when waiting for data
    LaunchedEffect(isConnected, liveDrivers.isEmpty()) {
        if (isConnected && liveDrivers.isEmpty()) {
            countdown = 30
            countdownFinished = false
            while (countdown > 0 && liveDrivers.isEmpty()) {
                kotlinx.coroutines.delay(1000)
                countdown--
            }
            if (liveDrivers.isEmpty()) {
                countdownFinished = true
            }
        }
    }
    
    // Connection is now managed at MainActivity level - persists across navigation
    // Just observe connection state here
    val shouldConnect = remember(raceWeekendState) {
        when (val state = raceWeekendState) {
            is com.f1tracker.data.models.RaceWeekendState.Active -> {
                state.currentEvent?.isLive == true
            }
            else -> false
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Header - only show when connecting/connected to live session
        if (shouldConnect) {
            Column {
                // Top Bar with Session Name, Lap Counter and Connection Status
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (sessionName.isNotEmpty()) sessionName.uppercase() else "LIVE TIMING",
                                fontFamily = michromaFont,
                                fontSize = 12.sp,
                                color = Color.White,
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold
                            )
                            
                            // Master Lap Counter (Inline)
                            if (liveDrivers.isNotEmpty()) {
                                val leader = liveDrivers.firstOrNull()
                                val currentLap = leader?.laps ?: "-"
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                Container(
                                    modifier = Modifier
                                        .background(Color(0xFF1A1A1A), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "LAP $currentLap",
                                        fontFamily = michromaFont,
                                        fontSize = 10.sp,
                                        color = Color(0xFFE6007E),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Animated connection indicator
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        if (isConnected) Color(0xFF00FF41) else if (connectionError != null) Color(0xFFFF0040) else Color(0xFFFFAA00),
                                        RoundedCornerShape(3.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when {
                                    !isConnected && connectionError != null -> "ERROR"
                                    !isConnected -> "CONNECTING..."
                                    liveDrivers.isEmpty() -> "WAITING..."
                                    else -> "LIVE"
                                },
                                fontFamily = michromaFont,
                                fontSize = 8.sp,
                                color = when {
                                    !isConnected && connectionError != null -> Color(0xFFFF0040)
                                    !isConnected -> Color(0xFFFFAA00)
                                    liveDrivers.isEmpty() -> Color(0xFFFFAA00)
                                    else -> Color(0xFF00FF41)
                                },
                                letterSpacing = 0.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // View mode buttons
                        if (liveDrivers.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (!showRawView) Color(0xFFE6007E).copy(alpha = 0.3f) else Color(0xFF1A1A1A),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (!showRawView) Color(0xFFE6007E) else Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .clickable { showRawView = false }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "GRID",
                                    fontFamily = michromaFont,
                                    fontSize = 8.sp,
                                    color = if (!showRawView) Color.White else Color.White.copy(alpha = 0.6f),
                                    letterSpacing = 0.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (showRawView) Color(0xFFE6007E).copy(alpha = 0.3f) else Color(0xFF1A1A1A),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (showRawView) Color(0xFFE6007E) else Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .clickable { showRawView = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "RAW",
                                    fontFamily = michromaFont,
                                    fontSize = 8.sp,
                                    color = if (showRawView) Color.White else Color.White.copy(alpha = 0.6f),
                                    letterSpacing = 0.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        // Reconnect button
                        if (connectionError != null) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        Color(0xFFE6007E),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .clickable { liveClient.connect() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "RETRY",
                                    fontFamily = michromaFont,
                                    fontSize = 8.sp,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
        
        when {
            // No active session - show countdown
            !shouldConnect -> {
                NoSessionActiveScreen(
                    raceWeekendState = raceWeekendState,
                    raceViewModel = raceViewModel,
                    brigendsFont = brigendsFont,
                    michromaFont = michromaFont
                )
            }
            // Show raw table view
            showRawView && liveDrivers.isNotEmpty() -> {
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .padding(16.dp)
                ) {
                    item {
                        androidx.compose.foundation.text.selection.SelectionContainer {
                            Column {
                                // Header
                                Text(
                                    text = "═══════════════════════════════════════════════════════════════════════════════════",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = Color(0xFF00FF41),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    text = String.format("%-5s %-20s %-20s %-15s %-10s %-6s", 
                                        "POS", "DRIVER", "TEAM", "TIME", "GAP", "PITS"),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = Color(0xFF00FF41),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "───────────────────────────────────────────────────────────────────────────────────",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = Color(0xFF00FF41),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                
                                // Drivers
                                liveDrivers.forEachIndexed { index, driver ->
                                    val driverName = "${driver.firstName ?: ""} ${driver.lastName ?: ""}".trim().take(20)
                                    val team = (driver.externalTeam ?: "").take(20)
                                    val time = (driver.raceTime ?: " ").take(15)
                                    val gap = calculateGapToCarAhead(liveDrivers, index).take(10)
                                    
                                    Text(
                                        text = String.format("%-5s %-20s %-20s %-15s %-10s %-6s",
                                            driver.positionDisplay ?: "-",
                                            driverName,
                                            team,
                                            time,
                                            gap,
                                            driver.pits ?: "0"
                                        ),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = Color(0xFF00FF41),
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            
            // Loading state
            !isConnected && connectionError == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            color = Color(0xFFE6007E),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "CONNECTING",
                            fontFamily = brigendsFont,
                            fontSize = 12.sp,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
            
            // Error state
            connectionError != null && liveDrivers.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WifiOff,
                            contentDescription = "Error",
                            tint = Color(0xFFFF0080),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Connection Error",
                            fontFamily = brigendsFont,
                            fontSize = 12.sp,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
            
            // No data but connected - with countdown
            liveDrivers.isEmpty() && isConnected -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Animated waiting circle
                        Box(
                            modifier = Modifier.size(60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(60.dp),
                                color = Color(0xFFFFAA00).copy(alpha = 0.3f),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "$countdown",
                                fontFamily = michromaFont,
                                fontSize = 18.sp,
                                color = Color(0xFFFFAA00),
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "AWAITING DATA",
                            fontFamily = brigendsFont,
                            fontSize = 12.sp,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
            
            // Show live data
            else -> {
                Column {
                    // Column Headers
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "POS",
                            fontFamily = michromaFont,
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.4f),
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.width(32.dp),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "DRIVER",
                            fontFamily = michromaFont,
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.4f),
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.width(52.dp),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Visible
                        )
                        Text(
                            text = "GAP",
                            fontFamily = michromaFont,
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.4f),
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "PIT",
                            fontFamily = michromaFont,
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.4f),
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.width(24.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Driver List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(liveDrivers) { index, driver ->
                            LiveDriverRow(
                                driver = driver,
                                gapToAhead = calculateGapToCarAhead(liveDrivers, index),
                                michromaFont = michromaFont
                            )
                        }
                        
                        // Bottom spacer
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }
}

// Helper Composable for Container (Box alias) to avoid ambiguity
@Composable
private fun Container(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier, content = content)
}

private fun generateDriverCode(firstName: String?, lastName: String?): String {
    if (firstName.isNullOrEmpty() && lastName.isNullOrEmpty()) return "???"
    
    // Create full name key for lookup
    val fullName = "${firstName?.trim()?.lowercase() ?: ""} ${lastName?.trim()?.lowercase() ?: ""}".trim()
    
    // Comprehensive driver name to code mapping
    val driverMap = mapOf(
        // Current Grid 2025
        "max verstappen" to "VER",
        "lewis hamilton" to "HAM",
        "charles leclerc" to "LEC",
        "carlos sainz" to "SAI",
        "george russell" to "RUS",
        "lando norris" to "NOR",
        "oscar piastri" to "PIA",
        "fernando alonso" to "ALO",
        "lance stroll" to "STR",
        "esteban ocon" to "OCO",
        "pierre gasly" to "GAS",
        "alex albon" to "ALB",
        "franco colapinto" to "COL",
        "yuki tsunoda" to "TSU",
        "liam lawson" to "LAW",
        "nico hulkenberg" to "HUL",
        "kevin magnussen" to "MAG",
        "oliver bearman" to "BEA",
        "valtteri bottas" to "BOT",
        "guanyu zhou" to "ZHO",
        "kimi antonelli" to "ANT",
        "andrea kimi antonelli" to "ANT",
        "isack hadjar" to "HAD",
        "gabriel bortoleto" to "BOR",
        // Additional variations
        "sergio perez" to "PER",
        "sergio pérez" to "PER",
        "checo perez" to "PER"
    )
    
    // Try exact match first
    driverMap[fullName]?.let { return it }
    
    // Try matching by last name only
    val lastNameLower = lastName?.trim()?.lowercase()
    driverMap.entries.find { it.key.endsWith(" $lastNameLower") }?.let { return it.value }
    
    // Fallback: generate code from name
    return if (!firstName.isNullOrEmpty() && !lastName.isNullOrEmpty()) {
        val first = firstName.trim().take(1).uppercase()
        val last = lastName.trim().take(2).uppercase()
        "$first$last"
    } else if (!lastName.isNullOrEmpty()) {
        lastName.trim().take(3).uppercase()
    } else if (!firstName.isNullOrEmpty()) {
        firstName.trim().take(3).uppercase()
    } else {
        "???"
    }
}

// Calculate gap to the car directly ahead instead of gap to leader
private fun calculateGapToCarAhead(
    drivers: List<LiveDriver>,
    currentIndex: Int
): String {
    // Leader shows "INTERVAL" or their race time
    if (currentIndex == 0) {
        return drivers[0].raceTime ?: "INTERVAL"
    }
    
    val currentDriver = drivers[currentIndex]
    val driverAhead = drivers[currentIndex - 1]
    
    // If current driver has gap to leader, we need to calculate interval to car ahead
    val currentGapStr = currentDriver.gap
    val aheadGapStr = driverAhead.gap
    
    // If either gap is missing or invalid, return the original gap
    if (currentGapStr == null || currentGapStr == "-" || aheadGapStr == null) {
        return currentGapStr ?: "-"
    }
    
    try {
        // Parse gap strings (format: "+12.345" or "12.345")
        val currentGap = currentGapStr.replace("+", "").toDoubleOrNull() ?: return currentGapStr
        val aheadGap = if (aheadGapStr == "-") 0.0 else aheadGapStr.replace("+", "").toDoubleOrNull() ?: 0.0
        
        // Calculate interval: gap_to_leader_current - gap_to_leader_ahead
        val interval = currentGap - aheadGap
        
        // Format the interval
        return if (interval >= 0.0) {
            "+%.3f".format(interval)
        } else {
            "%.3f".format(interval)
        }
    } catch (e: Exception) {
        return currentGapStr
    }
}

@Composable
private fun LiveDriverRow(
    driver: LiveDriver,
    gapToAhead: String,
    michromaFont: FontFamily
) {
    // Generate 3-letter abbreviation
    val driverCode = generateDriverCode(driver.firstName, driver.lastName)
    
    // Map team name to color
    val teamColor = when {
        driver.externalTeam?.contains("Red Bull", ignoreCase = true) == true -> Color(0xFF3671C6)
        driver.externalTeam?.contains("Ferrari", ignoreCase = true) == true -> Color(0xFFE8002D)
        driver.externalTeam?.contains("Mercedes", ignoreCase = true) == true -> Color(0xFF27F4D2)
        driver.externalTeam?.contains("McLaren", ignoreCase = true) == true -> Color(0xFFFF8000)
        driver.externalTeam?.contains("Aston Martin", ignoreCase = true) == true -> Color(0xFF229971)
        driver.externalTeam?.contains("Alpine", ignoreCase = true) == true -> Color(0xFFFF87BC)
        driver.externalTeam?.contains("Williams", ignoreCase = true) == true -> Color(0xFF64C4FF)
        driver.externalTeam?.contains("RB", ignoreCase = true) == true -> Color(0xFF6692FF)
        driver.externalTeam?.contains("Kick Sauber", ignoreCase = true) == true -> Color(0xFF52E252)
        driver.externalTeam?.contains("Haas", ignoreCase = true) == true -> Color(0xFFB6BABD)
        else -> Color(0xFF888888)
    }
    
    // Position-based accent color
    val positionColor = when (driver.positionDisplay?.toIntOrNull()) {
        1 -> Color(0xFFFFD700)  // Gold
        2 -> Color(0xFFC0C0C0)  // Silver
        3 -> Color(0xFFCD7F32)  // Bronze
        else -> Color(0xFFE6007E) // Hot Pink
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF161616),
                        Color(0xFF121212)
                    )
                ),
                RoundedCornerShape(6.dp)
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        teamColor.copy(alpha = 0.3f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Position with fixed width
        Box(
            modifier = Modifier.width(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(16.dp)
                        .background(
                            positionColor.copy(alpha = 0.8f),
                            RoundedCornerShape(1.dp)
                        )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = driver.positionDisplay ?: "-",
                    fontFamily = michromaFont,
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        // Team color indicator
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(24.dp)
                .background(
                    teamColor,
                    RoundedCornerShape(1.5.dp)
                )
        )
        
        // Driver code
        Text(
            text = driverCode,
            fontFamily = michromaFont,
            fontSize = 12.sp,
            color = Color.White,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(48.dp)
        )
        
        // Gap to car ahead (or leader interval/race time)
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = gapToAhead,
                fontFamily = michromaFont,
                fontSize = 11.sp,
                color = if (gapToAhead.contains("INTERVAL", ignoreCase = true) || gapToAhead.contains(":")) Color(0xFFFFD700) else Color.White.copy(alpha = 0.9f),
                fontWeight = if (gapToAhead.contains("INTERVAL", ignoreCase = true) || gapToAhead.contains(":")) FontWeight.Bold else FontWeight.Normal,
                letterSpacing = 0.5.sp
            )
        }
        
        // Pits
        Text(
            text = driver.pits ?: "0",
            fontFamily = michromaFont,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.width(24.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}



@Composable
private fun NoSessionActiveScreen(
    raceWeekendState: com.f1tracker.data.models.RaceWeekendState,
    raceViewModel: com.f1tracker.ui.viewmodels.RaceViewModel,
    brigendsFont: FontFamily,
    michromaFont: FontFamily
) {
    val accentColor = Color(0xFFFF0080)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        when (val state = raceWeekendState) {
            is com.f1tracker.data.models.RaceWeekendState.Active ->  {
                // Get FIRST session from ALL sessions (including practices, not filtered by main events)
                val allSessions = state.upcomingEvents + state.completedEvents.map { 
                    com.f1tracker.data.models.UpcomingEvent(
                        sessionType = it.sessionType,
                        sessionInfo = com.f1tracker.data.models.SessionInfo("", ""),
                        isCompleted = true
                    )
                }
                val nextSession = state.upcomingEvents.firstOrNull()
                
                if (nextSession != null) {
                    val sessionDateTime = parseSessionDateTime(nextSession.sessionInfo)
                    var countdown by remember { mutableStateOf("") }
                    
                    LaunchedEffect(sessionDateTime) {
                        while (true) {
                            val countdownText = raceViewModel.getCountdownTo(sessionDateTime)
                            countdown = countdownText
                            
                            // Check if countdown reached zero (all parts are 00)
                            if (countdownText == "00d 00h 00m 00s") {
                                raceViewModel.forceStateRefresh()
                                // Wait a bit longer to allow state update to propagate and avoid spamming
                                kotlinx.coroutines.delay(5000)
                            } else {
                                kotlinx.coroutines.delay(1000)
                            }
                        }
                    }
                    
                    CountdownDisplay(
                        title = "NEXT SESSION",
                        sessionName = nextSession.sessionType.displayName(),
                        countdown = countdown,
                        dateTime = sessionDateTime,
                        brigendsFont = brigendsFont,
                        michromaFont = michromaFont,
                        accentColor = accentColor
                    )
                } else {
                    NoUpcomingSessionsDisplay(brigendsFont, michromaFont)
                }
            }
            is com.f1tracker.data.models.RaceWeekendState.ComingUp -> {
                // Show countdown to FIRST session (could be Practice 1)
                val firstSession = state.upcomingEvents.firstOrNull()
                if (firstSession != null) {
                    val sessionDateTime = parseSessionDateTime(firstSession.sessionInfo)
                    var countdown by remember { mutableStateOf("") }
                    
                    LaunchedEffect(sessionDateTime) {
                        while (true) {
                            val countdownText = raceViewModel.getCountdownTo(sessionDateTime)
                            countdown = countdownText
                            
                            if (countdownText == "00d 00h 00m 00s") {
                                raceViewModel.forceStateRefresh()
                                kotlinx.coroutines.delay(5000)
                            } else {
                                kotlinx.coroutines.delay(1000)
                            }
                        }
                    }
                    
                    CountdownDisplay(
                        title = "NEXT SESSION",
                        sessionName = firstSession.sessionType.displayName(),
                        countdown = countdown,
                        dateTime = sessionDateTime,
                        brigendsFont = brigendsFont,
                        michromaFont = michromaFont,
                        accentColor = accentColor
                    )
                } else {
                    // Fallback to main event if upcomingEvents is empty
                    val sessionDateTime = parseSessionDateTime(state.nextMainEvent)
                    var countdown by remember { mutableStateOf("") }
                    
                    LaunchedEffect(sessionDateTime) {
                        while (true) {
                            countdown = raceViewModel.getCountdownTo(sessionDateTime)
                            kotlinx.coroutines.delay(1000)
                        }
                    }
                    
                    CountdownDisplay(
                        title = "NEXT SESSION",
                        sessionName = state.nextMainEventType.displayName(),
                        countdown = countdown,
                        dateTime = sessionDateTime,
                        brigendsFont = brigendsFont,
                        michromaFont = michromaFont,
                        accentColor = accentColor
                    )
                }
            }
            is com.f1tracker.data.models.RaceWeekendState.Loading -> {
                CircularProgressIndicator(color = accentColor)
            }
            is com.f1tracker.data.models.RaceWeekendState.Error -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFFF0040),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = state.message,
                        fontFamily = michromaFont,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun CountdownDisplay(
    title: String,
    sessionName: String,
    countdown: String,
    dateTime: java.time.LocalDateTime,
    brigendsFont: FontFamily,
    michromaFont: FontFamily,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            fontFamily = brigendsFont,
            fontSize = 10.sp,
            color = accentColor.copy(alpha = 0.8f),
            letterSpacing = 2.sp
        )
        
        Text(
            text = sessionName.uppercase(),
            fontFamily = michromaFont,
            fontSize = 16.sp,
            color = Color.White,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Bold
        )
        
        CountdownBoxLive(
            countdown = countdown,
            michromaFont = michromaFont,
            accentColor = accentColor
        )
        
        Text(
            text = formatFullDateTime(dateTime),
            fontFamily = michromaFont,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.8f)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Live timing will be available when a session is active",
            fontFamily = michromaFont,
            fontSize = 9.sp,
            color = Color.White.copy(alpha = 0.5f),
            letterSpacing = 0.5.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun CountdownBoxLive(
    countdown: String,
    michromaFont: FontFamily,
    accentColor: Color
) {
    val parts = countdown.split(" ")
    val timeUnits = mutableListOf<Pair<String, String>>()
    
    parts.forEach { part ->
        when {
            part.endsWith("d") -> timeUnits.add(part.removeSuffix("d") to "D")
            part.endsWith("h") -> timeUnits.add(part.removeSuffix("h") to "H")
            part.endsWith("m") -> timeUnits.add(part.removeSuffix("m") to "M")
            part.endsWith("s") -> timeUnits.add(part.removeSuffix("s") to "S")
        }
    }
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        timeUnits.forEach { (value, unit) ->
            Row(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.08f), androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = value.padStart(2, '0'),
                    fontFamily = michromaFont,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = unit,
                    fontFamily = michromaFont,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.6f),
                    letterSpacing = 0.sp,
                    modifier = Modifier.padding(start = 1.dp, bottom = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun NoUpcomingSessionsDisplay(
    brigendsFont: FontFamily,
    michromaFont: FontFamily
) {
    Column(
        modifier = Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        Text(
            text = "NO UPCOMING SESSIONS",
            fontFamily = brigendsFont,
            fontSize = 16.sp,
            color = Color.White,
            letterSpacing = 2.sp
        )
        Text(
            text = "The current race weekend has ended.\nCheck back soon for the next event!",
            fontFamily = michromaFont,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

private fun parseSessionDateTime(sessionInfo: com.f1tracker.data.models.SessionInfo): java.time.LocalDateTime {
    return try {
        val dateStr = sessionInfo.date
        val timeStr = sessionInfo.time
        val dateTimeStr = "${dateStr}T${timeStr}"
        
        // Parse as UTC (Zulu time) and convert to IST
        val instant = if (timeStr.endsWith("Z")) {
            java.time.Instant.parse(dateTimeStr)
        } else {
            java.time.Instant.parse("${dateTimeStr}Z")
        }
        
        // Convert to Asia/Kolkata (IST)
        instant.atZone(java.time.ZoneId.of("Asia/Kolkata")).toLocalDateTime()
    } catch (e: Exception) {
        java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata"))
    }
}

private fun formatFullDateTime(dateTime: java.time.LocalDateTime): String {
    val formatter = java.time.format.DateTimeFormatter.ofPattern("EEE, dd MMM yyyy • hh:mm a")
    return dateTime.format(formatter)
}
