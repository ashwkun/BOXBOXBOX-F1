package com.f1tracker.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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

import androidx.compose.material.icons.filled.ArrowForward

@Composable
fun PodcastsSection(
    podcasts: List<Podcast>,
    currentlyPlayingEpisode: PodcastEpisode?,
    isPlaying: Boolean,
    onEpisodeClick: (PodcastEpisode) -> Unit,
    onPlayPause: () -> Unit,
    onViewMoreClick: () -> Unit,
    onPodcastClick: (Podcast) -> Unit = {},
    selectedPodcast: Podcast? = null
) {
    val brigendsFont = FontFamily(Font(R.font.brigends_expanded, FontWeight.Normal))
    val michromaFont = FontFamily(Font(R.font.michroma, FontWeight.Normal))
    
    if (podcasts.isEmpty()) {
        return
    }
    
    // Flatten to get latest episodes from each podcast for the "Featured" carousel
    val featuredEpisodes = remember(podcasts) {
        podcasts.mapNotNull { podcast ->
            podcast.episodes.firstOrNull() // Get latest episode (already sorted)
        }.sortedByDescending { parseDate(it.publishedDate) }
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
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
                text = "LATEST EPISODES >",
                fontFamily = brigendsFont,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                letterSpacing = 2.sp
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Horizontal scrollable featured episodes
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp), 
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) {
            items(featuredEpisodes) { episode ->
                FeaturedEpisodeCard(
                    episode = episode,
                    isCurrentlyPlaying = currentlyPlayingEpisode?.audioUrl == episode.audioUrl,
                    isPlaying = isPlaying && currentlyPlayingEpisode?.audioUrl == episode.audioUrl,
                    michromaFont = michromaFont,
                    onEpisodeClick = onEpisodeClick,
                    onPlayPause = onPlayPause
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Podcast Channels Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CHANNELS",
                fontFamily = brigendsFont,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                letterSpacing = 2.sp
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Podcast Channels List
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) {
            items(podcasts) { podcast ->
                PodcastChannelCard(
                    podcast = podcast,
                    michromaFont = michromaFont,
                    onPodcastClick = onPodcastClick,
                    isSelected = selectedPodcast?.name == podcast.name
                )
            }
        }
        
        // Inline Episodes Expansion
        AnimatedVisibility(
            visible = selectedPodcast != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            selectedPodcast?.let { podcast ->
                androidx.compose.material3.Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                        .height(450.dp) // Fixed height for internal scrolling
                        .shadow(16.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = Color(0xFF1A1A1A)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Header inside the card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.3f))
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "EPISODES",
                                fontFamily = brigendsFont,
                                fontSize = 12.sp,
                                color = Color(0xFFFF0080),
                                letterSpacing = 2.sp
                            )
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Text(
                                text = podcast.name,
                                fontFamily = michromaFont,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        androidx.compose.material3.Divider(
                            color = Color.White.copy(alpha = 0.1f)
                        )
                        
                        // Virtualized List
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(podcast.episodes) { episode ->
                                PodcastEpisodeRow(
                                    episode = episode,
                                    isCurrentlyPlaying = currentlyPlayingEpisode?.audioUrl == episode.audioUrl,
                                    isPlaying = isPlaying && currentlyPlayingEpisode?.audioUrl == episode.audioUrl,
                                    michromaFont = michromaFont,
                                    onEpisodeClick = onEpisodeClick,
                                    onPlayPause = onPlayPause
                                )
                                
                                if (episode != podcast.episodes.last()) {
                                    androidx.compose.material3.Divider(
                                        modifier = Modifier.padding(horizontal = 20.dp),
                                        color = Color.White.copy(alpha = 0.05f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturedEpisodeCard(
    episode: PodcastEpisode,
    isCurrentlyPlaying: Boolean,
    isPlaying: Boolean,
    michromaFont: FontFamily,
    onEpisodeClick: (PodcastEpisode) -> Unit,
    onPlayPause: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                clip = false
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                if (isCurrentlyPlaying) onPlayPause() else onEpisodeClick(episode)
            }
    ) {
        // Background Image
        AsyncImage(
            model = episode.imageUrl,
            contentDescription = episode.title,
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
                            Color.Black.copy(alpha = 0.9f)
                        ),
                        startY = 100f
                    )
                )
        )
        
        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            // Play Button and Duration
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (isCurrentlyPlaying) Color(0xFFFF0080) else Color.White.copy(alpha = 0.2f),
                            androidx.compose.foundation.shape.CircleShape
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
                
                Spacer(modifier = Modifier.width(12.dp))
                
                if (episode.duration.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = episode.duration,
                            fontFamily = michromaFont,
                            fontSize = 10.sp,
                            color = Color.White
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = episode.title,
                fontFamily = michromaFont,
                fontSize = 14.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PodcastChannelCard(
    podcast: Podcast,
    michromaFont: FontFamily,
    onPodcastClick: (Podcast) -> Unit,
    isSelected: Boolean = false
) {
    Box(
        modifier = Modifier
            .width(140.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) Color(0xFFFF0080) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onPodcastClick(podcast) }
    ) {
        AsyncImage(
            model = podcast.imageUrl,
            contentDescription = podcast.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
        )
        
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        ) {
            Text(
                text = podcast.name,
                fontFamily = michromaFont,
                fontSize = 12.sp,
                color = if (isSelected) Color(0xFFFF0080) else Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = "${podcast.episodes.size} Episodes",
                fontFamily = michromaFont,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun PodcastEpisodeRow(
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
                .size(32.dp)
                .background(
                    if (isCurrentlyPlaying) Color(0xFFFF0080) else Color(0xFF2C2C2E),
                    androidx.compose.foundation.shape.CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isCurrentlyPlaying && isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = "Play",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = episode.title,
                fontFamily = michromaFont,
                fontSize = 12.sp,
                color = if (isCurrentlyPlaying) Color(0xFFFF0080) else Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = parseDate(episode.publishedDate).format(DateTimeFormatter.ofPattern("MMM dd")),
                    fontFamily = michromaFont,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
                
                if (episode.duration.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = episode.duration,
                        fontFamily = michromaFont,
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }
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

