package com.f1tracker.ui.screens.feed.components.bento

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun BentoPodcastCard(
    episode: PodcastEpisode,
    michromaFont: FontFamily,
    onEpisodeClick: (PodcastEpisode) -> Unit,
    cardSize: Int = 2
) {
    val cardHeight = when (cardSize) {
        1 -> 120.dp
        2 -> 160.dp
        else -> 200.dp
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .clickable { onEpisodeClick(episode) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = episode.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.5f to Color.Black.copy(alpha = 0.5f),
                            1f to Color.Black.copy(alpha = 0.9f)
                        )
                    )
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF9B59B6), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "PODCAST",
                            fontFamily = michromaFont,
                            fontSize = 8.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Column {
                    Text(
                        text = episode.title,
                        fontFamily = michromaFont,
                        fontSize = when (cardSize) { 1 -> 8.sp; 2 -> 9.sp; else -> 11.sp },
                        color = Color.White,
                        maxLines = when (cardSize) { 1 -> 2; 2 -> 2; else -> 3 },
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = when (cardSize) { 1 -> 11.sp; 2 -> 13.sp; else -> 15.sp }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = null,
                            tint = Color(0xFF9B59B6),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = episode.duration,
                            fontFamily = michromaFont,
                            fontSize = 8.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}
