package com.f1tracker.ui.screens.feed.components.bento

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
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
import com.f1tracker.data.models.InstagramPost

@Composable
fun BentoInstagramCard(
    post: InstagramPost,
    michromaFont: FontFamily,
    onClick: () -> Unit,
    cardSize: Int = 2
) {
    val cardHeight = when (cardSize) {
        1 -> 160.dp
        2 -> 200.dp
        else -> 260.dp
    }
    val isVideo = post.media_type == "VIDEO"
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val imageUrl = if (isVideo) post.thumbnail_url ?: post.media_url else post.media_url
            
            AsyncImage(
                model = imageUrl,
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
                            0.6f to Color.Black.copy(alpha = 0.3f),
                            1f to Color.Black.copy(alpha = 0.8f)
                        )
                    )
            )
            
            if (isVideo) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = "Play",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier
                        .size(if (cardSize == 3) 44.dp else 32.dp)
                        .align(Alignment.Center)
                )
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFFF0080), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isVideo) "REEL" else "SOCIAL",
                        fontFamily = michromaFont,
                        fontSize = 8.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (!post.caption.isNullOrEmpty()) {
                    Text(
                        text = post.caption,
                        fontFamily = michromaFont,
                        fontSize = when (cardSize) { 1 -> 8.sp; 2 -> 9.sp; else -> 10.sp },
                        color = Color.White,
                        maxLines = when (cardSize) { 1 -> 2; 2 -> 2; else -> 3 },
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = when (cardSize) { 1 -> 11.sp; 2 -> 13.sp; else -> 14.sp }
                    )
                }
            }
        }
    }
}
