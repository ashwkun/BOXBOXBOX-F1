package com.f1tracker.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import com.f1tracker.R
import com.f1tracker.data.models.NewsArticle
import com.f1tracker.data.models.Podcast
import com.f1tracker.data.models.PodcastEpisode
import com.f1tracker.data.models.F1Video
import com.f1tracker.ui.viewmodels.NewsViewModel
import com.f1tracker.ui.viewmodels.MultimediaViewModel
import com.f1tracker.ui.components.TabSelector
import com.f1tracker.ui.models.SocialFeedItem
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import com.f1tracker.util.NewsCategorizer
import com.f1tracker.util.NewsCategory

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SocialScreen(
    newsViewModel: NewsViewModel = hiltViewModel(),
    multimediaViewModel: MultimediaViewModel = hiltViewModel(),
    onNewsClick: (String?) -> Unit = {},
    onVideoClick: (String) -> Unit = {},
    onEpisodeClick: (PodcastEpisode) -> Unit = {},
    onPlayPause: () -> Unit = {},
    currentlyPlayingEpisode: PodcastEpisode? = null,
    isPlaying: Boolean = false,
    initialTab: Int = 0
) {
    val michromaFont = FontFamily(Font(R.font.michroma, FontWeight.Normal))

    
    val newsArticles by newsViewModel.newsArticles.collectAsState()
    val isRefreshing by newsViewModel.isRefreshing.collectAsState()
    val youtubeVideos by multimediaViewModel.youtubeVideos.collectAsState()
    val podcasts by multimediaViewModel.podcasts.collectAsState()
    val selectedTabIndex by multimediaViewModel.selectedTabIndex.collectAsState()
    
    // Hoisted scroll state for NewsList
    val newsListState = rememberLazyListState(
        initialFirstVisibleItemIndex = newsViewModel.newsScrollIndex,
        initialFirstVisibleItemScrollOffset = newsViewModel.newsScrollOffset
    )

    // Hoisted scroll state for LatestFeed
    val latestListState = rememberLazyListState(
        initialFirstVisibleItemIndex = multimediaViewModel.latestScrollIndex,
        initialFirstVisibleItemScrollOffset = multimediaViewModel.latestScrollOffset
    )

    // Hoisted scroll state for VideosList
    val videosListState = rememberLazyListState(
        initialFirstVisibleItemIndex = multimediaViewModel.videosScrollIndex,
        initialFirstVisibleItemScrollOffset = multimediaViewModel.videosScrollOffset
    )

    // Sync scroll state to ViewModel
    LaunchedEffect(newsListState.firstVisibleItemIndex, newsListState.firstVisibleItemScrollOffset) {
        newsViewModel.updateScrollPosition(
            newsListState.firstVisibleItemIndex,
            newsListState.firstVisibleItemScrollOffset
        )
    }

    LaunchedEffect(latestListState.firstVisibleItemIndex, latestListState.firstVisibleItemScrollOffset) {
        multimediaViewModel.updateLatestScrollPosition(
            latestListState.firstVisibleItemIndex,
            latestListState.firstVisibleItemScrollOffset
        )
    }

    LaunchedEffect(videosListState.firstVisibleItemIndex, videosListState.firstVisibleItemScrollOffset) {
        multimediaViewModel.updateVideosScrollPosition(
            videosListState.firstVisibleItemIndex,
            videosListState.firstVisibleItemScrollOffset
        )
    }
    
    val tabs = listOf(
        TabItem("LATEST", Icons.Default.FlashOn),
        TabItem("SOCIAL", Icons.Default.Public),
        TabItem("NEWS", Icons.Default.Article),
        TabItem("VIDEOS", Icons.Default.PlayCircle),
        TabItem("AUDIO", Icons.Default.Headphones)
    )
    // Initialize pager with the persisted state or initialTab if provided (though initialTab is mostly 0)
    // We prioritize the ViewModel state if it's not 0, otherwise we use initialTab
    val startPage = if (selectedTabIndex != 0) selectedTabIndex else initialTab
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(initialPage = startPage) { tabs.size }
    val coroutineScope = rememberCoroutineScope()
    
    // State for selected podcast detail view
    var selectedPodcast by remember { mutableStateOf<Podcast?>(null) }
    
    // Sync pager state with ViewModel
    LaunchedEffect(pagerState.currentPage) {
        multimediaViewModel.setSelectedTab(pagerState.currentPage)
        
        // Reset scroll positions if switching away from tabs
        if (pagerState.currentPage != 0) { // Not Latest
            multimediaViewModel.resetLatestScrollPosition()
            latestListState.scrollToItem(0)
        }
        
        if (pagerState.currentPage != 2) { // Not News (Index 2)
            newsViewModel.resetScrollPosition()
            newsListState.scrollToItem(0)
        }
        
        if (pagerState.currentPage != 3) { // Not Videos (Index 3)
            multimediaViewModel.resetVideosScrollPosition()
            videosListState.scrollToItem(0)
        }
    }
    
    // Scroll to initial tab only if it's explicitly requested and different from current
    LaunchedEffect(initialTab) {
        if (initialTab != 0 && initialTab != pagerState.currentPage) {
            pagerState.scrollToPage(initialTab)
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Header

            
            // Custom Tab Selector
            IconTabSelector(
                tabs = tabs,
                selectedTab = pagerState.currentPage,
                onTabSelected = { index ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                },
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Content with Swipe Navigation
            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> LatestFeed(
                        newsArticles = newsArticles,
                        videos = youtubeVideos,
                        podcasts = podcasts,
                        michromaFont = michromaFont,
                        onNewsClick = onNewsClick,
                        onVideoClick = onVideoClick,
                        onEpisodeClick = onEpisodeClick,
                        onPlayPause = onPlayPause,
                        currentlyPlayingEpisode = currentlyPlayingEpisode,
                        isPlaying = isPlaying,
                        onExploreClick = { tabIndex ->
                            coroutineScope.launch { pagerState.animateScrollToPage(tabIndex) }
                        },
                        listState = latestListState
                    )
                    1 -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                tint = Color(0xFFFF0080),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "SOCIAL FEED COMING SOON",
                                fontFamily = michromaFont,
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                    2 -> NewsList(
                        articles = newsArticles, 
                        michromaFont = michromaFont, 
                        onNewsClick = onNewsClick, 
                        listState = newsListState,
                        isRefreshing = isRefreshing,
                        onRefresh = { newsViewModel.refreshNews() }
                    )
                    3 -> VideosList(
                        videos = youtubeVideos, 
                        michromaFont = michromaFont, 
                        onVideoClick = onVideoClick,
                        listState = videosListState
                    )
                    4 -> PodcastsList(
                        podcasts = podcasts, 
                        michromaFont = michromaFont, 
                        currentlyPlayingEpisode = currentlyPlayingEpisode, 
                        isPlaying = isPlaying, 
                        onEpisodeClick = onEpisodeClick, 
                        onPlayPause = onPlayPause,
                        onPodcastClick = { podcast -> 
                            selectedPodcast = if (selectedPodcast?.name == podcast.name) null else podcast
                        },
                        selectedPodcast = selectedPodcast
                    )
                }
            }
        }
    }
}

@Composable
private fun LatestFeed(
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
    listState: LazyListState
) {
    val combinedItems = remember(newsArticles, videos, podcasts) {
        val latestNews = newsArticles.take(2).map { SocialFeedItem.NewsItem(it) }
        val latestVideos = videos.take(2).map { SocialFeedItem.VideoItem(it) }
        // Take 1 latest episode from EACH podcast
        val latestPodcasts = podcasts.mapNotNull { podcast ->
            podcast.episodes.maxByOrNull { parseDate(it.publishedDate) }?.let { episode ->
                SocialFeedItem.PodcastItem(episode)
            }
        }
            
        (latestNews + latestVideos + latestPodcasts)
            .sortedByDescending { parseDate(it.publishedDate) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(combinedItems) { item ->
            when (item) {
                is SocialFeedItem.NewsItem -> FullNewsCard(item.article, michromaFont, onNewsClick, showTag = true)
                is SocialFeedItem.VideoItem -> VideoCard(item.video, michromaFont, onVideoClick, showTag = true)
                is SocialFeedItem.PodcastItem -> LargePodcastCard(
                    episode = item.episode,
                    isCurrentlyPlaying = currentlyPlayingEpisode?.audioUrl == item.episode.audioUrl,
                    isPlaying = isPlaying && currentlyPlayingEpisode?.audioUrl == item.episode.audioUrl,
                    michromaFont = michromaFont,
                    onEpisodeClick = onEpisodeClick,
                    onPlayPause = onPlayPause
                )
            }
        }
        
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Explore more in the dedicated feeds",
                    fontFamily = michromaFont,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

private fun parseDate(dateString: String): ZonedDateTime {
    return try {
        ZonedDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME)
    } catch (e: Exception) {
        try {
            // Try RFC 1123 (common for RSS/Podcasts)
            ZonedDateTime.parse(dateString, DateTimeFormatter.RFC_1123_DATE_TIME)
        } catch (e2: Exception) {
            ZonedDateTime.now().minusYears(1) // Fallback
        }
    }
}

@Composable
private fun LargePodcastCard(
    episode: PodcastEpisode,
    isCurrentlyPlaying: Boolean,
    isPlaying: Boolean,
    michromaFont: FontFamily,
    onEpisodeClick: (PodcastEpisode) -> Unit,
    onPlayPause: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                if (isCurrentlyPlaying) onPlayPause() else onEpisodeClick(episode)
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        )
    ) {
        Column {
            // 200dp Height Image Area with Blurred Background + Square Art
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                // Blurred Background (Zoomed in)
                AsyncImage(
                    model = episode.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            drawContent()
                            drawRect(Color.Black.copy(alpha = 0.7f)) // Dark overlay
                        },
                    contentScale = ContentScale.Crop
                )
                
                // Content Row
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Square Artwork with Play Button
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .shadow(8.dp, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = episode.imageUrl,
                            contentDescription = episode.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        
                        // Play Overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        if (isCurrentlyPlaying) {
                                            Brush.linearGradient(
                                                colors = listOf(Color(0xFFFF0080), Color(0xFFE6007E))
                                            )
                                        } else {
                                            Brush.linearGradient(
                                                colors = listOf(Color(0xFF2C2C2E), Color(0xFF1C1C1E))
                                            )
                                        },
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isCurrentlyPlaying && isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (isCurrentlyPlaying && isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Right side info (optional, or just keep it clean)
                    // For now, we'll just keep the artwork prominent in this section
                }

                // Tag
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .background(Color(0xFFFF0080), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "PODCAST",
                        fontFamily = michromaFont,
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Duration badge
                if (episode.duration.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = episode.duration,
                            fontSize = 10.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = episode.title,
                    fontFamily = michromaFont,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrentlyPlaying) Color(0xFFFF0080) else Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatPublishedDate(episode.publishedDate),
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}




@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun NewsList(
    articles: List<NewsArticle>,
    michromaFont: FontFamily,
    onNewsClick: (String?) -> Unit,
    listState: LazyListState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = onRefresh
    )
    
    var selectedFilter by remember { mutableStateOf(NewsCategory.ALL) }
    
    val filteredArticles = remember(articles, selectedFilter) {
        if (selectedFilter == NewsCategory.ALL) {
            articles
        } else {
            articles.filter { article ->
                NewsCategorizer.categorize(article.headline) == selectedFilter
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Filter Chips Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    NewsCategory.values().forEach { category ->
                        val isSelected = selectedFilter == category
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (isSelected) Color(0xFFFF0080) else Color(0xFF1A1A1A)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color(0xFFFF0080) else Color.White.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(50)
                                )
                                .clickable { selectedFilter = category }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = category.label.uppercase(),
                                fontFamily = michromaFont,
                                fontSize = 9.sp,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            items(filteredArticles) { article ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    FullNewsCard(
                        article = article,
                        michromaFont = michromaFont,
                        onNewsClick = onNewsClick
                    )
                }
            }
        }
        
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = Color.Black,
            contentColor = Color(0xFFFF0080)
        )
    }
}

@Composable
private fun VideosList(
    videos: List<F1Video>,
    michromaFont: FontFamily,
    onVideoClick: (String) -> Unit,
    listState: LazyListState
) {
    var selectedFilter by remember { mutableStateOf("Top") }
    
    // Determine available filters based on content
    val availableFilters = remember(videos) {
        val filters = mutableListOf("All", "Top")
        
        if (videos.any { it.title.contains("highlight", ignoreCase = true) }) {
            filters.add("Highlights")
        }
        if (videos.any { it.title.contains("react", ignoreCase = true) || it.title.contains("reaction", ignoreCase = true) }) {
            filters.add("Reactions")
        }
        filters
    }
    
    // Filter and sort videos
    val filteredVideos = remember(videos, selectedFilter) {
        when (selectedFilter) {
            "All" -> videos // Default order (latest)
            "Top" -> videos.sortedByDescending { it.viewCount }
            "Highlights" -> videos.filter { it.title.contains("highlight", ignoreCase = true) }
            "Reactions" -> videos.filter { it.title.contains("react", ignoreCase = true) || it.title.contains("reaction", ignoreCase = true) }
            else -> videos
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(bottom = 16.dp), // Bottom padding for list
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Filter Chips Row (Fixed, no scrolling)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                availableFilters.forEach { filter ->
                    val isSelected = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isSelected) Color(0xFFFF0080) else Color(0xFF1A1A1A)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color(0xFFFF0080) else Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(50)
                            )
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 12.dp, vertical = 6.dp) // Smaller padding
                    ) {
                        Text(
                            text = filter.uppercase(),
                            fontFamily = michromaFont,
                            fontSize = 9.sp, // Smaller font
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        items(filteredVideos) { video ->
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                VideoCard(
                    video = video,
                    michromaFont = michromaFont,
                    onVideoClick = onVideoClick
                )
            }
        }
    }
}

@Composable
private fun PodcastsList(
    podcasts: List<Podcast>,
    michromaFont: FontFamily,
    currentlyPlayingEpisode: PodcastEpisode?,
    isPlaying: Boolean,
    onEpisodeClick: (PodcastEpisode) -> Unit,
    onPlayPause: () -> Unit,
    onPodcastClick: (Podcast) -> Unit,
    selectedPodcast: Podcast?
) {
    // Use the new PodcastsSection component which handles the layout internally
    // We wrap it in a LazyColumn to ensure it scrolls if content exceeds screen height
    // although PodcastsSection uses LazyRows internally, the main page structure expects a composable here.
    // Since PodcastsSection is a Column, we can put it inside a LazyColumn item or just use a Column with verticalScroll
    // But the parent `HorizontalPager` expects a page content.
    
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
        
        // Add some bottom padding
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun FullNewsCard(
    article: NewsArticle,
    michromaFont: FontFamily,
    onNewsClick: (String?) -> Unit,
    showTag: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNewsClick(article.links?.web?.href) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // News image
            val imageUrl = article.images?.firstOrNull { it.type == "header" }?.url 
                ?: article.images?.firstOrNull()?.url
            
            if (imageUrl != null) {
                Box {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = article.headline,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    
                    if (showTag) {
                        Box(
                            modifier = Modifier
                                .padding(12.dp)
                                .background(Color(0xFFFF0080), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .align(Alignment.TopStart)
                        ) {
                            Text(
                                text = "ARTICLE",
                                fontFamily = michromaFont,
                                fontSize = 10.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Source Chip (F1/ESPN)
                    val sourceName = when {
                        article.links?.web?.href?.contains("formula1.com") == true -> "F1"
                        article.links?.web?.href?.contains("motorsport.com") == true -> "MOTORSPORT.COM"
                        article.links?.web?.href?.contains("espn") == true -> "ESPN"
                        else -> "NEWS"
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = sourceName,
                            fontFamily = michromaFont,
                            fontSize = 10.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // Content section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Headline
                Text(
                    text = article.headline,
                    fontFamily = michromaFont,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
                
                // Description
                Text(
                    text = article.description,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
                
                // Published date
                Text(
                    text = formatPublishedDate(article.published),
                    fontFamily = michromaFont,
                    fontSize = 9.sp,
                    color = Color(0xFFFF0080),
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun VideoCard(
    video: F1Video,
    michromaFont: FontFamily,
    onVideoClick: (String) -> Unit,
    showTag: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onVideoClick(video.videoId) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        )
    ) {
        Column {
            Box(modifier = Modifier.height(200.dp)) {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = video.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Play button overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayCircle,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
                
                // Duration badge
                if (video.duration.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = video.duration,
                            fontSize = 10.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                if (showTag) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .background(Color(0xFFFF0080), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "VIDEO",
                            fontFamily = michromaFont,
                            fontSize = 10.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = video.title,
                    fontFamily = michromaFont,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = video.views,
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatPublishedDate(video.publishedDate),
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}



@Composable
private fun EpisodeCard(
    episode: PodcastEpisode,
    isCurrentlyPlaying: Boolean,
    isPlaying: Boolean,
    michromaFont: FontFamily,
    onEpisodeClick: (PodcastEpisode) -> Unit,
    onPlayPause: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(160.dp)
            .fillMaxHeight()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(12.dp),
                clip = false
            )
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isCurrentlyPlaying) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2C1C2E),
                            Color(0xFF1C1315)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1C1C1E),
                            Color(0xFF131315)
                        )
                    )
                }
            )
            .clickable {
                if (isCurrentlyPlaying) {
                    onPlayPause()
                } else {
                    onEpisodeClick(episode)
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Episode artwork
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = episode.imageUrl,
                    contentDescription = episode.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Play button overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (isCurrentlyPlaying) {
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFFFF0080),
                                            Color(0xFFE6007E)
                                        )
                                    )
                                } else {
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF2C2C2E),
                                            Color(0xFF1C1C1E)
                                        )
                                    )
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCurrentlyPlaying && isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isCurrentlyPlaying && isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Episode info
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = episode.title,
                    fontFamily = michromaFont,
                    fontSize = 10.sp,
                    color = if (isCurrentlyPlaying) Color(0xFFFF0080) else Color.White,
                    fontWeight = if (isCurrentlyPlaying) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 12.sp
                )
                
                if (episode.duration.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(12.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        Text(
                            text = episode.duration,
                            fontFamily = michromaFont,
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

private fun formatPublishedDate(publishedString: String): String {
    return try {
        val formatter = java.time.format.DateTimeFormatter.ISO_DATE_TIME
        val dateTime = java.time.ZonedDateTime.parse(publishedString, formatter)
        val now = java.time.ZonedDateTime.now()
        
        val hours = java.time.Duration.between(dateTime, now).toHours()
        when {
            hours < 1 -> "Just now"
            hours < 24 -> "${hours}h ago"
            hours < 48 -> "Yesterday"
            else -> {
                val days = hours / 24
                "${days}d ago"
            }
        }
    } catch (e: Exception) {
        "Recently"
    }
}

private data class TabItem(
    val label: String,
    val icon: ImageVector
)

@Composable
private fun IconTabSelector(
    tabs: List<TabItem>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color(0xFF0F0F0F), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = selectedTab == index
            
            Box(
                modifier = Modifier
                    .weight(if (isSelected) 3f else 1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) Color(0xFFFF0080).copy(alpha = 0.2f) else Color.Transparent)
                    .clickable { onTabSelected(index) }
                    .animateContentSize(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null,
                        tint = if (isSelected) Color(0xFFFF0080) else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                    
                    if (isSelected) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tab.label,
                            fontFamily = FontFamily(Font(R.font.michroma, FontWeight.Normal)),
                            fontSize = 10.sp,
                            color = Color.White,
                            maxLines = 1,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}


