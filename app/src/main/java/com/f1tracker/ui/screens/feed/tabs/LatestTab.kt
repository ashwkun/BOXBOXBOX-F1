package com.f1tracker.ui.screens.feed.tabs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.f1tracker.R
import com.f1tracker.data.models.F1Video
import com.f1tracker.data.models.InstagramPost
import com.f1tracker.data.models.NewsArticle
import com.f1tracker.data.models.Podcast
import com.f1tracker.data.models.PodcastEpisode
import com.f1tracker.ui.screens.feed.components.bento.*
import com.f1tracker.ui.screens.feed.util.parseDate

/**
 * Sealed class representing different types of feed items for the Latest tab.
 */
sealed class FeedItem {
    data class NewsItem(val article: NewsArticle) : FeedItem()
    data class VideoItem(val video: F1Video) : FeedItem()
    data class PodcastItem(val episode: PodcastEpisode) : FeedItem()
    data class InstagramItem(val post: InstagramPost) : FeedItem()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LatestTab(
    newsArticles: List<NewsArticle>,
    videos: List<F1Video>,
    podcasts: List<Podcast>,
    michromaFont: FontFamily,
    onNewsClick: (String?) -> Unit,
    onVideoClick: (String) -> Unit,
    onEpisodeClick: (PodcastEpisode) -> Unit,
    onPlayPause: () -> Unit,
    currentlyPlayingEpisode: PodcastEpisode?,
    isPlaying: Boolean,
    onExploreClick: (Int) -> Unit,
    instagramPosts: List<InstagramPost>,
    onNavigateToReels: (String) -> Unit,
    onSocialPostClick: (String) -> Unit
) {
    val brigendsFont = FontFamily(Font(R.font.brigends_expanded, FontWeight.Normal))
    
    val combinedItems = remember(newsArticles, videos, podcasts, instagramPosts) {
        val news = newsArticles
            .sortedByDescending { parseDate(it.published) }
            .take(10)
            .map { FeedItem.NewsItem(it) }
        
        val videoItems = videos
            .sortedByDescending { parseDate(it.publishedDate) }
            .take(8)
            .map { FeedItem.VideoItem(it) }
        
        val now = java.time.Instant.now()
        val insta = instagramPosts
            .map { post ->
                val likes = post.like_count.toDouble()
                val comments = post.comments_count.toDouble()
                val engagement = likes + (comments * 3)
                
                val hoursAgo = try {
                    val postTime = java.time.Instant.parse(post.timestamp)
                    java.time.Duration.between(postTime, now).toHours().toDouble()
                } catch (e: Exception) { 100.0 }
                
                val timeDecay = Math.pow(hoursAgo + 2.0, 1.5)
                var score = engagement / timeDecay
                
                if (post.media_type == "VIDEO") {
                    score *= 1.3
                }
                
                if (post.author == "f1") {
                    score *= 0.4
                }
                
                val randomFactor = 0.70 + (Math.random() * 0.60)
                post to (score * randomFactor)
            }
            .sortedByDescending { it.second }
            .take(8)
            .map { FeedItem.InstagramItem(it.first) }
        
        val podcastItems = podcasts.take(3).flatMap { podcast ->
            podcast.episodes
                .sortedByDescending { parseDate(it.publishedDate) }
                .take(1)
                .map { FeedItem.PodcastItem(it) }
        }
        
        val result = mutableListOf<FeedItem>()
        val queues = listOf(
            news.toMutableList(),
            videoItems.toMutableList(),
            insta.toMutableList(),
            podcastItems.toMutableList()
        )
        
        var typeIndex = 0
        while (result.size < 30 && queues.any { it.isNotEmpty() }) {
            val queue = queues[typeIndex % queues.size]
            if (queue.isNotEmpty()) {
                result.add(queue.removeAt(0))
            }
            typeIndex++
        }
        
        result
    }

    fun getCardSize(index: Int): Int {
        val pattern = listOf(1, 2, 3, 2, 1, 2, 3, 1, 2, 1)
        return pattern[index % pattern.size]
    }
            
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalItemSpacing = 10.dp
    ) {
        item(span = StaggeredGridItemSpan.FullLine) {
            BentoHotlapCard(
                michromaFont = michromaFont,
                brigendsFont = brigendsFont,
                onPlayClick = { onExploreClick(4) }
            )
        }
        
        items(combinedItems.size) { index ->
            val item = combinedItems[index]
            val cardSize = getCardSize(index)
            
            when (item) {
                is FeedItem.NewsItem -> BentoNewsCard(
                    article = item.article,
                    michromaFont = michromaFont,
                    onNewsClick = onNewsClick,
                    cardSize = cardSize
                )
                is FeedItem.VideoItem -> BentoVideoCard(
                    video = item.video,
                    michromaFont = michromaFont,
                    onVideoClick = onVideoClick,
                    cardSize = cardSize
                )
                is FeedItem.PodcastItem -> BentoPodcastCard(
                    episode = item.episode,
                    michromaFont = michromaFont,
                    onEpisodeClick = onEpisodeClick,
                    cardSize = cardSize
                )
                is FeedItem.InstagramItem -> BentoInstagramCard(
                    post = item.post,
                    michromaFont = michromaFont,
                    onClick = {
                        if (item.post.media_type == "VIDEO") {
                            onNavigateToReels(item.post.permalink)
                        } else {
                            onSocialPostClick(item.post.permalink)
                        }
                    },
                    cardSize = cardSize
                )
            }
        }
    }
}
