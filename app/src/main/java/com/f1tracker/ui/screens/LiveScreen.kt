package com.f1tracker.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import com.f1tracker.data.live.LiveDriver
import com.f1tracker.data.live.ConnectionStatus
import com.f1tracker.data.acestream.AceStreamRepository
import com.f1tracker.ui.components.FullScreenAudioPlayer

@Composable
fun LiveScreen(
    raceViewModel: com.f1tracker.ui.viewmodels.RaceViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    onStreamClick: () -> Unit = {}
) {
    val brigendsFont = FontFamily(Font(R.font.brigends_expanded, FontWeight.Normal))
    val michromaFont = FontFamily(Font(R.font.michroma, FontWeight.Normal))
    
    val liveClient = remember { SignalRLiveTimingClient.getInstance() }
    val liveDrivers by liveClient.liveDrivers.collectAsState()
    val sessionName by liveClient.sessionName.collectAsState()
    val isConnected by liveClient.isConnected.collectAsState()
    val connectionError by liveClient.connectionError.collectAsState()
    val connectionStatus by liveClient.connectionStatus.collectAsState()
    val currentLap by liveClient.currentLap.collectAsState()
    val totalLaps by liveClient.totalLaps.collectAsState()
    val feedSource by liveClient.feedSource.collectAsState()
    
    // Lifecycle observer — reconnect when app resumes from background
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                liveClient.ensureConnected()
                
                // Proactively wake up the Ace Stream service and rapidly poll its HTTP port until it responds.
                // This prevents the engine from appearing "Off" if Android suspended its network proxy.
                raceViewModel.wakeEngineAndCheckStatus(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Broadcast receiver for app installation
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_PACKAGE_ADDED) {
                    val packageName = intent.data?.schemeSpecificPart
                    if (packageName != null && AceStreamRepository.ACE_STREAM_PACKAGES.contains(packageName)) {
                        raceViewModel.checkAceStreamStatus()
                    }
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_PACKAGE_ADDED).apply {
            addDataScheme("package")
        }
        // Use RECEIVER_EXPORTED if running on Tiramisu+ for standard system broadcasts (not strictly required for PACKAGE_ADDED, but good practice if lint complains, though we can just use registerReceiver for now since it's a system broadcast and exempt)
        context.registerReceiver(receiver, filter)
        
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }
    
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
    
    val aceStreamRepo = remember { AceStreamRepository.getInstance() }
    val aceStreamState by raceViewModel.aceStreamState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
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
                    
                    // Lap counter (only for Race/Sprint sessions with lap data)
                    if ((sessionType == SessionType.RACE || sessionType == SessionType.SPRINT) &&
                        (currentLap > 0 || totalLaps > 0)
                    ) {
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
                
                Column(horizontalAlignment = Alignment.End) {
                    // Session type badge
                    Box(
                        modifier = Modifier
                            .background(
                                when (sessionType) {
                                    SessionType.RACE -> Color(0xFFE6007E).copy(alpha = 0.2f)
                                    SessionType.QUALIFYING, SessionType.SPRINT_QUALIFYING -> Color(0xFF00BFFF).copy(alpha = 0.2f)
                                    SessionType.SPRINT -> Color(0xFFFF6600).copy(alpha = 0.2f)
                                    SessionType.PRACTICE -> Color(0xFF00FF88).copy(alpha = 0.2f)
                                },
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = when (sessionType) {
                                SessionType.RACE -> "RACE"
                                SessionType.SPRINT -> "SPRINT"
                                SessionType.QUALIFYING -> "QUALI"
                                SessionType.SPRINT_QUALIFYING -> "SQ"
                                SessionType.PRACTICE -> "FP"
                            },
                            fontFamily = michromaFont,
                            fontSize = 8.sp,
                            color = when (sessionType) {
                                SessionType.RACE -> Color(0xFFE6007E)
                                SessionType.QUALIFYING, SessionType.SPRINT_QUALIFYING -> Color(0xFF00BFFF)
                                SessionType.SPRINT -> Color(0xFFFF6600)
                                SessionType.PRACTICE -> Color(0xFF00FF88)
                            },
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    if (feedSource.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = feedSource.uppercase(),
                            fontFamily = michromaFont,
                            fontSize = 7.sp,
                            color = Color.White.copy(alpha = 0.35f),
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
        
        // Debounced connection status bar — only show after 2s of being non-connected
        var showStatusBar by remember { mutableStateOf(false) }
        LaunchedEffect(connectionStatus) {
            if (connectionStatus != ConnectionStatus.CONNECTED) {
                kotlinx.coroutines.delay(2000) // Wait 2 seconds before showing
                showStatusBar = true
            } else {
                showStatusBar = false
            }
        }
        
        if (shouldConnect && liveDrivers.isNotEmpty() && showStatusBar && connectionStatus != ConnectionStatus.CONNECTED) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        when (connectionStatus) {
                            ConnectionStatus.RECONNECTING, ConnectionStatus.CONNECTING -> Color(0xFFFF8800).copy(alpha = 0.2f)
                            ConnectionStatus.DISCONNECTED -> Color(0xFFFF0040).copy(alpha = 0.2f)
                            else -> Color.Transparent
                        }
                    )
                    .clickable {
                        liveClient.ensureConnected()
                    }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (connectionStatus == ConnectionStatus.RECONNECTING || connectionStatus == ConnectionStatus.CONNECTING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(10.dp),
                        color = Color(0xFFFF8800),
                        strokeWidth = 1.5.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = when (connectionStatus) {
                        ConnectionStatus.RECONNECTING -> "RECONNECTING..."
                        ConnectionStatus.CONNECTING -> "CONNECTING..."
                        ConnectionStatus.DISCONNECTED -> "⚠ DISCONNECTED — TAP TO RETRY"
                        else -> ""
                    },
                    fontFamily = michromaFont,
                    fontSize = 7.sp,
                    color = when (connectionStatus) {
                        ConnectionStatus.RECONNECTING, ConnectionStatus.CONNECTING -> Color(0xFFFF8800)
                        ConnectionStatus.DISCONNECTED -> Color(0xFFFF0040)
                        else -> Color.Transparent
                    },
                    letterSpacing = 1.sp
                )
            }
        }
        
        var selectedTab by remember { mutableIntStateOf(0) }

        when {
            // No active session - streams still available via off-weekend UI
            !shouldConnect -> {
                NoSessionActiveScreen(
                    raceWeekendState = raceWeekendState,
                    raceViewModel = raceViewModel,
                    brigendsFont = brigendsFont,
                    michromaFont = michromaFont,
                    aceStreamState = aceStreamState,
                    onInstallAceStreamRequested = {
                        try {
                            context.startActivity(aceStreamRepo.buildInstallIntent())
                        } catch (e: Exception) {
                            Toast.makeText(context, "Install Ace Stream from Play Store", Toast.LENGTH_LONG).show()
                        }
                    },
                    onStartAceStreamEngineRequested = {
                        raceViewModel.wakeEngineAndCheckStatus(context, allowUiFallback = true)
                    },
                    onManualStreamRefreshRequested = {
                        raceViewModel.fetchAceStreams()
                    }
                )
            }

            // Live session: tabs always shown. SignalR state only affects Timing tab.
            else -> {
                val tabTitles = listOf("TIMING", "STREAM")
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF111111))
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        tabTitles.forEachIndexed { index, title ->
                            val isSelected = selectedTab == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedTab = index }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = title,
                                        fontFamily = michromaFont,
                                        fontSize = 10.sp,
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.4f),
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.6f)
                                            .height(2.dp)
                                            .background(
                                                if (isSelected)
                                                    Brush.horizontalGradient(listOf(Color(0xFFE6007E), Color(0xFFFF0080)))
                                                else
                                                    Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)),
                                                RoundedCornerShape(1.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }

                    when (selectedTab) {
                        0 -> TimingTabContent(
                            liveDrivers = liveDrivers,
                            sessionType = sessionType,
                            isConnected = isConnected,
                            connectionError = connectionError,
                            countdown = countdown,
                            feedSource = feedSource,
                            brigendsFont = brigendsFont,
                            michromaFont = michromaFont,
                            onRetryConnection = { liveClient.connect() }
                        )
                        1 -> Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            com.f1tracker.ui.components.AceStreamLiveCard(
                                raceViewModel = raceViewModel,
                                michromaFont = michromaFont,
                                onInstallRequested = {
                                    try {
                                        context.startActivity(aceStreamRepo.buildInstallIntent())
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Install Ace Stream from Play Store", Toast.LENGTH_LONG).show()
                                    }
                                },
                                onStartEngineRequested = {
                                    raceViewModel.wakeEngineAndCheckStatus(context, allowUiFallback = true)
                                },
                                onManualRefreshRequested = {
                                    raceViewModel.fetchAceStreams()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    } // Close outer Box
}


@Composable
private fun TimingTabContent(
    liveDrivers: List<LiveDriver>,
    sessionType: SessionType,
    isConnected: Boolean,
    connectionError: String?,
    countdown: Int,
    feedSource: String,
    brigendsFont: FontFamily,
    michromaFont: FontFamily,
    onRetryConnection: () -> Unit
) {
    when {
        !isConnected && connectionError == null -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.radialGradient(colors = listOf(Color(0xFF1A0A12), Color.Black), radius = 800f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(40.dp), color = Color(0xFFE6007E), strokeWidth = 2.dp)
                    Text(text = "CONNECTING", fontFamily = brigendsFont, fontSize = 14.sp, color = Color.White, letterSpacing = 3.sp)
                    Text(text = "Connecting to live leaderboard feed", fontFamily = michromaFont, fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                    Text(text = "Streams are available on the STREAM tab", fontFamily = michromaFont, fontSize = 9.sp, color = Color.White.copy(alpha = 0.35f))
                }
            }
        }
        connectionError != null && liveDrivers.isEmpty() -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.radialGradient(colors = listOf(Color(0xFF1A0808), Color.Black), radius = 800f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(imageVector = Icons.Filled.WifiOff, contentDescription = null, tint = Color(0xFFFF0040), modifier = Modifier.size(28.dp))
                    Text(text = "TIMING UNAVAILABLE", fontFamily = brigendsFont, fontSize = 14.sp, color = Color.White, letterSpacing = 2.sp)
                    Text(text = connectionError, fontFamily = michromaFont, fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text(text = "You can still watch on the STREAM tab", fontFamily = michromaFont, fontSize = 9.sp, color = Color.White.copy(alpha = 0.35f))
                    Box(
                        modifier = Modifier
                            .background(Brush.horizontalGradient(listOf(Color(0xFFE6007E), Color(0xFFFF0080))), RoundedCornerShape(6.dp))
                            .clickable(onClick = onRetryConnection)
                            .padding(horizontal = 24.dp, vertical = 10.dp)
                    ) {
                        Text(text = "RETRY TIMING", fontFamily = michromaFont, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }
        }
        liveDrivers.isEmpty() && isConnected -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.radialGradient(colors = listOf(Color(0xFF151008), Color.Black), radius = 800f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(text = if (countdown > 0) "$countdown" else "...", fontFamily = michromaFont, fontSize = 22.sp, color = Color(0xFFFFAA00), fontWeight = FontWeight.Bold)
                    Text(text = "AWAITING DATA", fontFamily = brigendsFont, fontSize = 14.sp, color = Color.White, letterSpacing = 3.sp)
                    Text(text = "Waiting for leaderboard updates.", fontFamily = michromaFont, fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                    Text(text = "Streams are available on the STREAM tab", fontFamily = michromaFont, fontSize = 9.sp, color = Color.White.copy(alpha = 0.35f))
                }
            }
        }
        else -> {
            val stopsOrLapsLabel = if (sessionType == SessionType.RACE || sessionType == SessionType.SPRINT) "STOPS" else "LAPS"
            LeaderboardSessionHeader(stopsOrLapsLabel = stopsOrLapsLabel, michromaFont = michromaFont)
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(liveDrivers) { index, driver ->
                    LeaderboardDriverRow(
                        driver = driver,
                        index = index,
                        showStops = sessionType == SessionType.RACE || sessionType == SessionType.SPRINT,
                        michromaFont = michromaFont
                    )
                }
                item {
                    Text(
                        text = "Leaderboard feed via $feedSource · gaps & classification only",
                        fontFamily = michromaFont,
                        fontSize = 8.sp,
                        color = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

// Session Type Enum
enum class SessionType {
    RACE, SPRINT, QUALIFYING, SPRINT_QUALIFYING, PRACTICE
}

@Composable
private fun LeaderboardSessionHeader(
    stopsOrLapsLabel: String,
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
        Text(
            text = "P",
            fontFamily = michromaFont,
            fontSize = 9.sp,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.width(28.dp),
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "DRIVER",
            fontFamily = michromaFont,
            fontSize = 9.sp,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "GAP",
            fontFamily = michromaFont,
            fontSize = 9.sp,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.width(72.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stopsOrLapsLabel,
            fontFamily = michromaFont,
            fontSize = 9.sp,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.width(40.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LeaderboardDriverRow(
    driver: LiveDriver,
    index: Int,
    showStops: Boolean,
    michromaFont: FontFamily
) {
    val teamColor = getTeamColor(driver.externalTeam, driver.teamColour)
    val isRetired = driver.status == 2 || driver.status == 3
    val positionColor = when (driver.positionDisplay?.toIntOrNull()) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> Color(0xFFE6007E)
    }
    val gapText = when {
        driver.positionDisplay == "1" -> driver.raceTime?.takeIf { it.isNotBlank() } ?: "LEADER"
        !driver.gap.isNullOrBlank() -> {
            val g = driver.gap.trim()
            if (g.startsWith("+")) g else "+ $g"
        }
        !driver.raceTime.isNullOrBlank() -> driver.raceTime
        else -> "—"
    }
    val stopsOrLaps = if (showStops) {
        driver.pits?.takeIf { it.isNotBlank() } ?: "0"
    } else {
        driver.laps?.takeIf { it.isNotBlank() } ?: "—"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        if (isRetired) Color(0xFFFF0040).copy(alpha = 0.12f) else Color(0xFF161616),
                        Color(0xFF121212)
                    )
                ),
                RoundedCornerShape(4.dp)
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(teamColor.copy(alpha = 0.35f), Color.White.copy(alpha = 0.05f))
                ),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.width(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(16.dp)
                    .background(positionColor, RoundedCornerShape(1.5.dp))
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = driver.positionDisplay ?: "${index + 1}",
                fontFamily = michromaFont,
                fontSize = 11.sp,
                color = if (isRetired) Color.White.copy(alpha = 0.4f) else Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = driver.lastName?.uppercase() ?: "DRIVER",
                fontFamily = michromaFont,
                fontSize = 11.sp,
                color = if (isRetired) Color.White.copy(alpha = 0.4f) else Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = driver.externalTeam ?: "",
                fontFamily = michromaFont,
                fontSize = 8.sp,
                color = teamColor.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }

        Text(
            text = gapText,
            fontFamily = michromaFont,
            fontSize = 10.sp,
            color = if (isRetired) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.85f),
            modifier = Modifier.width(72.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )

        Text(
            text = stopsOrLaps,
            fontFamily = michromaFont,
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.width(40.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

// Session-specific column header (legacy full-timing layout kept for reference)
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
                // Qualifying: P | DVR | LIVE SECTORS | BEST | GAP
                Text(
                    text = "LIVE",
                    fontFamily = michromaFont,
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "BEST",
                    fontFamily = michromaFont,
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.width(72.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "GAP",
                    fontFamily = michromaFont,
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.width(56.dp),
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

// Get team color - prefers API hex color, falls back to local data
private fun getTeamColor(teamName: String?, teamColour: String? = null): Color {
    // First try the API-provided hex color
    if (teamColour != null) {
        try {
            return Color(android.graphics.Color.parseColor("#$teamColour"))
        } catch (_: Exception) { }
    }
    
    if (teamName == null) return Color(0xFF888888)
    
    // Fallback to local JSON data
    val colorHex = com.f1tracker.data.local.F1DataProvider.getTeamColorByName(teamName)
    return if (colorHex != null) {
        try {
            Color(android.graphics.Color.parseColor("#$colorHex"))
        } catch (e: Exception) {
            Color(0xFF888888)
        }
    } else {
        Color(0xFF888888)
    }
}

// Get short 3-letter team abbreviation for broadcast style
private fun getShortTeamName(teamName: String?): String {
    if (teamName == null) return "---"
    val lower = teamName.lowercase()
    return when {
        "mercedes" in lower -> "MER"
        "ferrari" in lower -> "FER"
        "mclaren" in lower -> "MCL"
        "red bull" in lower -> "RBR"
        "aston martin" in lower -> "AMR"
        "alpine" in lower -> "ALP"
        "williams" in lower -> "WIL"
        "racing bulls" in lower || lower == "rb" -> "RB"
        "haas" in lower -> "HAA"
        "audi" in lower || "sauber" in lower -> "AUD"
        "cadillac" in lower -> "CAD"
        else -> teamName.take(3).uppercase()
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
    val driverCode = driver.tla ?: generateDriverCode(driver.firstName, driver.lastName)
    val teamColor = getTeamColor(driver.externalTeam, driver.teamColour)
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

// Knockout Zone Separator
@Composable
private fun KnockoutZoneSeparator(
    michromaFont: FontFamily,
    label: String = "KNOCKOUT ZONE"
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Color(0xFFFF0040).copy(alpha = 0.5f))
                    )
                )
        )
        Text(
            text = "  ✕ $label  ",
            fontFamily = michromaFont,
            fontSize = 7.sp,
            color = Color(0xFFFF0040).copy(alpha = 0.8f),
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFFFF0040).copy(alpha = 0.5f), Color.Transparent)
                    )
                )
        )
    }
}

// QUALIFYING / SPRINT QUALIFYING Driver Row - Two-Line Premium Card
@Composable
private fun QualiDriverRow(
    driver: LiveDriver,
    inDangerZone: Boolean = false,
    michromaFont: FontFamily
) {
    val driverCode = driver.tla ?: generateDriverCode(driver.firstName, driver.lastName)
    val teamColor = getTeamColor(driver.externalTeam, driver.teamColour)
    val tyreColor = getTyreColor(driver.tyreCompound)
    val isKnockedOut = driver.knockedOut
    
    // Position accent
    val positionColor = when {
        driver.positionDisplay?.toIntOrNull() == 1 -> Color(0xFFFFD700)  // Gold
        driver.positionDisplay?.toIntOrNull() == 2 -> Color(0xFFC0C0C0)  // Silver
        driver.positionDisplay?.toIntOrNull() == 3 -> Color(0xFFCD7F32)  // Bronze
        isKnockedOut -> Color(0xFFFF0040)  // Red - eliminated
        inDangerZone -> Color(0xFFFF6600)  // Orange - danger zone
        else -> Color.White.copy(alpha = 0.5f)
    }
    
    // Background based on state
    val bgBrush = when {
        isKnockedOut -> Brush.horizontalGradient(
            listOf(Color(0xFFFF0040).copy(alpha = 0.08f), Color(0xFF0D0D0D))
        )
        inDangerZone -> Brush.horizontalGradient(
            listOf(Color(0xFFFF6600).copy(alpha = 0.06f), Color(0xFF0D0D0D))
        )
        driver.inPit -> Brush.horizontalGradient(
            listOf(Color(0xFFFFAA00).copy(alpha = 0.08f), Color(0xFF0D0D0D))
        )
        driver.pitOut -> Brush.horizontalGradient(
            listOf(Color(0xFF00CC66).copy(alpha = 0.08f), Color(0xFF0D0D0D))
        )
        else -> Brush.horizontalGradient(
            listOf(Color(0xFF141414), Color(0xFF0D0D0D))
        )
    }
    
    // Check if driver has active mini-sector progress (on a hot lap)
    val hasActiveSectors = driver.miniSectors.any { sectorSegs ->
        sectorSegs.any { it > 0 }
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgBrush, RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        teamColor.copy(alpha = if (isKnockedOut) 0.15f else 0.35f),
                        Color.White.copy(alpha = 0.03f)
                    )
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(start = 0.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
    ) {
        // === LINE 1: Position | Team bar | TLA | Mini-sectors | Best Lap ===
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Position with accent bar
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(28.dp)
                    .background(positionColor, RoundedCornerShape(1.5.dp))
            )
            Spacer(modifier = Modifier.width(6.dp))
            
            // Position number
            Text(
                text = driver.positionDisplay ?: "-",
                fontFamily = michromaFont,
                fontSize = 13.sp,
                color = if (isKnockedOut) Color.White.copy(alpha = 0.4f) else Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(26.dp),
                maxLines = 1
            )
            
            Spacer(modifier = Modifier.width(6.dp))
            
            // Team color dot + Driver TLA
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(teamColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = driverCode,
                fontFamily = michromaFont,
                fontSize = 11.sp,
                color = if (isKnockedOut) Color.White.copy(alpha = 0.4f) else Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(48.dp),
                maxLines = 1
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Mini-sector progress visualization (fills remaining space)
            if (hasActiveSectors && !isKnockedOut) {
                MiniSectorBar(
                    miniSectors = driver.miniSectors,
                    modifier = Modifier.weight(1f)
                )
            } else {
                // Status text when not on a hot lap
                Text(
                    text = when {
                        isKnockedOut -> "ELIMINATED"
                        driver.inPit -> "IN PIT"
                        driver.pitOut -> "OUT LAP"
                        else -> ""
                    },
                    fontFamily = michromaFont,
                    fontSize = 8.sp,
                    color = when {
                        isKnockedOut -> Color(0xFFFF0040).copy(alpha = 0.7f)
                        driver.inPit -> Color(0xFFFFAA00).copy(alpha = 0.7f)
                        driver.pitOut -> Color(0xFF00CC66).copy(alpha = 0.7f)
                        else -> Color.Transparent
                    },
                    letterSpacing = 1.sp,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.width(6.dp))
            
            // Best lap time
            Text(
                text = driver.raceTime ?: "-",
                fontFamily = michromaFont,
                fontSize = 11.sp,
                color = if (isKnockedOut) Color.White.copy(alpha = 0.35f) else Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(72.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
            
            Spacer(modifier = Modifier.width(6.dp))
            
            // Gap to P1
            Text(
                text = when {
                    driver.positionDisplay == "1" -> "LEADER"
                    driver.gap.isNullOrEmpty() || driver.gap == "" -> "-"
                    else -> driver.gap!!
                },
                fontFamily = michromaFont,
                fontSize = if (driver.positionDisplay == "1") 7.sp else 10.sp,
                color = when {
                    driver.positionDisplay == "1" -> Color(0xFFFFD700).copy(alpha = 0.7f)
                    isKnockedOut -> Color(0xFFFF0040).copy(alpha = 0.5f)
                    else -> Color.White.copy(alpha = 0.6f)
                },
                modifier = Modifier.width(56.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                letterSpacing = if (driver.positionDisplay == "1") 0.5.sp else 0.sp
            )
        }
        
        Spacer(modifier = Modifier.height(3.dp))
        
        // === LINE 2: Team | Tyre | Laps | Sector times (when available) ===
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 17.dp), // Aligned after position bar
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Team name (short)
            Text(
                text = getShortTeamName(driver.externalTeam),
                fontFamily = michromaFont,
                fontSize = 7.sp,
                color = teamColor.copy(alpha = 0.6f),
                letterSpacing = 0.5.sp,
                modifier = Modifier.width(32.dp),
                maxLines = 1
            )
            
            // Tyre compound dot
            if (driver.tyreCompound != null) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(tyreColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = driver.tyreCompound?.take(1)?.uppercase() ?: "",
                    fontFamily = michromaFont,
                    fontSize = 7.sp,
                    color = tyreColor.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            
            // Laps count
            if (driver.laps != null) {
                Text(
                    text = "${driver.laps}L",
                    fontFamily = michromaFont,
                    fontSize = 7.sp,
                    color = Color.White.copy(alpha = 0.35f)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            // Sector times (S1 | S2 | S3) with colors — transient, show when available
            val sectorTimeAlpha = if (isKnockedOut) 0.3f else 0.7f
            for (i in 0..2) {
                val sectorTime = driver.sectors.getOrNull(i) ?: ""
                val sectorColorCode = driver.sectorColors.getOrNull(i) ?: 0
                val sectorColor = when (sectorColorCode) {
                    2 -> Color(0xFFAA00FF) // Purple - overall fastest
                    1 -> Color(0xFF00FF00) // Green - personal best
                    else -> Color.White.copy(alpha = sectorTimeAlpha)
                }
                if (sectorTime.isNotEmpty()) {
                    Text(
                        text = sectorTime,
                        fontFamily = michromaFont,
                        fontSize = 8.sp,
                        color = sectorColor.copy(alpha = sectorTimeAlpha),
                        fontWeight = if (sectorColorCode > 0) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.width(42.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Interval to car ahead
            if (!driver.interval.isNullOrEmpty() && driver.positionDisplay != "1") {
                Text(
                    text = "Δ${driver.interval}",
                    fontFamily = michromaFont,
                    fontSize = 7.sp,
                    color = Color.White.copy(alpha = 0.3f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }
        }
    }
}

// Mini-sector progress bar visualization
@Composable
private fun MiniSectorBar(
    miniSectors: List<List<Int>>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.height(12.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        miniSectors.forEachIndexed { sectorIdx, segments ->
            if (segments.isNotEmpty()) {
                // Each sector's segments as tiny colored blocks
                Row(
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    segments.forEach { status ->
                        val segColor = when {
                            status == 2064 || status == 2049 -> Color(0xFFAA00FF) // Purple - overall fastest
                            status == 2051 -> Color(0xFF00FF00) // Green - personal best
                            status >= 2048 -> Color(0xFFFFCC00) // Yellow - completed
                            else -> Color.White.copy(alpha = 0.1f) // Not reached
                        }
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(if (status >= 2048) 10.dp else 4.dp)
                                .background(segColor, RoundedCornerShape(0.5.dp))
                        )
                    }
                }
                
                // Separator between sectors (except after last)
                if (sectorIdx < miniSectors.size - 1) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(6.dp)
                            .background(Color.White.copy(alpha = 0.15f))
                    )
                }
            }
        }
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
    val driverCode = driver.tla ?: generateDriverCode(driver.firstName, driver.lastName)
    val teamColor = getTeamColor(driver.externalTeam, driver.teamColour)
    
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
    
    // Create full name for lookup
    val fullName = "${firstName?.trim() ?: ""} ${lastName?.trim() ?: ""}".trim()
    
    // Try to get driver code from F1DataProvider (reads from JSON)
    val driver = com.f1tracker.data.local.F1DataProvider.getDriverByName(fullName)
    if (driver != null) {
        return driver.code
    }
    
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
    
    // Map team name to color - use centralized function
    val teamColor = getTeamColor(driver.externalTeam)
    
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
    michromaFont: FontFamily,
    aceStreamState: com.f1tracker.ui.viewmodels.AceStreamState,
    onInstallAceStreamRequested: () -> Unit,
    onStartAceStreamEngineRequested: () -> Unit,
    onManualStreamRefreshRequested: () -> Unit
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
                        raceViewModel = raceViewModel,
                        title = "NEXT SESSION",
                        sessionName = nextSession.sessionType.displayName(),
                        countdown = countdown,
                        dateTime = sessionDateTime,
                        brigendsFont = brigendsFont,
                        michromaFont = michromaFont,
                        accentColor = accentColor,
                        aceStreamState = aceStreamState,
                        onInstallAceStreamRequested = onInstallAceStreamRequested,
                        onStartAceStreamEngineRequested = onStartAceStreamEngineRequested,
                        onManualStreamRefreshRequested = onManualStreamRefreshRequested
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
                        raceViewModel = raceViewModel,
                        title = "NEXT SESSION",
                        sessionName = firstSession.sessionType.displayName(),
                        countdown = countdown,
                        dateTime = sessionDateTime,
                        brigendsFont = brigendsFont,
                        michromaFont = michromaFont,
                        accentColor = accentColor,
                        aceStreamState = aceStreamState,
                        onInstallAceStreamRequested = onInstallAceStreamRequested,
                        onStartAceStreamEngineRequested = onStartAceStreamEngineRequested,
                        onManualStreamRefreshRequested = onManualStreamRefreshRequested
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
                        raceViewModel = raceViewModel,
                        title = "NEXT SESSION",
                        sessionName = state.nextMainEventType.displayName(),
                        countdown = countdown,
                        dateTime = sessionDateTime,
                        brigendsFont = brigendsFont,
                        michromaFont = michromaFont,
                        accentColor = accentColor,
                        aceStreamState = aceStreamState,
                        onInstallAceStreamRequested = onInstallAceStreamRequested,
                        onStartAceStreamEngineRequested = onStartAceStreamEngineRequested,
                        onManualStreamRefreshRequested = onManualStreamRefreshRequested
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
            is com.f1tracker.data.models.RaceWeekendState.SeasonCompleted -> {
                val nextSeasonYear = java.time.Year.now().value + 1
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF1A1A1A),
                                    Color.Black
                                ),
                                radius = 600f
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Animated Trophy / Flag Icon
                        val infiniteTransition = rememberInfiniteTransition(label = "season_end")
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.05f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(2000, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "scale"
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .background(
                                    Color(0xFFFFAA00).copy(alpha = 0.1f),
                                    CircleShape
                                )
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFFFFAA00).copy(alpha = 0.3f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "Season Completed",
                                tint = Color(0xFFFFAA00),
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "SEASON COMPLETED",
                                fontFamily = brigendsFont,
                                fontSize = 18.sp,
                                color = Color.White,
                                letterSpacing = 2.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            
                            Text(
                                text = "See you on track in $nextSeasonYear",
                                fontFamily = michromaFont,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.6f),
                                letterSpacing = 1.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                        
                        // Premium Divider
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(2.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color(0xFFE6007E),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                        
                        // Action / Subtext
                        Text(
                            text = "CHECK NEWS FOR UPDATES",
                            fontFamily = michromaFont,
                            fontSize = 10.sp,
                            color = Color(0xFFE6007E),
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CountdownDisplay(
    raceViewModel: com.f1tracker.ui.viewmodels.RaceViewModel,
    title: String,
    sessionName: String,
    countdown: String,
    dateTime: java.time.LocalDateTime,
    brigendsFont: FontFamily,
    michromaFont: FontFamily,
    accentColor: Color,
    aceStreamState: com.f1tracker.ui.viewmodels.AceStreamState,
    onInstallAceStreamRequested: () -> Unit,
    onStartAceStreamEngineRequested: () -> Unit,
    onManualStreamRefreshRequested: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Native Embedded Ace Stream Card
        com.f1tracker.ui.components.AceStreamLiveCard(
            raceViewModel = raceViewModel,
            michromaFont = michromaFont,
            onInstallRequested = onInstallAceStreamRequested,
            onStartEngineRequested = onStartAceStreamEngineRequested,
            onManualRefreshRequested = onManualStreamRefreshRequested
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
        instant.atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
    } catch (e: Exception) {
        java.time.LocalDateTime.now(java.time.ZoneId.systemDefault())
    }
}

private fun formatFullDateTime(dateTime: java.time.LocalDateTime): String {
    val formatter = java.time.format.DateTimeFormatter.ofPattern("EEE, dd MMM yyyy • hh:mm a")
    return dateTime.format(formatter)
}
