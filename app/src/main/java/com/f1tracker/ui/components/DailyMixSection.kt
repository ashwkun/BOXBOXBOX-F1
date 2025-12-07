package com.f1tracker.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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

// Premium Design Tokens
private val accentPink = Color(0xFFFF0080)
private val accentRed = Color(0xFFE10600)
private val accentInstagram = Color(0xFFE1306C)
private val accentSpotify = Color(0xFF1DB954)
private val accentYouTube = Color(0xFFFF0000)
private val cardBg = Color(0xFF0D0D0D)
private val glassWhite = Color.White.copy(alpha = 0.08f)

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
    
    Column(modifier = Modifier.fillMaxWidth()) {
        // Section Header with glow effect
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DAILY MIX",
                fontFamily = brigendsFont,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.7f),
                letterSpacing = 3.sp
            )
        }

        // Fully Scrollable Premium Cards
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. HERO NEWS CARD (Large)
            if (newsArticles.isNotEmpty()) {
                item {
                    HeroNewsCard(
                        newsItem = newsArticles[0],
                        onNewsClick = onNewsClick,
                        michromaFont = michromaFont,
                        brigendsFont = brigendsFont
                    )
                }
            }

            // 2. GAME CARD (Square, punchy)
            item {
                GameCard(
                    onGameClick = onGameClick,
                    michromaFont = michromaFont,
                    brigendsFont = brigendsFont
                )
            }

            // 3. VIDEO CARD (Wide)
            if (videos.isNotEmpty()) {
                item {
                    VideoCard(
                        video = videos[0],
                        onVideoClick = onVideoClick,
                        michromaFont = michromaFont,
                        brigendsFont = brigendsFont
                    )
                }
            }

            // 4. SOCIAL CARD (Square with gradient)
            if (socialPosts.isNotEmpty()) {
                item {
                    SocialCard(
                        post = socialPosts[0],
                        onSocialClick = onSocialClick,
                        michromaFont = michromaFont
                    )
                }
            }

            // 5. PODCAST CARD (Wide, dark)
            val latestPodcast = podcasts.firstOrNull { it.episodes.isNotEmpty() }
            val latestEpisode = latestPodcast?.episodes?.firstOrNull()
            if (latestEpisode != null) {
                item {
                    PodcastCard(
                        episode = latestEpisode,
                        podcastName = latestPodcast.name,
                        onPodcastClick = onPodcastClick,
                        michromaFont = michromaFont,
                        brigendsFont = brigendsFont
                    )
                }
            }

            // 6. NEWS #2 (Medium)
            if (newsArticles.size > 1) {
                item {
                    NewsCard(
                        newsItem = newsArticles[1],
                        onNewsClick = onNewsClick,
                        michromaFont = michromaFont,
                        brigendsFont = brigendsFont
                    )
                }
            }

            // 7. SOCIAL #2
            if (socialPosts.size > 1) {
                item {
                    SocialCard(
                        post = socialPosts[1],
                        onSocialClick = onSocialClick,
                        michromaFont = michromaFont
                    )
                }
            }

            // 8. VIDEO #2
            if (videos.size > 1) {
                item {
                    VideoCard(
                        video = videos[1],
                        onVideoClick = onVideoClick,
                        michromaFont = michromaFont,
                        brigendsFont = brigendsFont
                    )
                }
            }
        }
    }
}

// ============== PREMIUM CARD COMPONENTS ==============

@Composable
private fun HeroNewsCard(
    newsItem: NewsArticle,
    onNewsClick: (String?) -> Unit,
    michromaFont: FontFamily,
    brigendsFont: FontFamily
) {
    Box(
        modifier = Modifier
            .width(280.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .clickable { onNewsClick(newsItem.links?.web?.href) }
    ) {
        // Background Image
        val imageUrl = newsItem.images?.firstOrNull()?.url
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.95f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // Accent line at top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(accentPink, accentPink.copy(alpha = 0f))
                    )
                )
                .align(Alignment.TopStart)
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            // Badge
            Box(
                modifier = Modifier
                    .background(accentPink, RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "BREAKING",
                    fontFamily = brigendsFont,
                    fontSize = 9.sp,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = newsItem.headline,
                fontFamily = michromaFont,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                lineHeight = 18.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "MOTORSPORT.COM",
                fontFamily = michromaFont,
                fontSize = 9.sp,
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun GameCard(
    onGameClick: () -> Unit,
    michromaFont: FontFamily,
    brigendsFont: FontFamily
) {
    Box(
        modifier = Modifier
            .size(140.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF1A0A0A), Color(0xFF2D0A0A)),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(accentRed.copy(0.5f), accentRed.copy(0.1f))
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onGameClick() }
    ) {
        // Background image
        Image(
            painter = androidx.compose.ui.res.painterResource(id = R.drawable.game_hotlap),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.3f },
            contentScale = ContentScale.Crop
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated glow icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(accentRed.copy(0.2f), CircleShape)
                    .border(2.dp, accentRed.copy(0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = accentRed,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "HOT LAP",
                fontFamily = brigendsFont,
                fontSize = 12.sp,
                color = Color.White,
                letterSpacing = 2.sp
            )

            Text(
                text = "PLAY NOW",
                fontFamily = michromaFont,
                fontSize = 8.sp,
                color = accentRed,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun VideoCard(
    video: F1Video,
    onVideoClick: (String) -> Unit,
    michromaFont: FontFamily,
    brigendsFont: FontFamily
) {
    Box(
        modifier = Modifier
            .width(200.dp)
            .height(140.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .clickable { onVideoClick(video.videoId) }
    ) {
        AsyncImage(
            model = video.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Dark overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.2f),
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
        )

        // Play button (center)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White.copy(0.15f), CircleShape)
                    .border(2.dp, Color.White.copy(0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // YouTube badge + title
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // YouTube badge
            Box(
                modifier = Modifier
                    .background(accentYouTube, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "VIDEO",
                    fontFamily = brigendsFont,
                    fontSize = 8.sp,
                    color = Color.White
                )
            }

            Text(
                text = video.title,
                fontFamily = michromaFont,
                fontSize = 10.sp,
                color = Color.White,
                maxLines = 2,
                lineHeight = 13.sp,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SocialCard(
    post: InstagramPost,
    onSocialClick: (String) -> Unit,
    michromaFont: FontFamily
) {
    Box(
        modifier = Modifier
            .size(140.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        accentInstagram.copy(0.4f),
                        Color(0xFFFFDC80).copy(0.3f),
                        accentInstagram.copy(0.2f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onSocialClick(post.permalink) }
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

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        )

        // Video play indicator
        if (post.media_type == "VIDEO") {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White.copy(0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Author badge
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "@${post.author}",
                fontFamily = michromaFont,
                fontSize = 9.sp,
                color = Color.White
            )
            if (post.author == "f1") {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = null,
                    tint = Color(0xFF1DA1F2),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun PodcastCard(
    episode: PodcastEpisode,
    podcastName: String,
    onPodcastClick: (PodcastEpisode) -> Unit,
    michromaFont: FontFamily,
    brigendsFont: FontFamily
) {
    Box(
        modifier = Modifier
            .width(200.dp)
            .height(100.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF0A1A0A),
                        Color(0xFF0D2D0D)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(accentSpotify.copy(0.4f), accentSpotify.copy(0.1f))
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onPodcastClick(episode) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(accentSpotify.copy(0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Headphones,
                    contentDescription = null,
                    tint = accentSpotify,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "PODCAST",
                    fontFamily = brigendsFont,
                    fontSize = 8.sp,
                    color = accentSpotify,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = episode.title,
                    fontFamily = michromaFont,
                    fontSize = 10.sp,
                    color = Color.White,
                    maxLines = 2,
                    lineHeight = 13.sp,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = podcastName,
                    fontFamily = michromaFont,
                    fontSize = 8.sp,
                    color = Color.White.copy(0.5f)
                )
            }
        }
    }
}

@Composable
private fun NewsCard(
    newsItem: NewsArticle,
    onNewsClick: (String?) -> Unit,
    michromaFont: FontFamily,
    brigendsFont: FontFamily
) {
    Box(
        modifier = Modifier
            .width(180.dp)
            .height(140.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .clickable { onNewsClick(newsItem.links?.web?.href) }
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.9f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .background(accentPink.copy(0.9f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "NEWS",
                    fontFamily = brigendsFont,
                    fontSize = 8.sp,
                    color = Color.White
                )
            }

            Text(
                text = newsItem.headline,
                fontFamily = michromaFont,
                fontSize = 10.sp,
                color = Color.White,
                maxLines = 3,
                lineHeight = 13.sp,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// Backward compatibility
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
