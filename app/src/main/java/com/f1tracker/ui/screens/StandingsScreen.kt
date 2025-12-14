package com.f1tracker.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.f1tracker.R
import com.f1tracker.data.api.F1ApiService
import com.f1tracker.data.local.F1DataProvider
import com.f1tracker.data.models.ConstructorStanding
import com.f1tracker.data.models.DriverStanding
import com.f1tracker.ui.viewmodels.StandingsViewModel

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun StandingsScreen(
    viewModel: StandingsViewModel = hiltViewModel(),
    selectedTab: Int = 0,
    onTabChange: (Int) -> Unit = {}
) {
    val brigendsFont = FontFamily(Font(R.font.brigends_expanded, FontWeight.Normal))
    val michromaFont = FontFamily(Font(R.font.michroma, FontWeight.Normal))
    
    // selectedTab is now passed as a parameter
    
    val selectedYear by viewModel.selectedYear.collectAsState()
    val driverStandings by viewModel.driverStandings.collectAsState()
    val constructorStandings by viewModel.constructorStandings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    var showYearDropdown by remember { mutableStateOf(false) }
    val availableYears = com.f1tracker.data.SeasonConfig.AVAILABLE_YEARS
    
    // Reset to current year when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetToCurrentYear()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Compact header with year selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "STANDINGS",
                fontFamily = brigendsFont,
                fontSize = 16.sp,
                color = Color.White,
                letterSpacing = 2.sp
            )
            
            // Compact year selector
            Box {
                Row(
                    modifier = Modifier
                        .clickable { showYearDropdown = !showYearDropdown }
                        .background(Color(0xFF0F0F0F), RoundedCornerShape(8.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = selectedYear,
                        fontFamily = michromaFont,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                DropdownMenu(
                    expanded = showYearDropdown,
                    onDismissRequest = { showYearDropdown = false },
                    modifier = Modifier
                        .background(Color(0xFF0F0F0F))
                        .heightIn(max = 250.dp) // Limit height for scrollability
                ) {
                    // Show only recent seasons (last 5 years) for cleaner UI
                    availableYears.take(5).forEach { year ->
                        val isSelected = year.toString() == selectedYear
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Selection indicator
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .background(
                                                if (isSelected) Color(0xFFFF0080) else Color.Transparent,
                                                CircleShape
                                            )
                                    )
                                    Text(
                                        text = year.toString(),
                                        fontFamily = michromaFont,
                                        fontSize = 12.sp,
                                        color = if (isSelected) Color(0xFFFF0080) else Color.White,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            },
                            onClick = {
                                viewModel.selectYear(year.toString())
                                showYearDropdown = false
                            }
                        )
                    }
                }
            }
        }
        
        // Tab Selector
        com.f1tracker.ui.components.TabSelector(
            tabs = listOf("DRIVERS", "CONSTRUCTORS"),
            selectedTab = selectedTab,
            onTabSelected = onTabChange,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        
        // Horizontal Pager for swipeable tabs
        val pagerState = androidx.compose.foundation.pager.rememberPagerState(initialPage = selectedTab) { 2 }
        
        // Sync external selectedTab with Pager
        LaunchedEffect(selectedTab) {
            if (pagerState.currentPage != selectedTab) {
                pagerState.animateScrollToPage(selectedTab)
            }
        }
        
        // Sync Pager change to external onTabChange
        LaunchedEffect(pagerState.currentPage) {
            if (pagerState.currentPage != selectedTab) {
                onTabChange(pagerState.currentPage)
            }
        }

        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFFF0080))
                    }
                }
                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = error ?: "Unknown error",
                            color = Color.Red,
                            fontFamily = michromaFont,
                            fontSize = 12.sp
                        )
                    }
                }
                else -> {
                    if (page == 0) {
                        // Drivers Tab
                        if (driverStandings.isNullOrEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No driver standings available", color = Color.White, fontFamily = michromaFont, fontSize = 12.sp)
                            }
                        } else {
                            DriverStandingsList(
                                standings = driverStandings!!,
                                michromaFont = michromaFont,
                                brigendsFont = brigendsFont
                            )
                        }
                    } else {
                        // Constructors Tab
                        if (constructorStandings.isNullOrEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No constructor standings available", color = Color.White, fontFamily = michromaFont, fontSize = 12.sp)
                            }
                        } else {
                            ConstructorStandingsList(
                                standings = constructorStandings!!,
                                michromaFont = michromaFont,
                                brigendsFont = brigendsFont
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
private fun StandingsPodium(
    standings: List<DriverStanding>,
    michromaFont: FontFamily,
    brigendsFont: FontFamily
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(270.dp)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        standings.take(3).forEachIndexed { index, standing ->
            PodiumDriverCard(
                standing = standing,
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
    standing: DriverStanding,
    position: Int,
    michromaFont: FontFamily,
    brigendsFont: FontFamily,
    modifier: Modifier
) {
    val driverInfo = F1DataProvider.getDriverByApiId(standing.driver.driverId)
    val teamInfo = standing.constructors.firstOrNull()?.let { F1DataProvider.getTeamByApiId(it.constructorId) }
    val teamColor = teamInfo?.color?.let { Color(android.graphics.Color.parseColor("#$it")) } ?: Color(0xFF333333)
    
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
                SubcomposeAsyncImage(
                    model = driverInfo.headshotF1,
                    contentDescription = standing.driver.familyName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(2.2f)
                        .offset(y = 80.dp), // Increased offset to shift headshot down further
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
            val fullName = "${standing.driver.givenName} ${standing.driver.familyName}"
            val nameParts = fullName.split(" ")
            val displayName = if (nameParts.size > 2) {
                // Has middle name - use last two words (middle + last)
                nameParts.drop(1).joinToString(" ")
            } else {
                fullName
            }
            
            Text(
                text = displayName.uppercase(),
                fontFamily = brigendsFont,
                fontSize = 10.sp, // Reduced font size
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = 12.sp
            )
            
            // Team
            Text(
                text = teamInfo?.abbreviation ?: "",
                fontFamily = michromaFont,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Points
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = standing.points,
                    fontFamily = michromaFont,
                    fontSize = 14.sp,
                    color = Color(0xFFFF0080),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "PTS",
                    fontFamily = michromaFont,
                    fontSize = 8.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun DriverStandingsList(
    standings: List<DriverStanding>,
    michromaFont: FontFamily,
    brigendsFont: FontFamily
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp), // Bottom padding for nav bar
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top 3 Podium
        item {
            if (standings.size >= 3) {
                StandingsPodium(
                    standings = standings.take(3),
                    michromaFont = michromaFont,
                    brigendsFont = brigendsFont
                )
            }
        }
        
        // Rest of the list in a single Card
        item {
            if (standings.size > 3) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        standings.drop(3).forEachIndexed { index, driver ->
                            DriverStandingRow(
                                standing = driver,
                                position = index + 4,
                                michromaFont = michromaFont,
                                brigendsFont = brigendsFont
                            )
                            
                            if (index < standings.drop(3).lastIndex) {
                                Divider(
                                    color = Color.White.copy(alpha = 0.05f),
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(horizontal = 20.dp)
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
private fun DriverStandingRow(
    standing: DriverStanding,
    position: Int,
    michromaFont: FontFamily,
    brigendsFont: FontFamily,
    modifier: Modifier = Modifier
) {
    val driverInfo = F1DataProvider.getDriverByApiId(standing.driver.driverId)
    val teamInfo = standing.constructors.firstOrNull()?.let { F1DataProvider.getTeamByApiId(it.constructorId) }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp), // Reduced vertical padding
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Position
        Text(
            text = position.toString(),
            fontFamily = michromaFont,
            fontSize = 12.sp, // Reduced from 14.sp
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.width(28.dp), // Reduced width
            maxLines = 1,
            textAlign = TextAlign.End
        )
        
        Spacer(modifier = Modifier.width(12.dp)) // Reduced spacing
        
        // Driver Headshot (Small circle)
        if (driverInfo?.headshotF1 != null) {
            SubcomposeAsyncImage(
                model = driverInfo.headshotF1,
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp) // Reduced from 40.dp
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f)),
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter
            )
        } else {
            // Fallback circle
            Box(
                modifier = Modifier
                    .size(36.dp) // Reduced from 40.dp
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp)) // Reduced spacing
        
        // Name & Team
        Column(modifier = Modifier.weight(1f)) {
            // Smart name shortening: if has middle name, use middle + last only
            val fullName = "${standing.driver.givenName} ${standing.driver.familyName}"
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
                fontSize = 12.sp, // Reduced from 14.sp
                color = Color.White,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = teamInfo?.name ?: standing.constructors.firstOrNull()?.name ?: "",
                fontFamily = michromaFont,
                fontSize = 9.sp, // Reduced from 10.sp
                color = Color.White.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Points (Just the number)
        Text(
            text = standing.points,
            fontFamily = michromaFont,
            fontSize = 14.sp, // Reduced from 16.sp
            color = Color(0xFFFF0080),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ConstructorStandingsList(
    standings: List<ConstructorStanding>,
    michromaFont: FontFamily,
    brigendsFont: FontFamily
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp), // Bottom padding for nav bar
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top 3 Podium
        item {
            if (standings.size >= 3) {
                ConstructorPodium(
                    standings = standings.take(3),
                    michromaFont = michromaFont,
                    brigendsFont = brigendsFont
                )
            }
        }
        
        // Rest of the list in a single Card
        item {
            if (standings.size > 3) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        standings.drop(3).forEachIndexed { index, constructor ->
                            ConstructorStandingRow(
                                standing = constructor,
                                position = index + 4,
                                michromaFont = michromaFont,
                                brigendsFont = brigendsFont
                            )
                            
                            if (index < standings.drop(3).lastIndex) {
                                Divider(
                                    color = Color.White.copy(alpha = 0.05f),
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(horizontal = 20.dp)
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
private fun ConstructorStandingRow(
    standing: ConstructorStanding,
    position: Int,
    michromaFont: FontFamily,
    brigendsFont: FontFamily
) {
    val teamInfo = F1DataProvider.getTeamByApiId(standing.constructor.constructorId)
    val teamColor = teamInfo?.color?.let { Color(android.graphics.Color.parseColor("#$it")) } ?: Color(0xFF333333)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Position
        Text(
            text = position.toString(),
            fontFamily = michromaFont,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.End
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Team Symbol or Color
        if (teamInfo?.symbolUrl != null) {
            SubcomposeAsyncImage(
                model = teamInfo.symbolUrl,
                contentDescription = standing.constructor.name,
                modifier = Modifier.size(36.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(teamColor)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Name
        Text(
            text = formatConstructorName(standing.constructor.name).uppercase(),
            fontFamily = michromaFont, // Switched to second font
            fontSize = 12.sp,
            color = Color.White,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Points
        Text(
            text = standing.points,
            fontFamily = michromaFont,
            fontSize = 14.sp,
            color = Color(0xFFFF0080),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ConstructorPodium(
    standings: List<ConstructorStanding>,
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
        standings.take(3).forEachIndexed { index, standing ->
            PodiumConstructorCard(
                standing = standing,
                position = index + 1,
                michromaFont = michromaFont,
                brigendsFont = brigendsFont,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PodiumConstructorCard(
    standing: ConstructorStanding,
    position: Int,
    michromaFont: FontFamily,
    brigendsFont: FontFamily,
    modifier: Modifier
) {
    val teamInfo = F1DataProvider.getTeamByApiId(standing.constructor.constructorId)
    val teamColor = teamInfo?.color?.let { Color(android.graphics.Color.parseColor("#$it")) } ?: Color(0xFF333333)
    
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D0D0D),           // Near black at top
                        Color(0xFF111111),           // Slightly lighter
                        teamColor.copy(alpha = 0.4f), // Team color blends in
                        teamColor.copy(alpha = 0.6f)  // Stronger team color at bottom
                    ),
                    startY = 0f
                )
            )
    ) {
        // Car Image
        if (teamInfo?.carImageUrl != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RectangleShape),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = teamInfo.carImageUrl,
                    contentDescription = standing.constructor.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(1.5f)
                        .offset(y = 20.dp),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.Center
                )
            }
        }
        
        // Overlay Gradient for bottom text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.85f)
                        ),
                        startY = 100f
                    )
                )
        )
        
        // Top Logo
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 14.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            val logoUrl = teamInfo?.fullLogoUrl ?: teamInfo?.symbolUrl
            if (logoUrl != null) {
                SubcomposeAsyncImage(
                    model = logoUrl,
                    contentDescription = standing.constructor.name,
                    modifier = Modifier.size(72.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
        
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
            

            
            // Team Name
            Text(
                text = formatConstructorName(standing.constructor.name).uppercase(),
                fontFamily = brigendsFont,
                fontSize = 10.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = 12.sp,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Points
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = standing.points,
                    fontFamily = michromaFont,
                    fontSize = 14.sp,
                    color = Color(0xFFFF0080),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "PTS",
                    fontFamily = michromaFont,
                    fontSize = 8.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}



private fun formatConstructorName(name: String): String {
    return name.replace("F1 Team", "", ignoreCase = true)
        .replace("Racing", "", ignoreCase = true)
        .replace("Team", "", ignoreCase = true)
        .replace("Scuderia", "", ignoreCase = true)
        .replace("Formula 1", "", ignoreCase = true)
        .replace("Petronas", "", ignoreCase = true)
        .replace("Oracle", "", ignoreCase = true)
        .replace("MoneyGram", "", ignoreCase = true)
        .replace("Stake", "", ignoreCase = true)
        .replace("Kick", "", ignoreCase = true)
        .replace("Aramco", "", ignoreCase = true)
        .replace("Cognizant", "", ignoreCase = true)
        .replace("BWT", "", ignoreCase = true)
        .trim()
}


