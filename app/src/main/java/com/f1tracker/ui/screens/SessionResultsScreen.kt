package com.f1tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.f1tracker.R
import com.f1tracker.data.local.F1DataProvider
import com.f1tracker.data.models.DriverResult
import com.f1tracker.data.models.HighlightVideo
import com.f1tracker.data.models.SessionResult
import com.f1tracker.ui.components.AnimatedHeader
import com.f1tracker.ui.viewmodels.RaceViewModel

@Composable
fun SessionResultsScreen(
    sessionResult: SessionResult,
    raceName: String = "",
    onBackClick: () -> Unit,
    viewModel: RaceViewModel = hiltViewModel()
) {
    val brigendsFont = FontFamily(Font(R.font.brigends_expanded, FontWeight.Normal))
    val michromaFont = FontFamily(Font(R.font.michroma, FontWeight.Normal))
    val accentColor = Color(0xFFFF0080)
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    
    // Highlights state
    val allHighlights by viewModel.raceHighlights.collectAsState()
    var selectedVideoId by remember { mutableStateOf<String?>(null) }
    
    // Filter highlights for this race + current year + matching session type
    val currentYear = java.time.Year.now().value.toString()
    val raceNameNormalized = raceName.lowercase().replace("grand prix", "").trim()
    
    // Map SessionResult session name to highlight sessionType
    val sessionTypeMapping = mapOf(
        "race" to "Race",
        "qualifying" to "Qualifying",
        "sprint" to "Sprint",
        "sprint qualifying" to "Sprint Qualifying",
        "fp1" to "FP1",
        "fp2" to "FP2",
        "fp3" to "FP3",
        "free practice 1" to "FP1",
        "free practice 2" to "FP2",
        "free practice 3" to "FP3"
    )
    val targetSessionType = sessionTypeMapping[sessionResult.sessionName.lowercase()] ?: sessionResult.sessionName
    
    val sessionHighlight = remember(allHighlights, raceName, sessionResult.sessionName) {
        allHighlights.find { highlight ->
            val highlightRaceNormalized = highlight.raceName.lowercase().trim()
            highlight.year == currentYear && 
                highlightRaceNormalized.contains(raceNameNormalized) &&
                highlight.sessionType.equals(targetSessionType, ignoreCase = true)
        }
    }
    
    // Fetch highlights on mount
    LaunchedEffect(raceName) {
        if (raceName.isNotEmpty()) {
            viewModel.loadAllHighlights()
        }
    }
    
    // YouTube Player Dialog
    if (selectedVideoId != null) {
        LaunchedEffect(selectedVideoId) {
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        DisposableEffect(selectedVideoId) {
            onDispose {
                activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
        Dialog(
            onDismissRequest = { selectedVideoId = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                YouTubePlayerScreen(videoId = selectedVideoId!!)
                IconButton(
                    onClick = { selectedVideoId = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        
        // Session Title Header with Back Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .clickable { onBackClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Column {
                val sessionName = sessionResult.sessionName.uppercase()
                val useMichroma = sessionName.any { it.isDigit() }
                
                Text(
                    text = sessionName,
                    fontFamily = if (useMichroma) michromaFont else brigendsFont,
                    fontSize = 16.sp,
                    fontWeight = if (useMichroma) FontWeight.Bold else FontWeight.Normal,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "FULL RESULTS",
                    fontFamily = michromaFont,
                    fontSize = 10.sp,
                    color = accentColor,
                    letterSpacing = 2.sp
                )
            }
        }
        
        // Results List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp, start = 20.dp, end = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Highlight for this session (shown at top if available)
            if (sessionHighlight != null) {
                item {
                    SessionHighlightCard(
                        highlight = sessionHighlight,
                        michromaFont = michromaFont,
                        onClick = { selectedVideoId = sessionHighlight.id },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            // Podium Section (Top 3)
            if (sessionResult.results.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Podium (Top 3)
                            PodiumSection(
                                results = sessionResult.results.take(3),
                                michromaFont = michromaFont,
                                brigendsFont = brigendsFont
                            )
                            
                            if (sessionResult.results.size > 3) {
                                androidx.compose.material3.Divider(
                                    color = Color.White.copy(alpha = 0.05f),
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )
                                
                                // Rest of the field (4+)
                                sessionResult.results.drop(3).forEach { result ->
                                    DriverResultRow(
                                        result = result,
                                        michromaFont = michromaFont
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PodiumSection(
    results: List<DriverResult>,
    michromaFont: FontFamily,
    brigendsFont: FontFamily
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        results.forEach { result ->
            PodiumDriverCard(
                result = result,
                michromaFont = michromaFont,
                brigendsFont = brigendsFont,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Handle case where less than 3 drivers (unlikely for F1 but good for safety)
        if (results.size < 3) {
            repeat(3 - results.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PodiumDriverCard(
    result: DriverResult,
    michromaFont: FontFamily,
    brigendsFont: FontFamily,
    modifier: Modifier
) {
    // Lookup driver and team info
    val driverInfo = F1DataProvider.getDriverByESPNId(result.espnId) 
        ?: F1DataProvider.getDriverByName(result.driverName)
    
    val teamInfo = driverInfo?.team?.let { F1DataProvider.getTeamByApiId(it) }
    val teamColor = teamInfo?.color?.let { Color(android.graphics.Color.parseColor("#$it")) } ?: Color.Gray
    
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF111111))
    ) {
        // Team Color Background (faded)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(teamColor.copy(alpha = 0.2f))
        )
        
        // Driver Image (Cropped/Scaled like LastRaceCard)
        if (driverInfo?.headshotF1 != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RectangleShape),
                contentAlignment = Alignment.TopCenter
            ) {
                AsyncImage(
                    model = driverInfo.headshotF1,
                    contentDescription = result.driverCode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(2.2f)
                        .offset(y = 60.dp), // Increased offset to shift headshot down
                    contentScale = ContentScale.FillWidth,
                    alignment = Alignment.TopCenter
                )
            }
        }
        
        // Overlay Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.8f),
                            Color.Black
                        ),
                        startY = 100f
                    )
                )
        )
        
        // Content
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Position
            Text(
                text = result.position.toString(),
                fontFamily = michromaFont,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = when (result.position) {
                    1 -> Color(0xFFFFD700)
                    2 -> Color(0xFFC0C0C0)
                    3 -> Color(0xFFCD7F32)
                    else -> Color.White
                }
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Name
            Text(
                text = result.driverCode,
                fontFamily = brigendsFont,
                fontSize = 14.sp,
                color = Color.White
            )
            
            // Team
            Text(
                text = teamInfo?.abbreviation ?: result.team ?: "",
                fontFamily = michromaFont,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DriverResultRow(
    result: DriverResult,
    michromaFont: FontFamily
) {
    // Lookup driver and team info
    val driverInfo = F1DataProvider.getDriverByESPNId(result.espnId)
        ?: F1DataProvider.getDriverByName(result.driverName)
    
    val teamInfo = driverInfo?.team?.let { F1DataProvider.getTeamByApiId(it) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Position
            Text(
                text = result.position.toString(),
                fontFamily = michromaFont,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.width(32.dp),
                maxLines = 1,
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Driver Headshot (Small circle)
            if (driverInfo?.headshotF1 != null) {
                AsyncImage(
                    model = driverInfo.headshotF1,
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f)),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter
                )
            } else {
                // Fallback circle if no image
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Name & Team
            Column {
                // Smart name shortening
                val fullName = result.driverName
                val nameParts = fullName.split(" ")
                val displayName = if (nameParts.size > 2) {
                    // Has middle name - use last two words
                    nameParts.takeLast(2).joinToString(" ")
                } else {
                    fullName
                }
                
                Text(
                    text = displayName,
                    fontFamily = michromaFont,
                    fontSize = 14.sp,
                    color = Color.White
                )
                Text(
                    text = teamInfo?.name ?: result.team ?: "",
                    fontFamily = michromaFont,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
        
        // Time or Gap
        if (result.time != null) {
            Text(
                text = result.time,
                fontFamily = michromaFont,
                fontSize = 12.sp,
                color = if (result.position == 1) Color(0xFFFF0080) else Color.White.copy(alpha = 0.8f),
                fontWeight = if (result.position == 1) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
    
    androidx.compose.material3.Divider(
        color = Color.White.copy(alpha = 0.05f),
        thickness = 1.dp,
        modifier = Modifier.padding(horizontal = 20.dp)
    )
}

@Composable
private fun SessionHighlightCard(
    highlight: HighlightVideo,
    michromaFont: FontFamily,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.width(240.dp)
) {
    Card(
        modifier = modifier
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                AsyncImage(
                    model = highlight.thumbnail,
                    contentDescription = highlight.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Play Icon Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFFF0080).copy(alpha = 0.9f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            Text(
                text = highlight.title,
                fontFamily = michromaFont,
                fontSize = 11.sp,
                color = Color.White,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.padding(10.dp)
            )
        }
    }
}
