package com.f1tracker.ui.screens.feed.components.bento

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.f1tracker.data.models.NewsArticle

@Composable
fun BentoNewsCard(
    article: NewsArticle,
    michromaFont: FontFamily,
    onNewsClick: (String?) -> Unit,
    cardSize: Int = 2 // 1=small, 2=medium, 3=large
) {
    val cardHeight = when (cardSize) {
        1 -> 180.dp
        2 -> 220.dp
        else -> 280.dp
    }
    val imageUrl = article.images?.firstOrNull { it.type == "header" }?.url 
        ?: article.images?.firstOrNull()?.url
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .clickable { onNewsClick(article.links?.web?.href) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.3f to Color.Black.copy(alpha = 0.2f),
                            1f to Color.Black.copy(alpha = 0.95f)
                        )
                    )
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF00D4AA), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "NEWS",
                        fontFamily = michromaFont,
                        fontSize = 9.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text(
                    text = article.headline,
                    fontFamily = michromaFont,
                    fontSize = when (cardSize) { 1 -> 11.sp; 2 -> 12.sp; else -> 14.sp },
                    color = Color.White,
                    maxLines = when (cardSize) { 1 -> 4; 2 -> 5; else -> 6 },
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = when (cardSize) { 1 -> 15.sp; 2 -> 17.sp; else -> 19.sp }
                )
            }
        }
    }
}
