package com.f1tracker.ui.models

import com.f1tracker.data.models.NewsArticle
import com.f1tracker.data.models.PodcastEpisode
import com.f1tracker.data.models.F1Video

sealed interface FeedItem {
    val publishedDate: String
    
    data class NewsItem(val article: NewsArticle) : FeedItem {
        override val publishedDate: String = article.published
    }
    
    data class VideoItem(val video: F1Video) : FeedItem {
        override val publishedDate: String = video.publishedDate
    }
    
    data class PodcastItem(val episode: PodcastEpisode) : FeedItem {
        override val publishedDate: String = episode.publishedDate
    }

    data class InstagramItem(val post: com.f1tracker.data.models.InstagramPost) : FeedItem {
        override val publishedDate: String = post.timestamp
    }
}
