package com.f1tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    onNewsClick: (String?) -> Unit = {}
) {
    Card(
        modifier = modifier
            .width(300.dp)
            .height(320.dp)
            .clickable { onNewsClick(article.links?.web?.href) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // News image
            val imageUrl = article.images?.firstOrNull { it.type == "header" }?.url 
                ?: article.images?.firstOrNull()?.url
            
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = article.headline,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Placeholder if no image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        .background(Color(0xFF2A2A2A))
                )
            }
            
            // Content section
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Headline
                Text(
                    text = article.headline,
                    fontFamily = michromaFont,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
                
                // Description
                Text(
                    text = article.description,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp
                )
            }
        }
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
                text = "LATEST NEWS",
                fontFamily = brigendsFont,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.6f),
                letterSpacing = 2.sp
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = ">",
                fontFamily = brigendsFont,
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.6f)
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
                    onNewsClick = onNewsClick
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
    Card(
        modifier = Modifier
            .width(300.dp)
            .height(320.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "VIEW MORE",
                    fontFamily = brigendsFont,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.5.sp
                )
                
                Text(
                    text = ">",
                    fontFamily = brigendsFont,
                    fontSize = 40.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

