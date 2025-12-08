package com.f1tracker.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.f1tracker.R
import com.f1tracker.data.local.F1DataProvider
import com.f1tracker.data.models.ConstructorStanding
import com.f1tracker.data.models.DriverStanding
import com.f1tracker.ui.viewmodels.RaceViewModel
import com.f1tracker.ui.viewmodels.StandingsViewModel
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

// ═══════════════════════════════════════════════════════════════════════════════
// PREMIUM 2025 SEASON BREAK SCREEN
// Editorial magazine style - Clean, typographic, sophisticated
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
fun SeasonBreakScreen(
    raceViewModel: RaceViewModel,
    standingsViewModel: StandingsViewModel
) {
    val driverStandings by standingsViewModel.driverStandings.collectAsState()
    val constructorStandings by standingsViewModel.constructorStandings.collectAsState()
    val allRaces by raceViewModel.allRaces.collectAsState()
    
    val driverChampion = driverStandings?.firstOrNull()
    val constructorChampion = constructorStandings?.firstOrNull()
    
    val brigendsFont = FontFamily(Font(R.font.brigends_expanded, FontWeight.Normal))
    val michromaFont = FontFamily(Font(R.font.michroma, FontWeight.Normal))
    
    var showDrivers by remember { mutableStateOf(true) }
    var contentVisible by remember { mutableStateOf(false) }
    
    val displayedSeason = allRaces.firstOrNull()?.season ?: java.time.Year.now().value.toString()
    val nextSeasonYear = (displayedSeason.toIntOrNull() ?: java.time.Year.now().value) + 1

    val activeTeamColor = if (showDrivers) {
        driverChampion?.constructors?.firstOrNull()?.let { 
            F1DataProvider.getTeamByApiId(it.constructorId)?.color 
        }?.let { Color(android.graphics.Color.parseColor("#$it")) } ?: Color(0xFFE10600)
    } else {
        constructorChampion?.let { 
            F1DataProvider.getTeamByApiId(it.constructor.constructorId)?.color 
        }?.let { Color(android.graphics.Color.parseColor("#$it")) } ?: Color(0xFFE10600)
    }

    LaunchedEffect(Unit) {
        standingsViewModel.loadStandings()
        kotlinx.coroutines.delay(100)
        contentVisible = true
    }

    // Pure black background - no glows
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // ══════════════════════════════════════════════════════════════════
            // HERO SECTION - Full bleed champion showcase
            // ══════════════════════════════════════════════════════════════════
            item {
                PremiumHeroSection(
                    season = displayedSeason,
                    champion = driverChampion,
                    brigendsFont = brigendsFont,
                    michromaFont = michromaFont,
                    teamColor = activeTeamColor,
                    isVisible = contentVisible
                )
            }
            
            // ══════════════════════════════════════════════════════════════════
            // CHAMPIONSHIP STATS PILL
            // ══════════════════════════════════════════════════════════════════
            item {
                Spacer(modifier = Modifier.height(24.dp))
                GlassStatsPill(
                    champion = driverChampion,
                    michromaFont = michromaFont
                )
            }
            
            // ══════════════════════════════════════════════════════════════════
            // COUNTDOWN TO NEXT SEASON
            // ══════════════════════════════════════════════════════════════════
            item {
                Spacer(modifier = Modifier.height(24.dp))
                MinimalCountdown(
                    nextSeasonYear = nextSeasonYear,
                    brigendsFont = brigendsFont,
                    michromaFont = michromaFont
                )
            }
            
            // ══════════════════════════════════════════════════════════════════
            // STANDINGS TOGGLE
            // ══════════════════════════════════════════════════════════════════
            item {
                Spacer(modifier = Modifier.height(32.dp))
                MinimalToggle(
                    isDrivers = showDrivers,
                    onToggle = { showDrivers = it },
                    michromaFont = michromaFont
                )
            }
            
            // ══════════════════════════════════════════════════════════════════
            // STANDINGS TABLE
            // ══════════════════════════════════════════════════════════════════
            item {
                Spacer(modifier = Modifier.height(16.dp))
                AnimatedContent(
                    targetState = showDrivers,
                    transitionSpec = {
                        (fadeIn(tween(300)) + slideInVertically { 20 })
                            .togetherWith(fadeOut(tween(200)))
                    },
                    label = "standings"
                ) { isDrivers ->
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (isDrivers) {
                            driverStandings?.forEachIndexed { index, driver ->
                                MinimalDriverRow(
                                    position = index + 1,
                                    driver = driver,
                                    michromaFont = michromaFont
                                )
                            }
                        } else {
                            constructorStandings?.forEachIndexed { index, team ->
                                MinimalConstructorRow(
                                    position = index + 1,
                                    constructor = team,
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

// ═══════════════════════════════════════════════════════════════════════════════
// PREMIUM HERO SECTION - Full-bleed driver showcase
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun PremiumHeroSection(
    season: String,
    champion: DriverStanding?,
    brigendsFont: FontFamily,
    michromaFont: FontFamily,
    teamColor: Color,
    isVisible: Boolean
) {
    val driverInfo = champion?.driver?.let { F1DataProvider.getDriverByApiId(it.driverId) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(520.dp)
    ) {
        // Driver number image in background
        if (driverInfo?.headshotNumberUrl != null) {
            SubcomposeAsyncImage(
                model = driverInfo.headshotNumberUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .align(Alignment.TopCenter)
                    .graphicsLayer { 
                        alpha = 0.15f
                        translationY = 50f
                    },
                contentScale = ContentScale.Fit
            )
        }
        
        // Full-bleed driver image
        if (driverInfo?.headshotF1 != null) {
            SubcomposeAsyncImage(
                model = driverInfo.headshotF1,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = if (isVisible) 1f else 0f },
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter
            )
        }
        
        // Bottom gradient overlay for text
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.5f to Color.Black.copy(alpha = 0.3f),
                        0.75f to Color.Black.copy(alpha = 0.8f),
                        1f to Color.Black
                    )
                )
        )
        
        // Content overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Spacer for layout balance
            Spacer(modifier = Modifier.height(60.dp))
            
            // Bottom - Champion details
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(500, delayMillis = 300)) + slideInVertically { 30 }
            ) {
                Column {
                    // Champion badge
                    Text(
                        text = "WORLD CHAMPION",
                        fontFamily = michromaFont,
                        fontSize = 10.sp,
                        color = teamColor,
                        letterSpacing = 4.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Driver name - massive typography
                    if (champion != null) {
                        Text(
                            text = champion.driver.givenName.uppercase(),
                            fontFamily = brigendsFont,
                            fontSize = 24.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = champion.driver.familyName.uppercase(),
                            fontFamily = brigendsFont,
                            fontSize = 56.sp,
                            color = Color.White,
                            letterSpacing = 1.sp,
                            lineHeight = 56.sp
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Team name
                        val teamInfo = champion.constructors.firstOrNull()?.let {
                            F1DataProvider.getTeamByApiId(it.constructorId)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(14.dp)
                                    .background(teamColor, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = teamInfo?.name?.uppercase() ?: "",
                                fontFamily = michromaFont,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// GLASS STATS PILL - Horizontal championship stats
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun GlassStatsPill(
    champion: DriverStanding?,
    michromaFont: FontFamily
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                .padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem("POINTS", champion?.points ?: "0", michromaFont)
            
            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(Color.White.copy(alpha = 0.1f))
            )
            
            StatItem("WINS", champion?.wins ?: "0", michromaFont)
            
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(Color.White.copy(alpha = 0.1f))
            )
            
            StatItem("POSITION", "#1", michromaFont)
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, font: FontFamily) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontFamily = font,
            fontSize = 22.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontFamily = font,
            fontSize = 9.sp,
            color = Color.White.copy(alpha = 0.4f),
            letterSpacing = 1.sp
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// MINIMAL COUNTDOWN - Clean next season countdown
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun MinimalCountdown(
    nextSeasonYear: Int,
    brigendsFont: FontFamily,
    michromaFont: FontFamily
) {
    val targetDate = remember {
        LocalDateTime.of(nextSeasonYear, 3, 14, 0, 0) // Approx season start
    }
    
    var daysRemaining by remember { mutableStateOf(0L) }
    
    LaunchedEffect(Unit) {
        while (true) {
            daysRemaining = ChronoUnit.DAYS.between(LocalDateTime.now(), targetDate)
            kotlinx.coroutines.delay(60000)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$nextSeasonYear SEASON",
                fontFamily = michromaFont,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.4f),
                letterSpacing = 3.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$daysRemaining",
                    fontFamily = michromaFont, // Michroma for numbers
                    fontSize = 56.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "DAYS",
                    fontFamily = michromaFont,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 14.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "UNTIL LIGHTS OUT",
                fontFamily = michromaFont,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.3f),
                letterSpacing = 4.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// MINIMAL TOGGLE - Clean drivers/constructors switch
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun MinimalToggle(
    isDrivers: Boolean,
    onToggle: (Boolean) -> Unit,
    michromaFont: FontFamily
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        ToggleOption(
            text = "DRIVERS",
            isSelected = isDrivers,
            onClick = { onToggle(true) },
            font = michromaFont
        )
        
        Spacer(modifier = Modifier.width(32.dp))
        
        ToggleOption(
            text = "CONSTRUCTORS",
            isSelected = !isDrivers,
            onClick = { onToggle(false) },
            font = michromaFont
        )
    }
}

@Composable
private fun ToggleOption(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    font: FontFamily
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            fontFamily = font,
            fontSize = 11.sp,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.3f),
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(if (isSelected) 40.dp else 0.dp)
                .height(2.dp)
                .background(Color.White, RoundedCornerShape(1.dp))
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// MINIMAL STANDINGS ROWS
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun MinimalDriverRow(
    position: Int,
    driver: DriverStanding,
    michromaFont: FontFamily
) {
    val teamColor = driver.constructors.firstOrNull()?.let {
        F1DataProvider.getTeamByApiId(it.constructorId)?.color
    }?.let { Color(android.graphics.Color.parseColor("#$it")) } ?: Color.Gray
    
    val isChampion = position == 1
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isChampion) Color.White.copy(alpha = 0.06f)
                else Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Position
        Text(
            text = String.format("%02d", position),
            fontFamily = michromaFont,
            fontSize = 13.sp,
            color = Color.White.copy(alpha = if (isChampion) 0.9f else 0.4f),
            modifier = Modifier.width(32.dp)
        )
        
        // Team color bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(28.dp)
                .background(teamColor, RoundedCornerShape(2.dp))
        )
        
        Spacer(modifier = Modifier.width(14.dp))
        
        // Driver name
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = driver.driver.givenName,
                fontFamily = michromaFont,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
            Text(
                text = driver.driver.familyName.uppercase(),
                fontFamily = michromaFont,
                fontSize = 13.sp,
                color = Color.White,
                letterSpacing = 0.5.sp
            )
        }
        
        // Points
        Text(
            text = "${driver.points} PTS",
            fontFamily = michromaFont,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f),
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun MinimalConstructorRow(
    position: Int,
    constructor: ConstructorStanding,
    michromaFont: FontFamily
) {
    val teamInfo = F1DataProvider.getTeamByApiId(constructor.constructor.constructorId)
    val teamColor = teamInfo?.color?.let { 
        Color(android.graphics.Color.parseColor("#$it")) 
    } ?: Color.Gray
    
    val isChampion = position == 1
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isChampion) Color.White.copy(alpha = 0.06f)
                else Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Position
        Text(
            text = String.format("%02d", position),
            fontFamily = michromaFont,
            fontSize = 13.sp,
            color = Color.White.copy(alpha = if (isChampion) 0.9f else 0.4f),
            modifier = Modifier.width(32.dp)
        )
        
        // Team color bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(28.dp)
                .background(teamColor, RoundedCornerShape(2.dp))
        )
        
        Spacer(modifier = Modifier.width(14.dp))
        
        // Team name
        Text(
            text = teamInfo?.name?.uppercase() ?: constructor.constructor.name.uppercase(),
            fontFamily = michromaFont,
            fontSize = 13.sp,
            color = Color.White,
            letterSpacing = 0.5.sp,
            modifier = Modifier.weight(1f)
        )
        
        // Points
        Text(
            text = "${constructor.points} PTS",
            fontFamily = michromaFont,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f),
            letterSpacing = 0.5.sp
        )
    }
}
