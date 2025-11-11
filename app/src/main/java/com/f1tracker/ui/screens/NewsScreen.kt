package com.f1tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import com.f1tracker.ui.viewmodels.HomeViewModel

@Composable
fun NewsScreen(
    viewModel: HomeViewModel = remember { HomeViewModel.getInstance() },
    onNewsClick: (String?) -> Unit = {}
) {
    val michromaFont = FontFamily(Font(R.font.michroma, FontWeight.Normal))
    val brigendsFont = FontFamily(Font(R.font.brigends_expanded, FontWeight.Normal))
    
    val newsArticles by viewModel.newsArticles.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Header
        Text(
            text = "F1 NEWS",
            fontFamily = brigendsFont,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(20.dp)
        )
        
        // News List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(newsArticles) { article ->
                FullNewsCard(
                    article = article,
                    michromaFont = michromaFont,
                    onNewsClick = onNewsClick
                )
            }
        }
    }
}

@Composable
private fun FullNewsCard(
    article: NewsArticle,
    michromaFont: FontFamily,
    onNewsClick: (String?) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
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
            modifier = Modifier.fillMaxWidth()
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
                        .height(200.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentScale = ContentScale.Crop
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
                
                // Description
                Text(
                    text = article.description,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
                
                // Published date
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

