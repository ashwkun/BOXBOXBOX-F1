package com.f1tracker.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f1tracker.data.acestream.AceStreamChannel
import com.f1tracker.data.acestream.AceStreamRepository
import com.f1tracker.ui.viewmodels.AceStreamState

// Brand Colors
private val AceStreamOrange = Color(0xFFFF6B00)
private val AceStreamBlue = Color(0xFF0088FF)
private val BrandGradient = Brush.horizontalGradient(listOf(AceStreamOrange, AceStreamBlue))

@Composable
fun AceStreamLiveCard(
    state: AceStreamState,
    michromaFont: FontFamily,
    onInstallRequested: () -> Unit,
    onStartEngineRequested: () -> Unit,
    onManualRefreshRequested: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { AceStreamRepository.getInstance() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = when (state) {
                    is AceStreamState.NotInstalled -> AceStreamBlue.copy(alpha = 0.3f)
                    is AceStreamState.InstalledEngineOff -> AceStreamOrange.copy(alpha = 0.3f)
                    is AceStreamState.Searching -> AceStreamBlue.copy(alpha = 0.3f)
                    is AceStreamState.StreamsReady -> AceStreamOrange.copy(alpha = 0.5f)
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
                        .graphicsLayer(alpha = if (state is AceStreamState.NotInstalled || state is AceStreamState.InstalledEngineOff) 1f else alpha),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (state) {
                            is AceStreamState.NotInstalled -> "P2P STREAM INTEGRATION"
                            is AceStreamState.InstalledEngineOff -> "LIVE BROADCASTS DETECTED"
                            is AceStreamState.Searching -> "SCANNING P2P NETWORK"
                            is AceStreamState.StreamsReady -> "LIVE STREAMS READY"
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
                if (state is AceStreamState.StreamsReady || state is AceStreamState.Error) {
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
                targetState = state,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                }, label = "AceStreamStateContent"
            ) { targetState ->
                when (targetState) {
                    is AceStreamState.NotInstalled -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "F1 Tracker uses peer-to-peer technology to provide reliable, high-definition live broadcasts. Install the Ace Stream Engine to enable this feature.",
                                fontFamily = michromaFont,
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF222222))
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .clickable { onInstallRequested() }
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Google Play Store",
                                    tint = Color(0xFF00E676), // Android/Google Play green
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(verticalArrangement = Arrangement.Center) {
                                    Text(
                                        text = "GET IT ON",
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                                        fontSize = 8.sp,
                                        color = Color.White.copy(alpha = 0.7f),
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = "Google Play",
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                                        fontSize = 14.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    is AceStreamState.InstalledEngineOff -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Ace Stream is installed but sleeping. Start the engine to search for Sky Sports and ESPN feeds.",
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
                                    text = "START ENGINE",
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
                                    text = "Finding highest quality English streams...",
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
                                text = "Tap a channel to open securely in Ace Player",
                                fontFamily = michromaFont,
                                fontSize = 8.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            
                            targetState.channels.take(8).forEach { channel ->
                                AceStreamChannelItem(
                                    channel = channel,
                                    michromaFont = michromaFont,
                                    onClick = {
                                        try {
                                            val intent = repository.buildStreamIntent(channel.infohash)
                                            context.startActivity(intent)
                                        } catch (e: ActivityNotFoundException) {
                                            Toast.makeText(context, "Could not open Ace Stream", Toast.LENGTH_SHORT).show()
                                        }
                                    }
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
