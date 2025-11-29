package com.f1tracker.ui.models

import com.f1tracker.data.models.NewsArticle
import com.f1tracker.data.models.PodcastEpisode
import com.f1tracker.data.models.F1Video

sealed interface SocialFeedItem {
    val publishedDate: String
    
    data class NewsItem(val article: NewsArticle) : SocialFeedItem {
        override val publishedDate: String = article.published
    }
    
    data class VideoItem(val video: F1Video) : SocialFeedItem {
        override val publishedDate: String = video.publishedDate
    }
    
    data class PodcastItem(val episode: PodcastEpisode) : SocialFeedItem {
        override val publishedDate: String = episode.publishedDate
    }
}
