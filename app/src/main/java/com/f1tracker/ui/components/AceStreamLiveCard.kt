package com.f1tracker.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.ui.PlayerView
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.sp
import com.f1tracker.R
import com.f1tracker.data.acestream.AceStreamChannel
import com.f1tracker.data.acestream.AceStreamRepository
import com.f1tracker.ui.viewmodels.AceStreamState
import com.f1tracker.ui.viewmodels.RaceViewModel

// Brand Colors
private val AceStreamOrange = Color(0xFFFF6B00)
private val AceStreamBlue = Color(0xFF0088FF)
private val BrandGradient = Brush.horizontalGradient(listOf(AceStreamOrange, AceStreamBlue))

@Composable
fun AceStreamLiveCard(
    raceViewModel: RaceViewModel,
    michromaFont: FontFamily,
    onInstallRequested: () -> Unit,
    onStartEngineRequested: () -> Unit,
    onManualRefreshRequested: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { AceStreamRepository.getInstance() }
    val targetState by raceViewModel.aceStreamState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = when (targetState) {
                    is AceStreamState.NotInstalled -> AceStreamBlue.copy(alpha = 0.3f)
                    is AceStreamState.InstalledEngineOff -> AceStreamOrange.copy(alpha = 0.3f)
                    is AceStreamState.Searching -> AceStreamBlue.copy(alpha = 0.3f)
                    is AceStreamState.StreamsReady -> AceStreamOrange.copy(alpha = 0.5f)
                    is AceStreamState.PreviewingStream -> AceStreamOrange.copy(alpha = 0.8f)
                    is AceStreamState.PreviewError -> Color.Red.copy(alpha = 0.5f)
                    is AceStreamState.Error -> Color.Red.copy(alpha = 0.3f)
                },
                shape = RoundedCornerShape(16.dp)
            )
            .background(Color(0xFF111111), RoundedCornerShape(16.dp))
            .padding(16.dp)
            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Animated glowing icon
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.5f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(BrandGradient, RoundedCornerShape(8.dp))
                        .graphicsLayer(alpha = if (targetState is AceStreamState.NotInstalled || targetState is AceStreamState.InstalledEngineOff) 1f else alpha),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (targetState is AceStreamState.NotInstalled) Icons.Default.Settings else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (targetState) {
                            is AceStreamState.NotInstalled -> "STREAM SETUP REQUIRED"
                            is AceStreamState.InstalledEngineOff -> "START BROADCAST NETWORK"
                            is AceStreamState.Searching -> "SCANNING P2P NETWORK"
                            is AceStreamState.StreamsReady -> "AVAILABLE STREAMS"
                            is AceStreamState.PreviewingStream -> "STREAM PREVIEW"
                            is AceStreamState.PreviewError -> "LOAD TIMEOUT (CAN STILL LAUNCH)"
                            is AceStreamState.Error -> "STREAM FETCH FAILED"
                        },
                        fontFamily = michromaFont,
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Powered by Ace Stream",
                        fontFamily = michromaFont,
                        fontSize = 8.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 0.5.sp
                    )
                }
                
                // Refresh Action
                if (targetState is AceStreamState.StreamsReady || targetState is AceStreamState.Error || targetState is AceStreamState.PreviewingStream) {
                    IconButton(
                        onClick = onManualRefreshRequested,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Body Content based on State
            AnimatedContent(
                targetState = targetState,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                }, label = "AceStreamStateContent"
            ) { targetState ->
                when (targetState) {
                    is AceStreamState.NotInstalled -> {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = "Live race streams require a dedicated P2P video engine. Follow these steps to enable the feature:",
                                fontFamily = michromaFont,
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            
                            val steps = listOf(
                                "Install the Ace Stream app from the Play Store",
                                "Open the app once to grant necessary permissions",
                                "Come back to BOXBOXBOX to watch live sessions"
                            )
                            
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                steps.forEachIndexed { index, step ->
                                    Row(verticalAlignment = Alignment.Top) {
                                        Text(
                                            text = "${index + 1}.",
                                            fontFamily = michromaFont,
                                            fontSize = 11.sp,
                                            color = Color(0xFF00BFFF),
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Text(
                                            text = step,
                                            fontFamily = michromaFont,
                                            fontSize = 9.sp,
                                            color = Color.White.copy(alpha = 0.9f),
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onInstallRequested() },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.google_play_badge),
                                    contentDescription = "Get it on Google Play",
                                    modifier = Modifier.height(48.dp)
                                )
                            }
                        }
                    }

                    is AceStreamState.InstalledEngineOff -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Your stream engine is currently offline. Launch the Ace Stream app to connect, then head back to BOXBOXBOX to view live feeds.",
                                fontFamily = michromaFont,
                                fontSize = 10.sp,
                                color = AceStreamOrange.copy(alpha = 0.9f),
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BrandGradient)
                                    .clickable { onStartEngineRequested() }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "LAUNCH ACE STREAM",
                                    fontFamily = michromaFont,
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }

                    is AceStreamState.Searching -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = AceStreamOrange,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Loading streams...",
                                    fontFamily = michromaFont,
                                    fontSize = 8.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    is AceStreamState.Error -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color.Red.copy(alpha = 0.8f),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = targetState.message,
                                fontFamily = michromaFont,
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                            Text(
                                text = "F1 streams usually appear 30 mins before sessions.",
                                fontFamily = michromaFont,
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    is AceStreamState.StreamsReady -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Select a channel to preview stream health & quality",
                                fontFamily = michromaFont,
                                fontSize = 8.sp,
                                lineHeight = 12.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                targetState.channels.forEach { channel ->
                                    AceStreamChannelItem(
                                        channel = channel,
                                        michromaFont = michromaFont,
                                        onClick = {
                                            raceViewModel.previewStream(channel)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    is AceStreamState.PreviewingStream -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Back button at the top — always visible
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .clickable { raceViewModel.stopPreviewing() }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "BACK TO CHANNELS",
                                    fontFamily = michromaFont,
                                    fontSize = 9.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }

                            Text(
                                text = "Previewing: ${targetState.channel.name}",
                                fontFamily = michromaFont,
                                fontSize = 10.sp,
                                color = Color.White
                            )
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black)
                            ) {
                                // ExoPlayer with retry policy — retries 500 errors silently while showing buffering spinner.
                                // We do NOT pre-poll with OkHttp because the proxy is single-client:
                                // any GET that reads data consumes the stream start, so ExoPlayer gets mid-stream garbage.
                                val httpDataSourceFactory = remember {
                                    androidx.media3.datasource.DefaultHttpDataSource.Factory()
                                        .setConnectTimeoutMs(15_000)
                                        .setReadTimeoutMs(15_000)
                                        .setAllowCrossProtocolRedirects(true)
                                }
                                val retryPolicy = remember {
                                    object : androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy() {
                                        // Ace Stream's HTTP API requires Premium — free users always get 500.
                                        // Keep retries low (5 = ~15s) since waiting longer won't help.
                                        override fun getMinimumLoadableRetryCount(dataType: Int): Int = 5
                                        
                                        override fun getRetryDelayMsFor(loadErrorInfo: androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy.LoadErrorInfo): Long {
                                            val error = loadErrorInfo.exception
                                            val isHttpError = error is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException
                                            val isIOError = error is java.io.IOException
                                            return if ((isHttpError || isIOError) && loadErrorInfo.errorCount <= 5) {
                                                android.util.Log.d("AcePreview", "Retry ${loadErrorInfo.errorCount}/5 after ${error.javaClass.simpleName}: ${error.message}")
                                                3_000L // 3 second retry delay
                                            } else {
                                                android.util.Log.e("AcePreview", "Giving up after ${loadErrorInfo.errorCount} retries: ${error.message}")
                                                androidx.media3.common.C.TIME_UNSET
                                            }
                                        }
                                    }
                                }
                                val mediaSourceFactory = remember {
                                    androidx.media3.exoplayer.source.DefaultMediaSourceFactory(httpDataSourceFactory)
                                        .setLoadErrorHandlingPolicy(retryPolicy)
                                }
                                val exoPlayer = remember {
                                    ExoPlayer.Builder(context)
                                        .setMediaSourceFactory(mediaSourceFactory)
                                        .build().apply {
                                        addListener(object : androidx.media3.common.Player.Listener {
                                            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                                                android.util.Log.e("AcePreview", "ExoPlayer final error: code=${error.errorCode}, msg=${error.message}", error.cause)
                                                raceViewModel.setPreviewError(targetState.channel, "Preview requires Ace Stream Premium. Use the Launch button to watch with the free Ace Player app.")
                                                com.f1tracker.util.AnalyticsLogger.streamPreviewFailed(targetState.channel.name, error.message ?: "unknown")
                                            }
                                        })
                                    }
                                }

                                DisposableEffect(targetState.playbackUrl) {
                                    exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(targetState.playbackUrl)))
                                    exoPlayer.prepare()
                                    exoPlayer.playWhenReady = true
                                    
                                    onDispose {
                                        exoPlayer.stop()
                                        exoPlayer.release()
                                    }
                                }

                                AndroidView(
                                    factory = { ctx ->
                                        PlayerView(ctx).apply {
                                            player = exoPlayer
                                            useController = false
                                            setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                                        }
                                    },
                                    modifier = Modifier.matchParentSize(),
                                    onRelease = { view ->
                                        view.player = null
                                    }
                                )
                            }
                            
                            Text(
                                text = "In-app preview requires Ace Stream Premium. Free users can use the Launch button to watch via Ace Player (with a 30s ad).",
                                fontFamily = michromaFont,
                                fontSize = 8.sp,
                                lineHeight = 12.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BrandGradient)
                                    .clickable {
                                        try {
                                            val intent = repository.buildStreamIntent(targetState.channel.infohash)
                                            com.f1tracker.util.AnalyticsLogger.streamLaunched(targetState.channel.name, "ace_player")
                                            context.startActivity(intent)
                                        } catch (e: ActivityNotFoundException) {
                                            Toast.makeText(context, "Could not open Ace Stream", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "LAUNCH IN ACE PLAYER",
                                    fontFamily = michromaFont,
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }

                    is AceStreamState.PreviewError -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Back button at the top — always visible
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .clickable { raceViewModel.stopPreviewing() }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "BACK TO CHANNELS",
                                    fontFamily = michromaFont,
                                    fontSize = 9.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }

                            Text(
                                text = "Preview unavailable",
                                fontFamily = michromaFont,
                                fontSize = 10.sp,
                                color = Color.White
                            )
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = "Info",
                                        tint = Color(0xFFFFB74D),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(
                                        text = "The in-app preview couldn't load this stream. This is common with P2P streams and doesn't mean the stream is broken.",
                                        fontFamily = michromaFont,
                                        fontSize = 9.sp,
                                        color = Color.White.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                            
                            Text(
                                text = "Ace Player handles P2P connections more reliably. The stream will most likely work when launched directly.",
                                fontFamily = michromaFont,
                                fontSize = 8.sp,
                                lineHeight = 12.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BrandGradient)
                                    .clickable {
                                        try {
                                            val intent = repository.buildStreamIntent(targetState.channel.infohash)
                                            com.f1tracker.util.AnalyticsLogger.streamLaunched(targetState.channel.name, "ace_player")
                                            context.startActivity(intent)
                                        } catch (e: ActivityNotFoundException) {
                                            Toast.makeText(context, "Could not open Ace Stream", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "LAUNCH IN ACE PLAYER",
                                    fontFamily = michromaFont,
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
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
private fun AceStreamChannelItem(
    channel: AceStreamChannel,
    michromaFont: FontFamily,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Status indicator pulse
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
        
        val isHealthy = channel.isAvailable && channel.availability >= 0.8
        
        Box(
            modifier = Modifier
                .size(6.dp)
                .graphicsLayer { this.alpha = alpha }
                .background(
                    if (isHealthy) Color(0xFF00FF88) else Color(0xFFFFAA00),
                    CircleShape
                )
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = channel.name,
                fontFamily = michromaFont,
                fontSize = 10.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                // Quality badge
                val quality = AceStreamRepository.getQualityLabel(channel.name)
                Box(
                    modifier = Modifier
                        .background(
                            when (quality) {
                                "4K" -> Color(0xFF9C27B0).copy(alpha = 0.3f)
                                "FHD" -> Color(0xFF00BFA5).copy(alpha = 0.3f)
                                "HD" -> Color(0xFF2196F3).copy(alpha = 0.3f)
                                else -> Color.White.copy(alpha = 0.1f)
                            },
                            RoundedCornerShape(3.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = quality,
                        fontFamily = michromaFont,
                        fontSize = 6.sp,
                        color = when (quality) {
                            "4K" -> Color(0xFFCE93D8)
                            "FHD" -> Color(0xFF80CBC4)
                            "HD" -> Color(0xFF90CAF9)
                            else -> Color.White.copy(alpha = 0.7f)
                        },
                        fontWeight = FontWeight.Bold
                    )
                }

                // English badge (always prominently highlight verified English feeds)
                if (AceStreamRepository.isEnglish(channel)) {
                    Text(
                        text = "ENG",
                        fontFamily = michromaFont,
                        fontSize = 6.sp,
                        color = Color(0xFF00FF88),
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Reliable badge for Sky Sports Main Event
                if (AceStreamRepository.isSkyMainEvent(channel)) {
                    Box(
                        modifier = Modifier
                            .background(
                                Color(0xFF00FF88).copy(alpha = 0.15f),
                                RoundedCornerShape(3.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "RELIABLE",
                            fontFamily = michromaFont,
                            fontSize = 6.sp,
                            color = Color(0xFF00FF88),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Connection Health
                Text(
                    text = "${channel.availabilityPercent}%",
                    fontFamily = michromaFont,
                    fontSize = 7.sp,
                    color = if (isHealthy) Color(0xFF00FF88) else Color(0xFFFFAA00)
                )
            }
        }

        // Play icon
        Icon(
            imageVector = Icons.Default.PlayCircleOutline,
            contentDescription = "Play",
            tint = AceStreamOrange,
            modifier = Modifier.size(20.dp)
        )
    }
}
