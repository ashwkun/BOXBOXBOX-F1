package com.f1tracker.ui.components

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f1tracker.R
import com.f1tracker.data.acestream.AceStreamChannel
import com.f1tracker.data.acestream.AceStreamRepository
import kotlinx.coroutines.launch

// Ace Stream brand colors
private val AceStreamOrange = Color(0xFFFF6B00)
private val AceStreamBlue = Color(0xFF0088FF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AceStreamBottomSheet(
    showSheet: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { AceStreamRepository.getInstance() }
    val michromaFont = FontFamily(Font(R.font.michroma, FontWeight.Normal))

    var isLoading by remember { mutableStateOf(true) }
    var channels by remember { mutableStateOf<List<AceStreamChannel>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var engineRunning by remember { mutableStateOf(false) }

    // Load channels when sheet opens
    LaunchedEffect(showSheet) {
        if (showSheet) {
            isLoading = true
            errorMessage = null

            // First check if engine is running
            engineRunning = repository.isEngineRunning()

            if (!engineRunning) {
                errorMessage = "ENGINE_NOT_RUNNING"
                isLoading = false
                return@LaunchedEffect
            }

            // Search for F1 channels
            try {
                channels = repository.searchF1Channels()
                if (channels.isEmpty()) {
                    errorMessage = "NO_CHANNELS"
                }
            } catch (e: Exception) {
                errorMessage = "SEARCH_FAILED"
            }
            isLoading = false
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = Color(0xFF0D0D0D),
            contentColor = Color.White,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Ace Stream icon
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                Brush.linearGradient(listOf(AceStreamOrange, AceStreamBlue)),
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "ACE STREAM",
                            fontFamily = michromaFont,
                            fontSize = 12.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "F1 Live Channels",
                            fontFamily = michromaFont,
                            fontSize = 8.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                when {
                    isLoading -> {
                        // Loading state
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = AceStreamOrange,
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "SEARCHING P2P NETWORK...",
                                    fontFamily = michromaFont,
                                    fontSize = 9.sp,
                                    color = Color.White.copy(alpha = 0.6f),
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }

                    errorMessage == "ENGINE_NOT_RUNNING" -> {
                        // Engine not running state
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = AceStreamOrange,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = "ACE STREAM ENGINE\nNOT RUNNING",
                                    fontFamily = michromaFont,
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    letterSpacing = 1.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                                Text(
                                    text = "Please open the Ace Stream app first, then come back here",
                                    fontFamily = michromaFont,
                                    fontSize = 8.sp,
                                    color = Color.White.copy(alpha = 0.5f),
                                    letterSpacing = 0.5.sp,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Launch Engine button
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(AceStreamOrange, AceStreamBlue)
                                            )
                                        )
                                        .clickable {
                                            val launchIntent = repository.buildLaunchEngineIntent(context)
                                            if (launchIntent != null) {
                                                context.startActivity(launchIntent)
                                            } else {
                                                Toast.makeText(context, "Could not launch Ace Stream", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .padding(horizontal = 20.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = "OPEN ACE STREAM",
                                        fontFamily = michromaFont,
                                        fontSize = 9.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }

                                // Retry button
                                Text(
                                    text = "TAP TO RETRY",
                                    fontFamily = michromaFont,
                                    fontSize = 8.sp,
                                    color = AceStreamOrange,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier
                                        .clickable {
                                            scope.launch {
                                                isLoading = true
                                                errorMessage = null
                                                engineRunning = repository.isEngineRunning()
                                                if (!engineRunning) {
                                                    errorMessage = "ENGINE_NOT_RUNNING"
                                                } else {
                                                    channels = repository.searchF1Channels()
                                                    if (channels.isEmpty()) {
                                                        errorMessage = "NO_CHANNELS"
                                                    }
                                                }
                                                isLoading = false
                                            }
                                        }
                                        .padding(8.dp)
                                )
                            }
                        }
                    }

                    errorMessage == "NO_CHANNELS" -> {
                        // No channels found
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = "NO F1 CHANNELS FOUND",
                                    fontFamily = michromaFont,
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "F1 streams usually appear\n30 minutes before a session starts",
                                    fontFamily = michromaFont,
                                    fontSize = 8.sp,
                                    color = Color.White.copy(alpha = 0.4f),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }

                    channels.isNotEmpty() -> {
                        // Channel list
                        Text(
                            text = "${channels.size} F1 CHANNELS FOUND",
                            fontFamily = michromaFont,
                            fontSize = 8.sp,
                            color = AceStreamOrange,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.heightIn(max = 400.dp)
                        ) {
                            items(channels) { channel ->
                                AceStreamChannelItem(
                                    channel = channel,
                                    michromaFont = michromaFont,
                                    onClick = {
                                        try {
                                            val intent = repository.buildStreamIntent(channel.infohash)
                                            context.startActivity(intent)
                                        } catch (e: ActivityNotFoundException) {
                                            Toast.makeText(
                                                context,
                                                "Could not open Ace Stream player",
                                                Toast.LENGTH_SHORT
                                            ).show()
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
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Status indicator
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    when {
                        channel.isAvailable && channel.availability >= 0.8 -> Color(0xFF00FF88)
                        channel.isAvailable -> Color(0xFFFFAA00)
                        else -> Color(0xFFFF4444)
                    },
                    RoundedCornerShape(4.dp)
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
                verticalAlignment = Alignment.CenterVertically
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
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = quality,
                        fontFamily = michromaFont,
                        fontSize = 6.sp,
                        color = when (quality) {
                            "4K" -> Color(0xFFCE93D8)
                            "FHD" -> Color(0xFF80CBC4)
                            "HD" -> Color(0xFF90CAF9)
                            else -> Color.White.copy(alpha = 0.5f)
                        },
                        fontWeight = FontWeight.Bold
                    )
                }

                // English indicator
                if (AceStreamRepository.isEnglish(channel)) {
                    Text(
                        text = "ENG",
                        fontFamily = michromaFont,
                        fontSize = 6.sp,
                        color = Color(0xFF00FF88),
                        fontWeight = FontWeight.Bold
                    )
                }

                // Language from metadata
                channel.languages?.firstOrNull()?.let { lang ->
                    if (!AceStreamRepository.isEnglish(channel)) {
                        Text(
                            text = lang.uppercase(),
                            fontFamily = michromaFont,
                            fontSize = 7.sp,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                }

                // Category badge
                if (channel.categories.isNotEmpty()) {
                    Text(
                        text = channel.categories.first().uppercase(),
                        fontFamily = michromaFont,
                        fontSize = 7.sp,
                        color = AceStreamOrange.copy(alpha = 0.8f),
                        letterSpacing = 0.5.sp
                    )
                }

                // Availability
                Text(
                    text = "${channel.availabilityPercent}%",
                    fontFamily = michromaFont,
                    fontSize = 7.sp,
                    color = when {
                        channel.availability >= 0.8 -> Color(0xFF00FF88)
                        channel.availability >= 0.5 -> Color(0xFFFFAA00)
                        else -> Color(0xFFFF4444)
                    }
                )
            }
        }

        // Play icon
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Play",
            tint = AceStreamOrange,
            modifier = Modifier.size(24.dp)
        )
    }
}
