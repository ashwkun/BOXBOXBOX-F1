package com.f1tracker.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.f1tracker.data.models.NewsArticle
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import com.f1tracker.util.NewsCategorizer
import com.f1tracker.util.NewsCategory

val michromaFont = FontFamily(
    Font(R.font.michroma, FontWeight.Normal)
)

val brigendsFont = FontFamily(
    Font(R.font.brigends_expanded, FontWeight.Normal)
)

@Composable
fun NewsCard(
    article: NewsArticle,
    modifier: Modifier = Modifier,
    onNewsClick: (String?) -> Unit = {},
    showTag: Boolean = false,
    showDescription: Boolean = true
) {
    val category = remember(article.headline) { NewsCategorizer.categorize(article.headline) }
    
    // Pulsing animation for Nuclear news
    val infiniteTransition = rememberInfiniteTransition(label = "pulsing")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val borderColor = when (category) {
        NewsCategory.NUCLEAR -> Color(0xFFFF0080).copy(alpha = pulseAlpha)
        NewsCategory.MAJOR -> Color(0xFFFF0080).copy(alpha = 0.6f)
        else -> Color.Transparent
    }

    val borderWidth = when (category) {
        NewsCategory.NUCLEAR -> 2.dp
        NewsCategory.MAJOR -> 1.dp
        else -> 0.dp
    }

    Card(
        modifier = modifier
            .clickable { onNewsClick(article.links?.web?.href) }
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // News image
                val imageUrl = article.images?.firstOrNull { it.type == "header" }?.url 
                    ?: article.images?.firstOrNull()?.url
                
                if (imageUrl != null) {
                    Box {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = article.headline,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        
                        if (showTag) {
                            Box(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .background(Color(0xFFFF0080), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .align(Alignment.TopStart)
                            ) {
                                Text(
                                    text = "ARTICLE",
                                    fontFamily = michromaFont,
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Source Chip (F1/ESPN)
                        val sourceName = when {
                            article.links?.web?.href?.contains("formula1.com") == true -> "F1"
                            article.links?.web?.href?.contains("motorsport.com") == true -> "MOTORSPORT.COM"
                            article.links?.web?.href?.contains("espn") == true -> "ESPN"
                            else -> "NEWS"
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = sourceName,
                                fontFamily = michromaFont,
                                fontSize = 10.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    // Placeholder if no image
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                            .background(Color(0xFF2A2A2A))
                    )
                }
                
                // Content section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Headline
                    Text(
                        text = article.headline,
                        fontFamily = michromaFont,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                    
                    // Description (flexible height)
                    if (showDescription) {
                        Text(
                            text = article.description,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 16.sp
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    
                    // Published date (always visible at bottom)
                    Text(
                        text = formatPublishedDate(article.published),
                        fontFamily = michromaFont,
                        fontSize = 9.sp,
                        color = Color(0xFFFF0080),
                        letterSpacing = 0.5.sp
                    )
                }
            }


        }
    }
}

private fun formatPublishedDate(publishedString: String): String {
    return try {
        val formatter = java.time.format.DateTimeFormatter.ISO_DATE_TIME
        val dateTime = java.time.ZonedDateTime.parse(publishedString, formatter)
        val now = java.time.ZonedDateTime.now()
        
        val hours = java.time.Duration.between(dateTime, now).toHours()
        when {
            hours < 1 -> "Just now"
            hours < 24 -> "${hours}h ago"
            hours < 48 -> "Yesterday"
            else -> {
                val days = hours / 24
                "${days}d ago"
            }
        }
    } catch (e: Exception) {
        "Recently"
    }
}

@Composable
fun NewsSection(
    newsArticles: List<NewsArticle>,
    modifier: Modifier = Modifier,
    onNewsClick: (String?) -> Unit = {},
    onViewMoreClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        // Section header with navigation arrow
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onViewMoreClick() }
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "NEWS >",
                fontFamily = brigendsFont,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                letterSpacing = 2.sp
            )
        }
        
        // Horizontal scrollable news cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            newsArticles.take(3).forEach { article ->
                NewsCard(
                    article = article,
                    onNewsClick = onNewsClick,
                    showDescription = false,
                    modifier = Modifier
                        .width(300.dp)
                        .height(320.dp)
                )
            }
            
            // View More card
            ViewMoreCard(
                onClick = onViewMoreClick
            )
        }
    }
}

@Composable
private fun ViewMoreCard(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(160.dp) // Match other sections
            .height(320.dp) // Match NewsCard height
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A1A),
                        Color(0xFF0A0A0A)
                    )
                )
            )
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            ),
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




