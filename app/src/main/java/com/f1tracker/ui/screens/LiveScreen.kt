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

@Composable
fun LiveScreen() {
    val brigendsFont = FontFamily(Font(R.font.brigends_expanded, FontWeight.Normal))
    val michromaFont = FontFamily(Font(R.font.michroma, FontWeight.Normal))
    
    val liveClient = remember { SignalRLiveTimingClient.getInstance() }
    val liveDrivers by liveClient.liveDrivers.collectAsState()
    val sessionName by liveClient.sessionName.collectAsState()
    val isConnected by liveClient.isConnected.collectAsState()
    val connectionError by liveClient.connectionError.collectAsState()
    val rawData by liveClient.rawData.collectAsState()
    
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
    
    // Auto-connect on screen load
    LaunchedEffect(Unit) {
        liveClient.connect()
    }
    
    // Disconnect when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            liveClient.disconnect()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0A0A),
                        Color.Black
                    )
                )
            )
    ) {
        // Header
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = if (sessionName.isNotEmpty()) sessionName.uppercase() else "LIVE TIMING",
                        fontFamily = michromaFont,
                        fontSize = 14.sp,
                        color = Color.White,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Animated connection indicator
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (isConnected) Color(0xFF00FF41) else if (connectionError != null) Color(0xFFFF0040) else Color(0xFFFFAA00),
                                    RoundedCornerShape(4.dp)
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                !isConnected && connectionError != null -> "CONNECTION ERROR"
                                !isConnected -> "CONNECTING..."
                                liveDrivers.isEmpty() -> "AWAITING DATA..."
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
                            letterSpacing = 1.sp,
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
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "GRID",
                                fontFamily = michromaFont,
                                fontSize = 8.sp,
                                color = if (!showRawView) Color.White else Color.White.copy(alpha = 0.6f),
                                letterSpacing = 1.sp,
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
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "RAW",
                                fontFamily = michromaFont,
                                fontSize = 8.sp,
                                color = if (showRawView) Color.White else Color.White.copy(alpha = 0.6f),
                                letterSpacing = 1.sp,
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
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "RETRY",
                                fontFamily = michromaFont,
                                fontSize = 8.sp,
                                color = Color.White,
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
        
        when {
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
                                liveDrivers.forEach { driver ->
                                    val driverName = "${driver.firstName ?: ""} ${driver.lastName ?: ""}".trim().take(20)
                                    val team = (driver.externalTeam ?: "").take(20)
                                    val time = (driver.raceTime ?: "").take(15)
                                    val gap = (driver.gap ?: "-").take(10)
                                    
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
                                
                                Text(
                                    text = "═══════════════════════════════════════════════════════════════════════════════════",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = Color(0xFF00FF41),
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Session: ${sessionName.uppercase()}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = Color(0xFF00FF41).copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "Drivers: ${liveDrivers.size}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = Color(0xFF00FF41).copy(alpha = 0.7f)
                                )
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
                            modifier = Modifier.size(56.dp),
                            color = Color(0xFFE6007E),
                            strokeWidth = 4.dp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "CONNECTING",
                            fontFamily = brigendsFont,
                            fontSize = 14.sp,
                            color = Color.White,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Establishing SignalR connection...",
                            fontFamily = michromaFont,
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            letterSpacing = 0.5.sp
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
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Connection Error",
                            fontFamily = brigendsFont,
                            fontSize = 14.sp,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = connectionError ?: "Unknown error",
                            fontFamily = michromaFont,
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.5f)
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
                            modifier = Modifier.size(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(80.dp),
                                color = Color(0xFFFFAA00).copy(alpha = 0.3f),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "$countdown",
                                fontFamily = michromaFont,
                                fontSize = 24.sp,
                                color = Color(0xFFFFAA00),
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "AWAITING RESPONSE",
                            fontFamily = brigendsFont,
                            fontSize = 14.sp,
                            color = Color.White,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Connection established",
                            fontFamily = michromaFont,
                            fontSize = 10.sp,
                            color = Color(0xFF00FF41).copy(alpha = 0.8f),
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Waiting for SignalR data...",
                            fontFamily = michromaFont,
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "This may take up to 30 seconds",
                            fontFamily = michromaFont,
                            fontSize = 8.sp,
                            color = Color.White.copy(alpha = 0.3f),
                            letterSpacing = 0.5.sp
                        )
                        
                        // Show note only after countdown finishes
                        if (countdownFinished) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .background(
                                        Color(0xFFFFAA00).copy(alpha = 0.1f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = Color(0xFFFFAA00).copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "⚠ NOTE",
                                        fontFamily = michromaFont,
                                        fontSize = 8.sp,
                                        color = Color(0xFFFFAA00),
                                        letterSpacing = 1.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Live timing only works when",
                                        fontFamily = michromaFont,
                                        fontSize = 7.sp,
                                        color = Color.White.copy(alpha = 0.6f),
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = "an F1 session is LIVE",
                                        fontFamily = michromaFont,
                                        fontSize = 7.sp,
                                        color = Color.White.copy(alpha = 0.6f),
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
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
                            .background(Color(0xFF0A0A0A).copy(alpha = 0.5f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "POS",
                            fontFamily = michromaFont,
                            fontSize = 7.sp,
                            color = Color.White.copy(alpha = 0.4f),
                            letterSpacing = 1.sp,
                            modifier = Modifier.width(42.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "DRIVER",
                            fontFamily = michromaFont,
                            fontSize = 7.sp,
                            color = Color.White.copy(alpha = 0.4f),
                            letterSpacing = 1.sp,
                            modifier = Modifier.width(56.dp)
                        )
                        Text(
                            text = "TIME / GAP",
                            fontFamily = michromaFont,
                            fontSize = 7.sp,
                            color = Color.White.copy(alpha = 0.4f),
                            letterSpacing = 1.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "PITS",
                            fontFamily = michromaFont,
                            fontSize = 7.sp,
                            color = Color.White.copy(alpha = 0.4f),
                            letterSpacing = 1.sp,
                            modifier = Modifier.width(32.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                    }
                    
                    // Driver List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(
                            items = liveDrivers.filter { it.position != null && (it.firstName != null || it.lastName != null) },
                            key = { _, driver -> "${driver.position}_${driver.firstName}_${driver.lastName}_${System.currentTimeMillis()}" }
                        ) { _, driver ->
                            LiveDriverRow(
                                driver = driver,
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

@Composable
private fun LiveDriverRow(
    driver: com.f1tracker.data.live.LiveDriver,
    michromaFont: FontFamily
) {
    // Generate 3-letter abbreviation
    val driverCode = generateDriverCode(driver.firstName, driver.lastName)
    
    // Map team name to color
    val teamColor = when (driver.externalTeam?.lowercase()) {
        "red bull racing", "red bull" -> Color(0xFF3671C6)
        "ferrari" -> Color(0xFFE8002D)
        "mercedes" -> Color(0xFF27F4D2)
        "mclaren" -> Color(0xFFFF8000)
        "aston martin", "aston martin aramco" -> Color(0xFF229971)
        "alpine" -> Color(0xFF0093CC)
        "williams" -> Color(0xFF64C4FF)
        "alphatauri", "rb", "racing bulls", "visa rb" -> Color(0xFF6692FF)
        "alfa romeo", "kick sauber", "sauber" -> Color(0xFF52E252)
        "haas" -> Color(0xFFB6BABD)
        else -> Color(0xFFFF0080)
    }
    
    // Position color
    val positionColor = when (driver.position?.toIntOrNull()) {
        1 -> Color(0xFFFFD700) // Gold
        2 -> Color(0xFFC0C0C0) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> Color(0xFFE6007E) // Pink
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF1A1A1A),
                        Color(0xFF0F0F0F)
                    )
                ),
                RoundedCornerShape(8.dp)
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        teamColor.copy(alpha = 0.3f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Position with fixed width
        Box(
            modifier = Modifier.width(42.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(20.dp)
                        .background(
                            positionColor.copy(alpha = 0.8f),
                            RoundedCornerShape(1.dp)
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = driver.positionDisplay ?: "-",
                    fontFamily = michromaFont,
                    fontSize = 13.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        // Team color indicator
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(32.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            teamColor,
                            teamColor.copy(alpha = 0.4f)
                        )
                    ),
                    RoundedCornerShape(2.dp)
                )
        )
        
        // Driver code
        Text(
            text = driverCode,
            fontFamily = michromaFont,
            fontSize = 13.sp,
            color = Color.White,
            letterSpacing = 1.8.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(56.dp)
        )
        
        // Time/Gap
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = if (driver.gap == "-") (driver.raceTime ?: "-") else (driver.gap ?: "-"),
                fontFamily = michromaFont,
                fontSize = 10.sp,
                color = if (driver.gap == "-") Color(0xFFFFD700) else Color.White.copy(alpha = 0.7f),
                fontWeight = if (driver.gap == "-") FontWeight.Bold else FontWeight.Normal,
                letterSpacing = 0.5.sp
            )
        }
        
        // Pits with fixed width
        Box(
            modifier = Modifier.width(32.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = driver.pits ?: "0",
                fontFamily = michromaFont,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.6f),
                letterSpacing = 0.5.sp
            )
        }
    }
}
