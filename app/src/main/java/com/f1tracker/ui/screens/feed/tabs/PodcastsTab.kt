package com.f1tracker.ui.screens.feed.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.f1tracker.data.models.Podcast
import com.f1tracker.data.models.PodcastEpisode

@Composable
fun PodcastsTab(
    podcasts: List<Podcast>,
    currentlyPlayingEpisode: PodcastEpisode?,
    isPlaying: Boolean,
    onEpisodeClick: (PodcastEpisode) -> Unit,
    onPlayPause: () -> Unit,
    onPodcastClick: (Podcast) -> Unit,
    selectedPodcast: Podcast?
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            com.f1tracker.ui.components.PodcastsSection(
                podcasts = podcasts,
                currentlyPlayingEpisode = currentlyPlayingEpisode,
                isPlaying = isPlaying,
                onEpisodeClick = onEpisodeClick,
                onPlayPause = onPlayPause,
                onViewMoreClick = { /* TODO: Handle view more */ },
                onPodcastClick = onPodcastClick,
                selectedPodcast = selectedPodcast
            )
        }
        
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
