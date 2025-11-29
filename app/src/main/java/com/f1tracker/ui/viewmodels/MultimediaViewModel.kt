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

    fun setSelectedTab(index: Int) {
        _selectedTabIndex.value = index
    }

    init {
        loadYouTubeVideos()
        loadPodcasts()
    }

    fun loadYouTubeVideos() {
        viewModelScope.launch {
            val result = repository.getYouTubeVideos("UULFB_qr75-ydFVKSF9Dmo6izg")
            result.onSuccess { videos ->
                // Apply formatting
                val formattedVideos = videos.map { video ->
                    video.copy(
                        views = formatViews(video.views),
                        duration = formatDuration(video.duration.toIntOrNull() ?: 0),
                        viewCount = video.views.toLongOrNull() ?: 0L
                    )
                }
                _youtubeVideos.value = formattedVideos
            }.onFailure { e ->
                Log.e("MultimediaViewModel", "Failed to load YouTube videos", e)
            }
        }
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
}
