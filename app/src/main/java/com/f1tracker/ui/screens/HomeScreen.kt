package com.f1tracker.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDateTime
import com.f1tracker.R
import com.f1tracker.data.models.ConstructorStanding
import com.f1tracker.data.models.DriverStanding
import com.f1tracker.data.models.Race
import com.f1tracker.ui.components.ConstructorStandingsCard
import com.f1tracker.ui.components.DriverStandingsCard
import com.f1tracker.ui.components.HeroSectionFixed
import com.f1tracker.ui.components.LastRaceCard
import com.f1tracker.ui.viewmodels.HomeViewModel
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = remember { HomeViewModel.getInstance() },
    onNewsClick: (String?) -> Unit = {},
    onNavigateToNews: () -> Unit = {}
) {
    val raceWeekendState by viewModel.raceWeekendState.collectAsState()
    val lastRaceResult by viewModel.lastRaceResult.collectAsState()
    val driverStandings by viewModel.driverStandings.collectAsState()
    val constructorStandings by viewModel.constructorStandings.collectAsState()
    val newsArticles by viewModel.newsArticles.collectAsState()
    val scrollState = rememberScrollState()
    
    // Refresh data if stale when screen is displayed
    LaunchedEffect(Unit) {
        viewModel.refreshIfStale()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(scrollState)
    ) {
        // Hero Section
        HeroSectionFixed(
            state = raceWeekendState,
            getCountdown = { targetDateTime ->
                viewModel.getCountdownTo(targetDateTime)
            }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Horizontal Scrollable Cards Section
        HorizontalCardsSection(
            lastRaceResult = lastRaceResult,
            driverStandings = driverStandings,
            constructorStandings = constructorStandings
        )
        
        // News Section
        if (newsArticles.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            com.f1tracker.ui.components.NewsSection(
                newsArticles = newsArticles,
                onNewsClick = onNewsClick,
                onViewMoreClick = onNavigateToNews
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun HorizontalCardsSection(
    lastRaceResult: Race?,
    driverStandings: List<DriverStanding>?,
    constructorStandings: List<ConstructorStanding>?
) {
    val brigendsFont = FontFamily(Font(R.font.brigends_expanded))
    val michromaFont = FontFamily(Font(R.font.michroma))
    val scrollState = rememberScrollState()
    
    // Auto-scroll state
    var isUserInteracting by remember { mutableStateOf(false) }
    var currentCardIndex by remember { mutableStateOf(0) }
    val cardCount = 3
    val cardWidth = 280.dp
    val cardSpacing = 12.dp
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    
    // Calculate scroll position to center card
    val centerOffset = (screenWidth - cardWidth) / 2 - 20.dp
    
    // Auto-scroll effect
    LaunchedEffect(isUserInteracting, currentCardIndex) {
        if (!isUserInteracting) {
            delay(3000) // Wait 3 seconds
            val nextIndex = (currentCardIndex + 1) % cardCount
            val targetScroll = (nextIndex * (cardWidth + cardSpacing).value).toInt() - centerOffset.value.toInt()
            
            // Smooth scroll to next card
            scrollState.animateScrollTo(
                targetScroll.coerceAtLeast(0),
                animationSpec = tween(
                    durationMillis = 800,
                    easing = FastOutSlowInEasing
                )
            )
            currentCardIndex = nextIndex
        }
    }
    
    // Detect user interaction
    val interactionSource = remember { MutableInteractionSource() }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    isUserInteracting = true
                }
                is DragInteraction.Start -> {
                    isUserInteracting = true
                }
            }
        }
    }
    
    // Reset interaction flag after user stops interacting
    LaunchedEffect(scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress) {
            isUserInteracting = true
        } else if (isUserInteracting) {
            delay(5000) // Wait 5 seconds after user stops scrolling
            isUserInteracting = false
        }
    }
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Section Header - Aligned with Hero Section
        Text(
            text = "QUICK STATS",
            fontFamily = brigendsFont,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.6f),
            letterSpacing = 2.sp,
            modifier = Modifier
                .padding(horizontal = 20.dp) // Match Hero Section padding
                .padding(bottom = 12.dp)
        )
        
        // Horizontal Scrollable Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(start = 20.dp, end = 20.dp), // Padding on both sides
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Last Race Card
            LastRaceCard(race = lastRaceResult)
            
            // Driver Standings Card
            DriverStandingsCard(standings = driverStandings)
            
            // Constructor Standings Card
            ConstructorStandingsCard(standings = constructorStandings)
        }
    }
}

@Composable
private fun PlaceholderCard(
    title: String,
    subtitle: String,
    michromaFont: FontFamily,
    brigendsFont: FontFamily
) {
    Box(
        modifier = Modifier
            .width(280.dp)
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F0F0F), // Much darker, subtle offset from black
                        Color(0xFF0A0A0A)  // Nearly black
                    )
                )
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column {
                Text(
                    text = title,
                    fontFamily = brigendsFont,
                    fontSize = 12.sp,
                    color = Color(0xFFFF0080), // Match Hero Section pink accent
                    letterSpacing = 2.sp
                )
                Text(
                    text = subtitle,
                    fontFamily = michromaFont,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.7f), // Match Hero Section
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            // Placeholder Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Coming Soon",
                    fontFamily = michromaFont,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.3f)
                )
            }
        }
    }
}

