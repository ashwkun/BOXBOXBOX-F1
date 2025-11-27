package com.f1tracker.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.f1tracker.ui.components.AnimatedHeader
import com.f1tracker.ui.components.BottomNavBar
import com.f1tracker.ui.components.NavDestination
import com.f1tracker.R

@Composable
fun MainAppScreen() {
    var currentDestination by remember { mutableStateOf(NavDestination.HOME) }
    var webViewUrl by remember { mutableStateOf<String?>(null) }
    var selectedRace by remember { mutableStateOf<com.f1tracker.data.models.Race?>(null) }
    var selectedVideoId by remember { mutableStateOf<String?>(null) }
    var backPressedTime by remember { mutableLongStateOf(0L) }
    var scheduleSelectedTab by remember { mutableStateOf(0) } // Persist schedule tab state here
    var standingsSelectedTab by remember { mutableStateOf(0) } // Persist standings tab state here
    val context = LocalContext.current
    
    // Podcast audio player state
    val audioPlayerManager = remember { com.f1tracker.audio.AudioPlayerManager.getInstance(context) }
    var currentEpisode by remember { mutableStateOf<com.f1tracker.data.models.PodcastEpisode?>(null) }
    val isPlaying by audioPlayerManager.isPlaying.collectAsState()
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var showFullScreenPlayer by remember { mutableStateOf(false) }
    
    // Update audio position
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = audioPlayerManager.getCurrentPosition()
            duration = audioPlayerManager.getDuration()
            kotlinx.coroutines.delay(500) // Update every 500ms
        }
    }
    
    // Force black system bars when returning from video player
    LaunchedEffect(selectedVideoId) {
        if (selectedVideoId == null) {
            // Just returned from video player, force black colors
            (context as? android.app.Activity)?.window?.apply {
                statusBarColor = android.graphics.Color.BLACK
                navigationBarColor = android.graphics.Color.BLACK
            }
        }
    }
    
    // Handle system back button
    BackHandler {
        when {
            showFullScreenPlayer -> showFullScreenPlayer = false
            selectedVideoId != null -> selectedVideoId = null
            selectedRace != null -> selectedRace = null
            webViewUrl != null -> webViewUrl = null
            currentDestination != NavDestination.HOME -> currentDestination = NavDestination.HOME
            else -> {
                // Double back to exit
                val currentTime = System.currentTimeMillis()
                if (currentTime - backPressedTime < 2000) {
                    // Exit app
                    (context as? android.app.Activity)?.finish()
                } else {
                    backPressedTime = currentTime
                    Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    // Show Full Screen Player if expanded
    if (showFullScreenPlayer && currentEpisode != null) {
        com.f1tracker.ui.components.FullScreenAudioPlayer(
            episode = currentEpisode!!,
            isPlaying = isPlaying,
            currentPosition = currentPosition,
            duration = duration,
            onPlayPause = {
                if (isPlaying) {
                    audioPlayerManager.pause()
                } else {
                    audioPlayerManager.play()
                }
            },
            onSeek = { position ->
                audioPlayerManager.seekTo(position)
            },
            onClose = {
                showFullScreenPlayer = false
            }
        )
    } else if (selectedVideoId != null) {
        // Show YouTube Player if video is selected
        YouTubePlayerScreen(videoId = selectedVideoId!!)
    } else if (selectedRace != null) {
        // Show Race Detail if race is selected
        RaceDetailScreen(
            race = selectedRace!!,
            onBackClick = { selectedRace = null }
        )
    } else if (webViewUrl != null) {
        // Show WebView if URL is set
        WebViewScreen(url = webViewUrl!!)
    } else {
    Column(modifier = Modifier.fillMaxSize()) {
        // Top header (reduced height inside the composable)
        AnimatedHeader()

        // Main content area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (currentDestination) {
                    NavDestination.HOME -> HomeScreen(
                        onNewsClick = { url -> webViewUrl = url },
                        onNavigateToNews = { currentDestination = NavDestination.NEWS },
                        onRaceClick = { race -> selectedRace = race },
                        onVideoClick = { videoId -> selectedVideoId = videoId },
                        onEpisodeClick = { episode ->
                            currentEpisode = episode
                            audioPlayerManager.playEpisode(episode.audioUrl)
                        },
                        onPlayPause = {
                            if (isPlaying) {
                                audioPlayerManager.pause()
                            } else {
                                audioPlayerManager.play()
                            }
                        },
                        currentlyPlayingEpisode = currentEpisode,
                        isPlaying = isPlaying,
                        onNavigateToStandings = { tabIndex ->
                            standingsSelectedTab = tabIndex
                            currentDestination = NavDestination.STANDINGS
                        }
                )
                    NavDestination.SCHEDULE -> ScheduleScreen(
                        selectedTab = scheduleSelectedTab,
                        onTabChange = { tab -> scheduleSelectedTab = tab },
                        onRaceClick = { race -> selectedRace = race }
                    )
                    NavDestination.LIVE -> LiveScreen()
                    NavDestination.STANDINGS -> StandingsScreen(
                        selectedTab = standingsSelectedTab,
                        onTabChange = { standingsSelectedTab = it }
                    )
                    NavDestination.NEWS -> NewsScreen(
                        onNewsClick = { url -> webViewUrl = url }
                )
            }
        }

        // Fixed bottom navigation bar
        BottomNavBar(
            currentDestination = currentDestination,
            onNavigate = { destination -> currentDestination = destination }
        )
        
        // Mini Audio Player (appears above bottom nav when playing)
        com.f1tracker.ui.components.MiniAudioPlayer(
            currentEpisode = currentEpisode,
            isPlaying = isPlaying,
            currentPosition = currentPosition,
            duration = duration,
            onPlayPause = {
                if (isPlaying) {
                    audioPlayerManager.pause()
                } else {
                    audioPlayerManager.play()
                }
            },
            onClose = {
                audioPlayerManager.pause()
                currentEpisode = null
            },
            onExpand = {
                showFullScreenPlayer = true
            }
        )
        }
    }
}

@Composable
private fun PlaceholderContent(
    title: String,
    message: String
) {
    val michromaFont = FontFamily(
        Font(R.font.michroma, FontWeight.Normal)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title.uppercase(),
            fontFamily = michromaFont,
            fontSize = 20.sp,
            letterSpacing = 2.sp,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            fontFamily = michromaFont,
            fontSize = 14.sp,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

