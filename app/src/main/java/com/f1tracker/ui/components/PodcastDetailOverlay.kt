package com.f1tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.f1tracker.R
import com.f1tracker.data.models.Podcast
import com.f1tracker.data.models.PodcastEpisode

@Composable
fun PodcastDetailOverlay(
    podcast: Podcast,
    onClose: () -> Unit,
    currentlyPlayingEpisode: PodcastEpisode?,
    isPlaying: Boolean,
    onEpisodeClick: (PodcastEpisode) -> Unit,
    onPlayPause: () -> Unit
) {
    val michromaFont = FontFamily(Font(R.font.michroma, FontWeight.Normal))
    val brigendsFont = FontFamily(Font(R.font.brigends_expanded, FontWeight.Normal))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(enabled = false) {} // Consume clicks
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Header Image
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    AsyncImage(
                        model = podcast.imageUrl,
                        contentDescription = podcast.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Gradient Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black
                                    ),
                                    startY = 100f
                                )
                            )
                    )
                    
                    // Close Button
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .size(40.dp)
                            .background(Color.Black.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape)
                            .clickable { onClose() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                    
                    // Podcast Info
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "PODCAST",
                            fontFamily = brigendsFont,
                            fontSize = 12.sp,
                            color = Color(0xFFFF0080),
                            letterSpacing = 2.sp
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = podcast.name,
                            fontFamily = michromaFont,
                            fontSize = 24.sp,
                            color = Color.White,
                            lineHeight = 32.sp
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "${podcast.episodes.size} Episodes",
                            fontFamily = michromaFont,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            // Episodes List
            items(podcast.episodes) { episode ->
                EpisodeRow(
                    episode = episode,
                    isCurrentlyPlaying = currentlyPlayingEpisode?.audioUrl == episode.audioUrl,
                    isPlaying = isPlaying && currentlyPlayingEpisode?.audioUrl == episode.audioUrl,
                    michromaFont = michromaFont,
                    onEpisodeClick = onEpisodeClick,
                    onPlayPause = onPlayPause
                )
            }
        }
    }
}

@Composable
private fun EpisodeRow(
    episode: PodcastEpisode,
    isCurrentlyPlaying: Boolean,
    isPlaying: Boolean,
    michromaFont: FontFamily,
    onEpisodeClick: (PodcastEpisode) -> Unit,
    onPlayPause: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                if (isCurrentlyPlaying) onPlayPause() else onEpisodeClick(episode)
            }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Play Button
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    if (isCurrentlyPlaying) Color(0xFFFF0080) else Color(0xFF1A1A1A),
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isCurrentlyPlaying && isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = "Play",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = episode.title,
                fontFamily = michromaFont,
                fontSize = 14.sp,
                color = if (isCurrentlyPlaying) Color(0xFFFF0080) else Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatPublishedDate(episode.publishedDate),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                
                if (episode.duration.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = episode.duration,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
    
    androidx.compose.material3.Divider(
        color = Color.White.copy(alpha = 0.05f),
        thickness = 1.dp,
        modifier = Modifier.padding(horizontal = 20.dp)
    )
}

// Helper function for date formatting (duplicated from FeedScreen, could be moved to utils)
private fun formatPublishedDate(dateString: String): String {
    return try {
        val date = java.time.ZonedDateTime.parse(dateString, java.time.format.DateTimeFormatter.ISO_DATE_TIME)
        val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy")
        date.format(formatter)
    } catch (e: Exception) {
        try {
            val date = java.time.ZonedDateTime.parse(dateString, java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME)
            val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy")
            date.format(formatter)
        } catch (e2: Exception) {
            "Unknown Date"
        }
    }
}
