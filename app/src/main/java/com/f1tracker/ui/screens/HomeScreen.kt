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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDateTime
import androidx.hilt.navigation.compose.hiltViewModel
import com.f1tracker.R
import com.f1tracker.data.models.ConstructorStanding
import com.f1tracker.data.models.DriverStanding
import com.f1tracker.data.models.Race
import com.f1tracker.ui.components.ConstructorStandingsCard
import com.f1tracker.ui.components.DriverStandingsCard
import com.f1tracker.ui.components.HeroSectionFixed
import com.f1tracker.ui.components.LastRaceCard
import com.f1tracker.ui.components.DailyMixSection
import com.f1tracker.ui.viewmodels.MultimediaViewModel
import com.f1tracker.ui.viewmodels.NewsViewModel
import com.f1tracker.ui.viewmodels.RaceViewModel
import com.f1tracker.ui.viewmodels.StandingsViewModel
import kotlinx.coroutines.delay

import com.f1tracker.util.NewsCategorizer
import com.f1tracker.util.NewsCategory

@Composable
fun HomeScreen(
    raceViewModel: RaceViewModel = hiltViewModel(),
    standingsViewModel: StandingsViewModel = hiltViewModel(),
    newsViewModel: NewsViewModel = hiltViewModel(),
    multimediaViewModel: MultimediaViewModel = hiltViewModel(),
    onNewsClick: (String?) -> Unit = {},
    onNavigateToNews: () -> Unit = {},
    onNavigateToVideos: () -> Unit = {},
    onNavigateToPodcasts: () -> Unit = {},
    onNavigateToSocial: (String) -> Unit = {},
    onRaceClick: (Race) -> Unit = {},
    onVideoClick: (String) -> Unit = {},
    onEpisodeClick: (com.f1tracker.data.models.PodcastEpisode) -> Unit = {},
    onGameClick: () -> Unit = {},
    onPlayPause: () -> Unit = {},
    onNavigateToStandings: (Int) -> Unit = {}, // 0 for Drivers, 1 for Constructors
    onViewResults: (com.f1tracker.data.models.SessionResult) -> Unit = {},
    onNavigateToLive: () -> Unit = {},
    currentlyPlayingEpisode: com.f1tracker.data.models.PodcastEpisode? = null,
    isPlaying: Boolean = false
) {
    val raceWeekendState by raceViewModel.raceWeekendState.collectAsState()
    val lastRaceResult by raceViewModel.lastRaceResult.collectAsState()
    val driverStandings by standingsViewModel.driverStandings.collectAsState()
    val constructorStandings by standingsViewModel.constructorStandings.collectAsState()
    val newsArticles by newsViewModel.newsArticles.collectAsState()
    val youtubeVideos by multimediaViewModel.youtubeVideos.collectAsState()
    val podcasts by multimediaViewModel.podcasts.collectAsState()
    val scrollState = rememberScrollState()
    
    // Refresh data if stale when screen is displayed
    LaunchedEffect(Unit) {
        raceViewModel.refreshIfStale()
    }
    
    // Sort News for Daily Mix (Prioritize Nuclear/Major)
    val dailyMixNews = remember(newsArticles) {
        newsArticles.sortedBy { article ->
            val category = NewsCategorizer.categorize(article.headline)
            when (category) {
                NewsCategory.NUCLEAR -> 0
                NewsCategory.MAJOR -> 1
                else -> 2
            }
        }
    }

    // Filter news for the list below to avoid duplicates
    val remainingNews = remember(newsArticles, dailyMixNews) {
        val mixedUrls = dailyMixNews.take(2).mapNotNull { it.links?.web?.href }.toSet()
        newsArticles.filter { article -> 
            article.links?.web?.href !in mixedUrls 
        }
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
                raceViewModel.getCountdownTo(targetDateTime)
            },
            onRaceClick = onRaceClick,
            onViewResults = onViewResults,
            onLiveClick = onNavigateToLive
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Horizontal Scrollable Cards Section
        HorizontalCardsSection(
            lastRaceResult = lastRaceResult,
            driverStandings = driverStandings,
            constructorStandings = constructorStandings,
            onRaceClick = onRaceClick,
            onNavigateToStandings = onNavigateToStandings
        )
        
        // Bento "Daily Mix" Horizontal Scroll
        val instagramPosts by multimediaViewModel.instagramPosts.collectAsState()
        
        Spacer(modifier = Modifier.height(24.dp))
        
        DailyMixSection(
            newsArticles = dailyMixNews,
            videos = youtubeVideos,
            socialPosts = instagramPosts,
            podcasts = podcasts,
            onNewsClick = onNewsClick,
            onVideoClick = onVideoClick,
            onSocialClick = { permalink -> onNavigateToSocial(permalink) },
            onGameClick = onGameClick,
            onPodcastClick = onEpisodeClick
        )

        // Social Section (Replicating News Section style, but big cards)
        val socialSectionPosts = remember(instagramPosts) {
            val now = java.time.Instant.now()
            fun getScore(post: com.f1tracker.data.models.InstagramPost): Double {
                val likes = post.like_count.toDouble()
                val hours = try {
                     val instant = java.time.Instant.parse(post.timestamp)
                     java.time.Duration.between(instant, now).toHours().toDouble()
                } catch (e: Exception) { 100.0 }
                return likes / Math.pow(hours + 2.0, 1.5)
            }

            val sorted = instagramPosts.sortedByDescending { getScore(it) }
            val topReel = sorted.firstOrNull { it.media_type == "VIDEO" }
            val others = sorted.filter { it != topReel }.take(2)
            
            if (topReel != null) listOf(topReel) + others else others.take(3)
        }

        if (socialSectionPosts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            com.f1tracker.ui.components.SocialSection(
                posts = socialSectionPosts,
                onSocialClick = { permalink -> onNavigateToSocial(permalink) },
                onViewMoreClick = { onNavigateToSocial("") }
            )
        }

        // News Section (Remaining items)
        if (remainingNews.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            com.f1tracker.ui.components.NewsSection(
                newsArticles = remainingNews, 
                onNewsClick = onNewsClick,
                onViewMoreClick = onNavigateToNews
            )
        }
        
        // YouTube Highlights Section
        if (youtubeVideos.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            com.f1tracker.ui.components.YouTubeHighlightsSection(
                videos = youtubeVideos,
                onVideoClick = onVideoClick,
                onViewMoreClick = onNavigateToVideos
            )
        }
        
        // Podcasts Section
        if (podcasts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            com.f1tracker.ui.components.HomePodcastsSection(
                podcasts = podcasts,
                currentlyPlayingEpisode = currentlyPlayingEpisode,
                isPlaying = isPlaying,
                onEpisodeClick = onEpisodeClick,
                onPlayPause = onPlayPause,
                onViewMoreClick = onNavigateToPodcasts
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun HorizontalCardsSection(
    lastRaceResult: Race?,
    driverStandings: List<DriverStanding>?,
    constructorStandings: List<ConstructorStanding>?,
    onRaceClick: (Race) -> Unit = {},
    onNavigateToStandings: (Int) -> Unit = {}
) {
    val brigendsFont = FontFamily(Font(R.font.brigends_expanded))
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    
    // Auto-scroll state
    var isUserInteracting by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(0) }
    val cardWidth = 340.dp // Actual card width from LastRaceCard
    val cardSpacing = 12.dp
    val sidePadding = 20.dp
    val totalCards = 3
    
    // Convert to pixels properly - full card width including spacing
    val cardWithSpacingPx = with(density) { (cardWidth + cardSpacing).toPx() }
    
    // Auto-scroll effect
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000) // Wait 3 seconds
            if (!isUserInteracting) {
                // Move to next card
                currentPage = (currentPage + 1) % totalCards
                
                // Calculate exact pixel position for this card
                // Each card needs to move by full card width + spacing
                val targetPosition = (currentPage * cardWithSpacingPx).toInt()
                
                // Smooth scroll to exact position
                scrollState.animateScrollTo(
                    targetPosition,
                    animationSpec = tween(
                        durationMillis = 800,
                        easing = FastOutSlowInEasing
                    )
                )
            }
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
            LastRaceCard(
                race = lastRaceResult,
                onClick = { lastRaceResult?.let { onRaceClick(it) } }
            )
            
            // Driver Standings Card
            DriverStandingsCard(
                standings = driverStandings,
                onClick = { onNavigateToStandings(0) }
            )
            
            // Constructor Standings Card
            ConstructorStandingsCard(
                standings = constructorStandings,
                onClick = { onNavigateToStandings(1) }
            )
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

