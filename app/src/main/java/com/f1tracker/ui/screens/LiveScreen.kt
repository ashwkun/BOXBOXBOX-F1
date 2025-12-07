package com.f1tracker.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape

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
import androidx.compose.ui.graphics.graphicsLayer
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
    val currentLap by liveClient.currentLap.collectAsState()
    val totalLaps by liveClient.totalLaps.collectAsState()
    
    // Race weekend state for session detection
    val raceWeekendState by raceViewModel.raceWeekendState.collectAsState()
    
    // Determine session type from name
    val sessionType = remember(sessionName) {
        when {
            sessionName.contains("race", ignoreCase = true) && !sessionName.contains("sprint", ignoreCase = true) -> SessionType.RACE
            sessionName.contains("sprint", ignoreCase = true) && sessionName.contains("qualifying", ignoreCase = true) -> SessionType.SPRINT_QUALIFYING
            sessionName.contains("sprint", ignoreCase = true) -> SessionType.SPRINT
            sessionName.contains("qualifying", ignoreCase = true) -> SessionType.QUALIFYING
            sessionName.contains("practice", ignoreCase = true) || sessionName.contains("fp", ignoreCase = true) -> SessionType.PRACTICE
            else -> SessionType.RACE // Default to race layout
        }
    }
    
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
        // Minimal header - only show when we have live data
        if (shouldConnect && liveDrivers.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Session name and lap counter
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Live pulse indicator
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseAlpha"
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                Color(0xFFFF0080).copy(alpha = pulseAlpha),
                                RoundedCornerShape(4.dp)
                            )
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = if (sessionName.isNotEmpty()) sessionName.uppercase() else "LIVE",
                        fontFamily = michromaFont,
                        fontSize = 10.sp,
                        color = Color.White,
                        letterSpacing = 0.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 120.dp)
                    )
                    
                    // Lap counter (only for Race/Sprint sessions)
                    if (sessionType == SessionType.RACE || sessionType == SessionType.SPRINT) {
                        Spacer(modifier = Modifier.width(12.dp))
                        val lapDisplay = if (totalLaps > 0) "$currentLap/$totalLaps" else "$currentLap"
                        Text(
                            text = "LAP $lapDisplay",
                            fontFamily = michromaFont,
                            fontSize = 10.sp,
                            color = Color(0xFFE6007E),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Session type badge
                Box(
                    modifier = Modifier
                        .background(
                            when (sessionType) {
                                SessionType.RACE -> Color(0xFFE6007E).copy(alpha = 0.2f)
                                SessionType.QUALIFYING, SessionType.SPRINT_QUALIFYING -> Color(0xFF00BFFF).copy(alpha = 0.2f)
                                SessionType.SPRINT -> Color(0xFFFF8000).copy(alpha = 0.2f)
                                SessionType.PRACTICE -> Color(0xFF888888).copy(alpha = 0.2f)
                            },
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when (sessionType) {
                            SessionType.RACE -> "RACE"
                            SessionType.QUALIFYING -> "QUALI"
                            SessionType.SPRINT_QUALIFYING -> "SQ"
                            SessionType.SPRINT -> "SPRINT"
                            SessionType.PRACTICE -> "FP"
                        },
                        fontFamily = michromaFont,
                        fontSize = 8.sp,
                        color = Color.White,
                        letterSpacing = 0.5.sp,
                        fontWeight = FontWeight.Bold
                    )
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
            
            
            // Loading state - Premium connecting screen
            !isConnected && connectionError == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF1A0A12),
                                    Color.Black
                                ),
                                radius = 800f
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Animated pulsing ring
                        val infiniteTransition = rememberInfiniteTransition(label = "connecting")
                        val pulseScale by infiniteTransition.animateFloat(
                            initialValue = 0.9f,
                            targetValue = 1.1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "scale"
                        )
                        val pulseAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 0.8f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "alpha"
                        )
                        
                        Box(
                            modifier = Modifier.size(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Outer pulsing ring
                            Box(
                                modifier = Modifier
                                    .size((70 * pulseScale).dp)
                                    .border(
                                        width = 2.dp,
                                        brush = Brush.sweepGradient(
                                            listOf(
                                                Color(0xFFE6007E).copy(alpha = pulseAlpha),
                                                Color(0xFFE6007E).copy(alpha = 0.1f),
                                                Color(0xFFE6007E).copy(alpha = pulseAlpha)
                                            )
                                        ),
                                        shape = RoundedCornerShape(35.dp)
                                    )
                            )
                            // Inner progress
                            CircularProgressIndicator(
                                modifier = Modifier.size(40.dp),
                                color = Color(0xFFE6007E),
                                strokeWidth = 2.dp
                            )
                        }
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "CONNECTING",
                                fontFamily = brigendsFont,
                                fontSize = 14.sp,
                                color = Color.White,
                                letterSpacing = 3.sp
                            )
                            Text(
                                text = "Establishing live timing connection",
                                fontFamily = michromaFont,
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
            
            // Error state - Premium error screen
            connectionError != null && liveDrivers.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF1A0808),
                                    Color.Black
                                ),
                                radius = 800f
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(
                                    Color(0xFFFF0040).copy(alpha = 0.1f),
                                    RoundedCornerShape(32.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WifiOff,
                                contentDescription = "Error",
                                tint = Color(0xFFFF0040),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "CONNECTION FAILED",
                                fontFamily = brigendsFont,
                                fontSize = 14.sp,
                                color = Color.White,
                                letterSpacing = 2.sp
                            )
                            Text(
                                text = "Unable to connect to live timing",
                                fontFamily = michromaFont,
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                letterSpacing = 0.5.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Retry button
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFFE6007E),
                                            Color(0xFFFF0080)
                                        )
                                    ),
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { liveClient.connect() }
                                .padding(horizontal = 24.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "RETRY CONNECTION",
                                fontFamily = michromaFont,
                                fontSize = 10.sp,
                                color = Color.White,
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            // No data but connected - with countdown - Premium waiting screen
            liveDrivers.isEmpty() && isConnected -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF151008),
                                    Color.Black
                                ),
                                radius = 800f
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Animated countdown with ring
                        val infiniteTransition = rememberInfiniteTransition(label = "waiting")
                        val rotation by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(3000, easing = LinearEasing)
                            ),
                            label = "rotation"
                        )
                        
                        Box(
                            modifier = Modifier.size(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Rotating accent ring
                            Box(
                                modifier = Modifier
                                    .size(75.dp)
                                    .border(
                                        width = 2.dp,
                                        brush = Brush.sweepGradient(
                                            0f to Color(0xFFFFAA00).copy(alpha = 0.6f),
                                            0.5f to Color.Transparent,
                                            1f to Color(0xFFFFAA00).copy(alpha = 0.6f)
                                        ),
                                        shape = RoundedCornerShape(37.5.dp)
                                    )
                                    .graphicsLayer { rotationZ = rotation }
                            )
                            // Countdown number
                            Text(
                                text = if (countdown > 0) "$countdown" else "...",
                                fontFamily = michromaFont,
                                fontSize = 22.sp,
                                color = Color(0xFFFFAA00),
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "AWAITING DATA",
                                fontFamily = brigendsFont,
                                fontSize = 14.sp,
                                color = Color.White,
                                letterSpacing = 3.sp
                            )
                            Text(
                                text = "Session data will appear shortly. If specific data is missing, the session may have ended.",
                                fontFamily = michromaFont,
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                letterSpacing = 0.5.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center

                            )
                        }
                    }
                }
            }
            
            // Show live data
            else -> {
                Column {
                    // Session-specific column headers
                    SessionHeader(sessionType = sessionType, michromaFont = michromaFont)
                    
                    // Driver List with session-specific rows
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(liveDrivers) { index, driver ->
                            when (sessionType) {
                                SessionType.RACE, SessionType.SPRINT -> {
                                    RaceDriverRow(
                                        driver = driver,
                                        index = index,
                                        drivers = liveDrivers,
                                        michromaFont = michromaFont
                                    )
                                }
                                SessionType.QUALIFYING, SessionType.SPRINT_QUALIFYING -> {
                                    QualiDriverRow(
                                        driver = driver,
                                        isKnockoutZone = index >= 15, // Positions 16-20
                                        michromaFont = michromaFont
                                    )
                                }
                                SessionType.PRACTICE -> {
                                    PracticeDriverRow(
                                        driver = driver,
                                        index = index,
                                        drivers = liveDrivers,
                                        michromaFont = michromaFont
                                    )
                                }
                            }
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

// Session Type Enum
enum class SessionType {
    RACE, SPRINT, QUALIFYING, SPRINT_QUALIFYING, PRACTICE
}

// Session-specific column header
@Composable
private fun SessionHeader(
    sessionType: SessionType,
    michromaFont: FontFamily
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Position column
        Text(
            text = "P",
            fontFamily = michromaFont,
            fontSize = 9.sp,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.width(if (sessionType == SessionType.RACE || sessionType == SessionType.SPRINT) 48.dp else 24.dp),
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        
        // Driver column
        Text(
            text = "DVR",
            fontFamily = michromaFont,
            fontSize = 9.sp,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.width(50.dp),
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        
        when (sessionType) {
            SessionType.RACE, SessionType.SPRINT -> {
                // Tyre
                Text(
                    text = "TYR",
                    fontFamily = michromaFont,
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.width(36.dp),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                
                // Pits
                Text(
                    text = "PIT",
                    fontFamily = michromaFont,
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.width(24.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                
                // Sectors
                Text(
                    text = "SEC",
                    fontFamily = michromaFont,
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.width(42.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                
                // Interval/Gap
                Text(
                    text = "GAP",
                    fontFamily = michromaFont,
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            SessionType.QUALIFYING, SessionType.SPRINT_QUALIFYING -> {
                // Sectors S1, S2, S3
                Text(
                    text = "S1",
                    fontFamily = michromaFont,
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.width(36.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "S2",
                    fontFamily = michromaFont,
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.width(36.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "S3",
                    fontFamily = michromaFont,
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.width(36.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
                // Best Lap
                Text(
                    text = "BEST",
                    fontFamily = michromaFont,
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    fontWeight = FontWeight.Bold
                )
                // Gap
                Text(
                    text = "GAP",
                    fontFamily = michromaFont,
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.width(48.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    fontWeight = FontWeight.Bold
                )
            }
             SessionType.PRACTICE -> {
                // Best Lap
                Text(
                    text = "BEST",
                    fontFamily = michromaFont,
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    fontWeight = FontWeight.Bold
                )
                // Gap
                Text(
                    text = "GAP",
                    fontFamily = michromaFont,
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.width(48.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Get tyre color based on compound
private fun getTyreColor(compound: String?): Color {
    return when (compound?.uppercase()) {
        "SOFT" -> Color(0xFFFF0000)        // Red
        "MEDIUM" -> Color(0xFFFFD700)      // Yellow
        "HARD" -> Color(0xFFFFFFFF)        // White
        "INTERMEDIATE" -> Color(0xFF00FF00) // Green
        "WET" -> Color(0xFF0080FF)         // Blue
        else -> Color(0xFF888888)          // Unknown - Gray
    }
}

// Get team color for a driver
private fun getTeamColor(teamName: String?): Color {
    return when {
        teamName?.contains("Red Bull", ignoreCase = true) == true -> Color(0xFF3671C6)
        teamName?.contains("Ferrari", ignoreCase = true) == true -> Color(0xFFE8002D)
        teamName?.contains("Mercedes", ignoreCase = true) == true -> Color(0xFF27F4D2)
        teamName?.contains("McLaren", ignoreCase = true) == true -> Color(0xFFFF8000)
        teamName?.contains("Aston Martin", ignoreCase = true) == true -> Color(0xFF229971)
        teamName?.contains("Alpine", ignoreCase = true) == true -> Color(0xFFFF87BC)
        teamName?.contains("Williams", ignoreCase = true) == true -> Color(0xFF64C4FF)
        teamName?.contains("RB", ignoreCase = true) == true -> Color(0xFF6692FF)
        teamName?.contains("Kick Sauber", ignoreCase = true) == true -> Color(0xFF52E252)
        teamName?.contains("Haas", ignoreCase = true) == true -> Color(0xFFB6BABD)
        else -> Color(0xFF888888)
    }
}

// RACE / SPRINT Driver Row - Compact Single-Line
@Composable
private fun RaceDriverRow(
    driver: LiveDriver,
    index: Int,
    drivers: List<LiveDriver>,
    michromaFont: FontFamily
) {
    val driverCode = generateDriverCode(driver.firstName, driver.lastName)
    val teamColor = getTeamColor(driver.externalTeam)
    val tyreColor = getTyreColor(driver.tyreCompound)
    val gapToAhead = calculateGapToCarAhead(drivers, index)
    
    val isRetired = driver.status == 2 || driver.status == 3
    val isInPit = driver.status == 1
    
    val positionColor = when (driver.positionDisplay?.toIntOrNull()) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> Color(0xFFE6007E)
    }
    
    val rowBackground = when {
        isRetired -> Brush.horizontalGradient(listOf(Color(0xFFFF0040).copy(alpha = 0.15f), Color(0xFF121212)))
        isInPit -> Brush.horizontalGradient(listOf(Color(0xFFFFAA00).copy(alpha = 0.12f), Color(0xFF121212)))
        else -> Brush.horizontalGradient(listOf(Color(0xFF161616), Color(0xFF121212)))
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(rowBackground, RoundedCornerShape(4.dp))
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        teamColor.copy(alpha = if (isRetired) 0.15f else 0.3f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Position + Change (Fixed 48dp)
        Row(
            modifier = Modifier.width(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(14.dp)
                    .background(positionColor, RoundedCornerShape(1.5.dp))
            )
            Spacer(modifier = Modifier.width(4.dp))
            
            Text(
                text = driver.positionDisplay ?: "-",
                fontFamily = michromaFont,
                fontSize = 11.sp,
                color = if (isRetired) Color.White.copy(alpha = 0.4f) else Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            
            // Position Change
            val grid = driver.gridPos?.toIntOrNull()
            val current = driver.positionDisplay?.toIntOrNull()
            if (grid != null && current != null && grid > 0 && current > 0) {
                val change = grid - current
                if (change != 0) {
                    Spacer(modifier = Modifier.width(2.dp))
                    val color = if (change > 0) Color.Green else Color.Red
                    val arrow = if (change > 0) "▲" else "▼"
                    Text(
                        text = "$arrow${kotlin.math.abs(change)}",
                        fontFamily = michromaFont,
                        fontSize = 8.sp,
                        color = color,
                        maxLines = 1,
                        letterSpacing = (-0.5).sp
                    )
                }
            }
        }
        
        // 2. Driver Code (Fixed 50dp)
        Row(
            modifier = Modifier.width(50.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(18.dp)
                    .background(teamColor, RoundedCornerShape(1.5.dp))
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = driverCode,
                fontFamily = michromaFont,
                fontSize = 11.sp,
                color = if (isRetired) Color.White.copy(alpha = 0.4f) else Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
        
        // 3. Tyre (Fixed 36dp)
        Row(
            modifier = Modifier.width(36.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(tyreColor, CircleShape)
            )
            Text(
                text = "${driver.tyreAge ?: 0}",
                fontFamily = michromaFont,
                fontSize = 10.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
        
        // 4. Pit Count (Fixed 24dp)
        Box(
            modifier = Modifier
                .width(24.dp)
                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(2.dp))
                .padding(vertical = 1.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = driver.pits ?: "0",
                fontFamily = michromaFont,
                fontSize = 10.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }

        // 5. Mini Sectors (Fixed 42dp)
        Row(
            modifier = Modifier.width(42.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 3) {
                val colorCode = driver.sectorColors.getOrNull(i) ?: 0
                val color = when (colorCode) {
                    1 -> Color(0xFF00FF00) // Personal Best
                    2 -> Color(0xFFAA00FF) // Overall Best
                    else -> Color(0xFF333333) // Visible gray
                }
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .height(4.dp)
                        .background(color, RoundedCornerShape(1.dp))
                )
            }
        }
        
        // 6. Gap / Status (Flex)
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd
        ) {
             if (isRetired) {
                Text(
                    text = "OUT",
                    fontFamily = michromaFont,
                    fontSize = 10.sp,
                    color = Color(0xFFFF0040),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            } else if (isInPit) {
                Text(
                    text = "PIT",
                    fontFamily = michromaFont,
                    fontSize = 10.sp,
                    color = Color(0xFFFFAA00),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            } else {
                Text(
                    text = gapToAhead,
                    fontFamily = michromaFont,
                    fontSize = 11.sp,
                    color = if (index == 0) Color(0xFFFFD700) else Color.White,
                    fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1
                )
            }
        }
    }
}

// QUALIFYING / SPRINT QUALIFYING Driver Row
@Composable
private fun QualiDriverRow(
    driver: LiveDriver,
    isKnockoutZone: Boolean,
    michromaFont: FontFamily
) {
    val driverCode = generateDriverCode(driver.firstName, driver.lastName)
    val teamColor = getTeamColor(driver.externalTeam)
    
    // Position accent colors
    val positionColor = when (driver.positionDisplay?.toIntOrNull()) {
        1 -> Color(0xFFFFD700)  // Gold
        2 -> Color(0xFFC0C0C0)  // Silver
        3 -> Color(0xFFCD7F32)  // Bronze
        else -> Color(0xFFE6007E) // Hot Pink
    }
    
    // Sector colors: 0=white, 1=green (personal), 2=purple (overall)
    val sectorColors = driver.sectorColors.map { colorCode ->
        when (colorCode) {
            2 -> Color(0xFFAA00FF)  // Purple - overall fastest
            1 -> Color(0xFF00FF00)  // Green - personal best
            else -> Color.White.copy(alpha = 0.8f)  // White - normal
        }
    }
    
    // Background color for knockout zone
    val bgColor = if (isKnockoutZone) Color(0xFFFF0040).copy(alpha = 0.1f) else Color(0xFF161616)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(bgColor, Color(0xFF121212))
                ),
                RoundedCornerShape(6.dp)
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        if (isKnockoutZone) Color(0xFFFF0040).copy(alpha = 0.3f) else teamColor.copy(alpha = 0.3f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Position
        Row(
            modifier = Modifier.width(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(14.dp)
                    .background(positionColor.copy(alpha = 0.8f), RoundedCornerShape(1.dp))
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = driver.positionDisplay ?: "-",
                fontFamily = michromaFont,
                fontSize = 11.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Driver code with team color
        Row(
            modifier = Modifier.width(44.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(20.dp)
                    .background(teamColor, RoundedCornerShape(1.5.dp))
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = driverCode,
                fontFamily = michromaFont,
                fontSize = 11.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Sector 1
        Text(
            text = driver.sectors.getOrNull(0) ?: "-",
            fontFamily = michromaFont,
            fontSize = 9.sp,
            color = sectorColors.getOrNull(0) ?: Color.White.copy(alpha = 0.7f),
            modifier = Modifier.width(36.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            fontWeight = if ((driver.sectorColors.getOrNull(0) ?: 0) > 0) FontWeight.Bold else FontWeight.Normal
        )
        
        // Sector 2
        Text(
            text = driver.sectors.getOrNull(1) ?: "-",
            fontFamily = michromaFont,
            fontSize = 9.sp,
            color = sectorColors.getOrNull(1) ?: Color.White.copy(alpha = 0.7f),
            modifier = Modifier.width(36.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            fontWeight = if ((driver.sectorColors.getOrNull(1) ?: 0) > 0) FontWeight.Bold else FontWeight.Normal
        )
        
        // Sector 3
        Text(
            text = driver.sectors.getOrNull(2) ?: "-",
            fontFamily = michromaFont,
            fontSize = 9.sp,
            color = sectorColors.getOrNull(2) ?: Color.White.copy(alpha = 0.7f),
            modifier = Modifier.width(36.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            fontWeight = if ((driver.sectorColors.getOrNull(2) ?: 0) > 0) FontWeight.Bold else FontWeight.Normal
        )
        
        // Best lap time
        Text(
            text = driver.raceTime ?: "-",
            fontFamily = michromaFont,
            fontSize = 10.sp,
            color = Color.White,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            fontWeight = FontWeight.Bold
        )
        
        // Gap to P1
        Text(
            text = driver.gap ?: "-",
            fontFamily = michromaFont,
            fontSize = 9.sp,
            color = if (driver.gap == null || driver.gap == "-") Color(0xFFFFD700) else Color.White.copy(alpha = 0.7f),
            modifier = Modifier.width(48.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

// PRACTICE Driver Row
@Composable
private fun PracticeDriverRow(
    driver: LiveDriver,
    index: Int,
    drivers: List<LiveDriver>,
    michromaFont: FontFamily
) {
    val driverCode = generateDriverCode(driver.firstName, driver.lastName)
    val teamColor = getTeamColor(driver.externalTeam)
    
    // Position accent colors
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
                    colors = listOf(Color(0xFF161616), Color(0xFF121212))
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
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Position
        Row(
            modifier = Modifier.width(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(14.dp)
                    .background(positionColor.copy(alpha = 0.8f), RoundedCornerShape(1.dp))
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = driver.positionDisplay ?: "-",
                fontFamily = michromaFont,
                fontSize = 11.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Driver code with team color
        Row(
            modifier = Modifier.width(44.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(20.dp)
                    .background(teamColor, RoundedCornerShape(1.5.dp))
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = driverCode,
                fontFamily = michromaFont,
                fontSize = 11.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Best lap time
        Text(
            text = driver.raceTime ?: "-",
            fontFamily = michromaFont,
            fontSize = 10.sp,
            color = if (index == 0) Color(0xFFAA00FF) else Color.White,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal
        )
        
        // Gap
        Text(
            text = driver.gap ?: "-",
            fontFamily = michromaFont,
            fontSize = 10.sp,
            color = if (index == 0) Color(0xFFFFD700) else Color.White.copy(alpha = 0.7f),
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
        
        // Laps completed
        Text(
            text = driver.laps ?: "-",
            fontFamily = michromaFont,
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.width(32.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
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
