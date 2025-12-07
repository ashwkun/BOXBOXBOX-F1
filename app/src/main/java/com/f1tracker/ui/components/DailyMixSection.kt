package com.f1tracker.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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

// Design Tokens
private val accentPink = Color(0xFFFF0080)
private val accentRed = Color(0xFFE10600)
private val accentInstagram = Color(0xFFE1306C)
private val accentSpotify = Color(0xFF1DB954)
private val accentYouTube = Color(0xFFFF0000)
private val accentNews = Color(0xFF00D2BE)
private val cardBg = Color(0xFF0D0D0D)
private val gridHeight = 280.dp  // Increased from 240dp
private val cardGap = 12.dp
private val cornerRadius = 18.dp

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
        // Section Header
        Text(
            text = "DAILY MIX",
            fontFamily = brigendsFont,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.6f),
            letterSpacing = 3.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )

        // Bento Grid (2 rows, horizontal scroll)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeight),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ═══════════════════════════════════════════════════════════
            // BLOCK 1: Hero News (tall)
            // ═══════════════════════════════════════════════════════════
            if (newsArticles.isNotEmpty()) {
                item {
                    BentoCard(
                        modifier = Modifier.width(200.dp).fillMaxHeight(),
                        onClick = { onNewsClick(newsArticles[0].links?.web?.href) },
                        accentColor = accentPink
                    ) {
                        val imageUrl = newsArticles[0].images?.firstOrNull()?.url
                        if (imageUrl != null) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        GradientOverlay()
                        
                        // Icon badge
                        IconBadge(
                            icon = Icons.Default.Article,
                            color = accentPink,
                            modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
                        )
                        
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Text(
                                text = newsArticles[0].headline,
                                fontFamily = michromaFont,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                lineHeight = 16.sp,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════
            // BLOCK 2: 2x2 Grid (Game, Social, Video, Podcast)
            // ═══════════════════════════════════════════════════════════
            item {
                Column(
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(cardGap)
                ) {
                    // Top row
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(cardGap)
                    ) {
                        // Game Card
                        BentoCard(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onClick = onGameClick,
                            accentColor = accentRed
                        ) {
                            Image(
                                painter = androidx.compose.ui.res.painterResource(id = R.drawable.game_hotlap),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.4f },
                                contentScale = ContentScale.Crop
                            )
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.4f)))
                            
                            IconBadge(
                                icon = Icons.Default.SportsEsports,
                                color = accentRed,
                                modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                            )
                            
                            Column(
                                modifier = Modifier.fillMaxSize().padding(bottom = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                Text(
                                    text = "HOT LAP",
                                    fontFamily = brigendsFont,
                                    fontSize = 10.sp,
                                    color = Color.White
                                )
                            }
                        }

                        // Social Card
                        val social1 = socialPosts.getOrNull(0)
                        BentoCard(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onClick = { social1?.let { onSocialClick(it.permalink) } },
                            accentColor = accentInstagram
                        ) {
                            if (social1 != null) {
                                val img = social1.thumbnail_url ?: social1.media_url
                                if (img != null) {
                                    AsyncImage(
                                        model = img,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.3f)))
                                
                                IconBadge(
                                    icon = Icons.Default.Public,
                                    color = accentInstagram,
                                    modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                                )
                                
                                if (social1.media_type == "VIDEO") {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        PlayButtonSmall()
                                    }
                                }
                            }
                        }
                    }

                    // Bottom row
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(cardGap)
                    ) {
                        // Video Card
                        val video1 = videos.getOrNull(0)
                        BentoCard(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onClick = { video1?.let { onVideoClick(it.videoId) } },
                            accentColor = accentYouTube
                        ) {
                            if (video1 != null) {
                                AsyncImage(
                                    model = video1.thumbnailUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.3f)))
                                
                                IconBadge(
                                    icon = Icons.Default.PlayCircle,
                                    color = accentYouTube,
                                    modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                                )
                                
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    PlayButtonSmall()
                                }
                            }
                        }

                        // Podcast Card
                        val latestPodcast = podcasts.firstOrNull { it.episodes.isNotEmpty() }
                        val latestEpisode = latestPodcast?.episodes?.firstOrNull()
                        
                        BentoCard(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onClick = { latestEpisode?.let { onPodcastClick(it) } },
                            accentColor = accentSpotify
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(Color(0xFF0A1A0A), Color(0xFF0F2510))
                                        )
                                    )
                            )
                            
                            IconBadge(
                                icon = Icons.Default.Headphones,
                                color = accentSpotify,
                                modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                            )
                            
                            Column(
                                modifier = Modifier.fillMaxSize().padding(10.dp),
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                Text(
                                    text = latestEpisode?.title ?: "Podcast",
                                    fontFamily = michromaFont,
                                    fontSize = 9.sp,
                                    color = Color.White,
                                    maxLines = 2,
                                    lineHeight = 12.sp,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════
            // BLOCK 3: News #2 (Tall)
            // ═══════════════════════════════════════════════════════════
            if (newsArticles.size > 1) {
                item {
                    BentoCard(
                        modifier = Modifier.width(170.dp).fillMaxHeight(),
                        onClick = { onNewsClick(newsArticles[1].links?.web?.href) },
                        accentColor = accentNews
                    ) {
                        val imageUrl = newsArticles[1].images?.firstOrNull()?.url
                        if (imageUrl != null) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        GradientOverlay()
                        
                        IconBadge(
                            icon = Icons.Default.Article,
                            color = accentNews,
                            modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
                        )
                        
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Text(
                                text = newsArticles[1].headline,
                                fontFamily = michromaFont,
                                fontSize = 10.sp,
                                color = Color.White,
                                lineHeight = 14.sp,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════
            // BLOCK 4: 2x1 Stack (Social + Video)
            // ═══════════════════════════════════════════════════════════
            item {
                Column(
                    modifier = Modifier
                        .width(160.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(cardGap)
                ) {
                    // Social #2
                    val social2 = socialPosts.getOrNull(1)
                    BentoCard(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        onClick = { social2?.let { onSocialClick(it.permalink) } },
                        accentColor = accentInstagram
                    ) {
                        if (social2 != null) {
                            val img = social2.thumbnail_url ?: social2.media_url
                            if (img != null) {
                                AsyncImage(
                                    model = img,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.25f)))
                            
                            IconBadge(
                                icon = Icons.Default.Public,
                                color = accentInstagram,
                                modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                            )
                            
                            if (social2.media_type == "VIDEO") {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    PlayButtonSmall()
                                }
                            }
                        }
                    }

                    // Video #2
                    val video2 = videos.getOrNull(1)
                    BentoCard(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        onClick = { video2?.let { onVideoClick(it.videoId) } },
                        accentColor = accentYouTube
                    ) {
                        if (video2 != null) {
                            AsyncImage(
                                model = video2.thumbnailUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.25f)))
                            
                            IconBadge(
                                icon = Icons.Default.PlayCircle,
                                color = accentYouTube,
                                modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                            )
                            
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                PlayButtonSmall()
                            }
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════
            // BLOCK 5: Social #3 (Tall)
            // ═══════════════════════════════════════════════════════════
            if (socialPosts.size > 2) {
                item {
                    val social3 = socialPosts[2]
                    BentoCard(
                        modifier = Modifier.width(150.dp).fillMaxHeight(),
                        onClick = { onSocialClick(social3.permalink) },
                        accentColor = accentInstagram
                    ) {
                        val img = social3.thumbnail_url ?: social3.media_url
                        if (img != null) {
                            AsyncImage(
                                model = img,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.2f)))
                        
                        IconBadge(
                            icon = Icons.Default.Public,
                            color = accentInstagram,
                            modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
                        )
                        
                        if (social3.media_type == "VIDEO") {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                PlayButtonMedium()
                            }
                        }
                        
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "@${social3.author}",
                                fontFamily = michromaFont,
                                fontSize = 9.sp,
                                color = Color.White
                            )
                            if (social3.author == "f1") {
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
            }

            // ═══════════════════════════════════════════════════════════
            // BLOCK 6: News #3 + Video #3 Stack
            // ═══════════════════════════════════════════════════════════
            if (newsArticles.size > 2 || videos.size > 2) {
                item {
                    Column(
                        modifier = Modifier
                            .width(160.dp)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(cardGap)
                    ) {
                        // News #3
                        if (newsArticles.size > 2) {
                            val news3 = newsArticles[2]
                            BentoCard(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                onClick = { onNewsClick(news3.links?.web?.href) },
                                accentColor = accentPink
                            ) {
                                val imageUrl = news3.images?.firstOrNull()?.url
                                if (imageUrl != null) {
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                GradientOverlay()
                                
                                IconBadge(
                                    icon = Icons.Default.Article,
                                    color = accentPink,
                                    modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                                )
                            }
                        }

                        // Video #3
                        if (videos.size > 2) {
                            val video3 = videos[2]
                            BentoCard(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                onClick = { onVideoClick(video3.videoId) },
                                accentColor = accentYouTube
                            ) {
                                AsyncImage(
                                    model = video3.thumbnailUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.25f)))
                                
                                IconBadge(
                                    icon = Icons.Default.PlayCircle,
                                    color = accentYouTube,
                                    modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                                )
                                
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    PlayButtonSmall()
                                }
                            }
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════
            // BLOCK 7: Social #4 (Tall)
            // ═══════════════════════════════════════════════════════════
            if (socialPosts.size > 3) {
                item {
                    val social4 = socialPosts[3]
                    BentoCard(
                        modifier = Modifier.width(150.dp).fillMaxHeight(),
                        onClick = { onSocialClick(social4.permalink) },
                        accentColor = accentInstagram
                    ) {
                        val img = social4.thumbnail_url ?: social4.media_url
                        if (img != null) {
                            AsyncImage(
                                model = img,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.2f)))
                        
                        IconBadge(
                            icon = Icons.Default.Public,
                            color = accentInstagram,
                            modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
                        )
                        
                        if (social4.media_type == "VIDEO") {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                PlayButtonMedium()
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// COMPONENTS
// ═══════════════════════════════════════════════════════════

@Composable
private fun BentoCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    accentColor: Color,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(cardBg)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(accentColor.copy(0.35f), Color.White.copy(0.05f))
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .clickable { onClick() },
        content = content
    )
}

@Composable
private fun IconBadge(
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(28.dp)
            .background(color.copy(0.85f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun PlayButtonSmall() {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(Color.White.copy(0.2f), CircleShape)
            .border(1.dp, Color.White.copy(0.4f), CircleShape),
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

@Composable
private fun PlayButtonMedium() {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(Color.White.copy(0.15f), CircleShape)
            .border(2.dp, Color.White.copy(0.3f), CircleShape),
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

@Composable
private fun GradientOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.2f),
                        Color.Black.copy(alpha = 0.85f)
                    )
                )
            )
    )
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
fun GradientOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
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
