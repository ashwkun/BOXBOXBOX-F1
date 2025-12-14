package com.f1tracker.ui.screens.feed.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@Composable
fun EpisodeCard(
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
                            .size(48.dp)
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
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Episode info
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = episode.title,
                    fontFamily = michromaFont,
                    fontSize = 10.sp,
                    color = if (isCurrentlyPlaying) Color(0xFFFF0080) else Color.White,
                    fontWeight = if (isCurrentlyPlaying) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 12.sp
                )
                
                if (episode.duration.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(12.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        Text(
                            text = episode.duration,
                            fontFamily = michromaFont,
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}
