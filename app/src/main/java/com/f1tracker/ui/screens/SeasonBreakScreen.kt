package com.f1tracker.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.f1tracker.R
import com.f1tracker.data.local.F1DataProvider
import com.f1tracker.data.models.DriverStanding
import com.f1tracker.ui.viewmodels.RaceViewModel
import com.f1tracker.ui.viewmodels.StandingsViewModel
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SeasonBreakScreen(
    raceViewModel: RaceViewModel,
    standingsViewModel: StandingsViewModel
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    
    val driverStandings by standingsViewModel.driverStandings.collectAsState()
    val constructorStandings by standingsViewModel.constructorStandings.collectAsState()
    val allRaces by raceViewModel.allRaces.collectAsState()
    
    val driverChampion = driverStandings?.firstOrNull()
    val constructorChampion = constructorStandings?.firstOrNull()
    
    val brigendsFont = FontFamily(Font(R.font.brigends_expanded, FontWeight.Normal))
    val michromaFont = FontFamily(Font(R.font.michroma, FontWeight.Normal))
    
    var isDriverView by remember { mutableStateOf(true) }
    var contentVisible by remember { mutableStateOf(false) }
    
    val displayedSeason = allRaces.firstOrNull()?.season ?: java.time.Year.now().value.toString()
    val nextSeasonYear = (displayedSeason.toIntOrNull() ?: java.time.Year.now().value) + 1

    val activeTeamColor = if (isDriverView) {
        driverChampion?.constructors?.firstOrNull()?.let { 
            F1DataProvider.getTeamByApiId(it.constructorId)?.color 
        }?.let { Color(android.graphics.Color.parseColor("#$it")) } ?: Color(0xFFE10600)
    } else {
        constructorChampion?.let { 
            F1DataProvider.getTeamByApiId(it.constructor.constructorId)?.color 
        }?.let { Color(android.graphics.Color.parseColor("#$it")) } ?: Color(0xFFE10600)
    }
    
    val animatedTeamColor by animateColorAsState(
        targetValue = activeTeamColor,
        animationSpec = tween(800),
        label = "teamColor"
    )

    LaunchedEffect(Unit) {
        standingsViewModel.loadStandings()
        kotlinx.coroutines.delay(100)
        contentVisible = true
    }
    
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(10000)
            isDriverView = !isDriverView
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Ambient glow
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedTeamColor.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.2f),
                    radius = size.width * 1f
                )
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // HERO - Responsive height
            item {
                AnimatedContent(
                    targetState = isDriverView,
                    transitionSpec = {
                        // Smooth crossfade with subtle scale
                        (fadeIn(tween(500)) + scaleIn(
                            initialScale = 0.96f,
                            animationSpec = tween(500, easing = FastOutSlowInEasing)
                        )).togetherWith(
                            fadeOut(tween(400)) + scaleOut(
                                targetScale = 1.04f,
                                animationSpec = tween(400, easing = FastOutSlowInEasing)
                            )
                        )
                    },
                    label = "hero"
                ) { showDriver ->
                    if (showDriver) {
                        ResponsiveDriverHero(
                            season = displayedSeason,
                            champion = driverChampion,
                            teamColor = animatedTeamColor,
                            brigendsFont = brigendsFont,
                            michromaFont = michromaFont,
                            isVisible = contentVisible,
                            screenHeight = screenHeight
                        )
                    } else {
                        ResponsiveConstructorHero(
                            season = displayedSeason,
                            champion = constructorChampion,
                            teamColor = animatedTeamColor,
                            brigendsFont = brigendsFont,
                            michromaFont = michromaFont,
                            isVisible = contentVisible,
                            screenHeight = screenHeight
                        )
                    }
                }
            }

            // COUNTDOWN - Compact
            item {
                CompactCountdown(
                    nextSeasonYear = nextSeasonYear,
                    teamColor = animatedTeamColor,
                    brigendsFont = brigendsFont,
                    michromaFont = michromaFont
                )
            }
            
            // TOGGLE
            item {
                Spacer(modifier = Modifier.height(20.dp))
                CompactToggle(
                    isDriverSelected = isDriverView,
                    onSelectionChange = { isDriverView = it },
                    teamColor = animatedTeamColor,
                    font = michromaFont
                )
            }
            
            // HEADER
            item {
                Text(
                    text = if (isDriverView) "STANDINGS" else "CONSTRUCTOR STANDINGS",
                    fontFamily = brigendsFont,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.3f),
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp)
                )
            }

            // LIST
            item {
                AnimatedContent(
                    targetState = isDriverView,
                    transitionSpec = {
                        (fadeIn(tween(300)) + slideInVertically { 20 }).togetherWith(fadeOut(tween(150)))
                    },
                    label = "list"
                ) { showDrivers ->
                    Column {
                        if (showDrivers) {
                            driverStandings?.forEachIndexed { index, driver ->
                                CompactDriverRow(
                                    position = index + 1,
                                    driver = driver,
                                    font = michromaFont
                                )
                            }
                        } else {
                            constructorStandings?.forEachIndexed { index, team ->
                                CompactConstructorRow(
                                    position = index + 1,
                                    constructor = team,
                                    font = michromaFont
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResponsiveDriverHero(
    season: String,
    champion: DriverStanding?,
    teamColor: Color,
    brigendsFont: FontFamily,
    michromaFont: FontFamily,
    isVisible: Boolean,
    screenHeight: androidx.compose.ui.unit.Dp
) {
    val driverInfo = champion?.driver?.let { F1DataProvider.getDriverByApiId(it.driverId) }
    val teamInfo = champion?.constructors?.firstOrNull()?.let { 
        F1DataProvider.getTeamByApiId(it.constructorId) 
    }
    
    // Responsive hero height - 55% of screen
    val heroHeight = (screenHeight.value * 0.55f).dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(heroHeight)
            .clip(RectangleShape)
    ) {
        // Car background - subtle
        if (teamInfo?.carImageUrl != null) {
            AsyncImage(
                model = teamInfo.carImageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(1.8f)
                    .offset(x = 80.dp, y = (-20).dp)
                    .alpha(0.08f)
                    .blur(1.dp),
                contentScale = ContentScale.Fit
            )
        }
        
        // Team logo
        val logoRes = champion?.constructors?.firstOrNull()?.let { getTeamBigLogo(it.constructorId) }
        if (logoRes != null) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = logoRes),
                contentDescription = null,
                modifier = Modifier
                    .size(200.dp)
                    .align(Alignment.Center)
                    .offset(y = (-20).dp)
                    .alpha(0.06f),
                contentScale = ContentScale.Fit
            )
        }

        // Driver - cropped at bottom, moved down
        if (driverInfo?.headshotF1 != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RectangleShape), // Clips overflow
                contentAlignment = Alignment.BottomCenter
            ) {
                SubcomposeAsyncImage(
                    model = driverInfo.headshotF1,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxHeight(1.1f) // Slightly larger to allow cropping
                        .scale(1.3f)
                        .offset(y = 80.dp) // Moved DOWN more
                        .graphicsLayer { alpha = if (isVisible) 1f else 0f },
                    contentScale = ContentScale.Fit
                )
            }
        }
        
        // Gradients
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.6f),
                        0.2f to Color.Transparent,
                        0.75f to Color.Transparent,
                        1f to Color.Black
                    )
                )
        )
        
        // Content - positioned at edges
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Top left: Season
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(600)) + slideInVertically { -15 },
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Column {
                    Text(
                        text = season,
                        fontFamily = michromaFont,
                        fontSize = 32.sp,
                        color = Color.White.copy(alpha = 0.12f),
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(2.dp)
                            .background(teamColor)
                    )
                }
            }
            
            // Bottom left: Champion info
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(600, delayMillis = 200)) + slideInVertically { 30 },
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                Column {
                    Text(
                        text = "WORLD CHAMPION",
                        fontFamily = michromaFont,
                        fontSize = 9.sp,
                        color = teamColor,
                        letterSpacing = 3.sp
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    if (champion != null) {
                        Text(
                            text = champion.driver.givenName.uppercase(),
                            fontFamily = brigendsFont,
                            fontSize = 20.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Text(
                            text = champion.driver.familyName.uppercase(),
                            fontFamily = brigendsFont,
                            fontSize = 36.sp,
                            color = Color.White,
                            lineHeight = 36.sp
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .background(teamColor, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${champion.points} PTS",
                                    fontFamily = michromaFont,
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = teamInfo?.name?.uppercase() ?: "",
                                fontFamily = michromaFont,
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResponsiveConstructorHero(
    season: String,
    champion: com.f1tracker.data.models.ConstructorStanding?,
    teamColor: Color,
    brigendsFont: FontFamily,
    michromaFont: FontFamily,
    isVisible: Boolean,
    screenHeight: androidx.compose.ui.unit.Dp
) {
    val teamInfo = champion?.let { F1DataProvider.getTeamByApiId(it.constructor.constructorId) }
    val heroHeight = (screenHeight.value * 0.55f).dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(heroHeight)
            .clip(RectangleShape)
    ) {
        // Team logo - large, atmospheric
        val logoRes = champion?.let { getTeamBigLogo(it.constructor.constructorId) }
        if (logoRes != null) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = logoRes),
                contentDescription = null,
                modifier = Modifier
                    .size(350.dp)
                    .align(Alignment.Center)
                    .offset(y = (-30).dp)
                    .alpha(0.05f)
                    .blur(2.dp),
                contentScale = ContentScale.Fit
            )
        }

        // Car - center stage
        if (teamInfo?.carImageUrl != null) {
            AsyncImage(
                model = teamInfo.carImageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .align(Alignment.Center)
                    .offset(y = 10.dp)
                    .graphicsLayer { alpha = if (isVisible) 1f else 0f },
                contentScale = ContentScale.FillWidth
            )
        }
        
        // Gradients
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.6f),
                        0.25f to Color.Transparent,
                        0.7f to Color.Transparent,
                        1f to Color.Black
                    )
                )
        )
        
        // Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(600)) + slideInVertically { -15 },
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Column {
                    Text(
                        text = season,
                        fontFamily = michromaFont,
                        fontSize = 32.sp,
                        color = Color.White.copy(alpha = 0.12f),
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(2.dp)
                            .background(teamColor)
                    )
                }
            }
            
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(600, delayMillis = 200)) + slideInVertically { 30 },
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                Column {
                    Text(
                        text = "CONSTRUCTORS' CHAMPION",
                        fontFamily = michromaFont,
                        fontSize = 9.sp,
                        color = teamColor,
                        letterSpacing = 3.sp
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    if (champion != null) {
                        Text(
                            text = champion.constructor.name.uppercase(),
                            fontFamily = brigendsFont,
                            fontSize = 34.sp,
                            color = Color.White
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Box(
                            modifier = Modifier
                                .background(teamColor, RoundedCornerShape(4.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${champion.points} PTS",
                                fontFamily = michromaFont,
                                fontSize = 10.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompactCountdown(
    nextSeasonYear: Int,
    teamColor: Color,
    brigendsFont: FontFamily,
    michromaFont: FontFamily
) {
    val targetDate = LocalDateTime.of(nextSeasonYear, 3, 15, 12, 0)
    var timeLeft by remember { mutableStateOf(calculateTimeLeft(targetDate)) }
    
    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis { 
                timeLeft = calculateTimeLeft(targetDate)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.08f))
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Race info - centered
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "NEXT RACE",
                fontFamily = michromaFont,
                fontSize = 9.sp,
                color = Color.White.copy(alpha = 0.3f),
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "AUSTRALIAN GRAND PRIX",
                fontFamily = brigendsFont,
                fontSize = 20.sp,
                color = Color.White
            )
            Text(
                text = "MARCH 14-16 • MELBOURNE",
                fontFamily = michromaFont,
                fontSize = 9.sp,
                color = teamColor,
                letterSpacing = 1.sp
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Countdown - its own row, centered
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallTimeBlock(timeLeft.days, "DAYS", michromaFont)
                SmallTimeBlock(timeLeft.hours, "HRS", michromaFont)
                SmallTimeBlock(timeLeft.minutes, "MIN", michromaFont)
                SmallTimeBlock(timeLeft.seconds, "SEC", michromaFont, teamColor)
            }
        }
    }
}

@Composable
fun SmallTimeBlock(value: Long, label: String, font: FontFamily, accentColor: Color? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(52.dp)
            .background(
                if (accentColor != null) accentColor.copy(alpha = 0.12f) 
                else Color.White.copy(alpha = 0.05f),
                RoundedCornerShape(8.dp)
            )
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = String.format("%02d", value),
            fontFamily = font,
            fontSize = 20.sp,
            color = accentColor ?: Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            fontFamily = font,
            fontSize = 8.sp,
            color = Color.White.copy(alpha = 0.4f)
        )
    }
}

@Composable
fun CompactToggle(
    isDriverSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    teamColor: Color,
    font: FontFamily
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf(true to "DRIVERS", false to "TEAMS").forEach { (isDriver, label) ->
            val isSelected = isDriverSelected == isDriver
            val bg by animateColorAsState(
                if (isSelected) teamColor else Color.Transparent,
                tween(200), label = "bg"
            )
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(bg)
                    .then(
                        if (!isSelected) Modifier.border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(6.dp))
                        else Modifier
                    )
                    .clickable { onSelectionChange(isDriver) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontFamily = font,
                    fontSize = 10.sp,
                    color = if (isSelected) Color.White else Color.White.copy(0.4f),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun CompactDriverRow(
    position: Int,
    driver: DriverStanding,
    font: FontFamily
) {
    val driverInfo = F1DataProvider.getDriverByApiId(driver.driver.driverId)
    val teamInfo = driver.constructors.firstOrNull()?.let { 
        F1DataProvider.getTeamByApiId(it.constructorId) 
    }
    val teamColor = teamInfo?.color?.let { Color(android.graphics.Color.parseColor("#$it")) } ?: Color.Gray

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Position + team color bar
        Row(
            modifier = Modifier.width(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(20.dp)
                    .background(teamColor, RoundedCornerShape(1.dp))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$position",
                fontFamily = font,
                fontSize = 12.sp,
                color = when (position) {
                    1 -> Color(0xFFFFD700)
                    2 -> Color(0xFFC0C0C0)
                    3 -> Color(0xFFCD7F32)
                    else -> Color.White.copy(alpha = 0.5f)
                },
                fontWeight = FontWeight.Bold
            )
        }
        
        // Photo
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        ) {
            if (driverInfo?.headshotF1 != null) {
                SubcomposeAsyncImage(
                    model = driverInfo.headshotF1,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(1.3f)
                        .offset(y = 3.dp),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter
                )
            }
        }
        
        Spacer(modifier = Modifier.width(10.dp))
        
        // Name
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = driver.driver.familyName.uppercase(),
                fontFamily = font,
                fontSize = 12.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = teamInfo?.name ?: "",
                fontFamily = font,
                fontSize = 9.sp,
                color = Color.White.copy(alpha = 0.3f),
                maxLines = 1
            )
        }
        
        // Points
        Text(
            text = "${driver.points}",
            fontFamily = font,
            fontSize = 14.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CompactConstructorRow(
    position: Int,
    constructor: com.f1tracker.data.models.ConstructorStanding,
    font: FontFamily
) {
    val teamInfo = F1DataProvider.getTeamByApiId(constructor.constructor.constructorId)
    val teamColor = teamInfo?.color?.let { Color(android.graphics.Color.parseColor("#$it")) } ?: Color.Gray

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.width(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(20.dp)
                    .background(teamColor, RoundedCornerShape(1.dp))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$position",
                fontFamily = font,
                fontSize = 12.sp,
                color = when (position) {
                    1 -> Color(0xFFFFD700)
                    2 -> Color(0xFFC0C0C0)
                    3 -> Color(0xFFCD7F32)
                    else -> Color.White.copy(alpha = 0.5f)
                },
                fontWeight = FontWeight.Bold
            )
        }
        
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(teamColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            if (teamInfo?.symbolUrl != null) {
                SubcomposeAsyncImage(
                    model = teamInfo.symbolUrl,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    contentScale = ContentScale.Fit,
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(10.dp))
        
        Text(
            text = constructor.constructor.name.uppercase(),
            fontFamily = font,
            fontSize = 12.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        Text(
            text = "${constructor.points}",
            fontFamily = font,
            fontSize = 14.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun getTeamBigLogo(constructorId: String): Int? {
    return when (constructorId) {
        "mclaren" -> R.drawable.mclaren
        "mercedes" -> R.drawable.mercedes
        "red_bull" -> R.drawable.red_bull
        "ferrari" -> R.drawable.ferrari
        "williams" -> R.drawable.williams
        "rb" -> R.drawable.racing_bulls
        "aston_martin" -> R.drawable.aston_martin
        "haas" -> R.drawable.haas
        "sauber" -> R.drawable.kick_sauber
        "alpine" -> R.drawable.alpine
        else -> null
    }
}

data class TimeLeft(val days: Long, val hours: Long, val minutes: Long, val seconds: Long)

fun calculateTimeLeft(target: LocalDateTime): TimeLeft {
    val now = LocalDateTime.now()
    if (now.isAfter(target)) return TimeLeft(0, 0, 0, 0)
    
    val totalSeconds = ChronoUnit.SECONDS.between(now, target)
    val days = totalSeconds / (24 * 3600)
    val hours = (totalSeconds % (24 * 3600)) / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return TimeLeft(days, hours, minutes, seconds)
}
