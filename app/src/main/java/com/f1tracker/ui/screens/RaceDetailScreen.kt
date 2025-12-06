package com.f1tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.f1tracker.R
import com.f1tracker.data.local.F1DataProvider
import com.f1tracker.data.models.Race
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import androidx.hilt.navigation.compose.hiltViewModel
import com.f1tracker.ui.viewmodels.RaceViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import com.f1tracker.data.models.HighlightVideo
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable

fun RaceDetailScreen(
    race: Race,
    viewModel: RaceViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    LaunchedEffect(race) {
        viewModel.selectRace(race)
    }

    val lastYearResults by viewModel.lastYearRaceResults.collectAsState()
    val michromaFont = FontFamily(Font(R.font.michroma, FontWeight.Normal))
    val brigendsFont = FontFamily(Font(R.font.brigends_expanded, FontWeight.Normal))
    
    var showCircuitLayout by remember { mutableStateOf(false) }

    if (showCircuitLayout) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showCircuitLayout = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF111111), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "CIRCUIT LAYOUT",
                        fontFamily = brigendsFont,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    
                    AsyncImage(
                        model = getCircuitLayoutImage(race.circuit.circuitId),
                        contentDescription = "Circuit Layout",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.5f),
                        contentScale = ContentScale.Fit
                    )
                    
                    androidx.compose.material3.Button(
                        onClick = { showCircuitLayout = false },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF0080)
                        )
                    ) {
                        Text(
                            text = "CLOSE",
                            fontFamily = michromaFont,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }



    // Scroll state for sticky header
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val showStickyHeader by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 600
        }
    }

    // Get flag colors for gradient
    val flagColors = getFlagColorsForDetail(race.circuit.location.country)
    
    // Last Year's Results (Podium + Full List)
    val sprintResults by viewModel.lastYearSprintResults.collectAsState()
    var selectedResultType by remember { mutableStateOf(ResultType.RACE) }
    val hasSprintResults = sprintResults?.isNotEmpty() == true
    
    // Race Highlights
    val raceHighlights by viewModel.raceHighlights.collectAsState()
    val context = LocalContext.current
    
    // YouTube Player State
    var selectedVideoId by remember { mutableStateOf<String?>(null) }
    val activity = context as? android.app.Activity
    
    // In-app YouTube Player Dialog
    if (selectedVideoId != null) {
        // Force landscape orientation when opening video
        LaunchedEffect(selectedVideoId) {
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        
        // Restore portrait when dialog closes
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
                
                // Close button
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
        
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
        // Content
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Determine if race is completed (for section ordering)
            val isRaceCompleted = try {
                val nowUTC = LocalDateTime.now(java.time.ZoneId.of("UTC"))
                val raceDateTime = LocalDateTime.parse("${race.date}T${race.time}", DateTimeFormatter.ISO_DATE_TIME)
                raceDateTime.isBefore(nowUTC)
            } catch (e: Exception) {
                false
            }
            
            // Race Header (Circuit Image + Info)
            item {
                RaceHeaderSection(
                    race = race, 
                    michromaFont = michromaFont, 
                    brigendsFont = brigendsFont,
                    onViewCircuitClick = { showCircuitLayout = true }
                )
            }
            
            // Schedule Section - at top for upcoming races
            if (!isRaceCompleted) {
                item {
                    ScheduleSection(race, michromaFont, brigendsFont)
                }
            }
            
            // Highlights Section
            if (raceHighlights.isNotEmpty()) {
                item {
                    HighlightsSection(
                        highlights = raceHighlights,
                        isRaceCompleted = isRaceCompleted,
                        michromaFont = michromaFont,
                        brigendsFont = brigendsFont,
                        onHighlightClick = { highlight ->
                            // Extract video ID from URL
                            val videoId = highlight.id
                            selectedVideoId = videoId
                        }
                    )
                }
            }
            
            // Results Section (shows for both completed and upcoming with last year's data)
            if ((lastYearResults != null && lastYearResults?.results?.isNotEmpty() == true) || hasSprintResults) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {
                        // Header Row with Toggle
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = if (isRaceCompleted) "RESULTS" else "LAST YEAR'S RESULTS",
                                fontFamily = brigendsFont,
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                letterSpacing = 2.sp
                            )
                            
                            // Sprint/Race Toggle
                            if (hasSprintResults) {
                                Row(
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .padding(4.dp)
                                ) {
                                    ResultType.values().forEach { type ->
                                        val isSelected = selectedResultType == type
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSelected) Color(0xFFFF0080) else Color.Transparent)
                                                .clickable { selectedResultType = type }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = type.name,
                                                fontFamily = michromaFont,
                                                fontSize = 10.sp,
                                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        val resultsToShow = if (selectedResultType == ResultType.RACE) lastYearResults?.results else sprintResults
                        
                        if (resultsToShow != null && resultsToShow.isNotEmpty()) {
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
                                        results = resultsToShow.take(3),
                                        michromaFont = michromaFont,
                                        brigendsFont = brigendsFont
                                    )
                                    
                                    androidx.compose.material3.Divider(
                                        color = Color.White.copy(alpha = 0.05f),
                                        thickness = 1.dp,
                                        modifier = Modifier.padding(horizontal = 20.dp)
                                    )
                                    
                                    // Rest of the field (4+)
                                    resultsToShow.drop(3).forEachIndexed { index, result ->
                                        DriverResultRow(
                                            result = result, 
                                            position = index + 4, 
                                            michromaFont = michromaFont
                                        )
                                    }
                                }
                            }
                        } else {
                             Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "NO DATA AVAILABLE",
                                    fontFamily = michromaFont,
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            }
            
            // Schedule Section - at bottom for completed races
            if (isRaceCompleted) {
                item {
                    ScheduleSection(race, michromaFont, brigendsFont)
                }
            }
        }
        
        // Sticky Header / Back Button
        androidx.compose.animation.AnimatedVisibility(
            visible = true,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp) // Reduced height
                    .background(
                        if (showStickyHeader) Color.Black.copy(alpha = 0.95f) else Color.Transparent
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp), // Removed top padding
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (showStickyHeader) Color.Transparent else Color.Black.copy(alpha = 0.5f), 
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    
                    if (showStickyHeader) {
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = race.raceName.uppercase(),
                            fontFamily = brigendsFont,
                            fontSize = 16.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
                
                if (showStickyHeader) {
                    androidx.compose.material3.Divider(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        color = Color.White.copy(alpha = 0.1f)
                    )
                }
            }
        }
    }
    }
}

// ... (RaceHeaderSection and ScheduleSection remain unchanged) ...



private fun getFlagColorsForDetail(country: String): Pair<Color, Color> {
    return when (country.lowercase()) {
        "australia" -> Pair(Color(0xFF00008B), Color(0xFFFFD700))
        "bahrain" -> Pair(Color(0xFFCE1126), Color(0xFFFFFFFF))
        "china" -> Pair(Color(0xFFDE2910), Color(0xFFFFDE00))
        "monaco" -> Pair(Color(0xFFCE1126), Color(0xFFFFFFFF))
        "spain" -> Pair(Color(0xFFC60B1E), Color(0xFFFFC400))
        "canada" -> Pair(Color(0xFFFF0000), Color(0xFFFFFFFF))
        "austria" -> Pair(Color(0xFFED2939), Color(0xFFFFFFFF))
        "uk", "great britain" -> Pair(Color(0xFF012169), Color(0xFFC8102E))
        "hungary" -> Pair(Color(0xFFCD2A3E), Color(0xFF436F4D))
        "belgium" -> Pair(Color(0xFF000000), Color(0xFFFDDA24))
        "italy" -> Pair(Color(0xFF009246), Color(0xFFCE2B37))
        "singapore" -> Pair(Color(0xFFEF3340), Color(0xFFFFFFFF))
        "japan" -> Pair(Color(0xFFBC002D), Color(0xFFFFFFFF))
        "usa", "united states" -> Pair(Color(0xFF3C3B6E), Color(0xFFB22234))
        "mexico" -> Pair(Color(0xFF006847), Color(0xFFCE1126))
        "brazil" -> Pair(Color(0xFF009B3A), Color(0xFFFEDF00))
        "qatar" -> Pair(Color(0xFF8A1538), Color(0xFFFFFFFF))
        "uae", "abu dhabi" -> Pair(Color(0xFFFF0000), Color(0xFF00732F))
        "saudi arabia" -> Pair(Color(0xFF165B33), Color(0xFFFFFFFF))
        "netherlands" -> Pair(Color(0xFF21468B), Color(0xFFAE1C28))
        "azerbaijan" -> Pair(Color(0xFF00B5E2), Color(0xFFEF3340))
        "miami" -> Pair(Color(0xFF3C3B6E), Color(0xFFB22234))
        else -> Pair(Color(0xFF1A0033), Color(0xFFFF0080))
    }
}

// ... (Other helper functions remain unchanged) ...

@Composable
private fun RaceHeaderSection(
    race: Race,
    michromaFont: FontFamily,
    brigendsFont: FontFamily,
    onViewCircuitClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
    ) {
        // Circuit Image Background
        val circuitImage = getCircuitImageForDetail(race.circuit.circuitId)
        AsyncImage(
            model = circuitImage,
            contentDescription = race.circuit.circuitName,
            modifier = Modifier
                .fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.6f),
                            Color.Black
                        )
                    )
                )
        )
        
        // View Circuit Button (Top Right)
        androidx.compose.material3.Button(
            onClick = onViewCircuitClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 16.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = Color.Black.copy(alpha = 0.6f)
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = "VIEW CIRCUIT",
                fontFamily = michromaFont,
                fontSize = 10.sp,
                color = Color.White
            )
        }
        
        // Content
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            // Round Badge
            Text(
                text = "ROUND ${race.round}",
                fontFamily = michromaFont,
                fontSize = 12.sp,
                color = Color(0xFFFF0080),
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(Color(0xFFFF0080).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Race Name
            Text(
                text = race.raceName.uppercase(),
                fontFamily = brigendsFont,
                fontSize = 24.sp,
                color = Color.White,
                lineHeight = 28.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Location
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = "https://flagcdn.com/w40/${getCountryCodeForDetail(race.circuit.location.country)}.png",
                    contentDescription = null,
                    modifier = Modifier
                        .width(24.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${race.circuit.location.locality}, ${race.circuit.location.country}".uppercase(),
                    fontFamily = michromaFont,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Date
            Text(
                text = formatDateDetail(race.date),
                fontFamily = michromaFont,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun ScheduleSection(
    race: Race,
    michromaFont: FontFamily,
    brigendsFont: FontFamily
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = "WEEKEND SCHEDULE",
            fontFamily = brigendsFont,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.8f),
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val sessions = listOfNotNull(
                    race.firstPractice?.let { "FP1" to it },
                    race.secondPractice?.let { "FP2" to it },
                    race.thirdPractice?.let { "FP3" to it },
                    race.sprintQualifying?.let { "Sprint Quali" to it },
                    race.sprint?.let { "Sprint" to it },
                    race.qualifying?.let { "Qualifying" to it },
                    "Race" to com.f1tracker.data.models.SessionInfo(race.date, race.time)
                ).sortedBy { 
                    try {
                        LocalDateTime.parse("${it.second.date}T${it.second.time}", DateTimeFormatter.ISO_DATE_TIME)
                    } catch (e: Exception) {
                        LocalDateTime.MAX
                    }
                }

                sessions.forEach { (name, session) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(24.dp)
                                    .background(
                                        if (name == "Race") Color(0xFFFF0080) else Color.White.copy(alpha = 0.2f),
                                        RoundedCornerShape(2.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = name,
                                fontFamily = michromaFont,
                                fontSize = 14.sp,
                                color = if (name == "Race") Color.White else Color.White.copy(alpha = 0.9f),
                                fontWeight = if (name == "Race") FontWeight.Bold else FontWeight.Normal
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = formatDateDetail(session.date).substringBefore(","),
                                fontFamily = michromaFont,
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                            Text(
                                text = formatTimeDetail(session.time),
                                fontFamily = michromaFont,
                                fontSize = 12.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (session != sessions.last().second) {
                        androidx.compose.material3.Divider(
                            color = Color.White.copy(alpha = 0.05f),
                            thickness = 1.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PodiumSection(
    results: List<com.f1tracker.data.models.RaceResult>,
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
        results.forEachIndexed { index, result ->
            PodiumDriverCard(
                result = result,
                position = index + 1,
                michromaFont = michromaFont,
                brigendsFont = brigendsFont,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PodiumDriverCard(
    result: com.f1tracker.data.models.RaceResult,
    position: Int,
    michromaFont: FontFamily,
    brigendsFont: FontFamily,
    modifier: Modifier
) {
    val driverInfo = F1DataProvider.getDriverByApiId(result.driver.driverId)
    val teamInfo = F1DataProvider.getTeamByApiId(result.constructor.constructorId)
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
                    contentDescription = result.driver.code,
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
                text = position.toString(),
                fontFamily = michromaFont,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = when (position) {
                    1 -> Color(0xFFFFD700)
                    2 -> Color(0xFFC0C0C0)
                    3 -> Color(0xFFCD7F32)
                    else -> Color.White
                }
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Name
            Text(
                text = result.driver.code,
                fontFamily = brigendsFont,
                fontSize = 14.sp,
                color = Color.White
            )
            
            // Team
            Text(
                text = teamInfo?.abbreviation ?: "",
                fontFamily = michromaFont,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

private fun getCircuitLayoutImage(circuitId: String): Int {
    return when (circuitId) {
        "albert_park" -> R.drawable.layout_australia
        "bahrain" -> R.drawable.layout_bahrain
        "shanghai" -> R.drawable.layout_china
        "monaco" -> R.drawable.layout_monaco
        "catalunya" -> R.drawable.layout_spain
        "villeneuve" -> R.drawable.layout_canada
        "red_bull_ring" -> R.drawable.layout_austria
        "silverstone" -> R.drawable.layout_great_britain
        "hungaroring" -> R.drawable.layout_hungary
        "spa" -> R.drawable.layout_belgium
        "monza" -> R.drawable.layout_italy
        "marina_bay" -> R.drawable.layout_singapore
        "suzuka" -> R.drawable.layout_japan
        "americas" -> R.drawable.layout_usa
        "rodriguez" -> R.drawable.layout_mexico
        "interlagos" -> R.drawable.layout_brazil
        "vegas" -> R.drawable.layout_las_vegas
        "losail" -> R.drawable.layout_qatar
        "yas_marina" -> R.drawable.layout_abu_dhabi
        "jeddah" -> R.drawable.layout_saudi_arabia
        "miami" -> R.drawable.miami_circuit // Fallback or if layout exists
        "imola" -> R.drawable.layout_emilia_romagna
        "baku" -> R.drawable.layout_baku
        "zandvoort" -> R.drawable.layout_netherlands
        else -> R.drawable.layout_bahrain
    }
}

@Composable
private fun DriverResultRow(
    result: com.f1tracker.data.models.RaceResult,
    position: Int,
    michromaFont: FontFamily
) {
    val driverInfo = F1DataProvider.getDriverByApiId(result.driver.driverId)
    val teamInfo = F1DataProvider.getTeamByApiId(result.constructor.constructorId)
    
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
                text = position.toString(),
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
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Name & Team
            Column {
                // Smart name shortening: if has middle name, use middle + last only
                val fullName = "${result.driver.givenName} ${result.driver.familyName}"
                val nameParts = fullName.split(" ")
                val displayName = if (nameParts.size > 2) {
                    // Has middle name - use last two words (middle + last)
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
                    text = teamInfo?.name ?: result.constructor.name,
                    fontFamily = michromaFont,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
        
        // Points
        if (result.points.toFloatOrNull() ?: 0f > 0) {
            Text(
                text = "+${result.points}",
                fontFamily = michromaFont,
                fontSize = 12.sp,
                color = Color(0xFFFF0080),
                fontWeight = FontWeight.Bold
            )
        }
    }
    
    androidx.compose.material3.Divider(
        color = Color.White.copy(alpha = 0.05f),
        thickness = 1.dp,
        modifier = Modifier.padding(horizontal = 20.dp)
    )
}

private fun getCountryCodeForDetail(country: String): String {
    return when (country.lowercase()) {
        "bahrain" -> "bh"
        "saudi arabia" -> "sa"
        "australia" -> "au"
        "japan" -> "jp"
        "china" -> "cn"
        "usa", "united states" -> "us"
        "italy" -> "it"
        "monaco" -> "mc"
        "spain" -> "es"
        "canada" -> "ca"
        "austria" -> "at"
        "uk", "united kingdom", "great britain" -> "gb"
        "hungary" -> "hu"
        "belgium" -> "be"
        "netherlands" -> "nl"
        "azerbaijan" -> "az"
        "singapore" -> "sg"
        "mexico" -> "mx"
        "brazil" -> "br"
        "uae", "united arab emirates" -> "ae"
        "qatar" -> "qa"
        "miami" -> "us"
        else -> "un"
    }
}



private fun formatTimeDetail(timeString: String): String {
    return try {
        val utcTime = java.time.LocalTime.parse(timeString.substringBefore("Z"))
        val istTime = utcTime.atDate(java.time.LocalDate.now())
            .atZone(java.time.ZoneId.of("UTC"))
            .withZoneSameInstant(java.time.ZoneId.of("Asia/Kolkata"))
            .toLocalTime()
        
        val formatter = DateTimeFormatter.ofPattern("hh:mm a")
        istTime.format(formatter)
    } catch (e: Exception) {
        timeString
    }
}

private fun formatDateDetail(dateString: String): String {
    return try {
        val date = java.time.LocalDate.parse(dateString)
        val formatter = DateTimeFormatter.ofPattern("dd MMM")
        date.format(formatter).uppercase()
    } catch (e: Exception) {
        dateString
    }
}

private fun getCircuitImageForDetail(circuitId: String): Int {
    return when (circuitId) {
        "albert_park" -> R.drawable.australia_circuit
        "bahrain" -> R.drawable.bahrain_circuit
        "shanghai" -> R.drawable.china_circuit
        "monaco" -> R.drawable.monaco_circuit
        "catalunya" -> R.drawable.spain_circuit
        "villeneuve" -> R.drawable.canada_circuit
        "red_bull_ring" -> R.drawable.austria_circuit
        "silverstone" -> R.drawable.great_britain_circuit
        "hungaroring" -> R.drawable.hungary_circuit
        "spa" -> R.drawable.belgium_circuit
        "monza" -> R.drawable.italy_circuit
        "marina_bay" -> R.drawable.singapore_circuit
        "suzuka" -> R.drawable.japan_circuit
        "americas" -> R.drawable.usa_circuit
        "rodriguez" -> R.drawable.mexico_circuit
        "interlagos" -> R.drawable.brazil_circuit
        "vegas" -> R.drawable.las_vegas_circuit
        "losail" -> R.drawable.qatar_circuit
        "yas_marina" -> R.drawable.abu_dhabi_circuit
        "jeddah" -> R.drawable.saudi_arabia_circuit
        "miami" -> R.drawable.miami_circuit
        "imola" -> R.drawable.emilia_romagna_circuit
        "baku" -> R.drawable.azerbaijan_circuit
        "zandvoort" -> R.drawable.netherlands_circuit
        else -> R.drawable.bahrain_circuit
    }
}

private enum class ResultType {
    RACE, SPRINT
}

@Composable
private fun HighlightsSection(
    highlights: List<HighlightVideo>,
    isRaceCompleted: Boolean,
    michromaFont: FontFamily,
    brigendsFont: FontFamily,
    onHighlightClick: (HighlightVideo) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = if (isRaceCompleted) "HIGHLIGHTS" else "LAST YEAR'S HIGHLIGHTS",
            fontFamily = brigendsFont,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.8f),
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            highlights.forEach { highlight ->
                HighlightCard(
                    highlight = highlight,
                    michromaFont = michromaFont,
                    brigendsFont = brigendsFont,
                    onClick = { onHighlightClick(highlight) }
                )
            }
        }
    }
}

@Composable
private fun HighlightCard(
    highlight: HighlightVideo,
    michromaFont: FontFamily,
    brigendsFont: FontFamily,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column {
            // Thumbnail with Play Icon
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(157.dp)
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
                            .size(48.dp)
                            .background(Color(0xFFFF0080).copy(alpha = 0.9f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
            
            // Title
            Text(
                text = highlight.title,
                fontFamily = michromaFont,
                fontSize = 12.sp,
                color = Color.White,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}
