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
fun MainAppScreen(
    updateStatus: com.f1tracker.util.UpdateStatus?,
    showUpdateDialog: Boolean,
    onShowUpdateDialogChange: (Boolean) -> Unit,
    intentData: Pair<String, String>? = null,
    onIntentHandled: () -> Unit = {}
) {
    var currentDestination by remember { mutableStateOf(NavDestination.HOME) }
    var webViewUrl by remember { mutableStateOf<String?>(null) }
    var selectedRace by remember { mutableStateOf<com.f1tracker.data.models.Race?>(null) }
    var selectedVideoId by remember { mutableStateOf<String?>(null) }
    var selectedSessionResult by remember { mutableStateOf<com.f1tracker.data.models.SessionResult?>(null) }
    var backPressedTime by remember { mutableLongStateOf(0L) }
    var scheduleSelectedTab by remember { mutableStateOf(0) } // Persist schedule tab state here
    var standingsSelectedTab by remember { mutableStateOf(0) } // Persist standings tab state here
    var socialSelectedTab by remember { mutableStateOf(0) } // Persist social tab state here
    val context = LocalContext.current
    
    // Handle Intent Data (Deep Links / Notifications)
    LaunchedEffect(intentData) {
        intentData?.let { (url, targetTab) ->
            // 1. Set up the background state (where to go when back is pressed)
            if (targetTab == "news") {
                currentDestination = NavDestination.SOCIAL
                socialSelectedTab = 1
            }
            
            // 2. Open the content
            if (url.isNotEmpty()) {
                webViewUrl = url
            }
            
            // 3. Mark handled
            onIntentHandled()
        }
    }
    
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
            selectedSessionResult != null -> selectedSessionResult = null
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
    } else if (selectedSessionResult != null) {
        // Show Session Results
        SessionResultsScreen(
            sessionResult = selectedSessionResult!!,
            onBackClick = { selectedSessionResult = null }
        )
    } else if (selectedRace != null) {
        // Show Race Detail if race is selected
        RaceDetailScreen(
            race = selectedRace!!,
            onBackClick = { selectedRace = null }
        )
    } else if (webViewUrl != null) {
        // Show WebView if URL is set
        WebViewScreen(
            url = webViewUrl!!,
            isUpdateAvailable = updateStatus is com.f1tracker.util.UpdateStatus.UpdateAvailable,
            onUpdateClick = { onShowUpdateDialogChange(true) }
        )
    } else {
    Column(modifier = Modifier.fillMaxSize()) {
        // Top header (reduced height inside the composable)
        AnimatedHeader(
            isUpdateAvailable = updateStatus is com.f1tracker.util.UpdateStatus.UpdateAvailable,
            onUpdateClick = { onShowUpdateDialogChange(true) }
        )

        // Main content area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (currentDestination) {
                    NavDestination.HOME -> HomeScreen(
                        onNewsClick = { url -> webViewUrl = url },
                        onNavigateToNews = { 
                            socialSelectedTab = 1
                            currentDestination = NavDestination.SOCIAL 
                        },
                        onNavigateToVideos = { 
                            socialSelectedTab = 2
                            currentDestination = NavDestination.SOCIAL 
                        },
                        onNavigateToPodcasts = { 
                            socialSelectedTab = 3
                            currentDestination = NavDestination.SOCIAL 
                        },
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
                        },
                        onViewResults = { result ->
                            selectedSessionResult = result
                        },
                        onNavigateToLive = {
                            currentDestination = NavDestination.LIVE
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
                    NavDestination.SOCIAL -> SocialScreen(
                        onNewsClick = { url -> webViewUrl = url },
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
                        initialTab = socialSelectedTab
                    )
            }
        }

        // Secret Notification State
        var liveTabClickCount by remember { androidx.compose.runtime.mutableIntStateOf(0) }
        var lastLiveTabClickTime by remember { mutableLongStateOf(0L) }

        // Fixed bottom navigation bar
        BottomNavBar(
            currentDestination = currentDestination,
            onNavigate = { destination -> 
                if (destination == NavDestination.LIVE) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastLiveTabClickTime > 2000) {
                        liveTabClickCount = 0 // Reset if too slow
                    }
                    liveTabClickCount++
                    lastLiveTabClickTime = currentTime
                    
                    if (liveTabClickCount == 5) {
                        liveTabClickCount = 0
                        Toast.makeText(context, "🏎️ Secret Test Notification Sent!", Toast.LENGTH_SHORT).show()
                        sendTestNotification(context)
                    }
                } else {
                    liveTabClickCount = 0 // Reset if switched tab
                }
                currentDestination = destination 
            }
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

private fun sendTestNotification(context: android.content.Context) {
    val channelId = "f1_updates_channel_v3" // Use same channel to test sound/icon
    val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    
    // Ensure channel exists (it should, but good practice)
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        val existingChannel = notificationManager.getNotificationChannel(channelId)
        if (existingChannel == null) {
            // Re-create if missing (simplified version of Service logic)
            val soundUri = android.net.Uri.parse("android.resource://" + context.packageName + "/" + R.raw.notification_sound)
            val audioAttributes = android.media.AudioAttributes.Builder()
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                .build()
                
            val channel = android.app.NotificationChannel(
                channelId,
                "F1 Updates",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                setSound(soundUri, audioAttributes)
                enableLights(true)
                lightColor = android.graphics.Color.RED
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    val largeIcon = android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.ic_notification_large)
    val soundUri = android.net.Uri.parse("android.resource://" + context.packageName + "/" + R.raw.notification_sound)

    val notificationBuilder = androidx.core.app.NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_notification_small)
        .setLargeIcon(largeIcon)
        .setColor(android.graphics.Color.parseColor("#FF0080"))
        .setContentTitle("Hello World")
        .setContentText("Surprise! You found the secret button. Now get back to watching the race, you nerd! 🏎️💨")
        .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText("Surprise! You found the secret button. Now get back to watching the race, you nerd! 🏎️💨"))
        .setAutoCancel(true)
        .setSound(soundUri)
        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)

    notificationManager.notify(999, notificationBuilder.build())
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

