package com.f1tracker.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.f1tracker.R
import com.f1tracker.data.models.NewsArticle
import com.f1tracker.data.models.F1Video
import com.f1tracker.data.models.InstagramPost
import com.f1tracker.data.models.Podcast
import com.f1tracker.data.models.PodcastEpisode

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
    val accentColor = Color(0xFFFF0080)
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Section Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "DAILY MIX",
                fontFamily = brigendsFont,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                letterSpacing = 2.sp
            )
        }

        // Horizontal Scroll Container
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // BLOCK 1: Hero Mix (News #0 + Hot Lap Game + Social #0)
            item {
                HeroMixBlock(
                    newsItem = newsArticles.getOrNull(0),
                    socialItem = socialPosts.getOrNull(0),
                    onNewsClick = onNewsClick,
                    onSocialClick = onSocialClick,
                    onGameClick = onGameClick,
                    michromaFont = michromaFont,
                    brigendsFont = brigendsFont,
                    accentColor = accentColor
                )
            }
            
            // BLOCK 2: Podcast Feature (Latest Episode)
            val latestPodcast = podcasts.firstOrNull { it.episodes.isNotEmpty() }
            val latestEpisode = latestPodcast?.episodes?.firstOrNull()
            if (latestEpisode != null) {
                item {
                    PodcastBlock(
                        episode = latestEpisode,
                        podcastName = latestPodcast.name,
                        onPodcastClick = onPodcastClick,
                        michromaFont = michromaFont,
                        brigendsFont = brigendsFont
                    )
                }
            }

            // BLOCK 3: Wide News Feature (News #1)
            if (newsArticles.size > 1) {
                item {
                    WideNewsBlock(
                        newsItem = newsArticles[1],
                        onNewsClick = onNewsClick,
                        michromaFont = michromaFont,
                        brigendsFont = brigendsFont,
                        accentColor = Color(0xFF00D2BE) // Teal accent
                    )
                }
            }
            
            // BLOCK 4: Social Stack (Social #1 + Social #2)
            if (socialPosts.size > 2) {
                 item {
                     SocialStackBlock(
                         social1 = socialPosts[1],
                         social2 = socialPosts[2],
                         onSocialClick = onSocialClick,
                         michromaFont = michromaFont
                     )
                 }
            }

            // BLOCK 5: Video Stack (Video #0 + Video #1)
            if (videos.size > 1) {
                item {
                    VideoStackBlock(
                        video1 = videos[0],
                        video2 = videos[1],
                        onVideoClick = onVideoClick,
                        michromaFont = michromaFont,
                        brigendsFont = brigendsFont
                    )
                }
            }
        }
    }
}

@Composable
fun HeroMixBlock(
    newsItem: NewsArticle?,
    socialItem: InstagramPost?,
    onNewsClick: (String?) -> Unit,
    onSocialClick: (String) -> Unit,
    onGameClick: () -> Unit,
    michromaFont: FontFamily,
    brigendsFont: FontFamily,
    accentColor: Color
) {
    val height = 280.dp
    val width = 300.dp // Reduced width to allow peeking of next item

    Row(
        modifier = Modifier
            .height(height)
            .width(width),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Large News (60%)
        BentoCard(
            modifier = Modifier
                .weight(1.6f)
                .fillMaxHeight(),
            onClick = { newsItem?.links?.web?.href?.let { onNewsClick(it) } }
        ) {
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
                GradientOverlay()
                Column(
                     modifier = Modifier.fillMaxSize().padding(16.dp),
                     verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = "HEADLINE",
                        fontFamily = brigendsFont,
                        fontSize = 10.sp,
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = newsItem.headline,
                        fontFamily = michromaFont,
                        fontSize = 14.sp,
                        color = Color.White,
                        lineHeight = 18.sp,
                        maxLines = 4,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "MOTORSPORT.COM",
                        fontFamily = michromaFont,
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // Helper Stack (40%) - GAME + SOCIAL
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // HOT LAP GAME CARD
            BentoCard(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                onClick = { onGameClick() }
            ) {
                // Background
                Image(
                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.game_hotlap),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.4f))) // Darken
                
                // Game Indicator
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFF0000), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "GAME",
                            fontFamily = michromaFont,
                            fontSize = 8.sp,
                            color = Color.White
                        )
                    }
                }
                
                Column(
                    modifier = Modifier.fillMaxSize().padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = Color(0xFFFF0000), // Red
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "HOT LAP",
                        fontFamily = brigendsFont,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                    Text(
                        text = "PLAY NOW",
                        fontFamily = michromaFont,
                        fontSize = 8.sp,
                        color = Color.White.copy(0.8f)
                    )
                }
            }
            
            // Social Card
            BentoCard(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                 onClick = { socialItem?.let { onSocialClick(it.permalink) } }
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
                      GradientOverlay()
                      
                      // Instagram Icon Top Right
                      Box(modifier = Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.TopEnd) {
                          Icon(
                              imageVector = Icons.Default.PhotoCamera,
                              contentDescription = null,
                              tint = Color.White,
                              modifier = Modifier.size(16.dp)
                          )
                      }
                      
                      Box(modifier = Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.BottomStart) {
                           Text(
                               text = socialItem.caption ?: "",
                               fontFamily = michromaFont,
                               fontSize = 9.sp,
                               color = Color.White,
                               maxLines = 2,
                               lineHeight = 11.sp
                           )
                      }
                 }
            }
        }
    }
}

@Composable
fun PodcastBlock(
    episode: PodcastEpisode,
    podcastName: String,
    onPodcastClick: (PodcastEpisode) -> Unit,
    michromaFont: FontFamily,
    brigendsFont: FontFamily
) {
    BentoCard(
        modifier = Modifier
            .height(280.dp)
            .width(180.dp),
        onClick = { onPodcastClick(episode) }
    ) {
        AsyncImage(
            model = episode.imageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colors = listOf(Color.Black.copy(0.3f), Color.Black.copy(0.9f))
                )
            )
        )
        
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                tint = Color(0xFFFF0080),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "PODCAST",
                fontFamily = brigendsFont,
                fontSize = 10.sp,
                color = Color.White.copy(0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = episode.title,
                fontFamily = michromaFont,
                fontSize = 11.sp,
                color = Color.White,
                maxLines = 3,
                lineHeight = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = podcastName,
                fontFamily = michromaFont,
                fontSize = 9.sp,
                color = Color.White.copy(0.5f)
            )
        }
    }
}

@Composable
fun SocialStackBlock(
    social1: InstagramPost,
    social2: InstagramPost,
    onSocialClick: (String) -> Unit,
    michromaFont: FontFamily
) {
    Column(
        modifier = Modifier
            .height(280.dp)
            .width(160.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        listOf(social1, social2).forEach { post ->
            BentoCard(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                onClick = { onSocialClick(post.permalink) }
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
                GradientOverlay()
                Box(modifier = Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.BottomStart) {
                     Text(
                        text = post.caption ?: "",
                        fontFamily = michromaFont,
                        fontSize = 9.sp,
                        color = Color.White,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}


@Composable
fun WideNewsBlock(
    newsItem: NewsArticle,
    onNewsClick: (String?) -> Unit,
    michromaFont: FontFamily,
    brigendsFont: FontFamily,
    accentColor: Color
) {
    // A single wide card, 280dp height, 300dp width
    BentoCard(
        modifier = Modifier
            .height(280.dp)
            .width(260.dp),
         onClick = { newsItem.links?.web?.href?.let { onNewsClick(it) } }
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
        GradientOverlay()
        Column(
             modifier = Modifier.fillMaxSize().padding(16.dp),
             verticalArrangement = Arrangement.Bottom
        ) {
            Box(
                modifier = Modifier
                    .background(accentColor, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(text = "NEWS", fontFamily = brigendsFont, fontSize = 8.sp, color = Color.White)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = newsItem.headline,
                fontFamily = michromaFont,
                fontSize = 16.sp,
                color = Color.White,
                lineHeight = 20.sp,
                maxLines = 4,
                fontWeight = FontWeight.Bold
            )
             Spacer(modifier = Modifier.height(8.dp))
             Text(
                text = "MOTORSPORT.COM",
                fontFamily = michromaFont,
                fontSize = 9.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun VideoStackBlock(
    video1: F1Video,
    video2: F1Video,
    onVideoClick: (String) -> Unit,
    michromaFont: FontFamily,
    brigendsFont: FontFamily
) {
    Column(
        modifier = Modifier
            .height(280.dp)
            .width(180.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        listOf(video1, video2).forEach { video ->
            BentoCard(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                onClick = { onVideoClick(video.videoId) }
            ) {
                 AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                GradientOverlay()
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    PlayButton()
                }
                Box(modifier = Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.BottomStart) {
                     Text(
                        text = video.title,
                        fontFamily = michromaFont,
                        fontSize = 10.sp,
                        color = Color.White,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
fun SocialFeatureBlock(
    socialItem: InstagramPost,
    onSocialClick: (String) -> Unit,
    michromaFont: FontFamily,
    brigendsFont: FontFamily
) {
    BentoCard(
        modifier = Modifier
            .height(280.dp)
            .width(220.dp),
         onClick = { onSocialClick(socialItem.permalink) }
    ) {
          val img = socialItem.thumbnail_url ?: socialItem.media_url
          if (img != null) {
              AsyncImage(
                model = img,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
          }
          GradientOverlay()
          
          Column(
              modifier = Modifier.fillMaxSize().padding(16.dp),
              verticalArrangement = Arrangement.SpaceBetween
          ) {
              // Top Quote Icon
              Icon(
                  imageVector = Icons.Default.FormatQuote,
                  contentDescription = null,
                  tint = Color.White.copy(0.8f),
                  modifier = Modifier.size(24.dp).rotate(180f)
              )
              
              Column {
                   Text(
                        text = socialItem.caption ?: "",
                        fontFamily = michromaFont,
                        fontSize = 11.sp,
                        color = Color.White,
                        maxLines = 6,
                        lineHeight = 16.sp,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                   )
                   Spacer(modifier = Modifier.height(8.dp))
                   Row(verticalAlignment = Alignment.CenterVertically) {
                       Text(text = "@f1 • Instagram", fontFamily = michromaFont, fontSize = 9.sp, color = Color(0xFFE1306C))
                   }
              }
          }
    }
}

// Helper Composable for consistent card styling
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
            .background(Color.White.copy(alpha = 0.2f), androidx.compose.foundation.shape.CircleShape)
            .border(1.dp, Color.White, androidx.compose.foundation.shape.CircleShape),
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


