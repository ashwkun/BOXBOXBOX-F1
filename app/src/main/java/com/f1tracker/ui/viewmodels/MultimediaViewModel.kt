package com.f1tracker.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.f1tracker.data.models.F1Video
import com.f1tracker.data.models.Podcast
import com.f1tracker.data.repository.F1Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MultimediaViewModel @Inject constructor(
    private val repository: F1Repository
) : ViewModel() {

    private val _youtubeVideos = MutableStateFlow<List<F1Video>>(emptyList())
    val youtubeVideos: StateFlow<List<F1Video>> = _youtubeVideos.asStateFlow()

    private val _podcasts = MutableStateFlow<List<Podcast>>(emptyList())
    val podcasts: StateFlow<List<Podcast>> = _podcasts.asStateFlow()

    private val _selectedTabIndex = MutableStateFlow(0)
    val selectedTabIndex: StateFlow<Int> = _selectedTabIndex.asStateFlow()

    private val _instagramPosts = MutableStateFlow<List<com.f1tracker.data.models.InstagramPost>>(emptyList())
    val instagramPosts: StateFlow<List<com.f1tracker.data.models.InstagramPost>> = _instagramPosts.asStateFlow()

    fun setSelectedTab(index: Int) {
        _selectedTabIndex.value = index
    }

    private val _selectedVideoFilter = MutableStateFlow("Hot")
    val selectedVideoFilter: StateFlow<String> = _selectedVideoFilter.asStateFlow()

    fun setSelectedVideoFilter(filter: String) {
        _selectedVideoFilter.value = filter
    }

    init {
        loadYouTubeVideos()
        loadPodcasts()
        loadInstagramFeed()
    }

    private val _isInstagramRefreshing = MutableStateFlow(false)
    val isInstagramRefreshing: StateFlow<Boolean> = _isInstagramRefreshing.asStateFlow()

    fun loadInstagramFeed() {
        viewModelScope.launch {
            val result = repository.getInstagramFeed()
            result.onSuccess { posts ->
                _instagramPosts.value = sortByEngagement(posts)
            }.onFailure { e ->
                Log.e("MultimediaViewModel", "Failed to load Instagram feed", e)
            }
        }
    }

    fun refreshInstagramFeed() {
        viewModelScope.launch {
            _isInstagramRefreshing.value = true
            val result = repository.getInstagramFeed(forceRefresh = true)
            result.onSuccess { posts ->
                _instagramPosts.value = sortByEngagement(posts)
            }.onFailure { e ->
                Log.e("MultimediaViewModel", "Failed to refresh Instagram feed", e)
            }
            _isInstagramRefreshing.value = false
        }
    }
    
    /**
     * Score posts by engagement (likes + comments*3) with time decay.
     * Formula: (likes + comments*3) / (hours_ago + 2)^1.5
     * Videos get a 1.2x boost for better visibility.
     * Also applies author diversity (max 3 consecutive from same author).
     */
    private fun sortByEngagement(posts: List<com.f1tracker.data.models.InstagramPost>): List<com.f1tracker.data.models.InstagramPost> {
        val now = java.time.Instant.now()
        
        fun getScore(post: com.f1tracker.data.models.InstagramPost): Double {
            val likes = post.like_count.toDouble()
            val comments = post.comments_count.toDouble()
            val engagement = likes + (comments * 3) // Comments worth 3x likes
            
            val hoursAgo = try {
                val postTime = java.time.Instant.parse(post.timestamp)
                java.time.Duration.between(postTime, now).toHours().toDouble()
            } catch (e: Exception) { 100.0 }
            
            // Time decay: older posts score lower
            val timeDecay = Math.pow(hoursAgo + 2.0, 1.5)
            var score = engagement / timeDecay
            
            // Boost videos slightly for better visibility
            if (post.media_type == "VIDEO") {
                score *= 1.2
            }
            
            // Debuff F1 official account - they have massive follower advantage
            // This balances their likes to give smaller accounts a fair chance
            if (post.author == "f1") {
                score *= 0.4
            }
            
            // Add slight randomization (±15%) so refresh feels fresh
            val randomFactor = 0.85 + (Math.random() * 0.30)
            return score * randomFactor
        }
        
        // Sort by score
        val scoredPosts = posts.sortedByDescending { getScore(it) }
        
        // Apply author diversity: no more than 3 consecutive from same author
        val diversified = mutableListOf<com.f1tracker.data.models.InstagramPost>()
        val remaining = scoredPosts.toMutableList()
        
        while (remaining.isNotEmpty()) {
            // Count consecutive from last author
            val lastAuthor = diversified.lastOrNull()?.author
            val consecutiveCount = if (lastAuthor != null) {
                diversified.takeLastWhile { it.author == lastAuthor }.size
            } else 0
            
            // Find next post that doesn't exceed 3 consecutive from same author
            val nextPost = if (consecutiveCount >= 3) {
                remaining.firstOrNull { it.author != lastAuthor } ?: remaining.first()
            } else {
                remaining.first()
            }
            
            diversified.add(nextPost)
            remaining.remove(nextPost)
        }
        
        return diversified
    }

    /**
     * Load videos from multiple sources:
     * 1. YouTube RSS feed (real-time, 15 videos, official F1 only)
     * 2. f1_youtube.json (merged feed with tags and channel scores)
     * 3. f1_highlights.json (official F1 session highlights)
     * 
     * Deduplicates by videoId, applies smart scoring, and sorts.
     */
    fun loadYouTubeVideos() {
        viewModelScope.launch {
            val allVideos = mutableListOf<F1Video>()
            val seenIds = mutableSetOf<String>()
            
            // Source 1: RSS Feed (fastest, real-time official F1 content)
            val rssResult = repository.getYouTubeVideos("UULFB_qr75-ydFVKSF9Dmo6izg")
            rssResult.onSuccess { videos ->
                videos.forEach { video ->
                    if (seenIds.add(video.videoId)) {
                        allVideos.add(video.copy(
                            views = formatViews(video.views),
                            duration = formatDuration(video.duration.toIntOrNull() ?: 0),
                            viewCount = video.views.toLongOrNull() ?: 0L,
                            channelTitle = "FORMULA 1",
                            channelScore = 1.0,
                            tags = classifyVideoContent(video.title)
                        ))
                    }
                }
            }
            
            // Source 2: f1_youtube.json (merged multi-feed with tags and scores)
            val jsonResult = repository.getYouTubeVideosFromJson()
            jsonResult.onSuccess { videos ->
                videos.forEach { video ->
                    if (seenIds.add(video.id)) {
                        allVideos.add(F1Video(
                            videoId = video.id,
                            title = video.title,
                            thumbnailUrl = video.thumbnail,
                            views = formatViews(video.viewCount),
                            publishedDate = video.publishedAt,
                            duration = formatDuration(video.durationSec),
                            viewCount = video.viewCount.toLongOrNull() ?: 0L,
                            channelTitle = video.channelTitle,
                            channelScore = video.channelScore,
                            tags = (video.tags ?: emptyList()).ifEmpty { classifyVideoContent(video.title) }
                        ))
                    }
                }
            }
            
            // Source 3: f1_highlights.json (official session highlights)
            val highlightsResult = repository.getHighlights()
            highlightsResult.onSuccess { highlights ->
                highlights.forEach { highlight ->
                    if (seenIds.add(highlight.id)) {
                        allVideos.add(F1Video(
                            videoId = highlight.id,
                            title = highlight.title,
                            thumbnailUrl = highlight.thumbnail ?: "https://i.ytimg.com/vi/${highlight.id}/maxresdefault.jpg",
                            views = "",
                            publishedDate = highlight.publishedAt,
                            duration = "",
                            viewCount = 0L,
                            channelTitle = "FORMULA 1",
                            channelScore = 1.0,
                            tags = listOf(highlight.sessionType.uppercase().replace(" ", "_"))
                        ))
                    }
                }
            }
            
            // Apply smart scoring and sort
            val scoredVideos = allVideos.map { video ->
                video to calculateSmartScore(video)
            }.sortedByDescending { it.second }
             .map { it.first }
            
            Log.d("MultimediaViewModel", "Loaded ${scoredVideos.size} videos from 3 sources (deduplicated, scored)")
            _youtubeVideos.value = scoredVideos
        }
    }
    
    /**
     * Calculate smart score for video ranking.
     * Score = (freshness * 0.35) + (engagement * 0.25) + (channelQuality * 0.25) + (contentType * 0.15)
     */
    private fun calculateSmartScore(video: F1Video): Double {
        val now = java.time.Instant.now()
        
        // Freshness score (exponential decay, half-life 48 hours)
        val hoursAge = try {
            val publishTime = java.time.Instant.parse(video.publishedDate)
            java.time.Duration.between(publishTime, now).toHours().toDouble()
        } catch (e: Exception) { 720.0 } // 30 days fallback
        
        val freshnessScore = Math.exp(-hoursAge / 48.0)
        
        // Engagement score (normalized)
        val maxViews = 10_000_000.0 // Normalize against 10M views
        val engagementScore = Math.min(video.viewCount / maxViews, 1.0)
        
        // Channel quality score (from JSON or default)
        val channelScore = video.channelScore
        
        // Content type boost (session content preferred)
        val contentBoost = when {
            video.tags.any { it in listOf("RACE", "QUALI", "SPRINT") } -> 1.0
            video.tags.any { it in listOf("FP", "FP1", "FP2", "FP3") } -> 0.8
            video.tags.any { it in listOf("ANALYSIS", "TECH") } -> 0.7
            video.tags.any { it in listOf("REACTION", "NEWS") } -> 0.6
            else -> 0.5
        }
        
        return (freshnessScore * 0.35) + 
               (engagementScore * 0.25) + 
               (channelScore * 0.25) + 
               (contentBoost * 0.15)
    }
    
    /**
     * Classify video content based on title patterns.
     */
    private fun classifyVideoContent(title: String): List<String> {
        val tags = mutableListOf<String>()
        
        if (title.contains(Regex("Race.*Highlight|Grand Prix.*Highlight", RegexOption.IGNORE_CASE))) tags.add("RACE")
        if (title.contains(Regex("Qualifying|Pole Lap|Q[123]", RegexOption.IGNORE_CASE))) tags.add("QUALI")
        if (title.contains(Regex("FP[123]|Free Practice", RegexOption.IGNORE_CASE))) tags.add("FP")
        if (title.contains(Regex("Sprint.*Highlight|Sprint Quali", RegexOption.IGNORE_CASE))) tags.add("SPRINT")
        if (title.contains(Regex("React|Interview|Debrief|Press Conference", RegexOption.IGNORE_CASE))) tags.add("REACTION")
        if (title.contains(Regex("Explain|Analysis|Breakdown|Deep Dive", RegexOption.IGNORE_CASE))) tags.add("ANALYSIS")
        if (title.contains(Regex("Onboard|Hot Lap", RegexOption.IGNORE_CASE))) tags.add("ONBOARD")
        if (title.contains(Regex("Tech Talk|Upgrade|Aero|Wing", RegexOption.IGNORE_CASE))) tags.add("TECH")
        
        return tags.ifEmpty { listOf("GENERAL") }
    }


    fun loadPodcasts() {
        viewModelScope.launch {
            val feeds = listOf(
                "https://audioboom.com/channels/4964339.rss",
                "https://feeds.megaphone.fm/NYOOM4196406795",
                "https://feeds.acast.com/public/shows/67a4d8a83ef0b176ea9b64e1"
            )
            val result = repository.getPodcasts(feeds)
            result.onSuccess { podcasts ->
                // Apply formatting (strip HTML from description)
                val formattedPodcasts = podcasts.map { podcast ->
                    podcast.copy(
                        episodes = podcast.episodes.map { episode ->
                            episode.copy(
                                description = stripHtml(episode.description),
                                duration = formatPodcastDuration(episode.duration)
                            )
                        }.sortedByDescending { parseDate(it.publishedDate) }
                    )
                }
                _podcasts.value = formattedPodcasts
            }.onFailure { e ->
                Log.e("MultimediaViewModel", "Failed to load podcasts", e)
            }
        }
    }

    private fun formatViews(views: String): String {
        return try {
            val viewCount = views.toLongOrNull() ?: 0L
            when {
                viewCount >= 1_000_000 -> "${viewCount / 1_000_000}M views"
                viewCount >= 1_000 -> "${viewCount / 1_000}K views"
                else -> "$viewCount views"
            }
        } catch (e: Exception) {
            views
        }
    }

    private fun formatDuration(seconds: Int): String {
        if (seconds == 0) return ""
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, secs)
            minutes > 0 -> String.format("%d:%02d", minutes, secs)
            else -> String.format("0:%02d", secs)
        }
    }

    private fun formatPodcastDuration(duration: String): String {
        return try {
            val seconds = duration.toLongOrNull()
            if (seconds != null) {
                val hours = seconds / 3600
                val minutes = (seconds % 3600) / 60
                val secs = seconds % 60
                when {
                    hours > 0 -> String.format("%dh %dm", hours, minutes)
                    minutes > 0 -> String.format("%dm", minutes)
                    else -> String.format("%ds", secs)
                }
            } else {
                duration
            }
        } catch (e: Exception) {
            duration
        }
    }

    private fun stripHtml(html: String): String {
        return html
            .replace(Regex("<[^>]*>"), "")
            .replace(Regex("\\[CDATA\\[|\\]\\]"), "")
            .replace(Regex("&nbsp;"), " ")
            .replace(Regex("&amp;"), "&")
            .replace(Regex("&lt;"), "<")
            .replace(Regex("&gt;"), ">")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun parseDate(dateString: String): java.time.ZonedDateTime {
        return try {
            java.time.ZonedDateTime.parse(dateString, java.time.format.DateTimeFormatter.ISO_DATE_TIME)
        } catch (e: Exception) {
            try {
                // Try RFC 1123 (common for RSS/Podcasts)
                java.time.ZonedDateTime.parse(dateString, java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME)
            } catch (e2: Exception) {
                java.time.ZonedDateTime.now().minusYears(1) // Fallback
            }
        }
    }

    // Scroll Persistence for Latest Tab
    var latestScrollIndex = 0
    var latestScrollOffset = 0

    fun updateLatestScrollPosition(index: Int, offset: Int) {
        latestScrollIndex = index
        latestScrollOffset = offset
    }

    fun resetLatestScrollPosition() {
        latestScrollIndex = 0
        latestScrollOffset = 0
    }

    // Scroll Persistence for Videos Tab
    var videosScrollIndex = 0
    var videosScrollOffset = 0

    fun updateVideosScrollPosition(index: Int, offset: Int) {
        videosScrollIndex = index
        videosScrollOffset = offset
    }

    fun resetVideosScrollPosition() {
        videosScrollIndex = 0
        videosScrollOffset = 0
    }

    // Scroll Persistence for Instagram Feed
    var instagramScrollIndex = 0

    fun updateInstagramScrollPosition(index: Int) {
        instagramScrollIndex = index
    }

    fun resetInstagramScrollPosition() {
        instagramScrollIndex = 0
    }
}
