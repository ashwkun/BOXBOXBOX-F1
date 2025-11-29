package com.f1tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun HomePodcastsSection(
    podcasts: List<Podcast>,
    currentlyPlayingEpisode: PodcastEpisode?,
    isPlaying: Boolean,
    onEpisodeClick: (PodcastEpisode) -> Unit,
    onPlayPause: () -> Unit,
    onViewMoreClick: () -> Unit
) {
    val brigendsFont = FontFamily(Font(R.font.brigends_expanded, FontWeight.Normal))
    val michromaFont = FontFamily(Font(R.font.michroma, FontWeight.Normal))
    
    if (podcasts.isEmpty()) {
        return
    }
    
    // Get 2 latest episodes from each podcast and flatten
    val homeEpisodes = remember(podcasts) {
        podcasts.flatMap { podcast ->
            podcast.episodes
                .sortedByDescending { parseDate(it.publishedDate) }
                .take(2)
        }.sortedByDescending { parseDate(it.publishedDate) }
    }
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onViewMoreClick() }
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PODCASTS >",
                fontFamily = brigendsFont,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                letterSpacing = 2.sp
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Horizontal scrollable episodes
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp), // Fixed height for the row
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) {
            items(homeEpisodes) { episode ->
                HomeEpisodeCard(
                    episode = episode,
                    isCurrentlyPlaying = currentlyPlayingEpisode?.audioUrl == episode.audioUrl,
                    isPlaying = isPlaying && currentlyPlayingEpisode?.audioUrl == episode.audioUrl,
                    michromaFont = michromaFont,
                    onEpisodeClick = onEpisodeClick,
                    onPlayPause = onPlayPause
                )
            }
            
            item {
                ViewMoreCard(
                    onClick = onViewMoreClick,
                    michromaFont = michromaFont
                )
            }
        }
    }
}

@Composable
private fun HomeEpisodeCard(
    episode: PodcastEpisode,
    isCurrentlyPlaying: Boolean,
    isPlaying: Boolean,
    michromaFont: FontFamily,
    onEpisodeClick: (PodcastEpisode) -> Unit,
    onPlayPause: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(160.dp)
            .fillMaxHeight()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(12.dp),
                clip = false
            )
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isCurrentlyPlaying) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2C1C2E),
                            Color(0xFF1C1315)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1C1C1E),
                            Color(0xFF131315)
                        )
                    )
                }
            )
            .clickable {
                if (isCurrentlyPlaying) {
                    onPlayPause()
                } else {
                    onEpisodeClick(episode)
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Episode artwork
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = episode.imageUrl,
                    contentDescription = episode.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Play button overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (isCurrentlyPlaying) {
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFFFF0080),
                                            Color(0xFFE6007E)
                                        )
                                    )
                                } else {
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF2C2C2E),
                                            Color(0xFF1C1C1E)
                                        )
                                    )
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCurrentlyPlaying && isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isCurrentlyPlaying && isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Episode info
            Text(
                text = episode.title,
                fontFamily = michromaFont,
                fontSize = 10.sp,
                color = if (isCurrentlyPlaying) Color(0xFFFF0080) else Color.White,
                fontWeight = if (isCurrentlyPlaying) FontWeight.Bold else FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 12.sp
            )
        }
    }
}

@Composable
private fun ViewMoreCard(
    onClick: () -> Unit,
    michromaFont: FontFamily
) {
    Box(
        modifier = Modifier
            .width(160.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A1A),
                        Color(0xFF0A0A0A)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Color(0xFFFF0080).copy(alpha = 0.1f),
                        androidx.compose.foundation.shape.CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = "View More",
                    tint = Color(0xFFFF0080),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "View All",
                fontFamily = michromaFont,
                fontSize = 12.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun parseDate(dateString: String): ZonedDateTime {
    return try {
        ZonedDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME)
    } catch (e: Exception) {
        try {
            // Try RFC 1123 (common for RSS/Podcasts)
            ZonedDateTime.parse(dateString, DateTimeFormatter.RFC_1123_DATE_TIME)
        } catch (e2: Exception) {
            ZonedDateTime.now().minusYears(1) // Fallback
        }
    }
}
