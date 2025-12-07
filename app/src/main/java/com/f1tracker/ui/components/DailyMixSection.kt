package com.f1tracker.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import com.f1tracker.data.models.NewsArticle
import com.f1tracker.data.models.F1Video
import com.f1tracker.data.models.InstagramPost
import com.f1tracker.data.models.Podcast
import com.f1tracker.data.models.PodcastEpisode

// Design tokens
private val newsAccent = Color(0xFFFF0080)    // Pink
private val gameAccent = Color(0xFFFF0000)    // Red  
private val socialAccent = Color(0xFFE1306C)  // Instagram pink
private val podcastAccent = Color(0xFF1DB954) // Spotify green
private val videoAccent = Color(0xFFFF0000)   // YouTube red

@Composable
fun DailyMixSection(
    newsArticles: List<NewsArticle>,
    videos: List<F1Video>,
    socialPosts: List<InstagramPost>,
    podcasts: List<Podcast>,
    onNewsClick: (String?) -> Unit,
    onVideoClick: (String) -> Unit,
    onSocialClick: (String) -> Unit,
    onGameClick: () -> Unit,
    onPodcastClick: (PodcastEpisode) -> Unit
) {
    val michromaFont = FontFamily(Font(R.font.michroma, FontWeight.Normal))
    val brigendsFont = FontFamily(Font(R.font.brigends_expanded, FontWeight.Normal))
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        // Section Header
        Text(
            text = "DAILY MIX",
            fontFamily = brigendsFont,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.6f),
            letterSpacing = 2.sp,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // ROW 1: Static Bento Grid
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Left: Featured News (60%)
            BentoCardV2(
                modifier = Modifier
                    .weight(1.4f)
                    .fillMaxHeight(),
                onClick = { newsArticles.getOrNull(0)?.links?.web?.href?.let { onNewsClick(it) } },
                accentColor = newsAccent
            ) {
                val newsItem = newsArticles.getOrNull(0)
                if (newsItem != null) {
                    val imageUrl = newsItem.images?.firstOrNull()?.url
                    if (imageUrl != null) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    GradientOverlayV2()
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        ContentBadge(text = "HEADLINE", color = newsAccent)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = newsItem.headline,
                            fontFamily = michromaFont,
                            fontSize = 13.sp,
                            color = Color.White,
                            lineHeight = 17.sp,
                            maxLines = 4,
                            fontWeight = FontWeight.Bold,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Right: 2x2 Grid (40%)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Top row: Game + Social
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Game Card
                    BentoCardV2(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = onGameClick,
                        accentColor = gameAccent
                    ) {
                        Image(
                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.game_hotlap),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.5f)))
                        
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsEsports,
                                contentDescription = null,
                                tint = gameAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "PLAY",
                                fontFamily = brigendsFont,
                                fontSize = 9.sp,
                                color = Color.White
                            )
                        }
                    }

                    // Social Card
                    val socialItem = socialPosts.getOrNull(0)
                    BentoCardV2(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = { socialItem?.let { onSocialClick(it.permalink) } },
                        accentColor = socialAccent
                    ) {
                        if (socialItem != null) {
                            val img = socialItem.thumbnail_url ?: socialItem.media_url
                            if (img != null) {
                                AsyncImage(
                                    model = img,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.3f)))
                            
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Bottom: Podcast
                val latestPodcast = podcasts.firstOrNull { it.episodes.isNotEmpty() }
                val latestEpisode = latestPodcast?.episodes?.firstOrNull()
                
                BentoCardV2(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    onClick = { latestEpisode?.let { onPodcastClick(it) } },
                    accentColor = podcastAccent
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Podcast icon
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(podcastAccent.copy(0.2f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Headphones,
                                contentDescription = null,
                                tint = podcastAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(10.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = latestEpisode?.title ?: "Podcast",
                                fontFamily = michromaFont,
                                fontSize = 9.sp,
                                color = Color.White,
                                maxLines = 2,
                                lineHeight = 11.sp,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = latestPodcast?.name ?: "",
                                fontFamily = michromaFont,
                                fontSize = 7.sp,
                                color = Color.White.copy(0.5f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ROW 2: Horizontal Scroll
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Video 1
            if (videos.isNotEmpty()) {
                item {
                    VideoCardV2(
                        video = videos[0],
                        onVideoClick = onVideoClick,
                        michromaFont = michromaFont,
                        brigendsFont = brigendsFont
                    )
                }
            }

            // News 2
            if (newsArticles.size > 1) {
                item {
                    NewsCardV2(
                        newsItem = newsArticles[1],
                        onNewsClick = onNewsClick,
                        michromaFont = michromaFont,
                        brigendsFont = brigendsFont
                    )
                }
            }

            // Social 2
            if (socialPosts.size > 1) {
                item {
                    SocialCardV2(
                        post = socialPosts[1],
                        onSocialClick = onSocialClick,
                        michromaFont = michromaFont
                    )
                }
            }

            // Video 2
            if (videos.size > 1) {
                item {
                    VideoCardV2(
                        video = videos[1],
                        onVideoClick = onVideoClick,
                        michromaFont = michromaFont,
                        brigendsFont = brigendsFont
                    )
                }
            }

            // Social 3
            if (socialPosts.size > 2) {
                item {
                    SocialCardV2(
                        post = socialPosts[2],
                        onSocialClick = onSocialClick,
                        michromaFont = michromaFont
                    )
                }
            }
        }
    }
}

// ============== V2 Card Components ==============

@Composable
private fun BentoCardV2(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    accentColor: Color = Color.White.copy(0.1f),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0A0A0A))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.3f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() },
        content = content
    )
}

@Composable
private fun GradientOverlayV2() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.3f),
                        Color.Black.copy(alpha = 0.85f)
                    ),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
    )
}

@Composable
private fun ContentBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun VideoCardV2(
    video: F1Video,
    onVideoClick: (String) -> Unit,
    michromaFont: FontFamily,
    brigendsFont: FontFamily
) {
    BentoCardV2(
        modifier = Modifier
            .width(160.dp)
            .height(130.dp),
        onClick = { onVideoClick(video.videoId) },
        accentColor = videoAccent
    ) {
        AsyncImage(
            model = video.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        GradientOverlayV2()
        
        // Play button
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White.copy(0.2f), CircleShape)
                    .border(1.dp, Color.White.copy(0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        // Title
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            ContentBadge(text = "VIDEO", color = videoAccent)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = video.title,
                fontFamily = michromaFont,
                fontSize = 9.sp,
                color = Color.White,
                maxLines = 2,
                lineHeight = 11.sp,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun NewsCardV2(
    newsItem: NewsArticle,
    onNewsClick: (String?) -> Unit,
    michromaFont: FontFamily,
    brigendsFont: FontFamily
) {
    BentoCardV2(
        modifier = Modifier
            .width(160.dp)
            .height(130.dp),
        onClick = { onNewsClick(newsItem.links?.web?.href) },
        accentColor = newsAccent
    ) {
        val imageUrl = newsItem.images?.firstOrNull()?.url
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        GradientOverlayV2()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            ContentBadge(text = "NEWS", color = newsAccent)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = newsItem.headline,
                fontFamily = michromaFont,
                fontSize = 9.sp,
                color = Color.White,
                maxLines = 3,
                lineHeight = 11.sp,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SocialCardV2(
    post: InstagramPost,
    onSocialClick: (String) -> Unit,
    michromaFont: FontFamily
) {
    BentoCardV2(
        modifier = Modifier
            .width(130.dp)
            .height(130.dp),
        onClick = { onSocialClick(post.permalink) },
        accentColor = socialAccent
    ) {
        val img = post.thumbnail_url ?: post.media_url
        if (img != null) {
            AsyncImage(
                model = img,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.2f)))
        
        // Instagram badge
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.TopStart
        ) {
            ContentBadge(text = "@${post.author}", color = socialAccent)
        }
        
        // Video indicator if VIDEO
        if (post.media_type == "VIDEO") {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.White.copy(0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// Keep old components for backward compatibility (can be removed later)
@Composable
fun BentoCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF121212))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .clickable { onClick() },
        content = content
    )
}

@Composable
fun GradientOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.5f),
                        Color.Black.copy(alpha = 0.9f)
                    )
                )
            )
    )
}

@Composable
fun PlayButton() {
     Box(
        modifier = Modifier
            .size(32.dp)
            .background(Color.White.copy(alpha = 0.2f), CircleShape)
            .border(1.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Play",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}
