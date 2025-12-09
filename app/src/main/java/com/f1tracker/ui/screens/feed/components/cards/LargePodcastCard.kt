package com.f1tracker.ui.screens.feed.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.f1tracker.data.models.PodcastEpisode
import com.f1tracker.ui.screens.feed.util.formatPublishedDate

@Composable
fun LargePodcastCard(
    episode: PodcastEpisode,
    isCurrentlyPlaying: Boolean,
    isPlaying: Boolean,
    michromaFont: FontFamily,
    onEpisodeClick: (PodcastEpisode) -> Unit,
    onPlayPause: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                if (isCurrentlyPlaying) onPlayPause() else onEpisodeClick(episode)
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        )
    ) {
        Column {
            // 200dp Height Image Area with Blurred Background + Square Art
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                // Blurred Background (Zoomed in)
                AsyncImage(
                    model = episode.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            drawContent()
                            drawRect(Color.Black.copy(alpha = 0.7f)) // Dark overlay
                        },
                    contentScale = ContentScale.Crop
                )
                
                // Content Row
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Square Artwork with Play Button
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .shadow(8.dp, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = episode.imageUrl,
                            contentDescription = episode.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        
                        // Play Overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        if (isCurrentlyPlaying) {
                                            Brush.linearGradient(
                                                colors = listOf(Color(0xFFFF0080), Color(0xFFE6007E))
                                            )
                                        } else {
                                            Brush.linearGradient(
                                                colors = listOf(Color(0xFF2C2C2E), Color(0xFF1C1C1E))
                                            )
                                        },
                                        shape = CircleShape
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
                    
                    Spacer(modifier = Modifier.width(16.dp))
                }

                // Tag
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .background(Color(0xFFFF0080), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "PODCAST",
                        fontFamily = michromaFont,
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Duration badge
                if (episode.duration.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = episode.duration,
                            fontSize = 10.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = episode.title,
                    fontFamily = michromaFont,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
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
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
